package com.example.checkpoint.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.checkpoint.R
import com.example.checkpoint.ui.state.ProfileUiState
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing
import com.example.checkpoint.util.MediaStorageManager

/**
 * Schermata per la modifica dei dati del profilo (username, biografia e foto profilo).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit = {},
    onSave: (String, String, String?) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf(state.username) }
    var bio by remember { mutableStateOf(state.bio) }
    var profilePictureUri by remember { mutableStateOf(state.profilePictureUri) }

    val bioCharLimit = 300

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                val savedUri = MediaStorageManager.saveMediaToInternalStorage(context, it)
                if (savedUri != null) {
                    profilePictureUri = savedUri.toString()
                }
            }
        },
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_menu_edit),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ------------------------------------------------------------------------------------------------
            // Selezione Immagine Profilo
            // ------------------------------------------------------------------------------------------------
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (profilePictureUri != null) {
                        AsyncImage(
                            model = profilePictureUri,
                            contentDescription = stringResource(R.string.profile_picture),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(MaterialTheme.spacing.extraSmall + 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))


            // ------------------------------------------------------------------------------------------------
            // Campi Modifica Dati
            // ------------------------------------------------------------------------------------------------
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.field_username)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = MaterialTheme.shapes.large,
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= bioCharLimit) bio = it },
                label = { Text(stringResource(R.string.field_bio)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = MaterialTheme.shapes.large,
                supportingText = {
                    Text(
                        text = "${bio.length} / $bioCharLimit",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large + MaterialTheme.spacing.small))


            // ------------------------------------------------------------------------------------------------
            // Pulsante di Salvataggio
            // ------------------------------------------------------------------------------------------------
            Button(
                onClick = { onSave(username, bio, profilePictureUri) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.profile_edit_save),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Preview(showSystemUi = true, name = "Edit Profile Dark")
@Composable
fun EditProfileScreenDarkPreview() {
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        EditProfileScreen(
            state = ProfileUiState(
                username = "User Test 1",
                bio = "Developer & Gamer",
                isDarkTheme = true,
            ),
        )
    }
}

@Preview(showSystemUi = true, name = "Edit Profile Light")
@Composable
fun EditProfileScreenLightPreview() {
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        EditProfileScreen(
            state = ProfileUiState(
                username = "User Test 1",
                bio = "Developer & Gamer",
                isDarkTheme = false,
            ),
        )
    }
}
