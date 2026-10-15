

package com.mbash.earnlife.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.mbash.earnlife.data.UsageAccessHelper
import android.util.Log
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mbash.earnlife.data.AppUsageReader

@Composable
fun SetupScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "EARN",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Make your screen time count.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Spend time on what matters. Earn time for what you enjoy.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Before you start",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        PermissionItem(
            title = "Usage access",
            description = if (
                UsageAccessHelper.hasUsageAccess(context)
            ) {
                "Enabled — app usage can be queried."
            } else {
                "Allows EarnLife to reconcile app usage."
            },
            onSetup = {
                UsageAccessHelper.openUsageAccessSettings(context)
            }
        )

        PermissionItem(
            title = "Accessibility access",
            description = "Detect app transitions and support restrictions.",
            onSetup = {
                UsageAccessHelper.openAccessibilitySettings(context)
            }
        )

        TextButton(
            onClick = {
                scope.launch {
                    val usage = withContext(Dispatchers.IO) {
                        AppUsageReader.getRecentUsage(context)
                    }

                    Log.d("EarnLifeUsage", "Records found: ${usage.size}")

                    usage.forEach { stats ->
                        Log.d(
                            "EarnLifeUsage",
                            "${stats.packageName}: " +
                                    "${stats.totalTimeInForeground / 1000}s"
                        )
                    }
                }
            }
        ) {
            Text("Test usage query")
        }

        Card(
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Privacy first",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Usage tracking and credit accounting are designed to stay on your device.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Continue to dashboard")
        }

        Text(
            text = "UI prototype · Permissions are not connected yet",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    onSetup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)

            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(onClick = onSetup) {
                Text("Open settings")
            }
        }
    }
}
