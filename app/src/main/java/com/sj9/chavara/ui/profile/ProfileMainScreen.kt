package com.sj9.chavara.ui.profile

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ChavaraTheme
import com.sj9.chavara.ui.utils.*

@Composable
fun ProfileMainScreen(
    onEditProfileClick: () -> Unit = {},
    onAccountSettingsClick: () -> Unit = {},
    onAppInformationClick: () -> Unit = {},
    onResetAppClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var croppedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for the image cropper
    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            Log.d("ProfileMainScreen", "Cropped URI: ${result.uriContent}")
            croppedImageUri = result.uriContent
            // Optional: Persist URI access if you need to save it long-term
            // result.uriContent?.let { uri ->
            //     context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // }
        } else {
            Log.e("ProfileMainScreen", "Crop failed: ${result.error}")
        }
    }

    // Modern Photo Picker (gallery selection)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("ProfileMainScreen", "Selected URI: $it")
            val cropOptions = CropImageContractOptions(
                uri = it,
                cropImageOptions = CropImageOptions().apply {
                    fixAspectRatio = true
                    aspectRatioX = 1
                    aspectRatioY = 1 // Square for a profile picture
                    cropShape = CropImageView.CropShape.OVAL
                }
            )
            cropImageLauncher.launch(cropOptions)
        } ?: run {
            Log.e("ProfileMainScreen", "No URI returned from photo picker")
        }
    }

    // Background gradient matching the design
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF433042), // #433042
            Color(0xFF5E3762), // #5E3762
            Color(0xFF5E205D), // #5E205D
            Color(0xFF521652)  // #521652
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 200f),
        end = androidx.compose.ui.geometry.Offset(300f, 800f)
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        val dimensions = getResponsiveDimensions()

        // Background images - responsive positioning
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Profile background composition
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.55f)
                    .align(Alignment.TopStart)
            ) {
                // Conditionally display the cropped image or the original placeholder
                if (croppedImageUri != null) {
                    // Display the new, cropped image (circular and clickable)
                    AsyncImage(
                        model = croppedImageUri,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .size(dimensions.screenWidth * 0.48f)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .clickable {
                                // Launch photo picker on click
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Original placeholder (purple circle with Jesus inside, clickable)
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clickable {
                                // Launch photo picker on click
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                    ) {
                        // Purple circle background
                        Box(
                            modifier = Modifier
                                .size(dimensions.screenWidth * 0.48f)
                                .offset(x = 56.dp, y = 135.dp)
                                .clip(RoundedCornerShape(dimensions.screenWidth * 0.24f))
                                .background(Color(0xFF642B5D))
                        )

                        // Jesus image layered inside/onto the circle with original positioning
                        Image(
                            painter = painterResource(id = R.drawable.jes),
                            contentDescription = "Background Layer 1",
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .aspectRatio(.70f)
                                .offset(x = 2.dp, y = 85.dp)
                                .size(width = 359.dp, height = 515.dp)
                                .alpha(0.6f),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Bottom right decorative image
            Image(
                painter = painterResource(id = R.drawable.jesone),
                contentDescription = "Bottom Right Decorative Image",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(1f)
                    .offset(x = 50.dp, y = 5.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )
        }

        // Main content - improved responsive layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensions.horizontalPadding),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            // Push content to lower half
            Spacer(modifier = Modifier.weight(0.3f))

            // Menu buttons with consistent spacing
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensions.verticalSpacing * 1.5f)
            ) {
                // Edit Profile button
                ProfileMenuButton(
                    text = "Edit Profile",
                    width = dimensions.screenWidth * 0.47f,
                    onClick = onEditProfileClick
                )

                // Account Settings button
                ProfileMenuButton(
                    text = "Account Settings",
                    width = dimensions.screenWidth * 0.59f,
                    onClick = onAccountSettingsClick
                )

                // App Information button
                ProfileMenuButton(
                    text = "App Information",
                    width = dimensions.screenWidth * 0.59f,
                    onClick = onAppInformationClick
                )

                // Reset App button
                Box(
                    modifier = Modifier
                        .size(
                            width = dimensions.screenWidth * 0.47f,
                            height = dimensions.buttonHeight
                        )
                        .clip(RoundedCornerShape(dimensions.cornerRadius))
                        .background(Color(0x99AD79BF))
                        .clickable { onResetAppClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reset App",
                        color = Color(0xFF0A0A0A),
                        fontSize = responsiveFontSize(20f),
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Default
                    )
                }
            }

            // Bottom spacing
            Spacer(modifier = Modifier.weight(0.2f))
        }
    }
}

@Composable
private fun ProfileMenuButton(
    text: String,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 192.dp,
    onClick: () -> Unit = {}
) {
    val buttonHeight = getResponsiveButtonHeight()
    val cornerRadius = getResponsiveCornerRadius()
    val horizontalPadding = getResponsivePadding()

    Box(
        modifier = modifier
            .size(width = width, height = buttonHeight)
            .clickable { onClick() }
    ) {
        // Button background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color(0x99AD79BF)) // rgba(173, 121, 191, 0.60)
        )

        // Button text - responsive font size
        Text(
            text = text,
            color = Color(0xFF0A0A0A),
            fontSize = responsiveFontSize(20f),
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Default,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = horizontalPadding)
        )
    }
}

@Preview(
    name = "Profile Main Screen - Medium",
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
fun ProfileMainScreenMediumPreview() {
    ChavaraTheme {
        ProfileMainScreen()
    }
}