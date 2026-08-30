package com.krishimitra.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.krishimitra.app.ui.components.KrishiBottomNav
import com.krishimitra.app.ui.components.KrishiTopBar
import com.krishimitra.app.ui.navigation.KrishiNavGraph
import com.krishimitra.app.ui.navigation.Screen
import com.krishimitra.app.ui.theme.KrishiMitraTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime permissions gracefully on start
        checkAndRequestPermissions()

        val app = application as KrishiMitraApp

        setContent {
            val currentLang by app.languageManager.currentLanguage.collectAsState()
            val localizedContext = remember(currentLang) {
                app.languageManager.applyLocaleToConfiguration(this@MainActivity, currentLang.locale)
            }

            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides localizedContext,
                androidx.compose.ui.platform.LocalConfiguration provides localizedContext.resources.configuration
            ) {
                KrishiMitraTheme {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    var isOnline by remember { mutableStateOf(app.apiClient.isNetworkAvailable()) }

                    // Periodic check of network state
                    LaunchedEffect(Unit) {
                        while (true) {
                            isOnline = app.apiClient.isNetworkAvailable()
                            kotlinx.coroutines.delay(4000)
                        }
                    }

                    val title = when (currentRoute) {
                        Screen.Home.route -> stringResource(R.string.app_name)
                        Screen.Chat.route -> stringResource(R.string.assistant_title)
                        Screen.Camera.route -> stringResource(R.string.camera_title)
                        Screen.Weather.route -> stringResource(R.string.weather_title)
                        Screen.Schemes.route -> stringResource(R.string.schemes_title)
                        Screen.Loans.route -> stringResource(R.string.loans_title)
                        Screen.CropGuide.route -> stringResource(R.string.action_crops)
                        Screen.Sources.route -> stringResource(R.string.sources_attribution)
                        else -> stringResource(R.string.app_name)
                    }

                    val showBack = currentRoute == Screen.CropGuide.route ||
                                   currentRoute == Screen.Loans.route ||
                                   currentRoute == Screen.Sources.route

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            KrishiTopBar(
                                title = title,
                                isOnline = isOnline,
                                currentLanguage = currentLang,
                                onLanguageToggle = { app.languageManager.toggleLanguage() },
                                showBack = showBack,
                                onBackClick = { navController.popBackStack() },
                                onSourcesClick = { navController.navigate(Screen.Sources.route) }
                            )
                        },
                        bottomBar = {
                            KrishiBottomNav(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        KrishiNavGraph(
                            navController = navController,
                            paddingValues = innerPadding,
                            dbHelper = app.dbHelper,
                            apiClient = app.apiClient,
                            aiRouter = app.aiRouter,
                            diseaseClassifier = app.diseaseClassifier,
                            voiceManager = app.voiceManager
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            requestPermissionLauncher.launch(needed.toTypedArray())
        }
    }
}
