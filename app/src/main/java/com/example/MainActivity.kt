package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.model.GamepadProfile
import com.example.ui.screens.GamepadTesterScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LayoutEditorScreen
import com.example.ui.screens.TechReportScreen
import com.example.ui.theme.StealthBackground
import com.example.ui.theme.WarPadTheme
import com.example.ui.viewmodel.HomeViewModel

enum class AppScreen {
    HOME,
    LAYOUT_EDITOR,
    TECH_REPORT,
    TESTER
}

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WarPadTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
                var editingProfile by remember { mutableStateOf<GamepadProfile?>(null) }
                val orientationOption by viewModel.orientationOption.collectAsState()

                LaunchedEffect(orientationOption) {
                    requestedOrientation = orientationOption.activityOrientation
                }

                // Request Notification Permission on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { _ -> }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasNotificationPerm = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasNotificationPerm) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    containerColor = StealthBackground
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.HOME -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToEditor = { profile ->
                                        editingProfile = profile
                                        com.example.service.GamepadOverlayService.instance?.setTemporaryHudHidden(true)
                                        currentScreen = AppScreen.LAYOUT_EDITOR
                                    },
                                    onNavigateToTechReport = {
                                        com.example.service.GamepadOverlayService.instance?.setTemporaryHudHidden(true)
                                        currentScreen = AppScreen.TECH_REPORT
                                    },
                                    onNavigateToTester = {
                                        com.example.service.GamepadOverlayService.instance?.setTemporaryHudHidden(true)
                                        currentScreen = AppScreen.TESTER
                                    }
                                )
                            }
                            AppScreen.LAYOUT_EDITOR -> {
                                BackHandler {
                                    com.example.service.GamepadOverlayService.instance?.setTemporaryHudHidden(false)
                                    currentScreen = AppScreen.HOME
                                }
                                editingProfile?.let { profile ->
                                    LayoutEditorScreen(
                                        initialProfile = profile,
                                        onSaveProfile = { updated ->
                                            viewModel.updateSelectedProfile(updated)
                                        },
                                        onResetProfile = {
                                            viewModel.resetToDefaults()
                                        },
                                        onNavigateBack = {
                                            com.example.service.GamepadOverlayService.instance?.setTemporaryHudHidden(false)
                                            currentScreen = AppScreen.HOME
                                        }
                                    )
                                }
                            }
                            AppScreen.TECH_REPORT -> {
                                BackHandler {
                                    com.example.service.GamepadOverlayService.instance?.setTemporaryHudHidden(false)
                                    currentScreen = AppScreen.HOME
                                }
                                TechReportScreen(
                                    onNavigateBack = {
                                        com.example.service.GamepadOverlayService.instance?.setTemporaryHudHidden(false)
                                        currentScreen = AppScreen.HOME
                                    }
                                )
                            }
                            AppScreen.TESTER -> {
                                BackHandler {
                                    com.example.service.GamepadOverlayService.instance?.setTemporaryHudHidden(false)
                                    currentScreen = AppScreen.HOME
                                }
                                GamepadTesterScreen(
                                    onNavigateBack = {
                                        com.example.service.GamepadOverlayService.instance?.setTemporaryHudHidden(false)
                                        currentScreen = AppScreen.HOME
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkOverlayPermission()
        viewModel.checkAccessibilityPermission()
        viewModel.checkShizukuStatus(autoConnect = true)
        com.example.service.GamepadOverlayService.instance?.setTemporaryHudHidden(true)
    }

    override fun onPause() {
        super.onPause()
        com.example.service.GamepadOverlayService.instance?.setTemporaryHudHidden(false)
    }
}
