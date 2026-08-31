package com.krishimitra.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.krishimitra.app.data.local.DatabaseHelper
import com.krishimitra.app.data.remote.ApiClient
import com.krishimitra.app.domain.ai.HybridAIRouter
import com.krishimitra.app.ml.OnnxDiseaseClassifier
import com.krishimitra.app.ui.screens.*
import com.krishimitra.app.voice.VoiceManager

@Composable
fun KrishiNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
    dbHelper: DatabaseHelper,
    apiClient: ApiClient,
    aiRouter: HybridAIRouter,
    diseaseClassifier: OnnxDiseaseClassifier,
    voiceManager: VoiceManager
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(Screen.Home.route) {
            HomeScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(Screen.Chat.route) {
            ChatScreen(aiRouter = aiRouter, voiceManager = voiceManager)
        }
        composable(Screen.Camera.route) {
            CameraScreen(classifier = diseaseClassifier)
        }
        composable(Screen.Weather.route) {
            WeatherScreen(apiClient = apiClient)
        }
        composable(Screen.Schemes.route) {
            SchemesScreen(dbHelper = dbHelper)
        }
        composable(Screen.Loans.route) {
            LoansScreen(dbHelper = dbHelper)
        }
        composable(Screen.CropGuide.route) {
            CropGuideScreen(dbHelper = dbHelper)
        }
        composable(Screen.Mandi.route) {
            MandiScreen(dbHelper = dbHelper, onAskAI = { q -> navController.navigate(Screen.Chat.route) })
        }
        composable(Screen.Sources.route) {
            SourcesScreen()
        }
    }
}

