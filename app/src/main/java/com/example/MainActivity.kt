package com.example

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.StudioViewModel
import com.example.ui.screens.PlaygroundScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThemesScreen
import com.example.ui.screens.TranslationHubScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.KeyboardThemePreset
import com.example.ui.theme.PrismKeysTheme

private enum class StudioDest(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    PLAYGROUND("studio", "Keyboard", Icons.Filled.Keyboard, Icons.Outlined.Keyboard),
    THEMES("themes", "Themes", Icons.Filled.Palette, Icons.Outlined.Palette),
    TRANSLATE("translate", "Translate", Icons.Filled.Translate, Icons.Outlined.Translate),
    VAULT("vault", "Saved", Icons.Filled.FolderSpecial, Icons.Outlined.FolderSpecial),
    SETTINGS("privacy", "Settings", Icons.Filled.Shield, Icons.Outlined.Shield),
}

class MainActivity : ComponentActivity() {
    private val viewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val launchedForMic = intent?.getBooleanExtra("request_mic_permission", false) == true

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val micRequestCounter by viewModel.micPermissionRequestedEvent.collectAsStateWithLifecycle()

            val micLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { /* handled dynamically when voice starts */ }

            LaunchedEffect(launchedForMic, micRequestCounter) {
                if (launchedForMic || micRequestCounter > 0) {
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            val darkTheme = when (settings.appDarkModeOverride) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            PrismKeysTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.themePreset == KeyboardThemePreset.DYNAMIC_YOU,
            ) {
                PrismKeysStudioShell(
                    viewModel = viewModel,
                    onShowImePicker = {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshImeStatus()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            viewModel.refreshImeStatus()
        }
    }
}

@Composable
private fun PrismKeysStudioShell(
    viewModel: StudioViewModel,
    onShowImePicker: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        StudioDest.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("nav_${dest.route}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        StudioDest.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("nav_${dest.route}"),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = StudioDest.PLAYGROUND.route,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(StudioDest.PLAYGROUND.route) {
                        PlaygroundScreen(
                            viewModel = viewModel,
                            onShowImePicker = onShowImePicker,
                        )
                    }
                    composable(StudioDest.THEMES.route) {
                        ThemesScreen(viewModel = viewModel)
                    }
                    composable(StudioDest.TRANSLATE.route) {
                        TranslationHubScreen(viewModel = viewModel)
                    }
                    composable(StudioDest.VAULT.route) {
                        VaultScreen(viewModel = viewModel)
                    }
                    composable(StudioDest.SETTINGS.route) {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
