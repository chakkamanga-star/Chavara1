package com.sj9.chavara.ui.family

import android.util.Log
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.R
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.ui.components.AsyncMemberImage
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.viewmodel.FamilyMembersViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FamilyMemberScreen(
    modifier: Modifier = Modifier,
    memberId: Int = 0,
    viewModel: FamilyMembersViewModel,
    isNewMember: Boolean = false,
    initialPhotoUrl: String = "",
    onEditPhotoClick: () -> Unit = {},
    onSaveComplete: (FamilyMember) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    // State for form inputs
    var name by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var birthDay by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var residence by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var chavaraPart by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf(initialPhotoUrl) }
    var isSaving by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Update photoUrl if initialPhotoUrl changes
    LaunchedEffect(initialPhotoUrl) {
        if (initialPhotoUrl.isNotEmpty()) {
            photoUrl = initialPhotoUrl
        }
    }

    // Load existing member data if not new
    LaunchedEffect(memberId) {
        if (!isNewMember && memberId > 0) {
            isLoading = true
            val member = viewModel.getMemberById(memberId)
            member?.let {
                name = it.name
                course = it.course
                birthDay = it.birthday
                phoneNumber = it.phoneNumber
                residence = it.residence
                emailAddress = it.emailAddress
                chavaraPart = it.chavaraPart
                photoUrl = it.photoUrl
            }
            isLoading = false
        } else if (isNewMember) {
            name = ""
            course = ""
            birthDay = ""
            phoneNumber = ""
            residence = ""
            emailAddress = ""
            chavaraPart = ""
            photoUrl = initialPhotoUrl
            isLoading = false
        }
    }

    if (isLoading && !isNewMember) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading member data...", color = Color.White)
        }
    } else {
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
                AsyncMemberImage(
                    imageUrl = photoUrl,
                    memberName = name.ifEmpty { "Member" },
                    size = 150.dp,
                    cornerRadius = 16.dp
                )
            }

            // Main Content Card
            Box(
                modifier = Modifier
                    .offset(x = 16.dp, y = 208.dp)
                    .size(width = 388.dp, height = 833.dp)
            ) {
                // Background card
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

                // Jesus image
                Image(
                    painter = painterResource(id = R.drawable.jesus),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = (-2).dp, y = 133.dp)
                        .size(width = 388.dp, height = 690.dp)
                        .alpha(1f),
                    contentScale = ContentScale.Crop
                )

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

                EditableFormField("Name", name, { name = it }, Modifier.offset(x = 48.dp, y = 183.dp))
                EditableFormField("Course", course, { course = it }, Modifier.offset(x = 48.dp, y = 230.dp))
                EditableFormField("Birth Day (DD/MM/YYYY)", birthDay, { birthDay = it }, Modifier.offset(x = 48.dp, y = 277.dp))
                EditableFormField("Phone Number", phoneNumber, { phoneNumber = it }, Modifier.offset(x = 48.dp, y = 324.dp))
                EditableFormField("Residence", residence, { residence = it }, Modifier.offset(x = 48.dp, y = 371.dp))
                EditableFormField("Email Address", emailAddress, { emailAddress = it }, Modifier.offset(x = 48.dp, y = 418.dp))
                EditableFormField(
                    "How do you want to be part of Chavara Youth?",
                    chavaraPart,
                    { chavaraPart = it },
                    Modifier.offset(x = 48.dp, y = 465.dp),
                    isLarge = true
                )

                // Save button
                Box(
                    modifier = Modifier
                        .offset(x = 148.dp, y = 560.dp)
                        .size(width = 68.dp, height = 41.dp)
                        .clickable(enabled = !isSaving) {
                            if (name.isBlank()) {
                                println("Name cannot be empty")
                                return@clickable
                            }
                            coroutineScope.launch {
                                isSaving = true
                                try {
                                    val memberToSave = FamilyMember(
                                        id = if (isNewMember) 0 else memberId,
                                        name = name,
                                        course = course,
                                        birthday = birthDay,
                                        phoneNumber = phoneNumber,
                                        residence = residence,
                                        emailAddress = emailAddress,
                                        chavaraPart = chavaraPart,
                                        photoUrl = photoUrl,
                                        videoUrl = "",
                                        submissionDate = SimpleDateFormat(
                                            "yyyy-MM-dd HH:mm:ss",
                                            Locale.getDefault()
                                        ).format(Date())
                                    )
                                    viewModel.saveFamilyMember(memberToSave)
                                    onSaveComplete(memberToSave)
                                } catch (e: Exception) {
                                    Log.e("FamilyMemberScreen", "Error saving member", e)
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                        .background(Color(0xFF4EDAE9), shape = RoundedCornerShape(20.dp))
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

            // Edit photo button
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
                    contentDescription = "Edit Photo",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(width = 30.dp, height = 30.dp)
                )
            }
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
            .width(268.dp)
            .height(fieldHeight)
            .background(Color(0x3C04747C), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = if (isLarge) Alignment.TopStart else Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                color = Color.White,
                fontFamily = ris,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            ),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (value.isEmpty()) {
                        Text(
                            text = label,
                            color = Color.White.copy(alpha = 0.7f),
                            fontFamily = ris,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.align(if (isLarge) Alignment.TopStart else Alignment.CenterStart)
                        )
                    }
                    innerTextField()
                }
            },
            singleLine = !isLarge,
            maxLines = if (isLarge) 5 else 1
        )
    }
}

@Preview(showBackground = true, name = "New Member")
@Composable
fun FamilyMemberScreenNewPreview() {
    // Here you'd normally inject a fake ViewModel for preview purposes
    // Using a placeholder ViewModel with no repository calls
    val fakeViewModel = object : FamilyMembersViewModel(repository = TODO()) {}
    FamilyMemberScreen(
        isNewMember = true,
        viewModel = fakeViewModel,
        onSaveComplete = {}
    )
}

@Preview(showBackground = true, name = "Existing Member")
@Composable
fun FamilyMemberScreenExistingPreview() {
    val fakeViewModel = object : FamilyMembersViewModel(repository = TODO()) {}
    FamilyMemberScreen(
        memberId = 1,
        isNewMember = false,
        initialPhotoUrl = "https://via.placeholder.com/150",
        viewModel = fakeViewModel,
        onSaveComplete = {}
    )
}
