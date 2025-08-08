package com.sj9.chavara.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.R
import com.sj9.chavara.ui.components.AsyncMemberImage
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.viewmodel.ProfileViewModel

@Composable
fun ProfileMainScreen(
    viewModel: ProfileViewModel,
    onAccountSettingsClick: () -> Unit = {},
    onAppInformationClick: () -> Unit = {},
    onResetAppClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()

    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF433042), Color(0xFF5E3762),
            Color(0xFF5E205D), Color(0xFF521652)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncMemberImage(
                imageUrl = userProfile?.photoUrl ?: "",
                memberName = userProfile?.name ?: "User",
                size = 150.dp,
                cornerRadius = 75.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = userProfile?.name ?: "Chavara Member",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = userProfile?.emailAddress ?: "",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(48.dp))
            ProfileMenuButton(text = "Account Settings", onClick = onAccountSettingsClick)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileMenuButton(text = "App Information", onClick = onAppInformationClick)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileMenuButton(text = "Reset App", onClick = onResetAppClick, isDestructive = true)
        }
    }
}

@Composable
private fun ProfileMenuButton(
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val backgroundColor = if (isDestructive) Color(0x99FF5252) else Color(0x99AD79BF)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = ris
        )
    }
}
