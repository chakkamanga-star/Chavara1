package com.sj9.chavara.ui.utils

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Screen size categories for responsive design
 */
enum class ScreenSize {
    SMALL,   // < 360dp width
    MEDIUM,  // 360dp - 480dp width
    LARGE    // > 480dp width
}

/**
 * Get the current screen size category
 */
@Composable
fun getScreenSize(): ScreenSize {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    return when {
        screenWidth < 360.dp -> ScreenSize.SMALL
        screenWidth <= 480.dp -> ScreenSize.MEDIUM
        else -> ScreenSize.LARGE
    }
}

/**
 * Get responsive spacing based on screen size
 */
@Composable
fun getResponsiveSpacing(small: Dp = 8.dp, medium: Dp = 12.dp, large: Dp = 16.dp): Dp {
    return when (getScreenSize()) {
        ScreenSize.SMALL -> small
        ScreenSize.MEDIUM -> medium
        ScreenSize.LARGE -> large
    }
}

/**
 * Get responsive padding based on screen size
 */
@Composable
fun getResponsivePadding(small: Dp = 16.dp, medium: Dp = 20.dp, large: Dp = 24.dp): Dp {
    return when (getScreenSize()) {
        ScreenSize.SMALL -> small
        ScreenSize.MEDIUM -> medium
        ScreenSize.LARGE -> large
    }
}

/**
 * Get responsive font size based on screen width
 */
@Composable
fun responsiveFontSize(baseSizeSp: Float): TextUnit {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    return (baseSizeSp * screenWidth / 360f).sp
}

/**
 * Get responsive icon size based on screen size
 */
@Composable
fun getResponsiveIconSize(small: Dp = 48.dp, medium: Dp = 56.dp, large: Dp = 64.dp): Dp {
    return when (getScreenSize()) {
        ScreenSize.SMALL -> small
        ScreenSize.MEDIUM -> medium
        ScreenSize.LARGE -> large
    }
}

/**
 * Get responsive button height based on screen size
 */
@Composable
fun getResponsiveButtonHeight(small: Dp = 40.dp, medium: Dp = 48.dp, large: Dp = 56.dp): Dp {
    return when (getScreenSize()) {
        ScreenSize.SMALL -> small
        ScreenSize.MEDIUM -> medium
        ScreenSize.LARGE -> large
    }
}

/**
 * Get responsive corner radius based on screen size
 */
@Composable
fun getResponsiveCornerRadius(small: Dp = 12.dp, medium: Dp = 16.dp, large: Dp = 20.dp): Dp {
    return when (getScreenSize()) {
        ScreenSize.SMALL -> small
        ScreenSize.MEDIUM -> medium
        ScreenSize.LARGE -> large
    }
}

/**
 * Calculate responsive dimensions based on screen constraints
 */
@Composable
fun BoxWithConstraintsScope.getResponsiveDimensions(): ResponsiveDimensions {
    val screenSize = getScreenSize()
    return ResponsiveDimensions(
        screenWidth = maxWidth,
        screenHeight = maxHeight,
        screenSize = screenSize,
        horizontalPadding = getResponsivePadding(),
        verticalSpacing = getResponsiveSpacing(),
        iconSize = getResponsiveIconSize(),
        buttonHeight = getResponsiveButtonHeight(),
        cornerRadius = getResponsiveCornerRadius()
    )
}

/**
 * Data class to hold responsive dimension values
 */
data class ResponsiveDimensions(
    val screenWidth: Dp,
    val screenHeight: Dp,
    val screenSize: ScreenSize,
    val horizontalPadding: Dp,
    val verticalSpacing: Dp,
    val iconSize: Dp,
    val buttonHeight: Dp,
    val cornerRadius: Dp
)