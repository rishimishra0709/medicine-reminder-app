package com.example.medreminder.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AddMedication : Screen("add_medication")
}
