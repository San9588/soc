package com.soc.optionchain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soc.optionchain.network.ChartRepository
import com.soc.optionchain.network.HistoryApiClient
import com.soc.optionchain.network.TokenManager
import com.soc.optionchain.network.WebsocketClient
import com.soc.optionchain.screens.ChartScreen
import com.soc.optionchain.screens.HistoryScreen
import com.soc.optionchain.screens.LiveScreen
import com.soc.optionchain.screens.SettingsScreen
import com.soc.optionchain.theme.OptionChainTheme
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()
    private val wsClient = WebsocketClient()
    private val apiClient = HistoryApiClient()
    
    private lateinit val tokenManager: TokenManager
    private lateinit val chartRepo: ChartRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        tokenManager = TokenManager(this, client)
        chartRepo = ChartRepository(client, tokenManager)
        
        setContent {
            OptionChainTheme {
                MainScreen(wsClient, apiClient, chartRepo)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(wsClient: WebsocketClient, apiClient: HistoryApiClient, chartRepo: ChartRepository) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val items = listOf(
        Screen.Live,
        Screen.Chart,
        Screen.History,
        Screen.Settings
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("soc", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                items.forEach { screen ->
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = when(screen.route) {
                                    "live" -> Icons.Filled.Home
                                    "chart" -> Icons.Filled.List
                                    "history" -> Icons.Filled.List
                                    else -> Icons.Filled.Settings
                                },
                                contentDescription = screen.name
                            )
                        },
                        label = { Text(screen.name) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val title = items.find { it.route == currentRoute }?.name ?: "soc"

                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Live.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Live.route) { LiveScreen(wsClient) }
                composable(Screen.Chart.route) { ChartScreen(chartRepo) }
                composable(Screen.History.route) { HistoryScreen(apiClient) }
                composable(Screen.Settings.route) { SettingsScreen() }
            }
        }
    }
}

sealed class Screen(val route: String, val name: String) {
    object Live : Screen("live", "Live Data")
    object Chart : Screen("chart", "Trading Chart")
    object History : Screen("history", "History")
    object Settings : Screen("settings", "Settings")
}
