package com.sj9.chavara.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.data.service.GcsImageLoader

@Composable
fun AsyncMemberImage(
    imageUrl: String,
    memberName: String,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    cornerRadius: Dp = 20.dp
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF4EDAE9)),
        contentAlignment = Alignment.Center
    ) {
        when {
            imageUrl.isEmpty() -> {
                // No image URL provided, show member initials
                MemberInitials(memberName = memberName)
            }
            imageUrl.startsWith("gs://") -> {
                // For GCS URLs, convert to public HTTP URL if possible
                val gcsLoader = remember { GcsImageLoader(context) }
                val publicUrl = remember(imageUrl) { gcsLoader.getPublicGcsUrl(imageUrl) }

                if (publicUrl != null) {
                    val painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(publicUrl)
                            .crossfade(true)
                            .build()
                    )

                    when (painter.state) {
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
                                modifier = Modifier.size(size),
                                contentScale = ContentScale.Crop
                            )
                        }
                        is AsyncImagePainter.State.Error -> {
                            MemberInitials(memberName = memberName)
                        }
                        else -> {
                            MemberInitials(memberName = memberName)
                        }
                    }
                } else {
                    MemberInitials(memberName = memberName)
                }
            }
            else -> {
                // For regular URLs (including processed Google Drive URLs), use Coil
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build()
                )

                when (painter.state) {
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
                            modifier = Modifier.size(size),
                            contentScale = ContentScale.Crop
                        )
                    }
                    is AsyncImagePainter.State.Error -> {
                        MemberInitials(memberName = memberName)
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
        modifier = modifier
    )
}


