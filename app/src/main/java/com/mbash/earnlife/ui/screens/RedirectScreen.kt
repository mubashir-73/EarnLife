package com.mbash.earnlife.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mbash.earnlife.data.AppSelectionStore
import com.mbash.earnlife.data.ProductiveSessionStore
import com.mbash.earnlife.ui.components.InstalledApp

@Composable
fun RedirectScreen(
    restrictedAppName: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val productiveApps = remember {
        AppSelectionStore.load(context, productive = true)
    }

    var selectedPackage by rememberSaveable {
        mutableStateOf(productiveApps.firstOrNull()?.packageName.orEmpty())
    }

    val selectedApp = productiveApps.find {
        it.packageName == selectedPackage
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back to dashboard")
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "YOUR NEXT BREAK",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                "Earn your next break.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Choose a productive activity to earn more entertainment time.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Entertainment balance",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "0 min available",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (restrictedAppName != null) {
                        "You were opening $restrictedAppName"
                    } else {
                        "Preview mode — no app was intercepted"
                    }
                )
            }
        }

        Text(
            "What would you like to do?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (productiveApps.isEmpty()) {
            Text(
                "You haven't selected any productive apps yet. " +
                        "Return to the Dashboard and add one first.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose productive apps")
            }
        } else {
            productiveApps.forEach { app ->
                Card(
                    onClick = { selectedPackage = app.packageName },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPackage == app.packageName) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontWeight = FontWeight.SemiBold)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        RadioButton(
                            selected = selectedPackage == app.packageName,
                            onClick = { selectedPackage = app.packageName }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val packageName = selectedApp?.packageName

                    if (packageName == null) {
                        Toast.makeText(
                            context,
                            "Select a productive app first",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    val launchIntent =
                        context.packageManager.getLaunchIntentForPackage(packageName)

                    if (launchIntent == null) {
                        Toast.makeText(
                            context,
                            "Couldn't launch ${selectedApp.label}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        try {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ProductiveSessionStore.startSession(context, packageName)
                            context.startActivity(launchIntent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                "This app can't be opened right now",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = selectedApp != null
            ) {
                Text("Start ${selectedApp?.label ?: "activity"}")
            }
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Not now")
        }
    }
}
