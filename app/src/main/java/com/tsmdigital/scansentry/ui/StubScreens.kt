package com.tsmdigital.scansentry.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun DocumentsScreen() = Centered("Documents (TODO)")
@Composable fun HistoryScreen() = Centered("History (TODO)")
@Composable fun CreateScreen() = Centered("Create (TODO)")
@Composable fun SettingsScreen() = Centered("Settings (TODO)")

@Composable
private fun Centered(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(label)
    }
}
