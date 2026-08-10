package com.aube.minimallog.presentation.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aube.minimallog.R
import com.aube.minimallog.presentation.viewmodel.SettingsViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

// --- Public models ------------------------------------------------------------

enum class LanguageOption(val tag: String, val label: String) {
    SYSTEM("", "System"),
    EN("en", "English"),
    KO("ko", "한국어"),
    JA("ja", "日本語");
}

// --- Screen ------------------------------------------------------------------

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val driveState by vm.drive.collectAsState()
    val currentLanguage by vm.currentLanguage.collectAsState()
    var showBackupDialog by rememberSaveable { mutableStateOf(false) }
    var showRestoreDialog by rememberSaveable { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(res.data)
        try {
            val account = task.getResult(ApiException::class.java)

            // 스코프가 실제로 붙었는지 확인
            val hasDriveScope = GoogleSignIn.hasPermissions(
                account,
                Scope(DriveScopes.DRIVE_APPDATA)
            )
            if (hasDriveScope) {
                vm.onGoogleSignedIn(account)
            } else {
                // Log.e("SignIn", "Missing DRIVE_APPDATA scope; relaunching sign-in...")
            }
        } catch (e: ApiException) {
            // Log.e("SignIn", "GoogleSignIn failed: status=${e.statusCode}, msg=${e.message}", e)
        }
    }

    fun launchSignIn(context: Context) {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        val client = GoogleSignIn.getClient(context, signInOptions)
        signInLauncher.launch(client.signInIntent)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Language Section
        SettingCard(title = stringResource(R.string.text_language)) {
            LanguageSelectorRow(
                current = currentLanguage,
                onChange = { vm.changeLanguage(it) }
            )
        }

        // Privacy Policy Section
        SettingCard(title = stringResource(R.string.text_privacy_policy)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://aubecompany.blogspot.com/2025/09/privacy-policy-for-minimallog.html")
                        )
                        context.startActivity(intent)
                    }
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.text_privacy_policy_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SettingCard(title = stringResource(R.string.text_google_drive)) {
            DriveSettingsRow(
                connected = driveState.connected,
                accountName = driveState.accountName,
                lastBackup = driveState.lastBackup,
                busy = driveState.busy,
                onConnect = { launchSignIn(context) },
                onBackup = { showBackupDialog = true },
                onRestore = { showRestoreDialog = true },
                onDisconnect = { vm.disconnect(context) },
            )
        }
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(stringResource(R.string.text_back_up_now)) },
            text = { Text(stringResource(R.string.backup_confirm_message)) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            confirmButton = {
                TextButton(onClick = {
                    showBackupDialog = false
                    vm.backupNow()
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(stringResource(R.string.text_restore)) },
            text = { Text(stringResource(R.string.restore_confirm_message)) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            confirmButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    vm.restoreLatest()
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun DriveSettingsRow(
    connected: Boolean,
    accountName: String?,
    lastBackup: Long?,
    busy: Boolean,
    onConnect: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.text_back_up_detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (connected) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.text_status_connected) +
                        (accountName?.let { " ($it)" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                lastBackup?.let { timestamp ->
                    val formatter = java.time.format.DateTimeFormatter
                        .ofPattern("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    val formatted = java.time.Instant.ofEpochMilli(timestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .format(formatter)
                    Text(
                        text = stringResource(R.string.text_last_backup) + formatted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onBackup,
                    enabled = !busy,
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = stringResource(R.string.text_back_up_now),
                        maxLines = 1,
                    )
                }
                OutlinedButton(
                    onClick = onRestore,
                    enabled = !busy,
                    modifier = Modifier.weight(0.8f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.text_restore),
                        maxLines = 1,
                    )
                }
                OutlinedButton(
                    onClick = onDisconnect,
                    enabled = !busy,
                    modifier = Modifier.weight(1.15f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.text_disconnect),
                        maxLines = 1,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.text_status_not_connected),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onConnect, enabled = !busy) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(stringResource(R.string.text_connect_google_drive))
                }
            }
        }
    }
}

// --- Sub-UI -------------------------------------------------------------------

@Composable
private fun SettingCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        content()
    }
}

@Composable
private fun LanguageSelectorRow(
    current: LanguageOption,
    onChange: (LanguageOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LanguageOption.values().forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onChange(option) }
                    .background(
                        if (option == current) MaterialTheme.colorScheme.primaryContainer.copy(0.8f)
                        else MaterialTheme.colorScheme.primaryContainer
                    )
            ) {
                RadioButton(
                    selected = option == current,
                    onClick = { onChange(option) }
                )
                Spacer(Modifier.width(8.dp))
                Text(option.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
