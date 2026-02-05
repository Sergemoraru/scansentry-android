package com.tsmdigital.scansentry.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tsmdigital.scansentry.billing.EntitlementManager

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val entitlements = remember { EntitlementManager(context) }

    var isPro by remember { mutableStateOf(entitlements.isPro) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Pro", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (isPro) "Active" else "Free")
                Switch(
                    checked = isPro,
                    onCheckedChange = {
                        // Dev toggle placeholder until Billing is wired.
                        isPro = it
                        entitlements.isPro = it
                    }
                )
            }

            Divider()
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text("Scan Sentry (Android)")
        }
    }
}
