package com.sj9.chavara.ui.calendar

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ChavaraTheme
import com.sj9.chavara.ui.theme.ris // Make sure you import your custom font

@Composable
fun CalendarEventsScreen(
    modifier: Modifier = Modifier
) {
    // 1. State Management for notes and the selected image URI
    var notesText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 2. Photo Picker Launcher
    // This handles opening the gallery and getting the result
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            // When an image is picked, its URI is stored in our state variable
            selectedImageUri = uri
        }
    )

    val backgroundGradient = Brush.linearGradient(
        0.0f to Color(0xFFDB5658),
        0.2222f to Color(0xFF942E3D),
        0.4167f to Color(0xFF8C1C38),
        0.6019f to Color(0xFF630406),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(300f, 800f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp), // Overall top padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Container for the image and its edit button
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1.5f) // Rectangular shape for the image
            ) {
                // Header container for the image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(25.dp))
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color(0xFFE91F1F),
                                0.4231f to Color(0xFF992424),
                                0.6875f to Color(0xFF5E1F1F),
                                1.0f to Color(0xFF2C0505)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 3. Conditional Image Display
                    if (selectedImageUri != null) {
                        // If an image is selected, display it
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Otherwise, show placeholder text
                        Text(
                            text = "Add Photo",
                            color = Color.White.copy(alpha = 0.7f),
                            fontFamily = ris,
                            fontSize = 24.sp
                        )
                    }
                }

                // Edit button aligned to the bottom-right of the image container
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .align(Alignment.BottomEnd)
                        .clickable {
                            // Launch the photo picker
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "Edit",
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main notes container with red gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .weight(1f) // Takes remaining space
                    .clip(RoundedCornerShape(25.dp))

                    .alpha(0.6f)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xFFE91F1F),
                            0.4231f to Color(0xFF992424),
                            0.6875f to Color(0xFF5E1F1F),
                            1.0f to Color(0xFF2C0505)
                        )
                    )
                    .padding(14.dp)
            ) {
                // 4. Editable Notes Field
                BasicTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontFamily = ris,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    decorationBox = { innerTextField ->
                        if (notesText.isEmpty()) {
                            Text(
                                text = "Notes",
                                color = Color.White.copy(alpha = 0.7f),
                                fontFamily = ris,
                                fontSize = 35.sp,
                                fontWeight = FontWeight.W400
                            )
                        }
                        innerTextField()
                    }
                )
            }
            Spacer(modifier = Modifier.height(80.dp))
        }

        // Bookmark icon button at the bottom center
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xFFE91F1F),
                        0.4231f to Color(0xFF992424),
                        0.6875f to Color(0xFF5E1F1F),
                        1.0f to Color(0xFF260606)
                    )
                )
                .clickable { /* Handle notes click */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_bookmark),
                contentDescription = "Bookmark",
                modifier = Modifier.size(34.dp)
                    .alpha(.8f),
                tint = Color.Black
            )
        }
    }
}

@Preview(
    name = "Calendar Events Screen",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
fun CalendarEventsScreenPreview() {
    ChavaraTheme {
        CalendarEventsScreen()
    }
}