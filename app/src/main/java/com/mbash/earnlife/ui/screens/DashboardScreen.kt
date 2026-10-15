package com.mbash.earnlife.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbash.earnlife.ui.components.InstalledApp
import com.mbash.earnlife.ui.components.InstalledAppPicker
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import com.mbash.earnlife.data.AppSelectionStore
import com.mbash.earnlife.data.ProductiveSessionStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun DashboardScreen(
    onSettings: () -> Unit,
    onPreviewRedirect: () -> Unit
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }

    val balanceSeconds = remember(refreshKey) {
        ProductiveSessionStore.getEntertainmentBalanceSeconds(context)
    }

    val totalProductiveSeconds = remember(refreshKey) {
        ProductiveSessionStore.getTotalProductiveSeconds(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val balanceMinutes = balanceSeconds / 60
    val productiveMinutes = totalProductiveSeconds / 60

    val restrictedApps = remember(context) {
        mutableStateListOf<InstalledApp>().apply {
            addAll(AppSelectionStore.load(context, productive = false))
        }
    }

    val productiveApps = remember(context) {
        mutableStateListOf<InstalledApp>().apply {
            addAll(AppSelectionStore.load(context, productive = true))
        }
    }

    var pickerType by remember { mutableStateOf<String?>(null) }



    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "EARN",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Text(
                    "Your attention, reclaimed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "ENTERTAINMENT BALANCE",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            balanceMinutes.toString(),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "min available",
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { 0.45f },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "5 min productive time = 1 min entertainment",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                "Today's progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Productive" to "$productiveMinutes min",
                    "Entertainment" to "—",
                    "Earned" to "${balanceSeconds / 60} min"
                ).forEach { (label, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(value, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Text(
                "Manage your apps",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            AppSection(
                title = "Restricted apps",
                subtitle = "Apps that use entertainment credits",
                apps = restrictedApps,
                onAdd = { pickerType = "restricted" },
                onRemove = { app ->
                    restrictedApps.remove(app)
                    AppSelectionStore.save(
                        context,
                        productive = false,
                        apps = restrictedApps.toList()
                    )
                }
            )

            AppSection(
                title = "Productive apps",
                subtitle = "Activities that can earn credits",
                apps = productiveApps,
                onAdd = { pickerType = "productive" },
                onRemove = { app ->
                    productiveApps.remove(app)
                    AppSelectionStore.save(
                        context,
                        productive = true,
                        apps = productiveApps.toList()
                    )
                }
            )

            OutlinedButton(
                onClick = onPreviewRedirect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Preview productive app selection")
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    when (pickerType) {
        "restricted" -> InstalledAppPicker(
            title = "Choose restricted apps",
            initiallySelected = restrictedApps.toList(),
            onDismiss = { pickerType = null },
            onConfirm = { selected ->
                restrictedApps.clear()
                restrictedApps.addAll(selected)

                AppSelectionStore.save(
                    context,
                    productive = false,
                    apps = restrictedApps.toList()
                )

                pickerType = null
            }
        )

        "productive" -> InstalledAppPicker(
            title = "Choose productive apps",
            initiallySelected = productiveApps.toList(),
            onDismiss = { pickerType = null },
            onConfirm = { selected ->
                productiveApps.clear()
                productiveApps.addAll(selected)

                AppSelectionStore.save(
                    context,
                    productive = true,
                    apps = productiveApps.toList()
                )

                pickerType = null
            }
        )
    }
}



@Composable
private fun AppSection(
    title: String,
    subtitle: String,
    apps: List<InstalledApp>,
    onAdd: () -> Unit,
    onRemove: (InstalledApp) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onAdd) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add app"
                    )
                }
            }

            if (apps.isEmpty()) {
                Text(
                    "No apps selected yet. Tap + to add apps.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp),
                    userScrollEnabled = apps.size > 3
                ){
                    items(
                        items = apps,
                        key = { it.packageName }
                    ) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        app.label.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    app.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = { onRemove(app) }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove ${app.label}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
