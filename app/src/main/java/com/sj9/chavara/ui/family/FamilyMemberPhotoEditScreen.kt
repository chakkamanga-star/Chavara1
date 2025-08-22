package com.sj9.chavara.ui.family

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.viewmodel.FamilyMembersViewModel
import kotlinx.coroutines.launch
import java.io.InputStream

@Composable
fun FamilyMemberPhotoEditScreen(
    modifier: Modifier = Modifier,
    memberId: Int,
    viewModel: FamilyMembersViewModel,
    onDoneEditing: () -> Unit = {},
    initialPhotoUri: Uri? = null
) {
    Log.d("PhotoEditScreen", "Editing photo for member ID: $memberId")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. State for the selected image URI
    var selectedImageUri by remember { mutableStateOf(initialPhotoUri) }

    // 2. Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                Log.d("PhotoEditScreen", "Photo selected: $uri")
                coroutineScope.launch {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null) {
                        viewModel.uploadMemberPhoto(memberId, bytes, context.contentResolver.getType(uri) ?: "image/jpeg")
                    }
                }
            } else {
                Log.d("PhotoEditScreen", "No photo selected")
            }
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4BC9D7),
                        Color(0xFF26767E),
                        Color(0xFF0A1E20)
                    )
                )
            )
    ) {
        // Background card
        Box(
            modifier = Modifier
                .offset(x = 38.dp, y = 233.dp)
                .size(width = 346.dp, height = 530.dp)
                .rotate(-0.143f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF19595F),
                            Color(0xFF76E6F1)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        )

        // Photo area - This will now contain the AsyncImage or placeholder
        Box(
            modifier = Modifier
                .offset(x = 67.dp, y = 282.dp)
                .size(width = 288.dp, height = 354.dp)
                .rotate(-0.143f)
                .clip(RoundedCornerShape(20.dp)) // Clip before background for rounded corners
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4EDAE9),
                            Color(0xFF0F4248)
                        )
                    )
                )
                .clickable { // Make the whole area clickable to change photo
                    Log.d("PhotoEditScreen", "Photo area clicked, launching picker.")
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center // Center content like image or text
        ) {
            // 3. Conditional Image Display
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Family Member Photo",
                    modifier = Modifier.fillMaxSize(), // Fill the photo area
                    contentScale = ContentScale.Crop
                )
            } else {
                // Display the placeholder "Jesus" image or "Add Photo" text
                Image(
                    painter = painterResource(id = R.drawable.jesus), // Or a generic placeholder
                    contentDescription = "Tap to add photo",
                    modifier = Modifier
                        .fillMaxSize() // Fill the photo area
                        .alpha(0.8f), // Adjust alpha as needed
                    contentScale = ContentScale.Crop
                )
                // Optionally, add a text overlay like "Tap to change"
                Text(
                    text = "Tap to change",
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = ris,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }

        Text(
            text = "Done", // Changed text
            color = Color.White,
            fontFamily = ris,
            fontSize = 33.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .align(Alignment.BottomCenter) // Align to bottom center for better placement
                .padding(bottom = 70.dp) // Adjust padding
                .clickable {
                    Log.d("PhotoEditScreen", "Done button clicked.")
                    onDoneEditing() // This will now pop the back stack as defined in navigation
                }
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
fun FamilyMemberPhotoEditScreenPreview() {
    // ChavaraTheme { // Assuming you have a theme
    val fakeViewModel = object : FamilyMembersViewModel(repository = TODO()) {}
    FamilyMemberPhotoEditScreen(memberId = 0, onDoneEditing = {}, viewModel = fakeViewModel)
    // }
}