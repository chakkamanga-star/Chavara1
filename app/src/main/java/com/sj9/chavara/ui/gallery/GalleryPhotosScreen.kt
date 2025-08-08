package com.sj9.chavara.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.ui.components.AsyncMemberImage
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.viewmodel.GalleryViewModel

@Composable
fun GalleryPhotosScreen(
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    // Set the filter in the ViewModel to PHOTOS when this screen appears
    LaunchedEffect(Unit) {
        viewModel.setFilter(GalleryViewModel.GalleryFilter.PHOTOS)
    }

    val galleryItems by viewModel.filteredGalleryItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val backgroundGradient = Brush.linearGradient(
        0.0f to Color(0xFFC38732),
        0.4028f to Color(0xFFDCB72F),
        0.588f to Color(0xFFD3CA15),
        0.7594f to Color(0xFFDDE05F)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CY Photos",
                color = Color.White,
                fontSize = 41.sp,
                fontFamily = ris,
                fontWeight = FontWeight.W400,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 60.dp, bottom = 20.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else if (galleryItems.isEmpty()) {
                Text(
                    text = "No photos found.",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = ris
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(galleryItems) { item ->
                        if (item is GalleryViewModel.GalleryItem.Photo) {
                            AsyncMemberImage(
                                imageUrl = item.url,
                                memberName = item.member.name,
                                modifier = Modifier.aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}