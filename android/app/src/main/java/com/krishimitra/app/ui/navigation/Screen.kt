package com.krishimitra.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Chat : Screen("chat")
    object Camera : Screen("camera")
    object Weather : Screen("weather")
    object Schemes : Screen("schemes")
    object Loans : Screen("loans")
    object CropGuide : Screen("crop_guide")
    object Mandi : Screen("mandi")
    object CropDetail : Screen("crop_detail/{cropId}") {
        fun createRoute(cropId: String) = "crop_detail/$cropId"
    }
    object Sources : Screen("sources")
}

