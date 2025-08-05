package com.sj9.chavara.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ChavaraTheme
import com.sj9.chavara.data.repository.ChavaraRepository
import com.sj9.chavara.data.manager.GalleryManager
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.ui.components.AsyncMemberImage
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun GalleryMembersScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember {
        try {
            ChavaraRepository(context)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Collect family members from repository safely
    val familyMembers by (repository?.familyMembers ?: MutableStateFlow(emptyList())).collectAsState()

    // Organize members by month
    val membersByMonth = remember(familyMembers) {
        try {
            GalleryManager.organizeByMonth(GalleryManager.getMembersWithPhotos(familyMembers))
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    // Initialize repository safely
    LaunchedEffect(repository) {
        try {
            repository?.initialize()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    val backgroundGradient = Brush.linearGradient(
        0.0f to Color(0xFFC3B532),
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
                .fillMaxSize()
                .alpha(0.3f),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Birthdays title
            Text(
                text = "Birthdays Gallery",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ris,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )

            if (membersByMonth.isEmpty()) {
                // Show message when no data
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No birthday photos available.\nFetch data from spreadsheet first.",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontFamily = ris,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Show members organized by month
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(membersByMonth.toList()) { (month, members) ->
                        MonthSection(
                            monthName = month,
                            members = members
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Month header
        Text(
            text = monthName,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ris,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Members grid for this month
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(members) { member ->
                MemberPhotoCard(member = member)
            }
        }
    }
}

@Composable
private fun MemberPhotoCard(
    member: FamilyMember,
    modifier: Modifier = Modifier
) {
    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFCDD00E),
            Color(0xFFD5BC1A)
        )
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(cardGradient)
            .alpha(0.9f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Member photo
            AsyncMemberImage(
                imageUrl = member.photoUrl,
                memberName = member.name,
                size = 80.dp,
                cornerRadius = 12.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Member name
            Text(
                text = member.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ris,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
                maxLines = 1
            )

            // Birthday date formatted for display
            Text(
                text = GalleryManager.getFormattedDateForTab(member),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontFamily = ris,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Preview(
    name = "Gallery Members Screen",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
fun GalleryMembersScreenPreview() {
    ChavaraTheme {
        GalleryMembersScreen()
    }
}
