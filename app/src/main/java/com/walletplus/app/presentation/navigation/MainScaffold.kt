package com.walletplus.app.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.walletplus.app.core.ui.components.LocalMessenger
import com.walletplus.app.core.ui.components.Messenger
import com.walletplus.app.core.ui.theme.Motion
import com.walletplus.app.presentation.analysis.AnalysisScreen
import com.walletplus.app.presentation.categories.CategoriesScreen
import com.walletplus.app.presentation.dashboard.DashboardScreen
import com.walletplus.app.presentation.planning.PlanningScreen
import com.walletplus.app.presentation.settings.SettingsScreen
import com.walletplus.app.presentation.transactions.TransactionEditorSheet
import com.walletplus.app.presentation.transactions.TransactionsScreen
import com.walletplus.app.presentation.tour.FirstUseTourSteps
import com.walletplus.app.presentation.tour.GuidedTour
import com.walletplus.app.presentation.tour.LocalTourTargets
import com.walletplus.app.presentation.tour.TourIds
import com.walletplus.app.presentation.tour.TourTargets
import com.walletplus.app.presentation.tour.TourViewModel
import com.walletplus.app.presentation.tour.tourTarget
import com.walletplus.app.core.di.walletViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletplus.domain.model.TransactionType
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object TransactionsRoute
@Serializable data class PlanRoute(val tab: Int = 0)
@Serializable data object AnalysisRoute
@Serializable data object SettingsRoute
@Serializable data object CategoriesRoute

private data class TopLevel(
    val tourId: String?,
    val label: String,
    val route: Any,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
    val matches: (androidx.navigation.NavDestination) -> Boolean,
)

private val topLevels = listOf(
    TopLevel(null, "Inicio", HomeRoute, Icons.Rounded.Home, Icons.Outlined.Home) { it.hasRoute<HomeRoute>() },
    TopLevel(TourIds.TRANSACTIONS, "Movimientos", TransactionsRoute, Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong) { it.hasRoute<TransactionsRoute>() },
    TopLevel(TourIds.PLAN, "Planificar", PlanRoute(), Icons.Rounded.TrackChanges, Icons.Outlined.TrackChanges) { it.hasRoute<PlanRoute>() },
    TopLevel(TourIds.ANALYSIS, "Análisis", AnalysisRoute, Icons.Rounded.Insights, Icons.Outlined.Insights) { it.hasRoute<AnalysisRoute>() },
)

