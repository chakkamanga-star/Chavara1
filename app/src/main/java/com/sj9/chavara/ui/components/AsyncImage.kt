package com.sj9.chavara.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.sj9.chavara.data.service.GcsImageLoader
import com.sj9.chavara.ui.theme.ris
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AsyncMemberImage(
    imageUrl: String,
    memberName: String,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    cornerRadius: Dp = 20.dp
) {
    val context = LocalContext.current
    var finalImageUrl by remember(imageUrl) { mutableStateOf<String?>(null) }
    var isProcessingUrl by remember(imageUrl) { mutableStateOf(true) }

    // Process the URL when it changes
    LaunchedEffect(imageUrl) {
        Log.d("AsyncMemberImage", "Processing URL for $memberName: $imageUrl")

        if (imageUrl.isEmpty()) {
            Log.d("AsyncMemberImage", "Empty URL for $memberName")
            finalImageUrl = null
            isProcessingUrl = false
            return@LaunchedEffect
        }

        isProcessingUrl = true

        try {
            finalImageUrl = when {
                imageUrl.startsWith("gs://") -> {
                    Log.d("AsyncMemberImage", "Converting GCS URL for $memberName")
                    val gcsLoader = GcsImageLoader(context)
                    val publicUrl = gcsLoader.getPublicGcsUrl(imageUrl)
                    Log.d("AsyncMemberImage", "Converted URL for $memberName: $publicUrl")
                    publicUrl
                }
                imageUrl.startsWith("http") -> {
                    Log.d("AsyncMemberImage", "Using HTTP URL directly for $memberName")
                    imageUrl
                }
                else -> {
                    Log.w("AsyncMemberImage", "Unsupported URL format for $memberName: $imageUrl")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("AsyncMemberImage", "Error processing URL for $memberName", e)
            finalImageUrl = null
        } finally {
            isProcessingUrl = false
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF4EDAE9)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isProcessingUrl -> {
                // Show loading while processing URL
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }

            finalImageUrl.isNullOrEmpty() -> {
                // Show initials if no valid URL
                Log.d("AsyncMemberImage", "No valid URL for $memberName, showing initials")
                MemberInitials(memberName = memberName)
            }

            else -> {
                // Load image with Coil
                Log.d("AsyncMemberImage", "Loading image for $memberName from: $finalImageUrl")

                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(finalImageUrl)
                        .crossfade(true)
                        .listener(
                            onStart = {
                                Log.d("AsyncMemberImage", "Started loading image for $memberName")
                            },
                            onSuccess = { _, _ ->
                                Log.d("AsyncMemberImage", "Successfully loaded image for $memberName")
                            },
                            onError = { _, throwable ->
                                Log.e("AsyncMemberImage", "Failed to load image for $memberName", throwable.throwable)
                            }
                        )
                        .build()
                )

                when (val state = painter.state) {
                    is AsyncImagePainter.State.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }

                    is AsyncImagePainter.State.Success -> {
                        Image(
                            painter = painter,
                            contentDescription = "$memberName's photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    is AsyncImagePainter.State.Error -> {
                        Log.e("AsyncMemberImage", "Image load error for $memberName: ${state.result.throwable}")
                        // Show error state with more info for debugging
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MemberInitials(memberName = memberName)
                            if (Log.isLoggable("AsyncMemberImage", Log.DEBUG)) {
                                Text(
                                    text = "Load Error",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    else -> {
                        MemberInitials(memberName = memberName)
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberInitials(
    memberName: String,
    modifier: Modifier = Modifier
) {
    val initials = memberName.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    Text(
        text = initials,
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = ris,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}