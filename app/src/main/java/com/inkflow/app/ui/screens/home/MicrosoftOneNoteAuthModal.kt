package com.inkflow.app.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkflow.app.data.onenote.OneNoteSyncService

/**
 * Real Microsoft sign-in dialog (OAuth device-code flow).
 *
 * Microsoft does not allow third-party apps to collect an account password, so InkFlow never asks
 * for one. Instead the user signs in on microsoft.com with a short code, then InkFlow fetches
 * their OneNote pages through Microsoft Graph.
 */
@Composable
fun MicrosoftOneNoteAuthModal(
    initialClientId: String,
    isSyncing: Boolean,
    statusMessage: String?,
    deviceCode: OneNoteSyncService.DeviceCodeInfo?,
    onStartSignIn: (clientId: String) -> Unit,
    onSyncWithToken: (token: String) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var clientId by remember { mutableStateOf(initialClientId) }
    var token by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        title = { Text("Connect Microsoft OneNote") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (deviceCode != null) {
                    Text(
                        "1. Open the Microsoft sign-in page\n2. Enter this code\n3. Sign in with your Microsoft account and approve access",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = deviceCode.userCode,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                copyToClipboard(context, deviceCode.userCode)
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(deviceCode.verificationUri))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7719AA),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("Copy code & open") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Waiting for you to finish signing in...", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Text(
                        "Microsoft doesn't let apps collect your password. You'll sign in on Microsoft's own page using a short code — InkFlow never sees your password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { clientId = it },
                        label = { Text("Azure Application (client) ID") },
                        placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "One-time setup (free, ~2 min): portal.azure.com → App registrations → New registration → account type \"Personal Microsoft accounts\" (or any) → Authentication → enable \"Allow public client flows\" → API permissions → add Microsoft Graph delegated Notes.Read. Paste the Application (client) ID above.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { showAdvanced = !showAdvanced }) {
                        Text(if (showAdvanced) "Hide advanced" else "Advanced: paste a Graph access token")
                    }
                    if (showAdvanced) {
                        OutlinedTextField(
                            value = token,
                            onValueChange = { token = it },
                            label = { Text("Microsoft Graph access token") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = {
            if (deviceCode == null) {
                Button(
                    enabled = !isSyncing,
                    onClick = {
                        if (token.isNotBlank()) onSyncWithToken(token.trim())
                        else onStartSignIn(clientId.trim())
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7719AA),
                        contentColor = Color.White
                    )
                ) { Text(if (token.isNotBlank()) "Sync with token" else "Sign in with Microsoft") }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (isSyncing || deviceCode != null) onCancel() else onDismiss() }) {
                Text(if (isSyncing || deviceCode != null) "Cancel sign-in" else "Close")
            }
        }
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Microsoft code", text))
}
