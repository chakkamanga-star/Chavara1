package com.sj9.chavara.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.ui.components.AsyncMemberImage
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.viewmodel.GalleryViewModel

@Composable
fun GalleryMembersScreen(
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val membersByMonth by viewModel.membersWithMediaByMonth.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val backgroundGradient = Brush.linearGradient(
        0.0f to Color(0xFFC3B532),
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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Birthdays Gallery",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ris,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 60.dp, bottom = 20.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else if (membersByMonth.isEmpty()) {
                Text(
                    text = "No members with photos or videos found.",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = ris,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(membersByMonth.entries.toList()) { (month, members) ->
                        MonthSection(
                            monthName = month,
                            members = members,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSection(
    monthName: String,
    members: List<FamilyMember>,
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = monthName,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ris,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 800.dp), // Allow grid to expand
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = false // Disable scrolling within the inner grid
        ) {
            items(members) { member ->
                MemberPhotoCard(member = member, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun MemberPhotoCard(
    member: FamilyMember,
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AsyncMemberImage(
            imageUrl = member.photoUrl,
            memberName = member.name,
            size = 100.dp,
            cornerRadius = 16.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = member.name,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ris,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = viewModel.getFormattedDateForTab(member),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontFamily = ris,
            textAlign = TextAlign.Center
        )
    }
}