package com.sj9.chavara.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sj9.chavara.ui.HomeScreen
import com.sj9.chavara.ui.SplashScreen
import com.sj9.chavara.ui.LoginScreen
import com.sj9.chavara.ui.WelcomeScreen
import com.sj9.chavara.ui.SpreadsheetScreen
import com.sj9.chavara.ui.SavedScreen

import com.sj9.chavara.ui.gallery.GalleryMainScreen
import com.sj9.chavara.ui.gallery.GalleryPhotosScreen
import com.sj9.chavara.ui.gallery.GalleryVideosScreen
import com.sj9.chavara.ui.gallery.GalleryMembersScreen
import com.sj9.chavara.ui.calendar.CalendarScreen
import com.sj9.chavara.ui.calendar.CalendarEventsScreen
import com.sj9.chavara.ui.profile.ProfileMainScreen
import com.sj9.chavara.ui.profile.EditProfilePhotoScreen
import com.sj9.chavara.ui.profile.AccountSettingsScreen
import com.sj9.chavara.ui.profile.AppInformationScreen
import com.sj9.chavara.ui.profile.AppResetScreen
import com.sj9.chavara.ui.family.FamilyMembersListScreen
import com.sj9.chavara.ui.family.FamilyMemberScreen
import com.sj9.chavara.ui.family.FamilyMemberPhotoEditScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.sj9.chavara.ui.theme.*
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Brush


