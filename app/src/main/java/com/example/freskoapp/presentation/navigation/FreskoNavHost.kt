package com.example.freskoapp.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.freskoapp.presentation.addproduct.AddProductScreen
import com.example.freskoapp.presentation.detail.ProductDetailScreen
import com.example.freskoapp.presentation.inventory.InventoryScreen
import com.example.freskoapp.presentation.scanner.ScannerScreen
import com.example.freskoapp.presentation.settings.SettingsScreen

// Opaque slides, no fades: the default crossfade makes both screens translucent and lets the window show through
// (the light flash). The new screen covers the old one, which moves only a quarter of the width (parallax).
private val slideSpec = tween<IntOffset>(durationMillis = 300)

// String routes on purpose: type-safe routes would need the kotlinx-serialization plugin.
@Composable
fun FreskoNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = INVENTORY_ROUTE,
        enterTransition = { slideInHorizontally(slideSpec) { it } },
        exitTransition = { slideOutHorizontally(slideSpec) { -it / 4 } },
        popEnterTransition = { slideInHorizontally(slideSpec) { -it / 4 } },
        popExitTransition = { slideOutHorizontally(slideSpec) { it } },
    ) {
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
                onScan = { navController.navigate(SCANNER_ROUTE) },
            )
        }
        composable(SCANNER_ROUTE) {
            // Hand the outcome to the form below and leave, at most once: a late callback must not pop the form too.
            fun returnToForm(result: SavedStateHandle.() -> Unit) {
                if (navController.currentDestination?.route != SCANNER_ROUTE) return
                navController.previousBackStackEntry?.savedStateHandle?.result()
                navController.popBackStack()
            }
            ScannerScreen(
                onScanned = { code -> returnToForm { putScannedBarcode(code) } },
                onCameraUnavailable = { returnToForm { putCameraUnavailable() } },
                onBack = { returnToForm { } },
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
