package com.sj9.chavara.navigation

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.sj9.chavara.data.repository.ChavaraRepository
import com.sj9.chavara.ui.*
import com.sj9.chavara.ui.calendar.CalendarEventsScreen
import com.sj9.chavara.ui.calendar.CalendarScreen
import com.sj9.chavara.ui.family.FamilyMemberPhotoEditScreen
import com.sj9.chavara.ui.family.FamilyMemberScreen
import com.sj9.chavara.ui.family.FamilyMembersListScreen
import com.sj9.chavara.ui.gallery.GalleryMainScreen
import com.sj9.chavara.ui.gallery.GalleryMembersScreen
import com.sj9.chavara.ui.gallery.GalleryPhotosScreen
import com.sj9.chavara.ui.gallery.GalleryVideosScreen
import com.sj9.chavara.ui.profile.*
import com.sj9.chavara.ui.theme.*
import com.sj9.chavara.viewmodel.*

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(private val repository: ChavaraRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MyViewModel::class.java) -> MyViewModel(repository) as T
            modelClass.isAssignableFrom(FamilyMembersViewModel::class.java) -> FamilyMembersViewModel(repository) as T
            modelClass.isAssignableFrom(GalleryViewModel::class.java) -> GalleryViewModel(repository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(repository) as T
            modelClass.isAssignableFrom(CalendarViewModel::class.java) -> CalendarViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

private enum class AuthScreen { Login, Welcome }

object AppDestinations {
    const val SPLASH = "splash"
    const val AUTH_FLOW = "auth_flow"
    const val HOME = "home"
    const val SPREADSHEET = "spreadsheet"
    const val SAVED = "saved"
    const val GALLERY_ROUTE = "gallery_route"
    const val CALENDAR_ROUTE = "calendar_route"
    const val PROFILE_ROUTE = "profile_route"
    const val FAMILY_ROUTE = "family_route"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val repository = remember { ChavaraRepository(context) }
    val viewModelFactory = remember { ViewModelFactory(repository) }
    val sharedPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val startDestination = if (sharedPrefs.getBoolean("onboarding_complete", false)) AppDestinations.HOME else AppDestinations.SPLASH

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppDestinations.SPLASH) {
            SplashScreen(onSplashComplete = {
                val nextRoute = if (sharedPrefs.getBoolean("onboarding_complete", false)) AppDestinations.HOME else AppDestinations.AUTH_FLOW
                navController.navigate(nextRoute) { popUpTo(AppDestinations.SPLASH) { inclusive = true } }
            })
        }

        composable(AppDestinations.AUTH_FLOW) {
            AuthAnimationController(onOnboardingComplete = {
                sharedPrefs.edit { putBoolean("onboarding_complete", true) }
                navController.navigate(AppDestinations.HOME) { popUpTo(AppDestinations.AUTH_FLOW) { inclusive = true } }
            })
        }

        composable(AppDestinations.HOME) {
            // FIX: HomeScreen does not take a ViewModel. It creates its own repository.
            HomeScreen(
                onOrientationClick = { navController.navigate(AppDestinations.GALLERY_ROUTE) },
                onCalendarClick = { navController.navigate(AppDestinations.CALENDAR_ROUTE) },
                onProfileClick = { navController.navigate(AppDestinations.PROFILE_ROUTE) },
                onFamilyClick = { navController.navigate(AppDestinations.FAMILY_ROUTE) },
                onSpreadsheetClick = { navController.navigate(AppDestinations.SPREADSHEET) }
            )
        }

        composable(AppDestinations.SPREADSHEET) {
            val spreadsheetViewModel: SpreadsheetViewModel = viewModel()
            SpreadsheetScreen(
                viewModel = spreadsheetViewModel,
                onProcessComplete = { navController.navigate(AppDestinations.SAVED) }
            )
        }

        composable(AppDestinations.SAVED) { SavedScreen() }

        navigation(startDestination = "calendar_main", route = AppDestinations.CALENDAR_ROUTE) {
            composable("calendar_main") {
                // FIX: CalendarScreen expects a repository, not a ViewModel.
                CalendarScreen(
                    repository = repository,
                    onDateClick = { navController.navigate("calendar_events") }
                )
            }
            composable("calendar_events") { CalendarEventsScreen() }
        }

        navigation(startDestination = "gallery_main", route = AppDestinations.GALLERY_ROUTE) {
            composable("gallery_main") {
                GalleryMainScreen(
                    onNavigateToPhotos = { navController.navigate("gallery_photos") },
                    onNavigateToVideos = { navController.navigate("gallery_videos") },
                    onNavigateToMembers = { navController.navigate("gallery_members") }
                )
            }
            composable("gallery_photos") {
                val parentEntry = remember(it) { navController.getBackStackEntry(AppDestinations.GALLERY_ROUTE) }
                val galleryViewModel: GalleryViewModel = viewModel(viewModelStoreOwner = parentEntry, factory = viewModelFactory)
                GalleryPhotosScreen(viewModel = galleryViewModel)
            }
            composable("gallery_videos") {
                val parentEntry = remember(it) { navController.getBackStackEntry(AppDestinations.GALLERY_ROUTE) }
                val galleryViewModel: GalleryViewModel = viewModel(viewModelStoreOwner = parentEntry, factory = viewModelFactory)
                GalleryVideosScreen(viewModel = galleryViewModel)
            }
            composable("gallery_members") {
                val parentEntry = remember(it) { navController.getBackStackEntry(AppDestinations.GALLERY_ROUTE) }
                val galleryViewModel: GalleryViewModel = viewModel(viewModelStoreOwner = parentEntry, factory = viewModelFactory)
                GalleryMembersScreen(viewModel = galleryViewModel)
            }
        }

        navigation(startDestination = "family_list", route = AppDestinations.FAMILY_ROUTE) {
            composable("family_list") {
                val parentEntry = remember(it) { navController.getBackStackEntry(AppDestinations.FAMILY_ROUTE) }
                val familyViewModel: FamilyMembersViewModel = viewModel(viewModelStoreOwner = parentEntry, factory = viewModelFactory)
                FamilyMembersListScreen(
                    viewModel = familyViewModel,
                    onMemberClick = { member -> navController.navigate("family_detail/${member.id}") },
                    onAddMemberClick = { navController.navigate("family_detail/new") }
                )
            }
            composable("family_detail/{memberId}") { backStackEntry ->
                val memberIdString = backStackEntry.arguments?.getString("memberId") ?: "0"
                val isNewMember = memberIdString == "new"
                // FIX: FamilyMemberScreen does not take a ViewModel. It creates its own repository.
                FamilyMemberScreen(
                    isNewMember = isNewMember,
                    memberId = if (isNewMember) -1 else memberIdString.toIntOrNull() ?: 0,
                    onEditPhotoClick = { navController.navigate("family_photo_edit/$memberIdString") },
                    onSaveComplete = { navController.popBackStack() }
                )
            }
            composable("family_photo_edit/{memberId}") { backStackEntry ->
                val memberId = backStackEntry.arguments?.getString("memberId")?.toIntOrNull() ?: 0
                FamilyMemberPhotoEditScreen(
                    memberId = memberId,
                    onDoneEditing = { navController.popBackStack() }
                )
            }
        }

        navigation(startDestination = "profile_main", route = AppDestinations.PROFILE_ROUTE) {
            composable("profile_main") {
                val parentEntry = remember(it) { navController.getBackStackEntry(AppDestinations.PROFILE_ROUTE) }
                val profileViewModel: ProfileViewModel = viewModel(viewModelStoreOwner = parentEntry, factory = viewModelFactory)
                ProfileMainScreen(
                    viewModel = profileViewModel,
                    onAccountSettingsClick = { navController.navigate("account_settings") },
                    onAppInformationClick = { navController.navigate("app_information") },
                    onResetAppClick = { navController.navigate("app_reset") }
                )
            }
            composable("account_settings") { AccountSettingsScreen() }
            composable("app_information") { AppInformationScreen() }
            composable("app_reset") {
                val parentEntry = remember(it) { navController.getBackStackEntry(AppDestinations.PROFILE_ROUTE) }
                val profileViewModel: ProfileViewModel = viewModel(viewModelStoreOwner = parentEntry, factory = viewModelFactory)
                AppResetScreen(
                    viewModel = profileViewModel,
                    onResetComplete = {
                        navController.navigate(AppDestinations.AUTH_FLOW) { popUpTo(AppDestinations.HOME) { inclusive = true } }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AuthAnimationController(onOnboardingComplete: () -> Unit) {
    var currentScreen by remember { mutableStateOf(AuthScreen.Login) }
    var userName by remember { mutableStateOf("") }
    val backgroundBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.2222f to LoginGradientStart,
            0.3565f to LoginGradientMid1,
            0.5027f to LoginGradientMid2,
            0.6992f to LoginGradientEnd
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentScreen,
            label = "AuthScreenAnimation",
            transitionSpec = {
                (slideInVertically(animationSpec = tween(600)) { height -> height } + fadeIn(animationSpec = tween(600)))
                    .togetherWith(slideOutVertically(animationSpec = tween(600)) { height -> -height } + fadeOut(animationSpec = tween(600)))
            }
        ) { targetScreen ->
            when (targetScreen) {
                AuthScreen.Login -> LoginScreen(onNameEntered = { name ->
                    userName = name
                    currentScreen = AuthScreen.Welcome
                })
                AuthScreen.Welcome -> WelcomeScreen(
                    userName = userName,
                    onGetStarted = onOnboardingComplete
                )
            }
        }
    }
}