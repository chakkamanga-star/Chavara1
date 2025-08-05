package com.sj9.chavara.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ChavaraTheme

@Composable
fun GalleryVideosScreen(
    videos: List<VideoItem> = getDefaultVideos(),
    modifier: Modifier = Modifier
) {
    val backgroundGradient = Brush.linearGradient(
        0.0f to Color(0xFFC38732),
        0.4028f to Color(0xFFDCB72F),
        0.588f to Color(0xFFD3CA15),
        0.7594f to Color(0xFFDDE05F),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(412f, 917f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        // Background image with opacity
        Image(
            painter = painterResource(id = R.drawable.jesus),
            contentDescription = null,
            modifier = Modifier
                .offset(x = 0.dp, y = 146.dp)
                .size(width = 388.dp, height = 690.dp)
                .alpha(1f),
            contentScale = ContentScale.Crop
        )

        // Cy Videos title
        Text(
            text = "Cy Videos",
            color = Color.White,
            fontSize = 41.sp,
            fontWeight = FontWeight.W400,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .offset(x = 111.dp, y = 50.dp)
                .size(width = 216.dp, height = 43.dp)
        )

        // January subtitle
        Text(
            text = "January",
            color = Color.White,
            fontSize = 29.sp,
            fontWeight = FontWeight.W400,
            modifier = Modifier
                .offset(x = 39.dp, y = 135.dp)
                .size(width = 108.dp, height = 36.dp)
        )

        // Videos grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .offset(x = 26.dp, y = 194.dp)
                .size(width = 362.dp, height = 597.dp)
                .alpha(.75f),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            items(videos) { video ->
                VideoGridItem(video = video)
            }
        }
    }
}

@Composable
private fun VideoGridItem(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    val cardGradient = Brush.verticalGradient(
    0.0f to Color(0xFFCDD00E),
    1.0f to Color(0xFFD5BC1A)
)

    Box(
        modifier = modifier
            .size(width = 166.dp, height = 183.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(cardGradient)
            .alpha(1f),
        contentAlignment = Alignment.Center
    ) {
        // Placeholder for video content
        // In a real app, you would load actual video thumbnails here
        Text(
            text = video.name,
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

data class VideoItem(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val videoUrl: String? = null
)

private fun getDefaultVideos(): List<VideoItem> {
    // In a real app, this would fetch from a database or API
    // The number of items can grow dynamically based on actual content
    return (1..6).map { index ->
        VideoItem(
            id = "video_jan_$index",
            name = "Jan ${index}th",
            thumbnailUrl = null, // Would contain actual thumbnail URLs
            videoUrl = null // Would contain actual video URLs
        )
    }
}

@Preview(
    name = "Gallery Videos Screen",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
fun GalleryVideosScreenPreview() {
    ChavaraTheme {
        GalleryVideosScreen()
    }
}
