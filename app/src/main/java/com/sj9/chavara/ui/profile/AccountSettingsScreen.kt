package com.sj9.chavara.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ChavaraTheme
import com.sj9.chavara.ui.theme.ris

@Composable
fun AccountSettingsScreen(
    modifier: Modifier = Modifier
) {
    // Background gradient matching the design
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF433042), // #433042
            Color(0xFF5E3762), // #5E3762
            Color(0xFF5E205D), // #5E205D
            Color(0xFF521652)  // #521652
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 200f),
        end = androidx.compose.ui.geometry.Offset(300f, 800f)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(horizontal = 32.dp), // Use fixed dp for padding
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Use a Weight modifier for proportional spacing
        Spacer(modifier = Modifier.weight(0.16f))

        Text(
            text = "Account Settings",
            color = Color.Black,
            fontSize = 42.sp, // Use a fixed sp value
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(24.dp)) // Fixed height

        // Settings container
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Fill 90% of the parent's width
                .fillMaxHeight(0.3f) // Fill 30% of the remaining height
                .clip(RoundedCornerShape(30.dp))
                .alpha(0.8f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xB89923D4), Color(0xA8872EB3),
                            Color(0xB861307A), Color(0xB84F126E)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Pass fixed font sizes and use actual Switch
                SettingRow(label = "Sync With Gmail", fontSize = 24.sp)
                SettingRow(label = "Backup to \nGoogle Drive", fontSize = 22.sp)
                SettingRow(label = "Notifications", fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.weight(0.08f))

        // Save button
        Box(
            modifier = Modifier
                .size(64.dp) // Use a fixed size for the button container
                .clip(RoundedCornerShape(15.dp))
                .alpha(0.8f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xB89923D4), Color(0xA8872EB3),
                            Color(0xB861307A), Color(0xB84F126E)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_edit),
                contentDescription = "Save",
                modifier = Modifier.size(48.dp), // Fixed icon size
                contentScale = ContentScale.Fit
            )
        }

        // Add a final spacer to push content up if needed
        Spacer(modifier = Modifier.weight(0.2f))
    }
}

@Composable
private fun SettingRow(
    label: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Black,
            fontSize = fontSize,
            fontWeight = FontWeight.Normal,
            fontFamily = ris
        )
        // Replaced icon with actual Switch, enabled by default
        var isChecked by remember { mutableStateOf(true) }
        Switch(
            checked = isChecked,
            onCheckedChange = { isChecked = it },
            modifier = Modifier.size(width = 32.dp, height = 40.dp) // Match previous size
        )
    }
}

@Preview(
    name = "Account Settings Screen",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
fun AccountSettingsScreenPreview() {
    ChavaraTheme {
        AccountSettingsScreen()
    }
}