/** Petición para abrir el editor de movimientos. id = 0 crea uno nuevo. */
data class EditorRequest(val id: Long = 0, val type: TransactionType = TransactionType.EXPENSE, val nonce: Long = System.nanoTime())

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = destination == null || topLevels.any { it.matches(destination) }

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messenger = remember { Messenger(snackbarHost, scope) }
    var editor by remember { mutableStateOf<EditorRequest?>(null) }
    val tourTargets = remember { TourTargets() }
    val tourViewModel = walletViewModel { TourViewModel(it.settings, it.updatePreferences) }
    val showTour by tourViewModel.shouldShow.collectAsStateWithLifecycle()

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openEditor: (Long, TransactionType) -> Unit = { id, type -> editor = EditorRequest(id, type) }

    CompositionLocalProvider(LocalMessenger provides messenger, LocalTourTargets provides tourTargets) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                SnackbarHost(snackbarHost) { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        actionColor = MaterialTheme.colorScheme.inversePrimary,
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(Motion.emphasized()) { it } + fadeIn(),
                    exit = slideOutVertically(Motion.emphasized()) { it } + fadeOut(),
                ) {
                    WalletBottomBar(
                        isSelected = { item -> destination?.let(item.matches) ?: (item.route == HomeRoute) },
                        onSelect = { navigateTopLevel(it.route) },
                        onAdd = { openEditor(0, TransactionType.EXPENSE) },
                    )
                }
            },
        ) { padding ->
          Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeThroughIn() },
                exitTransition = { fadeOut(Motion.emphasized(Motion.SHORT)) },
                popEnterTransition = { fadeThroughIn() },
                popExitTransition = { fadeOut(Motion.emphasized(Motion.SHORT)) },
            ) {
                composable<HomeRoute> {
                    DashboardScreen(
                        contentPadding = padding,
                        onAddTransaction = openEditor,
                        onEditTransaction = { openEditor(it, TransactionType.EXPENSE) },
                        onOpenTransactions = { navigateTopLevel(TransactionsRoute) },
                        onOpenPlan = { tab -> navigateTopLevel(PlanRoute(tab)) },
                        onOpenAnalysis = { navigateTopLevel(AnalysisRoute) },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                    )
                }
                composable<TransactionsRoute> {
                    TransactionsScreen(
                        contentPadding = padding,
                        onEditTransaction = { openEditor(it, TransactionType.EXPENSE) },
                        onAddTransaction = { openEditor(0, TransactionType.EXPENSE) },
                    )
                }
                composable<PlanRoute> { entry ->
                    val route = entry.toRoute<PlanRoute>().tab
                    PlanningScreen(contentPadding = padding, initialTab = route)
                }
                composable<AnalysisRoute> {
                    AnalysisScreen(contentPadding = padding, onAddTransaction = { openEditor(0, TransactionType.EXPENSE) })
                }
                composable<SettingsRoute>(
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, Motion.emphasized()) + fadeIn() },
                    exitTransition = { fadeOut(Motion.emphasized(Motion.SHORT)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, Motion.emphasized()) + fadeOut() },
                ) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onReplayTour = { navController.popBackStack(HomeRoute, inclusive = false) },
                        onOpenCategories = { navController.navigate(CategoriesRoute) },
                    )
                }
                composable<CategoriesRoute>(
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, Motion.emphasized()) + fadeIn() },
                    exitTransition = { fadeOut(Motion.emphasized(Motion.SHORT)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, Motion.emphasized()) + fadeOut() },
                ) {
                    CategoriesScreen(onBack = { navController.popBackStack() })
                }
            }
            // Fondo bajo la barra de estado para que el contenido no se mezcle con los íconos.
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f)),
            )
          }
        }

        editor?.let { request ->
            TransactionEditorSheet(
                request = request,
                onDismiss = { editor = null },
            )
        }

        // Recorrido guiado de primer uso: solo sobre Inicio y si no hay otra hoja abierta.
        val onHome = destination?.hasRoute<HomeRoute>() ?: false
        if (showTour == true && onHome && editor == null) {
            GuidedTour(steps = FirstUseTourSteps, targets = tourTargets, onFinish = tourViewModel::finish)
        }
    }
}

private fun fadeThroughIn() =
    fadeIn(Motion.emphasized(Motion.MEDIUM)) + scaleIn(Motion.emphasized(Motion.MEDIUM), initialScale = 0.98f)

@Composable
private fun WalletBottomBar(
    isSelected: (TopLevel) -> Boolean,
    onSelect: (TopLevel) -> Unit,
    onAdd: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            topLevels.take(2).forEach { item -> BarItem(item, isSelected(item)) { onSelect(item) } }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onAdd()
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .tourTarget(TourIds.ADD)
                        .size(56.dp)
                        .shadow(10.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)
                        .semantics { role = Role.Button },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, contentDescription = "Agregar movimiento", modifier = Modifier.size(28.dp))
                    }
                }
            }
            topLevels.drop(2).forEach { item -> BarItem(item, isSelected(item)) { onSelect(item) } }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BarItem(item: TopLevel, selected: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "barColor",
    )
    val indicator by animateFloatAsState(if (selected) 1f else 0f, Motion.bouncy(), label = "indicator")
    Column(
        modifier = Modifier
            .weight(1f)
            .then(if (item.tourId != null) Modifier.tourTarget(item.tourId) else Modifier)
            .clip(MaterialTheme.shapes.medium)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.Tab
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f * indicator),
            contentColor = color,
        ) {
            Box(Modifier.padding(horizontal = 18.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
                Icon(
                    if (selected) item.selectedIcon else item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).scale(0.92f + 0.08f * indicator),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(item.label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}
