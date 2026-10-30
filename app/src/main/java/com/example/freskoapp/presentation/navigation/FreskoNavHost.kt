package com.example.freskoapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.freskoapp.presentation.addproduct.AddProductScreen
import com.example.freskoapp.presentation.inventory.InventoryScreen
import com.example.freskoapp.presentation.settings.SettingsScreen

// String routes on purpose: type-safe routes would need the kotlinx-serialization plugin.
@Composable
fun FreskoNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = INVENTORY_ROUTE) {
        composable(
            route = INVENTORY_ROUTE,
            arguments = listOf(
                navArgument(INVENTORY_STATUS_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(navDeepLink { uriPattern = INVENTORY_DEEP_LINK_PATTERN }),
        ) {
            InventoryScreen(
                onAddClick = { navController.navigate("add_product") },
                onSettingsClick = { navController.navigate("settings") },
            )
        }
        composable("add_product") {
            AddProductScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
