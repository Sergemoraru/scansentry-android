package com.tsmdigital.scansentry.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tsmdigital.scansentry.billing.BillingManager

@Composable
fun PaywallScreen(
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val billing = remember { BillingManager.get(context) }
    val isPro by billing.isPro.collectAsState()
    val error by billing.error.collectAsState()

    val monthly = billing.basePlanMonthly()
    val yearly = billing.basePlanYearly()

    val monthlyPrice = billing.priceText(monthly)
    val yearlyPrice = billing.priceText(yearly)

    LaunchedEffect(Unit) {
        billing.startConnection()
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Upgrade to Pro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Unlock unlimited scanning and exporting.")

                if (isPro) {
                    Text("Pro is active", color = MaterialTheme.colorScheme.secondary)
                }

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (activity != null) billing.launchPurchase(activity, monthly)
                        },
                        enabled = activity != null && !isPro,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Monthly${monthlyPrice?.let { " • $it" } ?: ""}")
                    }

                    OutlinedButton(
                        onClick = {
                            if (activity != null) billing.launchPurchase(activity, yearly)
                        },
                        enabled = activity != null && !isPro,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Yearly${yearlyPrice?.let { " • $it" } ?: ""}")
                    }

                    TextButton(
                        onClick = { billing.restorePurchases() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Restore")
                    }
                }

                Text(
                    "Auto-renewing subscription. Cancel anytime in Google Play → Payments & subscriptions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Close") }
        },
    )
}
