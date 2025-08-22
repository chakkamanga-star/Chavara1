package com.sj9.chavara.ui.family

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.R
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.ui.components.AsyncMemberImage
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.viewmodel.FamilyMembersViewModel
import kotlinx.coroutines.launch

@Composable
fun FamilyMembersListScreen(
    viewModel: FamilyMembersViewModel,
    onMemberClick: (FamilyMember) -> Unit = {},
    onAddMemberClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val familyMembers by viewModel.familyMembers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

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
        Image(
            painter = painterResource(id = R.drawable.jesus),
            contentDescription = null,
            modifier = Modifier
                .offset(x = 19.dp, y = 151.dp)
                .size(width = 388.dp, height = 690.dp)
                .alpha(1f),
            contentScale = ContentScale.Crop
        )

        if (isLoading && familyMembers.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Members",
                    color = Color.Black,
                    fontFamily = ris,
                    fontSize = 49.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 103.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 198.dp),
                    horizontalArrangement = Arrangement.spacedBy(31.dp),
                    verticalArrangement = Arrangement.spacedBy(23.dp)
                ) {
                    items(familyMembers) { member ->
                        FamilyMemberCard(
                            member = member,
                            onClick = { onMemberClick(member) },
                            viewModel = viewModel
                        )
                    }
                    item {
                        AddMemberCard(onClick = onAddMemberClick)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 20.dp)
                .size(width = 56.dp, height = 68.dp)
                .clickable { onAddMemberClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 68.dp)
                    .rotate(-0.143f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4EDAE9),
                                Color(0xFF0F4248)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            )
            Image(
                painter = painterResource(id = R.drawable.penguin),
                contentDescription = "Add Member",
                modifier = Modifier.size(width = 47.dp, height = 51.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun FamilyMemberCard(
    member: FamilyMember,
    onClick: () -> Unit,
    viewModel: FamilyMembersViewModel,
    modifier: Modifier = Modifier
) {
    var signedImageUrl by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(member.photoUrl) {
        coroutineScope.launch {
            if (member.photoUrl.startsWith("gs://")) {
                signedImageUrl = viewModel.getAuthenticatedImageUrl(member.photoUrl)
            } else {
                signedImageUrl = member.photoUrl
            }
        }
    }
    Box(
        modifier = modifier
            .size(width = 170.dp, height = 209.dp)
            .rotate(-0.143f)
            .clickable { onClick() }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4EDAE9),
                        Color(0xFF0F4248)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncMemberImage(
            imageUrl = signedImageUrl ?: "",
            memberName = member.name,
            size = 140.dp,
            cornerRadius = 16.dp
        )
    }
}

@Composable
private fun AddMemberCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 170.dp, height = 209.dp)
            .rotate(-0.143f)
            .clickable { onClick() }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4EDAE9).copy(alpha = 0.5f),
                        Color(0xFF0F4248).copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Normal
        )
    }
}