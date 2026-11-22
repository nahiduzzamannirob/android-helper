package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.BatteryHealthScreen
import com.example.ui.screens.ChargingStatsScreen
import com.example.ui.screens.DisplayWidthScreen
import com.example.ui.screens.RefreshRateScreen
import com.example.ui.screens.WarningSettingsScreen
import com.example.ui.theme.VoltPulseTheme
import com.example.ui.viewmodel.VoltPulseViewModel
import kotlinx.coroutines.launch

const val ROUTE_HEALTH = "health"
const val ROUTE_CHARGING = "charging"
const val ROUTE_FPS = "fps"
const val ROUTE_DISPLAY = "display"
const val ROUTE_WARNINGS = "warnings"

private data class NavDestinationItem(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val NAV_ITEMS =
  listOf(
    NavDestinationItem(
      route = ROUTE_HEALTH,
      labelRes = R.string.nav_health,
      selectedIcon = Icons.Rounded.BatteryChargingFull,
      unselectedIcon = Icons.Outlined.BatteryChargingFull,
      testTag = "nav_item_health",
    ),
    NavDestinationItem(
      route = ROUTE_CHARGING,
      labelRes = R.string.nav_charging,
      selectedIcon = Icons.Rounded.Bolt,
      unselectedIcon = Icons.Outlined.Bolt,
      testTag = "nav_item_charging",
    ),
    NavDestinationItem(
      route = ROUTE_FPS,
      labelRes = R.string.nav_fps,
      selectedIcon = Icons.Rounded.Speed,
      unselectedIcon = Icons.Outlined.Speed,
      testTag = "nav_item_fps",
    ),
    NavDestinationItem(
      route = ROUTE_DISPLAY,
      labelRes = R.string.nav_display,
      selectedIcon = Icons.Rounded.DisplaySettings,
      unselectedIcon = Icons.Outlined.DisplaySettings,
      testTag = "nav_item_display",
    ),
    NavDestinationItem(
      route = ROUTE_WARNINGS,
      labelRes = R.string.nav_warnings,
      selectedIcon = Icons.Rounded.NotificationsActive,
      unselectedIcon = Icons.Outlined.NotificationsActive,
      testTag = "nav_item_warnings",
    ),
  )

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { VoltPulseRoot() }
  }
}

