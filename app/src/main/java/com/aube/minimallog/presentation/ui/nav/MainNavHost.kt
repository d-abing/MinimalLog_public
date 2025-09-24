package com.aube.minimallog.presentation.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aube.minimallog.presentation.ui.screens.AddScreen
import com.aube.minimallog.presentation.ui.screens.HomeScreen
import com.aube.minimallog.presentation.ui.screens.LogScreen
import com.aube.minimallog.presentation.ui.screens.MemoryScreen
import com.aube.minimallog.presentation.ui.screens.SettingsScreen

@Composable
fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(navController, startDestination = Screen.Home.route) {
        composable(
            route = Screen.Home.route,
        ) {
            HomeScreen(modifier) {
                navController.navigate("memory/$it")
            }
        }
        composable(
            route = Screen.Log.route,
        ) {
            LogScreen(modifier) {
                navController.navigate("memory/$it")
            }
        }
        composable(
            route = Screen.Memory.route,
            arguments = listOf(navArgument("memoryId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            MemoryScreen(
                modifier = modifier,
                onDelete = {
                    navController.navigate(Screen.Log.route)
                },
                onEdit = {
                    navController.navigate("add?editId=${it.id}")
                }
            )
        }
        composable(
            route = Screen.Add.route,
            arguments = listOf(navArgument("editId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            AddScreen(
                modifier = modifier,
                onSavedNavigateBack = {
                    navController.navigate(Screen.Log.route)
                }
            )
        }
        composable(
            route = Screen.Settings.route,
        ) {
            SettingsScreen(modifier)
        }
    }
}