private enum class AuthScreen { Login, Welcome }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthAnimationController(
    onOnboardingComplete: () -> Unit // Call this when the user finishes the Welcome screen
) {
    var currentScreen by remember { mutableStateOf(AuthScreen.Login) }
    var userName by remember { mutableStateOf("") }

    // This is the shared background that will NOT change during the animation
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
                // Animation: Content slides up and out, new content slides up and in
                (slideInVertically(animationSpec = tween(600)) { height -> height } + fadeIn(animationSpec = tween(600)))
                    .togetherWith(slideOutVertically(animationSpec = tween(600)) { height -> -height } + fadeOut(animationSpec = tween(600)))
            }
        ) { targetScreen ->
            // Based on the state, we call your existing screen composables
            when (targetScreen) {
                AuthScreen.Login -> {
                    LoginScreen(onNameEntered = { name ->
                        userName = name
                        currentScreen = AuthScreen.Welcome // Trigger the animation
                    })
                }
                AuthScreen.Welcome -> {
                    WelcomeScreen(
                        userName = userName,
                        onGetStarted = onOnboardingComplete
                    )
                }
            }
        }
    }
}
object AppDestinations {
    const val SPLASH = "splash"
    const val AUTH_FLOW = "auth_flow"
    const val HOME = "home"
    const val GALLERY_MAIN = "gallery_main"
    const val GALLERY_PHOTOS = "gallery_photos"
    const val GALLERY_VIDEOS = "gallery_videos"
    const val GALLERY_MEMBERS = "gallery_members"
    const val CALENDAR = "calendar"
    const val CALENDAR_EVENTS = "calendar_events"
    const val PROFILE_MAIN = "profile_main"
    const val PROFILE_EDIT_PHOTO = "profile_edit_photo"
    const val PROFILE_EDIT = "profile_edit"
    const val ACCOUNT_SETTINGS = "account_settings"
    const val APP_INFORMATION = "app_information"
    const val APP_RESET = "app_reset"
    const val FAMILY_MEMBERS_LIST = "family_members_list"
    const val FAMILY_MEMBER_DETAIL = "family_member_detail"
    const val FAMILY_MEMBER_PHOTO_EDIT = "family_member_photo_edit"
    const val SPREADSHEET = "spreadsheet"
    const val SAVED = "saved"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()

) {
    val context = LocalContext.current
    val sharedPrefs = remember {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    // Check if onboarding is complete. Default to 'false'.
    val startDestination = AppDestinations.SPLASH
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppDestinations.SPLASH) {
            SplashScreen(
                onSplashComplete = {
                    // Check if the user has already completed the login/welcome flow
                    val isOnboardingComplete = sharedPrefs.getBoolean("onboarding_complete", false)

                    if (isOnboardingComplete) {
                        // If they have, go directly to the Home screen
                        navController.navigate(AppDestinations.HOME) {
                            popUpTo(AppDestinations.SPLASH) { inclusive = true }
                        }
                    } else {
                        // If it's their first time, go to the Login/Welcome flow
                        navController.navigate(AppDestinations.AUTH_FLOW) {
                            popUpTo(AppDestinations.SPLASH) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
            )
        }

        composable(AppDestinations.AUTH_FLOW) {
            AuthAnimationController(
                onOnboardingComplete = {
                    with(sharedPrefs.edit()) {
                        putBoolean("onboarding_complete", true)
                        apply()
                    }
                    navController.navigate(AppDestinations.HOME) {
                        popUpTo(AppDestinations.AUTH_FLOW) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestinations.HOME) {
            HomeScreen(
                onOrientationClick = {
                    navController.navigate(AppDestinations.GALLERY_MAIN)
                },
                onCalendarClick = {
                    navController.navigate(AppDestinations.CALENDAR)
                },
                onProfileClick = {
                    navController.navigate(AppDestinations.PROFILE_MAIN)
                },
                onFamilyClick = {
                    navController.navigate(AppDestinations.FAMILY_MEMBERS_LIST)
                },
                onSpreadsheetClick = {
                    navController.navigate(AppDestinations.SPREADSHEET)
                },
                modifier = Modifier
            )
        }

        composable(AppDestinations.GALLERY_MAIN) {
            GalleryMainScreen(
                onNavigateToPhotos = {
                    navController.navigate(AppDestinations.GALLERY_PHOTOS)
                },
                onNavigateToVideos = {
                    navController.navigate(AppDestinations.GALLERY_VIDEOS)
                },
                onNavigateToMembers = {
                    navController.navigate(AppDestinations.GALLERY_MEMBERS)
                },
                modifier = Modifier
            )
        }

        composable(AppDestinations.GALLERY_PHOTOS) {
            GalleryPhotosScreen(
                modifier = Modifier
            )
        }

        composable(AppDestinations.GALLERY_VIDEOS) {
            GalleryVideosScreen(
                modifier = Modifier
            )
        }

        composable(AppDestinations.GALLERY_MEMBERS) {
            GalleryMembersScreen(
                modifier = Modifier
            )
        }

        composable(AppDestinations.CALENDAR) {
            CalendarScreen(
                onDateClick = {
                    // Tell the app to go to the events screen when a date is clicked
                    navController.navigate(AppDestinations.CALENDAR_EVENTS)
                },
                modifier = Modifier
            )
        }

        composable(AppDestinations.CALENDAR_EVENTS) {
            CalendarEventsScreen(

                modifier = Modifier
            )
        }

        composable(AppDestinations.PROFILE_MAIN) {
            ProfileMainScreen(
                onEditProfileClick = {
                    navController.navigate(AppDestinations.PROFILE_EDIT_PHOTO)
                },
                onAccountSettingsClick = {
                    navController.navigate(AppDestinations.ACCOUNT_SETTINGS)
                },
                onAppInformationClick = {
                    navController.navigate(AppDestinations.APP_INFORMATION)
                },
                onResetAppClick = {
                    navController.navigate(AppDestinations.APP_RESET)
                },
                modifier = Modifier
            )
        }

        composable(AppDestinations.PROFILE_EDIT_PHOTO) {
            EditProfilePhotoScreen(

                modifier = Modifier
            )
        }

        composable(AppDestinations.ACCOUNT_SETTINGS) {
            AccountSettingsScreen(
                modifier = Modifier
            )
        }

        composable(AppDestinations.APP_INFORMATION) {
            AppInformationScreen(
                modifier = Modifier
            )
        }

        composable(AppDestinations.APP_RESET) {
            AppResetScreen(
                modifier = Modifier
            )
        }

        composable(AppDestinations.SPREADSHEET) {
            SpreadsheetScreen(
                onProcessComplete = {
                    navController.navigate(AppDestinations.SAVED)
                },
                modifier = Modifier
            )
        }

        composable(AppDestinations.SAVED) {
            SavedScreen(
                modifier = Modifier
            )
        }

        composable(AppDestinations.FAMILY_MEMBERS_LIST) {
            FamilyMembersListScreen(
                onMemberClick = { member ->
                    navController.navigate("${AppDestinations.FAMILY_MEMBER_DETAIL}/${member.id}")
                },
                onAddMemberClick = {
                    navController.navigate("${AppDestinations.FAMILY_MEMBER_DETAIL}/new")
                },
                modifier = Modifier
            )
        }

        composable("${AppDestinations.FAMILY_MEMBER_DETAIL}/{memberId}") { backStackEntry ->
            val memberIdString = backStackEntry.arguments?.getString("memberId") ?: "0"
            val memberId = if (memberIdString == "new") -1 else memberIdString.toIntOrNull() ?: 0
            val isNewMember = memberIdString == "new"

            FamilyMemberScreen(
                memberId = memberId,
                isNewMember = isNewMember,
                onEditPhotoClick = {
                    navController.navigate("${AppDestinations.FAMILY_MEMBER_PHOTO_EDIT}/$memberIdString")
                },
                onSaveComplete = {
                    navController.popBackStack()
                },
                modifier = Modifier
            )
        }

        composable("${AppDestinations.FAMILY_MEMBER_PHOTO_EDIT}/{memberId}") { backStackEntry ->
            val memberIdString = backStackEntry.arguments?.getString("memberId")
            val memberId = memberIdString?.toIntOrNull() ?: 0
            //    Example: Fetch from a ViewModel based on memberId.
            val currentPhotoUri: Uri? = null
            FamilyMemberPhotoEditScreen(
                modifier = Modifier, // Optional: if you have a specific modifier here
                memberId = memberId,
                initialPhotoUri = currentPhotoUri,
                onDoneEditing = {
                    Log.d("AppNavigation", "Photo editing done for member $memberId. Navigating back.")
                navController.popBackStack()
            }
            )
        }
    }
}
