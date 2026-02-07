package com.tsmdigital.scansentry.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen() {
    val context = LocalContext.current
    val billing = remember { com.tsmdigital.scansentry.billing.BillingManager.get(context) }
    val isPro by billing.isPro.collectAsState()

    var text by remember { mutableStateOf("https://") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPaywall by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        billing.startConnection()
    }

    LaunchedEffect(text) {
        bitmap = runCatching { generateQr(text) }.getOrNull()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Create") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Text or URL") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )

            if (bitmap != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "QR",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (!isPro) {
                            showPaywall = true
                        } else {
                            bitmap?.let { shareBitmap(context, it) }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Share")
                }
                OutlinedButton(
                    onClick = {
                        if (!isPro) {
                            showPaywall = true
                        } else {
                            bitmap?.let {
                                val ok = saveToPhotos(context, it)
                                message = if (ok) "Saved to Photos" else "Failed to save"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }

            if (message != null) {
                Text(message!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }

            Text(
                text = if (isPro) "" else "Export requires Pro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (showPaywall) {
            PaywallScreen(onClose = { showPaywall = false })
        }
    }
}

private fun generateQr(text: String): Bitmap {
    val writer = QRCodeWriter()
    val hints = mapOf(
        EncodeHintType.MARGIN to 1
    )
    val matrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512, hints)

    val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    for (x in 0 until 512) {
        for (y in 0 until 512) {
            bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bmp
}

private fun shareBitmap(context: Context, bitmap: Bitmap) {
    // Minimal share: write to MediaStore cache and share URI.
    val uri = saveToMediaStore(context, bitmap, "scansentry_qr_share") ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share QR"))
}

private fun saveToPhotos(context: Context, bitmap: Bitmap): Boolean {
    return saveToMediaStore(context, bitmap, "scansentry_qr") != null
}

private fun saveToMediaStore(context: Context, bitmap: Bitmap, baseName: String): Uri? {
    val values = android.content.ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "${baseName}_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ScanSentry")
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    return try {
        resolver.openOutputStream(uri).use { out: OutputStream? ->
            if (out == null) return null
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        uri
    } catch (_: Throwable) {
        null
    }
}
