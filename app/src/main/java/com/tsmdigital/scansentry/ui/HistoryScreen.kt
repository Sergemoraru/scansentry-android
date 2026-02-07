package com.tsmdigital.scansentry.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tsmdigital.scansentry.data.AppDatabase
import com.tsmdigital.scansentry.data.ScanRecord
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).scanDao() }
    val scope = rememberCoroutineScope()

    val scans by dao.observeAll().collectAsState(initial = emptyList())

    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                actions = {
                    IconButton(onClick = { showClearConfirm = true }, enabled = scans.isNotEmpty()) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                }
            )
        }
    ) { padding ->
        if (scans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No scans yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(scans, key = { it.id }) { scan ->
                    ScanRow(scan = scan)
                    Divider()
                }
            }
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Clear history?") },
                text = { Text("This deletes all scans from this device.") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearConfirm = false
                        scope.launch { dao.clearAll() }
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun ScanRow(scan: ScanRecord) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Text(
            text = scan.rawValue,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = (scan.format ?: "").uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { copyToClipboard(context, scan.rawValue) }) { Text("Copy") }
            TextButton(onClick = { shareText(context, scan.rawValue) }) { Text("Share") }
            if (looksLikeUrl(scan.rawValue)) {
                TextButton(onClick = { openUrl(context, scan.rawValue) }) { Text("Open") }
            }
        }
    }
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
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }
}

private fun looksLikeUrl(s: String): Boolean {
    return s.startsWith("http://") || s.startsWith("https://")
}
