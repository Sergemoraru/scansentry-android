package com.tsmdigital.scansentry

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

enum class AppTab(
    val label: String,
    val icon: ImageVector,
) {
    Scan("Scan", Icons.Filled.CameraAlt),
    Documents("Import", Icons.Filled.PhotoLibrary),
    History("History", Icons.Filled.Schedule),
    Create("Create", Icons.Filled.Add),
    Settings("Settings", Icons.Filled.Settings),
}