@Composable
fun VoltPulseRoot(viewModel: VoltPulseViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val useDarkTheme = uiState.settings.darkThemeOverride ?: isSystemInDarkTheme()
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  LaunchedEffect(uiState.settings.selectedRefreshRateHz) {
    val activity = context.findActivity() ?: return@LaunchedEffect
    val selectedHz = uiState.settings.selectedRefreshRateHz
    val targetMode = viewModel.getPreferredDisplayModeForSettings(selectedHz)
    val params = activity.window.attributes
    val targetModeId = targetMode?.modeId ?: 0
    val targetRate = targetMode?.exactHz ?: 0f
    if (params.preferredDisplayModeId != targetModeId || params.preferredRefreshRate != targetRate) {
      params.preferredDisplayModeId = targetModeId
      params.preferredRefreshRate = targetRate
      activity.window.attributes = params
      viewModel.refreshRefreshRateState()
    }
  }

  LaunchedEffect(uiState.statusToastMessage) {
    val msg = uiState.statusToastMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusToast()
    }
  }

  VoltPulseTheme(darkTheme = useDarkTheme) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HEALTH

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavigationRail = maxWidth >= 600.dp

      Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (!useNavigationRail) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              windowInsets = WindowInsets.navigationBars,
            ) {
              NAV_ITEMS.forEach { item ->
                val selected = currentRoute == item.route
                val label = stringResource(item.labelRes)
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateToTab(navController, item.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                      contentDescription = label,
                    )
                  },
                  label = { Text(label) },
                  modifier = Modifier.testTag(item.testTag),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (useNavigationRail) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
              NAV_ITEMS.forEach { item ->
                val selected = currentRoute == item.route
                val label = stringResource(item.labelRes)
                NavigationRailItem(
                  selected = selected,
                  onClick = { navigateToTab(navController, item.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                      contentDescription = label,
                    )
                  },
                  label = { Text(label) },
                  modifier = Modifier.testTag(item.testTag),
                )
              }
            }
          }

          NavHost(
            navController = navController,
            startDestination = ROUTE_HEALTH,
            modifier = Modifier.weight(1f),
          ) {
            composable(ROUTE_HEALTH) {
              BatteryHealthScreen(
                telemetry = uiState.telemetry,
                settings = uiState.settings,
                activeWarnings = uiState.activeWarnings,
                onRefresh = viewModel::refreshTelemetryNow,
                onSetCustomCapacity = viewModel::setCustomDesignCapacity,
                onDismissTestWarning = viewModel::dismissTestWarning,
                onNavigateToCharging = { navigateToTab(navController, ROUTE_CHARGING) },
                onNavigateToDisplay = { navigateToTab(navController, ROUTE_DISPLAY) },
                onNavigateToWarnings = { navigateToTab(navController, ROUTE_WARNINGS) },
              )
            }

            composable(ROUTE_CHARGING) {
              ChargingStatsScreen(
                telemetry = uiState.telemetry,
                settings = uiState.settings,
                samples = uiState.samples,
                sessions = uiState.recentSessions,
                aggregateStats = uiState.aggregateStats,
                onRefresh = viewModel::refreshTelemetryNow,
                onSaveSnapshot = viewModel::recordManualSessionSnapshot,
                onClearHistory = viewModel::clearChargingHistory,
              )
            }

            composable(ROUTE_FPS) {
              val targetForAdb =
                uiState.settings.selectedRefreshRateHz ?: uiState.refreshRateState.maxSupportedHz
              RefreshRateScreen(
                refreshRateState = uiState.refreshRateState,
                selectedRefreshRateHz = uiState.settings.selectedRefreshRateHz,
                batteryTempC = uiState.telemetry.temperatureC,
                highTempThresholdC = uiState.settings.highTempThresholdC,
                feedback = uiState.refreshRateFeedback,
                adbGrantCommand = viewModel.getAdbGrantCommand(),
                adbLockCommand = viewModel.getFpsAdbCommand(targetForAdb),
                onSelectMode = viewModel::selectRefreshRateMode,
                onLockSystemWide = viewModel::lockRefreshRateSystemWide,
                onResetSystemWide = viewModel::resetRefreshRateSystemWide,
                onLockViaRoot = viewModel::lockRefreshRateViaRoot,
                onOpenDisplaySettings = { viewModel.openSystemDisplaySettingsScreen() },
                onOpenDeveloperSettings = { viewModel.openDeveloperSettingsScreen() },
                onRefreshState = viewModel::refreshRefreshRateState,
                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
              )
            }

            composable(ROUTE_DISPLAY) {
              DisplayWidthScreen(
                displayState = uiState.displayState,
                targetSwDp = uiState.targetSmallestWidthDp,
                calculatedTargetDpi = viewModel.calculateDpiPreview(uiState.targetSmallestWidthDp),
                feedback = uiState.displayFeedback,
                adbGrantCommand = viewModel.getAdbGrantCommand(),
                adbDensityCommand = viewModel.getAdbDensityCommand(uiState.targetSmallestWidthDp),
                onTargetSwChanged = viewModel::updateTargetSmallestWidth,
                onApplyDirect = viewModel::applySmallestWidthDirect,
                onResetDirect = viewModel::resetSmallestWidthDirect,
                onApplyRoot = viewModel::applySmallestWidthViaRoot,
                onOpenDeveloperSettings = { viewModel.openDeveloperSettingsScreen() },
                onRefreshState = viewModel::refreshDisplayState,
                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
              )
            }

            composable(ROUTE_WARNINGS) {
              WarningSettingsScreen(
                settings = uiState.settings,
                onUpdateSettings = viewModel::updateSettings,
                onTestWarning = viewModel::triggerTestWarning,
                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
              )
            }
          }
        }
      }
    }
  }
}

private fun navigateToTab(navController: NavHostController, route: String) {
  navController.navigate(route) {
    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
  }
}

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
