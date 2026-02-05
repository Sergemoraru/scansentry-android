package com.tsmdigital.scansentry

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

enum class AppTab(
    val label: String,
    val icon: ImageVector,
) {
    Scan("Scan", Icons.Filled.QrCodeScanner),
    Documents("Documents", Icons.Filled.Description),
    History("History", Icons.Filled.History),
    Create("Create", Icons.Filled.QrCode),
    Settings("Settings", Icons.Filled.Settings),
}
