package com.soc.optionchain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soc.optionchain.network.HistoryApiClient
import com.soc.optionchain.network.WebsocketClient
import com.soc.optionchain.screens.HistoryScreen
import com.soc.optionchain.screens.LiveScreen
import com.soc.optionchain.screens.SettingsScreen
import com.soc.optionchain.theme.OptionChainTheme

class MainActivity : ComponentActivity() {
    private val wsClient = WebsocketClient()
    private val apiClient = HistoryApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OptionChainTheme {
                MainScreen(wsClient, apiClient)
            }
        }
    }
}

@Composable
fun MainScreen(wsClient: WebsocketClient, apiClient: HistoryApiClient) {
    val navController = rememberNavController()
    
    val items = listOf(
        Screen.Live,
        Screen.History,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = when(screen.route) {
                                    "live" -> Icons.Filled.Home
                                    "history" -> Icons.Filled.List
                                    else -> Icons.Filled.Settings
                                }, 
                                contentDescription = screen.name
                            ) 
                        },
                        label = { Text(screen.name) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Live.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Live.route) {
                LiveScreen(wsClient)
            }
            composable(Screen.History.route) {
                HistoryScreen(apiClient)
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val name: String) {
    object Live : Screen("live", "Live Data")
    object History : Screen("history", "History")
    object Settings : Screen("settings", "Settings")
}
