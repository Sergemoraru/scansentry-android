package com.tsmdigital.scansentry

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.tsmdigital.scansentry.ui.CreateScreen
import com.tsmdigital.scansentry.ui.DocumentsScreen
import com.tsmdigital.scansentry.ui.HistoryScreen
import com.tsmdigital.scansentry.ui.ScanScreen
import com.tsmdigital.scansentry.ui.SettingsScreen

@Composable
fun AppRoot() {
    var tab by remember { mutableStateOf(AppTab.Scan) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = (tab == t),
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            when (tab) {
                AppTab.Scan -> ScanScreen()
                AppTab.Documents -> DocumentsScreen()
                AppTab.History -> HistoryScreen()
                AppTab.Create -> CreateScreen()
                AppTab.Settings -> SettingsScreen()
            }
        }
    }
}
