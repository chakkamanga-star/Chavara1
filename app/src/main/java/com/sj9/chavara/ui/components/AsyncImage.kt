package com.sj9.chavara.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.sj9.chavara.ui.theme.ris

@Composable
fun AsyncMemberImage(
    imageUrl: String,
    memberName: String,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    cornerRadius: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xA1E9D44E)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isEmpty()) {
            MemberInitials(memberName = memberName)
        } else {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build()
            )

            when (val state = painter.state) {
                is AsyncImagePainter.State.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size / 4),
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
                    MemberInitials(memberName = memberName)
                }
                else -> {
                    MemberInitials(memberName = memberName)
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