package com.example.freskoapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.freskoapp.presentation.addproduct.AddProductScreen
import com.example.freskoapp.presentation.detail.ProductDetailScreen
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
                onAddClick = { navController.navigate(addProductRoute()) },
                onSettingsClick = { navController.navigate("settings") },
                onProductClick = { id -> navController.navigate(productDetailRoute(id)) },
            )
        }
        composable(
            route = PRODUCT_DETAIL_ROUTE,
            arguments = listOf(navArgument(PRODUCT_ID_ARG) { type = NavType.LongType }),
        ) {
            ProductDetailScreen(
                onEdit = { id -> navController.navigate(editProductRoute(id)) },
                onBack = { navController.popBackStack() },
                onRemoved = { removal ->
                    // Hand the result to the Inventory (it shows "Deshacer") and pop only up to it, so a repeated
                    // callback can never pop the Inventory itself and close the app.
                    navController.getBackStackEntry(INVENTORY_ROUTE).savedStateHandle.putRemoval(removal)
                    navController.popBackStack(INVENTORY_ROUTE, inclusive = false)
                },
            )
        }
        composable(
            route = ADD_PRODUCT_ROUTE,
            arguments = listOf(navArgument(PRODUCT_ID_ARG) { type = NavType.LongType; defaultValue = NO_PRODUCT_ID }),
        ) {
            AddProductScreen(
                onBack = { navController.popBackStack() },
                onSaved = { merged ->
                    if (merged) navController.getBackStackEntry(INVENTORY_ROUTE).savedStateHandle.putLotMerged()
                    navController.popBackStack()
                },
                onUnavailable = {
                    // Edit mode: the entry below is the product detail, which shows the notice.
                    navController.previousBackStackEntry?.savedStateHandle?.putProductUnavailable()
                    navController.popBackStack()
                },
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
