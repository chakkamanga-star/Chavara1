package com.sj9.chavara.ui.family

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import android.util.Log
import androidx.compose.material3.Text // Keep this if used elsewhere, or remove if not
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.repository.ChavaraRepository
import com.sj9.chavara.ui.components.AsyncMemberImage
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
    initialPhotoUrl: String = "", // Add this to allow parent to pass initial/updated photo URL
    onEditPhotoClick: () -> Unit = {},
    onSaveComplete: (FamilyMember) -> Unit = {} // Consider passing the saved member back
) {
    val context = LocalContext.current
    val repository = remember {
        try {
            ChavaraRepository(context) // Non-Composable call
        } catch (e: Exception) {
            // Log error or show a message to the user via a state variable
            e.printStackTrace()
            null // Repository initialization failed
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
    var photoUrl by remember { mutableStateOf(initialPhotoUrl) } // State for photo URL
    var isSaving by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // For loading indicator

    // Update photoUrl if initialPhotoUrl changes from parent (e.g., after photo edit)
    LaunchedEffect(initialPhotoUrl) {
        if (initialPhotoUrl.isNotEmpty()) {
            photoUrl = initialPhotoUrl
        }
    }

    // Load existing member data if not new
    LaunchedEffect(memberId, repository) {
        if (!isNewMember && memberId > 0) {
            val member = viewModel.getMemberById(memberId)
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
                    photoUrl = it.photoUrl // Populate photoUrl state
                    // If initialPhotoUrl was also provided, decide precedence or merge logic
                    if (initialPhotoUrl.isNotEmpty() && initialPhotoUrl != it.photoUrl) {
                        // This might happen if parent updates photoUrl while data is also loading
                        // Decide which one takes precedence or how to handle.
                        // For now, let's assume loaded data is more current for existing member.
                        photoUrl = it.photoUrl
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error, e.g., show a Snackbar or a message via a state variable
            }
            isLoading = false
        } else if (isNewMember) {
            // If it's a new member, ensure fields are clear or set to defaults
            // (they are already from mutableStateOf(""), but good for explicitness)
            name = ""
            course = ""
            birthDay = ""
            phoneNumber = ""
            residence = ""
            emailAddress = ""
            chavaraPart = ""
            photoUrl = initialPhotoUrl // Use initialPhotoUrl if provided for new member
            isLoading = false
        }
    }

    if (repository == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: Could not initialize repository. Please try again later.", color = Color.Red)
        }
        return // Stop rendering if repository is null
    }

    if (isLoading && !isNewMember) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Replace with your actual CircularProgressIndicator or loading UI
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
                // Show actual member photo if available (or placeholder via AsyncMemberImage)
                // AsyncMemberImage will handle empty photoUrl by showing initials or its own placeholder
                AsyncMemberImage(
                    imageUrl = photoUrl, // Use the photoUrl state
                    memberName = name.ifEmpty { "Member" }, // Provide a fallback name if name is empty
                    size = 150.dp,
                    cornerRadius = 16.dp
                )
            }

            // Main Content Card
            Box(
                modifier = Modifier
                    .offset(x = 16.dp, y = 208.dp)
                    .size(width = 388.dp, height = 833.dp) // Consider if this size needs to be dynamic
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
                        .alpha(1f), // Ensure alpha is between 0f and 1f
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
                        .size(width = 139.dp, height = 95.dp) // Consider using wrapContentSize
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
                        .clickable(enabled = !isSaving && repository != null) {
                            // Basic validation (optional, enhance as needed)
                            if (name.isBlank()) {
                                // Show some error to the user (e.g., Toast, Snackbar, or update a state variable)
                                println("Name cannot be empty") // Replace with user-facing error
                                return@clickable
                            }

                            coroutineScope.launch {
                                isSaving = true
                                try {
                                    val memberToSave = FamilyMember(
                                        id = if (isNewMember) 0 else memberId, // For new member, ID is typically set by backend/DB
                                        name = name,
                                        course = course,
                                        birthday = birthDay,
                                        phoneNumber = phoneNumber,
                                        residence = residence,
                                        emailAddress = emailAddress,
                                        chavaraPart = chavaraPart,
                                        photoUrl = photoUrl, // Use the current photoUrl state
                                        videoUrl = "", // Default or from state
                                        submissionDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                            .format(Date())
                                    )

                                    // 'saveFamilyMember' from your repository returns Boolean
                                    val wasSaveSuccessful: Boolean = repository!!.saveFamilyMember(memberToSave)

                                    if (wasSaveSuccessful) {
                                        // If save was successful, call onSaveComplete with the
                                        // FamilyMember object you attempted to save.
                                        onSaveComplete(memberToSave)
                                    } else {
                                        // Handle the case where saving to the repository failed
                                        // You might want to show an error message to the user here
                                        // using a Snackbar or updating another state variable.
                                        Log.e("FamilyMemberScreen", "Failed to save family member.")
                                        // Optionally, you could have an onSaveFailed callback or similar.
                                    }

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    // Handle other exceptions (e.g., network issues if not caught in repo)
                                    Log.e("FamilyMemberScreen", "Exception while saving family member", e)
                                } finally {
                                    isSaving = false
                                }
                            }
// ...
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

            // Edit button for Photo
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
                        .align(Alignment.Center) // Center the icon
                        .size(width = 30.dp, height = 30.dp) // Adjust size as needed
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
            .background(
                Color(0x3C04747C), // Consider using Color.Transparent and a Border if this is meant to be an outline
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp), // Adjusted padding
        contentAlignment = if (isLarge) Alignment.TopStart else Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(), // Fill the padded Box
            textStyle = TextStyle(
                color = Color.White,
                fontFamily = ris,
                fontSize = 20.sp, // Slightly reduced for better fit
                fontWeight = FontWeight.Normal
            ),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) { // Ensure decoration box fills
                    if (value.isEmpty()) {
                        Text(
                            text = label,
                            color = Color.White.copy(alpha = 0.7f),
                            fontFamily = ris,
                            fontSize = 20.sp, // Match text style
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.align(if (isLarge) Alignment.TopStart else Alignment.CenterStart)
                        )
                    }
                    innerTextField() // This is where the actual text input appears
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
    // Wrap in your theme if needed, e.g., ChavaraTheme { ... }
    FamilyMemberScreen(isNewMember = true, onSaveComplete = {})
}

@Preview(showBackground = true, name = "Existing Member")
@Composable
fun FamilyMemberScreenExistingPreview() {
    // Wrap in your theme if needed
    FamilyMemberScreen(
        memberId = 1,
        isNewMember = false,
        initialPhotoUrl = "https://via.placeholder.com/150", // Example URL
        onSaveComplete = {}
    )
}
