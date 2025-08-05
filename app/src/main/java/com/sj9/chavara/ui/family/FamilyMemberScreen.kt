package com.sj9.chavara.ui.family

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.data.model.FamilyMember // Added import
import com.sj9.chavara.data.repository.ChavaraRepository // Added import
import com.sj9.chavara.ui.components.AsyncMemberImage // Added import
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat // Added import
import java.util.Date // Added import
import java.util.Locale // Added import (if needed for SimpleDateFormat)

@Composable
fun FamilyMemberScreen(
    memberId: Int = 0,
    isNewMember: Boolean = false,
    onEditPhotoClick: () -> Unit = {},
    onSaveComplete: () -> Unit = {},
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
    val coroutineScope = rememberCoroutineScope()

    // State for form inputs
    var name by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var birthDay by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var residence by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var chavaraPart by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Load existing member data if not new
    LaunchedEffect(memberId, repository) {
        if (!isNewMember && memberId > 0 && repository != null) {
            try {
                val member = repository.getMemberById(memberId)
                member?.let {
                    name = it.name
                    course = it.course
                    birthDay = it.birthday
                    phoneNumber = it.phoneNumber
                    residence = it.residence
                    emailAddress = it.emailAddress
                    chavaraPart = it.chavaraPart
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
        // Profile Photo Area
        Box(
            modifier = Modifier
                .offset(x = 39.dp, y = 122.dp)
                .size(width = 170.dp, height = 209.dp)
                .rotate(-0.143f)
                .clickable { onEditPhotoClick() }
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
            // Show actual member photo if available
            if (!isNewMember && name.isNotEmpty() && repository != null) {
                try {
                    val member = repository.getMemberById(memberId)
                    member?.let {
                        AsyncMemberImage(
                            imageUrl = it.photoUrl,
                            memberName = it.name,
                            size = 150.dp,
                            cornerRadius = 16.dp
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Main Content Card
        Box(
            modifier = Modifier
                .offset(x = 16.dp, y = 208.dp)
                .size(width = 388.dp, height = 833.dp)
        ) {
            // Background card with gradient
            Box(
                modifier = Modifier
                    .offset(x = 13.dp, y = 143.dp)
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

            // Jesus image overlay
            Image(
                painter = painterResource(id = R.drawable.jesus),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = (-2).dp, y = 133.dp)
                    .size(width = 388.dp, height = 690.dp)
                    .alpha(1f),
                contentScale = ContentScale.Crop
            )

            // Profile Photo text
            Text(
                text = "Profile Photo",
                color = Color.White,
                fontFamily = ris,
                fontSize = 27.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .offset(x = 58.dp, y = 0.dp)
                    .size(width = 139.dp, height = 95.dp)
            )

            // Form fields with input capability
            EditableFormField(
                label = "Name",
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.offset(x = 48.dp, y = 183.dp)
            )

            EditableFormField(
                label = "Course",
                value = course,
                onValueChange = { course = it },
                modifier = Modifier.offset(x = 48.dp, y = 230.dp)
            )

            EditableFormField(
                label = "Birth Day (DD/MM/YYYY)",
                value = birthDay,
                onValueChange = { birthDay = it },
                modifier = Modifier.offset(x = 48.dp, y = 277.dp)
            )

            EditableFormField(
                label = "Phone Number",
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.offset(x = 48.dp, y = 324.dp)
            )

            EditableFormField(
                label = "Residence",
                value = residence,
                onValueChange = { residence = it },
                modifier = Modifier.offset(x = 48.dp, y = 371.dp)
            )

            EditableFormField(
                label = "Email Address",
                value = emailAddress,
                onValueChange = { emailAddress = it },
                modifier = Modifier.offset(x = 48.dp, y = 418.dp)
            )

            // Chavara Part field (larger)
            EditableFormField(
                label = "How do you want to be part of Chavara Youth?",
                value = chavaraPart,
                onValueChange = { chavaraPart = it },
                modifier = Modifier.offset(x = 48.dp, y = 465.dp),
                isLarge = true
            )

            // Save button
            Box(
                modifier = Modifier
                    .offset(x = 148.dp, y = 560.dp)
                    .size(width = 68.dp, height = 41.dp)
                    .clickable {
                        if (!isSaving && repository != null) {
                            coroutineScope.launch {
                                isSaving = true

                                try {
                                    val member = FamilyMember(
                                        id = if (isNewMember) System.currentTimeMillis().toInt() else memberId,
                                        name = name,
                                        course = course,
                                        birthday = birthDay,
                                        phoneNumber = phoneNumber,
                                        residence = residence,
                                        emailAddress = emailAddress,
                                        chavaraPart = chavaraPart,
                                        photoUrl = "", // Default or from state (add if needed)
                                        videoUrl = "", // Default or from state
                                        submissionDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                            .format(Date())
                                    )

                                    val saved = repository.saveFamilyMember(member)

                                    if (saved) {
                                        onSaveComplete()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                isSaving = false
                            }
                        }
                    }
                    .background(
                        Color(0xFF4EDAE9),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .alpha(if (isSaving) 0.5f else 0.7f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSaving) "..." else "Save",
                    color = Color.Black,
                    fontFamily = ris,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // Edit button
        Box(
            modifier = Modifier
                .offset(x = 234.dp, y = 263.dp)
                .size(width = 56.dp, height = 68.dp)
                .clickable { onEditPhotoClick() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                painter = painterResource(id = R.drawable.ic_edit),
                contentDescription = "Edit",
                modifier = Modifier
                    .offset(x = 3.dp, y = 12.dp)
                    .size(width = 49.dp, height = 46.dp)
            )
        }
    }
}

@Composable
private fun EditableFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val fieldHeight = if (isLarge) 94.dp else 51.dp

    Box(
        modifier = modifier
            .width(268.dp) // Set width directly
            .height(fieldHeight)
            .background(
                Color(0x3C04747C),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 20.dp), // Add padding for the content inside
        contentAlignment = Alignment.CenterStart // Align text to the start (left)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle( // This is the style for the text the user types
                color = Color.White, // Changed to white to be visible
                fontFamily = ris,
                fontSize = 25.sp,
                fontWeight = FontWeight.Normal
            ),
            decorationBox = { innerTextField ->
                // This box allows us to place a placeholder
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        // If the input is empty, show the label as a placeholder
                        Text(
                            text = label,
                            color = Color.White.copy(alpha = 0.7f), // Lighter color for placeholder
                            fontFamily = ris,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    // This is where the user's actual typed text will appear
                    innerTextField()
                }
            },
            singleLine = !isLarge
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
fun FamilyMemberScreenPreview() {
    FamilyMemberScreen(isNewMember = true) // Preview with the save button visible
}