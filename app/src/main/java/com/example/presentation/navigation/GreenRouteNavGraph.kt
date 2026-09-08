package com.example.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.GreenRouteAppContainer
import com.example.domain.model.UserRole
import com.example.presentation.auth.AuthScreen
import com.example.presentation.auth.AuthViewModel
import com.example.presentation.auth.RoleSelectionScreen
import com.example.presentation.components.LanguageSelectorDialog
import com.example.presentation.household.HouseholdDashboardScreen
import com.example.presentation.household.HouseholdViewModel
import com.example.presentation.household.MakeAvailabilityScreen
import com.example.presentation.kawadiwala.KawadiwalaMapScreen
import com.example.presentation.kawadiwala.KawadiwalaRequestsScreen
import com.example.presentation.kawadiwala.KawadiwalaViewModel
import com.example.presentation.kawadiwala.tracking.BookedLocationTrackingScreen
import com.example.presentation.kawadiwala.tracking.KawadiwalaTrackingViewModel
import com.example.presentation.market.MarketDetailsScreen
import com.example.presentation.market.MarketViewModel
import com.example.presentation.notifications.NotificationsScreen
import com.example.presentation.profile.ProfileScreen
import com.example.presentation.splash.SplashScreen

@Composable
fun GreenRouteNavGraph(
    appContainer: GreenRouteAppContainer,
    onLanguageChanged: (String) -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val authViewModel = remember { AuthViewModel(appContainer.authRepository) }
    val currentUser by authViewModel.currentUser.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }

    val householdViewModel = remember(currentUser?.id) {
        HouseholdViewModel(
            householdId = currentUser?.id ?: "demo_household_1",
            householdName = currentUser?.name ?: "Ramesh Sharma (Household)",
            wasteRepository = appContainer.wasteRepository,
            requestRepository = appContainer.requestRepository,
            locationHelper = appContainer.locationHelper
        )
    }

    val kawadiwalaViewModel = remember(currentUser?.id) {
        KawadiwalaViewModel(
            kawadiwalaId = currentUser?.id ?: "demo_kawadiwala_1",
            kawadiwalaName = currentUser?.name ?: "Subhash Mondal (Kawadiwala)",
            wasteRepository = appContainer.wasteRepository,
            requestRepository = appContainer.requestRepository,
            locationHelper = appContainer.locationHelper
        )
    }

    val marketViewModel = remember { MarketViewModel(appContainer.marketRepository) }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                currentUser = currentUser,
                onNavigateToHome = { role ->
                    val destination = if (role == UserRole.KAWADIWALA) Screen.KawadiwalaMap.route else Screen.HouseholdDashboard.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                authViewModel = authViewModel,
                onAuthSuccess = { role ->
                    val destination = if (role == UserRole.KAWADIWALA) Screen.KawadiwalaMap.route else Screen.HouseholdDashboard.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onOpenLanguage = { showLanguageDialog = true }
            )
        }

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                currentRole = currentUser?.role ?: UserRole.HOUSEHOLD,
                onRoleSelected = { selectedRole ->
                    authViewModel.switchRole(selectedRole)
                    val destination = if (selectedRole == UserRole.KAWADIWALA) Screen.KawadiwalaMap.route else Screen.HouseholdDashboard.route
                    navController.navigate(destination) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                },
                onOpenLanguage = { showLanguageDialog = true }
            )
        }

        // HOUSEHOLD DESTINATIONS
        composable(Screen.HouseholdDashboard.route) {
            HouseholdDashboardScreen(
                householdViewModel = householdViewModel,
                onNavigateToMakeAvailable = { navController.navigate(Screen.MakeAvailability.route) },
                onNavigateToMarket = { navController.navigate(Screen.MarketDetails.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onOpenLanguage = { showLanguageDialog = true }
            )
        }

        composable(Screen.MakeAvailability.route) {
            MakeAvailabilityScreen(
                householdViewModel = householdViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSubmittedSuccessfully = { navController.popBackStack() }
            )
        }

        // KAWADIWALA DESTINATIONS
        composable(Screen.KawadiwalaMap.route) {
            KawadiwalaMapScreen(
                kawadiwalaViewModel = kawadiwalaViewModel,
                onNavigateToRequests = { navController.navigate(Screen.KawadiwalaRequests.route) },
                onNavigateToMarket = { navController.navigate(Screen.MarketDetails.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onOpenLanguage = { showLanguageDialog = true },
                onNavigateToTracking = { availabilityId ->
                    navController.navigate(Screen.BookedLocationTracking.createRoute(availabilityId))
                }
            )
        }

        composable(Screen.KawadiwalaRequests.route) {
            KawadiwalaRequestsScreen(
                kawadiwalaViewModel = kawadiwalaViewModel,
                onNavigateToMap = { navController.navigate(Screen.KawadiwalaMap.route) },
                onNavigateToMarket = { navController.navigate(Screen.MarketDetails.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onOpenLanguage = { showLanguageDialog = true },
                onNavigateToTracking = { availabilityId ->
                    navController.navigate(Screen.BookedLocationTracking.createRoute(availabilityId))
                }
            )
        }

        composable(
            route = Screen.BookedLocationTracking.route,
            arguments = listOf(navArgument("availabilityId") { type = NavType.StringType })
        ) { backStackEntry ->
            val availabilityId = backStackEntry.arguments?.getString("availabilityId") ?: ""
            val trackingViewModel: KawadiwalaTrackingViewModel = viewModel(
                key = "tracking_$availabilityId",
                factory = KawadiwalaTrackingViewModel.provideFactory(
                    availabilityId = availabilityId,
                    kawadiwalaId = currentUser?.id ?: "",
                    wasteRepository = appContainer.wasteRepository,
                    routeRepository = appContainer.routeRepository,
                    locationHelper = appContainer.locationHelper,
                    requestRepository = appContainer.requestRepository
                )
            )
            BookedLocationTrackingScreen(
                viewModel = trackingViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // COMMON DESTINATIONS
        composable(Screen.MarketDetails.route) {
            MarketDetailsScreen(
                marketViewModel = marketViewModel,
                userRole = currentUser?.role ?: UserRole.HOUSEHOLD,
                onNavigateBack = { navController.popBackStack() },
                onOpenLanguage = { showLanguageDialog = true }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(
                notificationRepository = appContainer.notificationRepository,
                userId = currentUser?.id ?: "all",
                onNavigateBack = { navController.popBackStack() },
                onOpenLanguage = { showLanguageDialog = true }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                user = currentUser,
                onSwitchRole = { navController.navigate(Screen.RoleSelection.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenLanguage = { showLanguageDialog = true },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguageCode = appContainer.languagePreferences.currentLanguageCode,
            onLanguageSelected = { langCode ->
                appContainer.languagePreferences.currentLanguageCode = langCode
                onLanguageChanged(langCode)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}
