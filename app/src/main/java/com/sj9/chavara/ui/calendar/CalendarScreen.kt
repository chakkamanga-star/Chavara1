package com.sj9.chavara.ui.calendar

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ChavaraTheme
import java.util.*

@Composable
fun CalendarScreen(
    onDateClick: (Calendar) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var croppedImageUri by remember { mutableStateOf<Uri?>(null) }

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            Log.d("CalendarScreen", "Cropped URI: ${result.uriContent}")
            croppedImageUri = result.uriContent
            // Optional: Persist URI access if you need to save it long-term
            // result.uriContent?.let { uri ->
            //     context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // }
        } else {
            Log.e("CalendarScreen", "Crop failed: ${result.error}")
        }
    }

    // NEW: Use modern Photo Picker (replaces GetContent)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("CalendarScreen", "Selected URI: $it")
            val cropOptions = CropImageContractOptions(
                uri = it,
                cropImageOptions = CropImageOptions().apply {
                    fixAspectRatio = true
                    aspectRatioX = 16
                    aspectRatioY = 9
                    cropShape = CropImageView.CropShape.RECTANGLE
                }
            )
            cropImageLauncher.launch(cropOptions)
        } ?: run {
            Log.e("CalendarScreen", "No URI returned from photo picker")
        }
    }

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
        // --- RESTORED BACKGROUND ELEMENTS ---
        Image(
            painter = painterResource(id = R.drawable.jesus),
            contentDescription = null,
            modifier = Modifier
                .offset(x = 8.dp, y = 240.dp)
                .size(width = 388.dp, height = 690.dp)
                .alpha(1f),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .offset(x = 28.dp, y = 95.dp)
                .size(width = 358.dp, height = 706.dp)
                .clip(RoundedCornerShape(25.dp))
                .alpha(0.5f)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xFFE91F1F),
                        0.4231f to Color(0xFF992424),
                        0.6875f to Color(0xFF5E1F1F),
                        1.0f to Color(0xFF2C0505)
                    )
                )
        )
        // --- END OF RESTORED ELEMENTS ---

        // New, more robust layout using Column
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(122.dp))

            Box(
                modifier = Modifier
                    .width(313.dp)
                    .height(191.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(25.dp))
                        .alpha(0.8f)
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
                    if (croppedImageUri != null) {
                        AsyncImage(
                            model = croppedImageUri,
                            contentDescription = "Selected Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                            // Placeholder removed to avoid unresolved reference - add back once you create R.drawable.placeholder
                            // placeholder = painterResource(id = R.drawable.placeholder)
                        )
                    } else {
                        Text("Add Photo", color = Color.White.copy(alpha = 0.7f))
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 10.dp)
                        .size(57.dp, 54.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color(0xFFE91F1F),
                                0.4231f to Color(0xFF992424)
                            )
                        )
                        .clickable {
                            // Launch the new photo picker (no permission check needed)
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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

            CalendarMonthView(
                monthCalendar = Calendar.getInstance(),
                onDateClick = onDateClick,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
}

@Composable
private fun CalendarMonthView(
    monthCalendar: Calendar,
    onDateClick: (Calendar) -> Unit,
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthName = monthCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Month"
    var selectedMonthOffset by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 125.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "<",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.W400,
                modifier = Modifier.clickable { onPreviousMonth() }
            )
            Text(
                text = monthName,
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.W400,
                textAlign = TextAlign.Center
            )
            Text(
                text = ">",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.W400,
                modifier = Modifier.clickable { onNextMonth() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .heightIn(max = 410.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items((1..daysInMonth).toList()) { day ->
                CalendarDateButton(
                    day = day,
                    onClick = {
                        val dayCalendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, monthCalendar.get(Calendar.YEAR))
                            set(Calendar.MONTH, monthCalendar.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, day)
                        }
                        onDateClick(dayCalendar)
                    }
                )
            }
        }
    }
}

@Composable
private fun CalendarDateButton(
    day: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 57.dp, height = 54.dp)
            .clip(RoundedCornerShape(25.dp))
            .alpha(0.8f)
            .background(
                Brush.verticalGradient(
                    0.0f to Color(0xFFE91F1F),
                    0.4231f to Color(0xFF992424),
                    0.6875f to Color(0xFF5E1F1F),
                    1.0f to Color(0xFF2C0505)
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.W400,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(
    name = "Calendar Screen",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
fun CalendarScreenPreview() {
    ChavaraTheme {
        CalendarScreen()
    }
}