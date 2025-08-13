package com.sj9.chavara.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ChavaraTheme
import androidx.compose.ui.unit.dp
import com.sj9.chavara.ui.utils.*
import com.sj9.chavara.data.repository.ChavaraRepository
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.ui.components.AsyncMemberImage

@Composable
fun HomeScreen(
    onOrientationClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onFamilyClick: () -> Unit = {},
    onSpreadsheetClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Make repository initialization safe with error handling
    val repository = remember {
        try {
            ChavaraRepository(context)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Get today's birthday members safely
    val todaysBirthdayMembers = remember(repository) {
        derivedStateOf {
            try {
                repository?.getTodaysBirthdayMembers() ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }.value

    // Initialize repository safely
    LaunchedEffect(repository) {
        try {
            repository?.initialize()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // Background gradient matching the React design
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF433042), // #433042
            Color(0xFF5E3762), // #5E3762
            Color(0xFF5E205D), // #5E205D
            Color(0xFF521652)  // #521652
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(300f, 800f)
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        val dimensions = getResponsiveDimensions()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensions.horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(dimensions.verticalSpacing)
        ) {
            // Top spacing
            Spacer(modifier = Modifier.height(dimensions.screenHeight * 0.04f))

            // User profile icon section (responsive positioned at top)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .clickable { onProfileClick() }
                ) {
                    // Blue ellipse background for profile icon
                    Box(
                        modifier = Modifier
                            .size(
                                width = dimensions.screenWidth * 0.19f,
                                height = dimensions.screenWidth * 0.18f
                            )
                            .offset(x = 15.dp, y = 1.dp)
                            .clip(RoundedCornerShape(dimensions.cornerRadius * 2))
                            .background(Color(0x993C78C1)) // #3C78C1 with 60% opacity
                    )

                    // Profile icon
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.user),
                        contentDescription = "User Profile",
                        modifier = Modifier
                            .size(dimensions.iconSize)
                            .offset(x = 15.dp, y = 1.dp)
                            .align(Alignment.Center),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensions.screenHeight * 0.02f))

            // Birthday Members Section
            if (todaysBirthdayMembers.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(dimensions.screenHeight * 0.15f)
                        .align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(dimensions.cornerRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.8f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 Today's Birthdays! 🎉",
                            color = Color.White,
                            fontFamily = ris,
                            fontSize = responsiveFontSize(20f),
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(todaysBirthdayMembers) { member ->
                                BirthdayMemberCard(member = member)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dimensions.screenHeight * 0.02f))
            }

            // Purple gradient card (middle section) - responsive
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(if (todaysBirthdayMembers.isNotEmpty()) 1.2f else 1.5f)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(dimensions.cornerRadius * 2),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                val purpleGradient = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF390E3E), // #390E3E
                        Color(0xFF681971), // #681971
                        Color(0xFF9724A4)  // #9724A4
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = purpleGradient,
                            shape = RoundedCornerShape(dimensions.cornerRadius * 2)
                        )
                )
            }

            Spacer(modifier = Modifier.height(dimensions.screenHeight * 0.02f))// Bottom section with icons - responsive grid
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .aspectRatio(.95f)
                    .alpha(0.5f)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(dimensions.cornerRadius*2),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensions.horizontalPadding),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // First row of icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LocalIconButton(
                            drawableRes = R.drawable.orientation,
                            contentDescription = "Orientation Landscape",
                            iconSize = dimensions.iconSize,
                            onClick = onOrientationClick
                        )

                        LocalIconButton(
                            drawableRes = R.drawable.homewindow,
                            contentDescription = "Home Window",
                            iconSize = dimensions.iconSize,
                            onClick = onFamilyClick
                        )
                    }

                    // Second row of icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LocalIconButton(
                            drawableRes = R.drawable.calendar,
                            contentDescription = "Calendar",
                            iconSize = dimensions.iconSize,
                            onClick = onCalendarClick
                        )

                        LocalIconButton(
                            drawableRes = R.drawable.cloud,
                            contentDescription = "Cloud",
                            iconSize = dimensions.iconSize,
                            onClick = onSpreadsheetClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BirthdayMemberCard(
    member: com.sj9.chavara.data.model.FamilyMember,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(width = 60.dp, height = 80.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Member photo or initials fallback
            AsyncMemberImage(
                imageUrl = member.photoUrl,
                memberName = member.name,
                size = 32.dp,
                cornerRadius = 16.dp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = member.name.split(" ").firstOrNull() ?: "",
                color = Color.Black,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Text(
                text = member.birthday.split("/").take(2).joinToString("/"),
                color = Color.Gray,
                fontSize = 8.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LocalIconButton(
    drawableRes: Int,
    contentDescription: String,
    iconSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val buttonSize = iconSize * 1.6f
    val cornerRadius = getResponsiveCornerRadius()

    Box(
        modifier = modifier
            .size(buttonSize)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Blue background circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color(0x993D719A)) // #3D719A with 60% opacity
        ) {
            // Shine overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = buttonSize.value
                        )
                    )
            )
        }

        // Icon image from local drawable
        androidx.compose.foundation.Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            contentScale = ContentScale.Fit
        )
    }
}




@Preview(
    name = "Home Screen - Large Phone",
    showBackground = true,
    widthDp = 480,
    heightDp = 960,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun HomeScreenLargePreview() {
    ChavaraTheme {
        HomeScreen()
    }
}
