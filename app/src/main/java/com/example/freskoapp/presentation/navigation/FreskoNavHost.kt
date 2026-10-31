package com.example.freskoapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.freskoapp.presentation.addproduct.AddProductScreen
import com.example.freskoapp.presentation.addproduct.AddProductViewModel
import com.example.freskoapp.presentation.detail.ProductDetailScreen
import com.example.freskoapp.presentation.detail.ProductDetailViewModel
import com.example.freskoapp.presentation.inventory.InventoryScreen
import com.example.freskoapp.presentation.inventory.InventoryViewModel
import com.example.freskoapp.presentation.scanner.ScannerScreen
import com.example.freskoapp.presentation.settings.SettingsScreen

// Screens pass results back through the NavBackStackEntry's own SavedStateHandle. A ViewModel's
// createSavedStateHandle() is a DIFFERENT handle (one per ViewModel key), so a ViewModel never sees them.
// The route watches the entry's handle instead: when [key] changes, [take] reads and clears the result,
// and [onResult] passes it to the ViewModel. Runs while the route is on screen; a pending result waits in the handle.
@Composable
private fun <T : Any> NavBackStackEntry.ConsumeResult(key: String, take: SavedStateHandle.() -> T?, onResult: (T) -> Unit) {
    val handle = savedStateHandle
    val currentOnResult by rememberUpdatedState(onResult)
    LaunchedEffect(handle) {
        handle.getStateFlow<Any?>(key, null).collect { handle.take()?.let(currentOnResult) }
    }
}

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
        ) { entry ->
            val vm: InventoryViewModel = viewModel(factory = InventoryViewModel.Factory)
            entry.ConsumeResult(REMOVAL_ID_KEY, { takeRemoval() }, vm::onRemoval)
            entry.ConsumeResult(LOT_MERGED_KEY, { takeFlag(LOT_MERGED_KEY) }) { vm.onLotMerged() }
            InventoryScreen(
                onAddClick = { navController.navigate(addProductRoute()) },
                onSettingsClick = { navController.navigate("settings") },
                onProductClick = { id -> navController.navigate(productDetailRoute(id)) },
                viewModel = vm,
            )
        }
        composable(
            route = PRODUCT_DETAIL_ROUTE,
            arguments = listOf(navArgument(PRODUCT_ID_ARG) { type = NavType.LongType }),
        ) { entry ->
            val vm: ProductDetailViewModel = viewModel(factory = ProductDetailViewModel.Factory)
            entry.ConsumeResult(PRODUCT_UNAVAILABLE_KEY, { takeFlag(PRODUCT_UNAVAILABLE_KEY) }) { vm.onProductUnavailable() }
            ProductDetailScreen(
                viewModel = vm,
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
        ) { entry ->
            val vm: AddProductViewModel = viewModel(factory = AddProductViewModel.Factory)
            entry.ConsumeResult(SCANNED_BARCODE_KEY, { takeScannedBarcode() }, vm::onBarcodeScanned)
            entry.ConsumeResult(CAMERA_UNAVAILABLE_KEY, { takeFlag(CAMERA_UNAVAILABLE_KEY) }) { vm.onCameraUnavailable() }
            AddProductScreen(
                viewModel = vm,
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
