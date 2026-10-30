package com.example.freskoapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.freskoapp.presentation.addproduct.AddProductScreen
import com.example.freskoapp.presentation.inventory.InventoryScreen

// String routes on purpose: type-safe routes would need the kotlinx-serialization plugin.
@Composable
fun FreskoNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "inventory") {
        composable("inventory") {
            InventoryScreen(onAddClick = { navController.navigate("add_product") })
        }
        composable("add_product") {
            AddProductScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
