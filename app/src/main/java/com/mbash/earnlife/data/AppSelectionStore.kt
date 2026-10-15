package com.mbash.earnlife.data

import android.content.Context
import com.mbash.earnlife.ui.components.InstalledApp
import org.json.JSONArray
import org.json.JSONObject

object AppSelectionStore {

    private const val PREFS_NAME = "earnlife_app_selections"
    private const val RESTRICTED_KEY = "restricted_apps"
    private const val PRODUCTIVE_KEY = "productive_apps"

    private fun preferences(context: Context) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun load(
        context: Context,
        productive: Boolean
    ): List<InstalledApp> {
        val key = if (productive) {
            PRODUCTIVE_KEY
        } else {
            RESTRICTED_KEY
        }

        val json = preferences(context).getString(key, "[]")
            ?: "[]"

        return try {
            val array = JSONArray(json)

            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index)
                    ?: return@mapNotNull null

                val packageName = item.optString("packageName")
                val label = item.optString("label")

                if (packageName.isBlank() || label.isBlank()) {
                    null
                } else {
                    InstalledApp(
                        packageName = packageName,
                        label = label
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(
        context: Context,
        productive: Boolean,
        apps: List<InstalledApp>
    ) {
        val key = if (productive) {
            PRODUCTIVE_KEY
        } else {
            RESTRICTED_KEY
        }

        val array = JSONArray()

        apps.forEach { app ->
            array.put(
                JSONObject().apply {
                    put("packageName", app.packageName)
                    put("label", app.label)
                }
            )
        }

        preferences(context)
            .edit()
            .putString(key, array.toString())
            .apply()
    }
}
