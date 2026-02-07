package com.tsmdigital.scansentry

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

enum class AppTab(
    val label: String,
    val icon: ImageVector,
) {
    // Use widely-available Material icons to avoid version-specific missing symbols.
    Scan("Scan", Icons.Filled.CameraAlt),
    Documents("Documents", Icons.Filled.Folder),
    History("History", Icons.Filled.Schedule),
    Create("Create", Icons.Filled.Add),
    Settings("Settings", Icons.Filled.Settings),
}
