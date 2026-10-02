package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.presentation.auth.LoginScreen
import com.example.presentation.branding.AshianSplashScreen
import com.example.presentation.auth.LoginViewModel
import com.example.presentation.creation.PropertyCreationViewModel
import com.example.presentation.creation.PropertyCreationWizardScreen
import com.example.presentation.demands.DemandDetailScreen
import com.example.presentation.demands.DemandsScreen
import com.example.presentation.demands.DemandsViewModel
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeViewModel
import com.example.presentation.navigation.AshianMelkBottomBar
import com.example.presentation.navigation.Screen
import com.example.presentation.notifications.NotificationsScreen
import com.example.presentation.notifications.NotificationsViewModel
import com.example.presentation.tasks.TasksScreen
import com.example.presentation.tasks.TasksViewModel
import com.example.presentation.profile.ProfileScreen
import com.example.presentation.profile.ProfileViewModel
import com.example.presentation.properties.PropertyDetailScreen
import com.example.presentation.properties.PropertyListScreen
import com.example.presentation.properties.PropertyListViewModel
import com.example.ui.theme.AshianMelkTheme
import com.example.security.AppAccessPolicy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            AshianMelkApp.instance.tokenStorage.screenshotProtection.collectLatest { enabled ->
                if (enabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        setContent {
            AshianMelkTheme {
                AshianMelkMainApp()
            }
        }
    }
}

