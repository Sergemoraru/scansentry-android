package com.tsmdigital.scansentry.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.tsmdigital.scansentry.data.AppDatabase
import com.tsmdigital.scansentry.data.ScanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).scanDao() }

    var processing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var latestText by remember { mutableStateOf<String?>(null) }
    var latestFormat by remember { mutableStateOf<String?>(null) }

    fun saveScan(raw: String, format: String?) {
        val createdAt = System.currentTimeMillis()
        scope.launch(Dispatchers.IO) {
            dao.insert(
                ScanRecord(
                    rawValue = raw,
                    format = format,
                    createdAtEpochMs = createdAt
                )
            )
        }
        latestText = raw
        latestFormat = format
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        processing = true
        message = null

        runCatching { InputImage.fromFilePath(context, uri) }
            .onSuccess { inputImage ->
                val scanner = BarcodeScanning.getClient()
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val code = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                        if (code == null) {
                            message = "No barcode found in selected image."
                        } else {
                            val raw = code.rawValue.orEmpty()
                            saveScan(raw, code.format.toString())
                            message = "Imported from photo."
                        }
                    }
                    .addOnFailureListener {
                        message = "Failed to scan selected image."
                    }
                    .addOnCompleteListener {
                        processing = false
                        scanner.close()
                    }
            }
            .onFailure {
                processing = false
                message = "Unable to open selected image."
            }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Import") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Import a code from gallery or clipboard.",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !processing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Photo")
                }

                OutlinedButton(
                    onClick = {
                        val value = clipboardText(context)
                        if (value.isNullOrBlank()) {
                            message = "Clipboard is empty."
                        } else {
                            saveScan(value, "PASTE")
                            message = "Imported from clipboard."
                        }
                    },
                    enabled = !processing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Paste")
                }
            }

            if (message != null) {
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (latestText != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = latestText.orEmpty(),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (!latestFormat.isNullOrBlank()) {
                            Text(
                                text = latestFormat.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Row {
                            TextButton(onClick = { copyToClipboard(context, latestText.orEmpty()) }) {
                                Text("Copy")
                            }
                            TextButton(onClick = { shareText(context, latestText.orEmpty()) }) {
                                Text("Share")
                            }
                            if (looksLikeUrl(latestText.orEmpty())) {
                                TextButton(onClick = { openUrl(context, latestText.orEmpty()) }) {
                                    Text("Open")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

private fun clipboardText(context: Context): String? {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val item = manager.primaryClip?.getItemAt(0) ?: return null
    return item.coerceToText(context)?.toString()?.trim()
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("scan", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

private fun openUrl(context: Context, text: String) {
    runCatching {
        val uri = Uri.parse(text)
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

private fun looksLikeUrl(s: String): Boolean {
    return s.startsWith("http://") || s.startsWith("https://")
}
