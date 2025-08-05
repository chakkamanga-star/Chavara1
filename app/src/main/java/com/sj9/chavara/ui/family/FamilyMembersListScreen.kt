package com.sj9.chavara.ui.family

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.repository.ChavaraRepository
import com.sj9.chavara.ui.components.AsyncMemberImage
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun FamilyMembersListScreen(
    onMemberClick: (FamilyMember) -> Unit = {},
    onAddMemberClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Make repository initialization safe
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

    // Initialize repository safely
    LaunchedEffect(repository) {
        try {
            repository?.initialize()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4BC9D7), // #4BC9D7
                        Color(0xFF26767E), // #26767E at 49.52%
                        Color(0xFF0A1E20)  // #0A1E20
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Background Jesus image with opacity
        Image(
            painter = painterResource(id = R.drawable.jesus),
            contentDescription = null,
            modifier = Modifier
                .offset(x = 19.dp, y = 151.dp)
                .size(width = 388.dp, height = 690.dp)
                .alpha(1f),
            contentScale = ContentScale.Crop
        )

        // Penguin icon container - positioned based on member count
        Box(
            modifier = Modifier
                .let {
                    if (familyMembers.isEmpty()) {
                        // Center the penguin when no members
                        it.fillMaxSize()
                    } else {
                        // Top-right position when members exist
                        it.offset(x = 336.dp, y = 18.dp)
                    }
                }
                .size(width = 56.dp, height = 68.dp)
                .clickable { onAddMemberClick() },
            contentAlignment = if (familyMembers.isEmpty()) Alignment.Center else Alignment.TopStart
        ) {
            // Gradient background for penguin container
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

            // Penguin icon
            Image(
                painter = painterResource(id = R.drawable.penguin),
                contentDescription = "Add Member",
                modifier = Modifier

                    .size(width = 47.dp, height = 51.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Show Members title and grid only when members exist
        if (familyMembers.isNotEmpty()) {
            // Members title
            Text(
                text = "Members",
                color = Color.Black,
                fontFamily = ris,
                fontSize = 49.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .offset(x = 112.dp, y = 103.dp)
                    .size(width = 188.dp, height = 57.dp)
            )

            // Family members grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .offset(x = 20.dp, y = 198.dp)
                    .size(width = 372.dp, height = 650.dp),
                horizontalArrangement = Arrangement.spacedBy(31.dp),
                verticalArrangement = Arrangement.spacedBy(23.dp)
            ) {
                items(familyMembers) { member ->
                    FamilyMemberCard(
                        member = member,
                        onClick = { onMemberClick(member) }
                    )
                }

                // Add member card (if there's space for more)
                if (familyMembers.size < 6) {
                    item {
                        AddMemberCard(onClick = onAddMemberClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyMemberCard(
    member: FamilyMember,
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
                        Color(0xFF4EDAE9),
                        Color(0xFF0F4248)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Show actual member photo or initials as fallback
        AsyncMemberImage(
            imageUrl = member.photoUrl,
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

private fun getSampleFamilyMembers(): List<FamilyMember> {
    return listOf(
        FamilyMember(1, "John"),
        FamilyMember(2, "Mary"),
        FamilyMember(3, "David"),
        FamilyMember(4, "Sarah"),
        FamilyMember(5, "Michael"),
        FamilyMember(6, "Anna")
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
fun FamilyMembersListScreenPreview() {
    FamilyMembersListScreen()
}
