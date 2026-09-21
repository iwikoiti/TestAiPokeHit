package com.example.testaipokehit.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.testaipokehit.PokeHitApplication
import com.example.testaipokehit.R
import com.example.testaipokehit.viewmodel.DetailViewModel
import com.example.testaipokehit.viewmodel.DetailViewModelFactory
import com.example.testaipokehit.viewmodel.FavoritesViewModel
import com.example.testaipokehit.viewmodel.FavoritesViewModelFactory
import com.example.testaipokehit.viewmodel.HomeViewModel
import com.example.testaipokehit.viewmodel.HomeViewModelFactory

private object Route {
    const val HOME = "home"
    const val FAVORITES = "favorites"
    const val DETAIL = "pokemon"
}

@Composable
fun PokeHitApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == Route.HOME || currentRoute == Route.FAVORITES

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Route.HOME,
                        onClick = {
                            navController.navigate(Route.HOME) {
                                popUpTo(navController.graph.id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_home)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Route.FAVORITES,
                        onClick = {
                            navController.navigate(Route.FAVORITES) {
                                popUpTo(navController.graph.id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.FavoriteBorder, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_favorites)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.HOME) {
                val application = LocalContext.current.applicationContext as PokeHitApplication
                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(application))
                HomeScreen(
                    viewModel = homeViewModel,
                    onOpenDetail = { id -> navController.navigate("${Route.DETAIL}/$id") }
                )
            }
            composable(Route.FAVORITES) {
                val application = LocalContext.current.applicationContext as PokeHitApplication
                val favoritesViewModel: FavoritesViewModel =
                    viewModel(factory = FavoritesViewModelFactory(application))
                FavoritesScreen(
                    viewModel = favoritesViewModel,
                    onOpenDetail = { id -> navController.navigate("${Route.DETAIL}/$id") }
                )
            }
            composable(
                route = "${Route.DETAIL}/{pokemonId}",
                arguments = listOf(navArgument("pokemonId") { type = NavType.IntType })
            ) { entry ->
                val pokemonId = entry.arguments?.getInt("pokemonId") ?: return@composable
                val application = LocalContext.current.applicationContext as PokeHitApplication
                val detailViewModel: DetailViewModel = viewModel(
                    key = "detail_$pokemonId",
                    factory = DetailViewModelFactory(application, pokemonId)
                )
                DetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}