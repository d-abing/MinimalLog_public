package com.aube.minimallog.data.repository

import android.content.Context
import com.aube.minimallog.data.database.MinimalLogDatabase
import com.aube.minimallog.presentation.util.DriveServiceFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class DriveBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MinimalLogDatabase,
) {
    private val sp by lazy { context.getSharedPreferences("drive", Context.MODE_PRIVATE) }

    private companion object {
        const val TAG = "DriveBackup"
        const val BACKUP_PREFIX = "minimalog_backup_"
        private val ZIP_MIME_Q = "(mimeType='application/zip' or mimeType='application/octet-stream')"
    }

    fun persistAccountName(name: String?) {
        sp.edit().putString("account", name).apply()
    }
    fun restoreAccountName(): String? = sp.getString("account", null)

    fun persistLastBackupTime(ts: Long) {
        sp.edit().putLong("last_backup", ts).apply()
    }
    fun restoreLastBackupTime(): Long? = sp.getLong("last_backup", 0L).takeIf { it > 0L }

    suspend fun signOut(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        GoogleSignIn.getClient(context, gso).signOut().await()
    }

    // ---- 백업 ----
    suspend fun backupNow(accountName: String): Long = withContext(Dispatchers.IO) {
        val service = DriveServiceFactory.create(context, accountName)

        database.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(TRUNCATE)")
            .close()

        // 1) ZIP 만들기
        val zip = makeBackupZip()
        validateBackupZip(zip)

        // 2) 업로드 (appDataFolder)
        val meta = com.google.api.services.drive.model.File().apply {
            name = "${BACKUP_PREFIX}${timeStamp()}.zip"
            parents = listOf("appDataFolder")
            mimeType = "application/zip"
        }
        val media = com.google.api.client.http.FileContent("application/zip", zip)

        val created = service.files().create(meta, media)
            .setFields("id,name,createdTime,modifiedTime,size,mimeType,parents")
            .execute()

        persistLastBackupTime(System.currentTimeMillis())
        zip.delete()
        System.currentTimeMillis()
    }

    // ---- 복원 ----
    suspend fun restoreLatest(accountName: String) = withContext(Dispatchers.IO) {
        val service = DriveServiceFactory.create(context, accountName)

        // 최신 백업 찾기 (이름 prefix + zip mime, modifiedTime desc)
        val list = service.files().list()
            .setSpaces("appDataFolder")
            .setQ(
                """
                name contains '$BACKUP_PREFIX' and 
                $ZIP_MIME_Q and 
                trashed=false
            """.trimIndent()
            )
            .setOrderBy("modifiedTime desc")
            .setPageSize(5)
            .setFields("files(id,name,modifiedTime,size,mimeType)")
            .execute()

        val file = list.files?.firstOrNull() ?: error("No backup found in appDataFolder")

        // 다운로드
        val dest = File(context.cacheDir, "restore_${System.currentTimeMillis()}.zip")
        BufferedOutputStream(FileOutputStream(dest)).use { out ->
            service.files().get(file.id).executeMediaAndDownloadTo(out)
        }
        if (!dest.exists() || dest.length() == 0L) {
            dest.delete()
            error("Downloaded backup is empty (id=${file.id}, name=${file.name})")
        }

        // ZIP 풀고 적용
        validateDownloadedZip(dest)
        database.close()
        restoreFromZip(dest)
        dest.delete()

        // DB 교체 직후 앱 재시작이 가장 안전.
        sp.edit().putBoolean("db_restored", true).apply()
    }

    private fun makeBackupZip(): File {
        val out = File(context.cacheDir, "backup_${System.currentTimeMillis()}.zip")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(out))).use { zos ->
            // 1) DB 디렉터리의 관련 파일 전부 백업 (*.db, -wal, -shm, -journal)
            putAllDatabases(zos)

            // 2) files/images
            val imgDir = File(context.filesDir, "images")
            if (imgDir.exists()) {
                imgDir.walkTopDown().filter { it.isFile }.forEach { f ->
                    val rel = "files/images/${f.relativeTo(imgDir).invariantSeparatorsPath}"
                    putFile(zos, f, rel)
                }
            }
        }
        return out
    }

    private fun putAllDatabases(zos: ZipOutputStream) {
        val dbDir = context.getDatabasePath("dummy").parentFile // /data/data/<pkg>/databases
        if (dbDir?.exists() != true) return

        // 백업할 패턴들
        val want = Regex(""".*\.(db|sqlite)$""", RegexOption.IGNORE_CASE)
        val wal  = Regex(""".*\.db-wal$""", RegexOption.IGNORE_CASE)
        val shm  = Regex(""".*\.db-shm$""", RegexOption.IGNORE_CASE)
        val jnl  = Regex(""".*-(journal|wal|shm)$""", RegexOption.IGNORE_CASE)

        dbDir.listFiles()?.forEach { f ->
            if (!f.isFile) return@forEach
            val name = f.name
            val shouldBackup =
                want.matches(name) || wal.matches(name) || shm.matches(name) || jnl.matches(name)
            if (shouldBackup) {
                putFile(zos, f, "databases/$name")
            }
        }
    }

    private fun restoreFromZip(zip: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zip))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name // e.g., databases/minimalog.db or files/images/...
                if (!entry.isDirectory) {
                    when {
                        name.startsWith("databases/") -> {
                            val dbName = name.removePrefix("databases/") // minimalog.db
                            if (dbName.isNotBlank()) {
                                val dbRoot = context.getDatabasePath("dummy").parentFile
                                    ?: error("Database directory is unavailable")
                                val dbFile = safeTarget(dbRoot, dbName)
                                writeEntry(zis, dbFile)
                            }
                        }
                        name.startsWith("files/") -> {
                            val rel = name.removePrefix("files/") // images/...
                            if (rel.isNotBlank()) {
                                val fileTarget = safeTarget(context.filesDir, rel)
                                writeEntry(zis, fileTarget)
                            }
                        }
                        else -> {
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun safeTarget(root: File, relativePath: String): File {
        val target = File(root, relativePath)
        val rootPath = root.canonicalFile.toPath()
        check(target.canonicalFile.toPath().startsWith(rootPath)) {
            "Unsafe backup entry: $relativePath"
        }
        return target
    }

    private fun writeEntry(zis: ZipInputStream, dest: File) {
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, dest.name + ".tmp")
        BufferedOutputStream(FileOutputStream(tmp)).use { out ->
            zis.copyTo(out)
        }
        if (dest.exists()) dest.delete()
        if (!tmp.renameTo(dest)) {
            // 실패 시 직접 복사
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
    }

    private fun putFile(zos: ZipOutputStream, file: File, zipPath: String) {
        if (!file.exists()) return
        FileInputStream(file).use { fis ->
            zos.putNextEntry(ZipEntry(zipPath))
            fis.copyTo(zos)
            zos.closeEntry()
        }
    }

    private fun validateBackupZip(zipFile: File) {
        ZipFile(zipFile).use { zip ->
            check(zip.getEntry("databases/minimallog.db") != null) {
                "Backup does not contain minimallog.db"
            }
            val imageDir = File(context.filesDir, "images")
            val referencedImages = mutableListOf<String>()
            database.openHelper.readableDatabase
                .query("SELECT imagePath FROM memories WHERE imagePath IS NOT NULL")
                .use { cursor ->
                    val pathIndex = cursor.getColumnIndexOrThrow("imagePath")
                    while (cursor.moveToNext()) {
                        referencedImages += File(cursor.getString(pathIndex)).name
                    }
                }
            referencedImages.forEach { fileName ->
                val image = File(imageDir, fileName)
                check(image.isFile) { "Referenced image is missing: $fileName" }
                check(zip.getEntry("files/images/$fileName") != null) {
                    "Backup is missing referenced image: $fileName"
                }
            }
            if (imageDir.exists()) {
                imageDir.walkTopDown().filter { it.isFile }.forEach { image ->
                    val relative = image.relativeTo(imageDir).invariantSeparatorsPath
                    val entry = zip.getEntry("files/images/$relative")
                        ?: error("Backup is missing image $relative")
                    check(entry.size == image.length()) {
                        "Backup image size mismatch: $relative"
                    }
                    check(entry.crc == crc32(image)) {
                        "Backup image checksum mismatch: $relative"
                    }
                }
            }
        }
    }

    private fun validateDownloadedZip(zipFile: File) {
        ZipFile(zipFile).use { zip ->
            check(zip.getEntry("databases/minimallog.db") != null) {
                "Downloaded backup does not contain minimallog.db"
            }
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            zip.entries().asSequence().filterNot { it.isDirectory }.forEach { entry ->
                zip.getInputStream(entry).use { input ->
                    while (input.read(buffer) != -1) Unit
                }
            }
        }
    }

    private fun crc32(file: File): Long {
        val crc = CRC32()
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                crc.update(buffer, 0, count)
            }
        }
        return crc.value
    }

    private fun timeStamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
}
