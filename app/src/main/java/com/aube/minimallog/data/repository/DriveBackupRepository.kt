package com.aube.minimallog.data.repository

import android.content.Context
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
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class DriveBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sp by lazy { context.getSharedPreferences("drive", Context.MODE_PRIVATE) }

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

        // 1) ZIP 만들기
        val zip = makeBackupZip() // 아래 함수 예시

        // 2) 업로드 (appDataFolder)
        val meta = com.google.api.services.drive.model.File().apply {
            name = "minimalog_backup_${timeStamp()}.zip"
            parents = listOf("appDataFolder")
            mimeType = "application/zip"
        }
        val media = com.google.api.client.http.FileContent("application/zip", zip)
        service.files().create(meta, media).setFields("id, name, createdTime").execute()

        persistLastBackupTime(System.currentTimeMillis())
        zip.delete() // 임시 파일 정리
        return@withContext System.currentTimeMillis()
    }

    // ---- 복원 ----
    suspend fun restoreLatest(accountName: String) = withContext(Dispatchers.IO) {
        val service = DriveServiceFactory.create(context, accountName)

        // 최신 백업 1건 찾기
        val list = service.files().list()
            .setSpaces("appDataFolder")
            .setQ("mimeType='application/zip' and trashed=false")
            .setOrderBy("createdTime desc")
            .setPageSize(1)
            .setFields("files(id,name,createdTime)")
            .execute()
        val file = list.files?.firstOrNull() ?: error("No backup found")

        // 다운로드
        val dest = File(context.cacheDir, "restore.zip")
        dest.outputStream().use { out ->
            service.files().get(file.id).executeMediaAndDownloadTo(out)
        }

        // ZIP 풀고 적용
        restoreFromZip(dest)
        dest.delete()
    }

    // ---- ZIP 생성/복원 유틸 ----
    private fun makeBackupZip(): File {
        val out = File(context.cacheDir, "backup_${System.currentTimeMillis()}.zip")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(out))).use { zos ->
            // 1) DB
            val db = context.getDatabasePath("minimalog.db")
            putFile(zos, db, "db/minimalog.db")

            // 2) 이미지 폴더(예: /files/images)
            val imgDir = File(context.filesDir, "images")
            if (imgDir.exists()) imgDir.walkTopDown().filter { it.isFile }.forEach { f ->
                val rel = "images/${f.relativeTo(imgDir).invariantSeparatorsPath}"
                putFile(zos, f, rel)
            }
        }
        return out
    }

    private fun restoreFromZip(zip: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zip))).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                val dest = File(context.filesDir.parentFile, e.name) // /data/data/<pkg>/db/... 등
                if (e.isDirectory) dest.mkdirs() else {
                    dest.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(dest)).use { out -> zis.copyTo(out) }
                }
                e = zis.nextEntry
            }
        }
        // ROOM DB 교체 후 프로세스 재시작이 안전. 즉시 반영 원하면 Room close→reopen 필요.
    }

    private fun putFile(zos: ZipOutputStream, file: File, zipPath: String) {
        if (!file.exists()) return
        zos.putNextEntry(ZipEntry(zipPath))
        file.inputStream().use { it.copyTo(zos) }
        zos.closeEntry()
    }

    private fun timeStamp(): String = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
}
