
package com.mbash.earnlife

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.mbash.earnlife.ui.theme.EarnLifeTheme
import com.mbash.earnlife.ui.screens.SetupScreen
import com.mbash.earnlife.ui.screens.DashboardScreen
import com.mbash.earnlife.ui.screens.RedirectScreen

private enum class EarnScreen {
    Setup,
    Dashboard,
    Redirect
}

class MainActivity : ComponentActivity() {

    private var redirectRequest by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        redirectRequest = intent.getStringExtra(EXTRA_RESTRICTED_APP)

        setContent {
            EarnLifeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EarnApp(
                        redirectRequest = redirectRequest,
                        onRedirectConsumed = {
                            redirectRequest = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        redirectRequest = intent.getStringExtra(EXTRA_RESTRICTED_APP)
    }

    companion object {
        const val EXTRA_RESTRICTED_APP =
            "com.mbash.earnlife.RESTRICTED_APP"
    }
}

@Composable
private fun EarnApp(
    redirectRequest: String?,
    onRedirectConsumed: () -> Unit
) {
    var currentScreen by rememberSaveable {
        mutableStateOf(EarnScreen.Setup.name)
    }

    var restrictedAppName by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(redirectRequest) {
        if (redirectRequest != null) {
            restrictedAppName = redirectRequest
            currentScreen = EarnScreen.Redirect.name
            onRedirectConsumed()
        }
    }

    when (EarnScreen.valueOf(currentScreen)) {
        EarnScreen.Setup -> SetupScreen(
            onContinue = {
                currentScreen = EarnScreen.Dashboard.name
            }
        )

        EarnScreen.Dashboard -> DashboardScreen(
            onSettings = {
                currentScreen = EarnScreen.Setup.name
            },
            onPreviewRedirect = {
                restrictedAppName = null
                currentScreen = EarnScreen.Redirect.name
            }
        )

        EarnScreen.Redirect -> RedirectScreen(
            restrictedAppName = restrictedAppName,
            onBack = {
                restrictedAppName = null
                currentScreen = EarnScreen.Dashboard.name
            }
        )
    }
}