@Composable
fun AshianMelkMainApp() {
    val app = AshianMelkApp.instance
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentUser = app.authRepository.getCurrentUser()
    val access = remember(currentUser.role, currentUser.capabilities) {
        AppAccessPolicy.forUser(currentUser)
    }

    // Listen for session revocation (401 refresh fail)
    LaunchedEffect(Unit) {
        app.sessionRevokedEvents.collectLatest {
            if (navController.currentDestination?.route != Screen.Login.route) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    val topLevelRoutes = listOf(
        Screen.Home.route,
        Screen.Properties.route,
        Screen.Demands.route,
        Screen.Tasks.route,
        Screen.Notifications.route,
        Screen.Profile.route
    )

    val isTopLevelScreen = currentRoute in topLevelRoutes

    // Unread counters
    val unreadNotifications by app.notificationRepository.observeUnreadCount().collectAsState(initial = 0)
    val cachedTasks by app.taskRepository.observeCachedTasks().collectAsState(initial = emptyList())
    val todayTasksCount = cachedTasks.count { !it.isCompleted }

    // Always show the branded launch animation; it routes to Home or Login.
    val startDestination = Screen.Splash.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelScreen) {
                AshianMelkBottomBar(
                    currentRoute = currentRoute,
                    unreadNotificationsCount = unreadNotifications,
                    todayTasksCount = todayTasksCount,
                    access = access,
                    onNavigate = { screen ->
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Splash.route) {
                AshianSplashScreen(
                    onFinished = {
                        val target = if (app.authRepository.isLoggedIn()) {
                            Screen.Home.route
                        } else {
                            Screen.Login.route
                        }
                        navController.navigate(target) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 1. Home / Today Screen
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel {
                    HomeViewModel(app.dashboardRepository, app.authRepository, app.networkMonitor)
                }
                HomeScreen(
                    viewModel = homeViewModel,
                    access = access,
                    onNavigateToCreateProperty = { navController.navigate(Screen.PropertyCreate.route) },
                    onNavigateToProperties = { _ -> navController.navigate(Screen.Properties.route) },
                    onNavigateToDemands = { navController.navigate(Screen.Demands.route) },
                    onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToPropertyDetail = { propertyId ->
                        navController.navigate(Screen.PropertyDetail.createRoute(propertyId))
                    },
                    onNavigateToDemandDetail = { demandId ->
                        navController.navigate(Screen.DemandDetail.createRoute(demandId))
                    }
                )
            }

            // 2. Properties List Screen
            composable(Screen.Properties.route) {
                val propertyListViewModel: PropertyListViewModel = viewModel {
                    PropertyListViewModel(app.propertyRepository, app.networkMonitor)
                }
                PropertyListScreen(
                    viewModel = propertyListViewModel,
                    canCreateProperty = access.canCreateProperty,
                    onPropertyClick = { id ->
                        navController.navigate(Screen.PropertyDetail.createRoute(id))
                    },
                    onCreatePropertyClick = {
                        navController.navigate(Screen.PropertyCreate.route)
                    }
                )
            }

            // 3. Property Detail Screen
            composable(
                route = Screen.PropertyDetail.route,
                arguments = listOf(navArgument("propertyId") { type = NavType.LongType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getLong("propertyId") ?: 0L
                PropertyDetailScreen(
                    propertyId = propertyId,
                    propertyRepository = app.propertyRepository,
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { id ->
                        navController.navigate(Screen.PropertyCreate.route)
                    }
                )
            }

            // 4. Property Creation Wizard Screen (5 Steps, Offline encrypted drafts)
            composable(Screen.PropertyCreate.route) {
                if (access.canCreateProperty) {
                    val creationViewModel: PropertyCreationViewModel = viewModel {
                        PropertyCreationViewModel(app.propertyRepository)
                    }
                    PropertyCreationWizardScreen(
                        viewModel = creationViewModel,
                        onBackClick = { navController.popBackStack() },
                        onSuccessFinish = { newId ->
                            navController.popBackStack()
                            navController.navigate(Screen.PropertyDetail.createRoute(newId))
                        }
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // 5. Demands List Screen
            composable(Screen.Demands.route) {
                val demandsViewModel: DemandsViewModel = viewModel {
                    DemandsViewModel(app.demandRepository)
                }
                DemandsScreen(
                    viewModel = demandsViewModel,
                    onDemandClick = { id ->
                        navController.navigate(Screen.DemandDetail.createRoute(id))
                    }
                )
            }

            // 6. Demand Detail Screen
            composable(
                route = Screen.DemandDetail.route,
                arguments = listOf(navArgument("demandId") { type = NavType.LongType })
            ) { backStackEntry ->
                val demandId = backStackEntry.arguments?.getLong("demandId") ?: 0L
                val demandsViewModel: DemandsViewModel = viewModel {
                    DemandsViewModel(app.demandRepository)
                }
                DemandDetailScreen(
                    demandId = demandId,
                    viewModel = demandsViewModel,
                    onBackClick = { navController.popBackStack() },
                    onPropertyClick = { propertyId ->
                        navController.navigate(Screen.PropertyDetail.createRoute(propertyId))
                    }
                )
            }

            // 7. Tasks Screen
            composable(Screen.Tasks.route) {
                val tasksViewModel: TasksViewModel = viewModel {
                    TasksViewModel(app.taskRepository)
                }
                com.example.presentation.tasks.TasksScreen(
                    viewModel = tasksViewModel
                )
            }

            // 8. Notifications Screen
            composable(Screen.Notifications.route) {
                val notificationsViewModel: NotificationsViewModel = viewModel {
                    NotificationsViewModel(app.notificationRepository)
                }
                NotificationsScreen(
                    viewModel = notificationsViewModel,
                    onDeepLink = { type, targetId ->
                        when (type) {
                            "property" -> targetId?.let { navController.navigate(Screen.PropertyDetail.createRoute(it)) }
                            "demand" -> targetId?.let { navController.navigate(Screen.DemandDetail.createRoute(it)) }
                            "task" -> navController.navigate(Screen.Tasks.route)
                            "appointment" -> navController.navigate(Screen.Home.route)
                            else -> {}
                        }
                    }
                )
            }

            // 9. Profile & Settings Screen
            composable(Screen.Profile.route) {
                val profileViewModel: ProfileViewModel = viewModel {
                    ProfileViewModel(app.authRepository, app.tokenStorage)
                }
                ProfileScreen(
                    viewModel = profileViewModel,
                    onLogoutDone = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 10. Login Screen
            composable(Screen.Login.route) {
                val loginViewModel: LoginViewModel = viewModel {
                    LoginViewModel(app.authRepository)
                }
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
