package com.sj9.chavara.ui.family

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ris

@Composable
fun FamilyMemberPhotoEditScreen(
    memberId: Int = 0,
    onPhotoSelected: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
        // Status Bar


        // Bottom Home Bar


        // Background card
        Box(
            modifier = Modifier
                .offset(x = 38.dp, y = 233.dp)
                .size(width = 346.dp, height = 530.dp)
                .rotate(-0.143f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF19595F),
                            Color(0xFF76E6F1)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        )

        // Photo area with gradient
        Box(
            modifier = Modifier
                .offset(x = 67.dp, y = 282.dp)
                .size(width = 288.dp, height = 354.dp)
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

        // Jesus image overlay
        Image(
            painter = painterResource(id = R.drawable.jesus),
            contentDescription = null,
            modifier = Modifier
                .offset(x = 18.dp, y = 212.dp)
                .size(width = 388.dp, height = 640.dp)
                .aspectRatio(.8f)
                .alpha(2f),
            contentScale = ContentScale.Crop
        )

        // Change Photo text
        Text(
            text = "Change Photo",
            color = Color.White,
            fontFamily = ris,
            fontSize = 33.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .offset(x = 115.dp, y = 584.dp)
                .size(width = 294.dp, height = 81.dp)
                .clickable { onPhotoSelected() }
        )
    }
}


@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
fun FamilyMemberPhotoEditScreenPreview() {
    FamilyMemberPhotoEditScreen()
}
