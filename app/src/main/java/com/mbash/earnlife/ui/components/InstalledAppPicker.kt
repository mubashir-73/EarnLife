package com.mbash.earnlife.ui.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

data class InstalledApp(
    val packageName: String,
    val label: String
)

@Composable
fun InstalledAppPicker(
    title: String,
    initiallySelected: List<InstalledApp> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (List<InstalledApp>) -> Unit
) {
    val context = LocalContext.current

    // Discover apps that have a launcher activity.
    val installedApps = remember(context) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        context.packageManager
            .queryIntentActivities(intent, 0)
            .map { result ->
                InstalledApp(
                    packageName = result.activityInfo.packageName,
                    label = result.loadLabel(
                        context.packageManager
                    ).toString()
                )
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    var search by remember { mutableStateOf("") }

    val selectedPackages = remember {
        mutableStateListOf<String>().apply {
            addAll(initiallySelected.map { it.packageName })
        }
    }

    val filteredApps = installedApps.filter {
        it.label.contains(search, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search installed apps") }
                )

                Text(
                    "${selectedPackages.size} selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (filteredApps.isEmpty()) {
                    Text(
                        "No matching apps found.",
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 380.dp)
                    ) {
                        items(
                            items = filteredApps,
                            key = { it.packageName }
                        ) { app ->
                            val checked =
                                app.packageName in selectedPackages

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) {
                                            selectedPackages.remove(
                                                app.packageName
                                            )
                                        } else {
                                            selectedPackages.add(
                                                app.packageName
                                            )
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(app.label)
                                    Text(
                                        app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            if (app.packageName !in selectedPackages) {
                                                selectedPackages.add(
                                                    app.packageName
                                                )
                                            }
                                        } else {
                                            selectedPackages.remove(
                                                app.packageName
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onConfirm(
                                installedApps.filter {
                                    it.packageName in selectedPackages
                                }
                            )
                        }
                    ) {
                        Text("Add selected")
                    }
                }
            }
        }
    }
}
