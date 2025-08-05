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
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.ui.utils.*

@Composable
fun EditProfilePhotoScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // State to hold the URI of the cropped image
    var croppedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for the image cropper
    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            Log.d("EditProfilePhotoScreen", "Cropped URI: ${result.uriContent}")
            croppedImageUri = result.uriContent
            // Optional: Persist URI access if you need to save it long-term
            // result.uriContent?.let { uri ->
            //     context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // }
        } else {
            Log.e("EditProfilePhotoScreen", "Crop failed: ${result.error}")
        }
    }

    // Modern Photo Picker (gallery selection)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("EditProfilePhotoScreen", "Selected URI: $it")
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
            Log.e("EditProfilePhotoScreen", "No URI returned from photo picker")
        }
    }

    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF433042),
            Color(0xFF5E3762),
            Color(0xFF5E205D),
            Color(0xFF521652)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(horizontal = getResponsivePadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val cornerRadius = getResponsiveCornerRadius()
        val buttonHeight = getResponsiveButtonHeight()

        // Background composition for the photo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f),
            contentAlignment = Alignment.Center
        ) {
            // Conditionally display the selected image or the original placeholder
            if (croppedImageUri != null) {
                // Display the new, cropped image
                AsyncImage(
                    model = croppedImageUri,
                    contentDescription = "Profile Photo",
                    modifier = Modifier
                        .size(screenWidth * 0.6f)
                        .clip(CircleShape), // Clip the selected image to a circle
                    contentScale = ContentScale.Crop
                )
            } else {
                // THIS IS THE RESTORED PLACEHOLDER AS PER YOUR ORIGINAL CODE
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Large purple circle background
                    Box(
                        modifier = Modifier
                            .size(screenWidth * 0.6f)
                            .clip(RoundedCornerShape(screenWidth * 0.3f))
                            .background(Color(0xFF642B5D))
                    )

                    // Original Jesus image with its specific modifiers
                    Image(
                        painter = painterResource(id = R.drawable.jes),
                        contentDescription = "Profile Background",
                        modifier = Modifier
                            .size(screenWidth * 0.65f)
                            .aspectRatio(.5f)
                            .alpha(0.6f)
                            .offset(y = 110.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(getResponsiveSpacing() * 2))

        // Change Photo button
        Box(
            modifier = Modifier
                .size(
                    width = screenWidth * 0.48f,
                    height = buttonHeight
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color(0xFF592F6B))
                .clickable {
                    // Launch the photo picker (which will then trigger the cropper)
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Change Photo",
                color = Color.Black,
                fontSize = responsiveFontSize(23f),
                fontWeight = FontWeight.Normal,
                fontFamily = ris,
            )
        }
    }
}

@Preview(
    name = "Edit Profile Photo Screen - Large",
    showBackground = true,
    widthDp = 480,
    heightDp = 960
)
@Composable
fun ProfileEditScreenMediumPreview() {
    ChavaraTheme {
        EditProfilePhotoScreen()
    }
}