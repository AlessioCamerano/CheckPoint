package com.example.checkpoint.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.checkpoint.R
import com.example.checkpoint.data.local.entity.UserEntity
import com.example.checkpoint.ui.components.UserItem
import com.example.checkpoint.ui.state.MainUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalUsersScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onRadiusChange: (Int) -> Unit,
    onSearchNearbyUsers: (Double, Double) -> Unit,
    onUserClick: (UserEntity) -> Unit
) {
    val context = LocalContext.current
    val locationRequiredText = stringResource(R.string.local_users_location_required)
    var locationError by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                locationError = locationRequiredText
            } else {
                fetchCurrentLocation(context, onSearchNearbyUsers, onError = { locationError = it })
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.local_users_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = stringResource(R.string.local_users_radius_label, state.localUsersRadiusKm))
            Slider(
                value = state.localUsersRadiusKm.toFloat(),
                onValueChange = { onRadiusChange(it.roundToInt()) },
                valueRange = 1f..150f
            )

            Button(
                onClick = {
                    val permission = Manifest.permission.ACCESS_FINE_LOCATION
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        fetchCurrentLocation(context, onSearchNearbyUsers, onError = { locationError = it })
                    } else {
                        launcher.launch(permission)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (state.isLocalUsersLoading) {
                        stringResource(R.string.local_users_searching)
                    } else {
                        stringResource(R.string.local_users_search)
                    }
                )
            }

            locationError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            state.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

            if (state.localUsers.isEmpty() && !state.isLocalUsersLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.local_users_no_results))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.localUsers) { geoUser ->
                        UserItem(user = geoUser.user, onClick = onUserClick)
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    context: android.content.Context,
    onSearchNearbyUsers: (Double, Double) -> Unit,
    onError: (String) -> Unit
) {
    val locationManager = context.getSystemService(android.location.LocationManager::class.java)
        as? android.location.LocationManager

    val location = locationManager?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
        ?: locationManager?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

    if (location == null) {
        onError(context.getString(R.string.local_users_updating_position))
    } else {
        onSearchNearbyUsers(location.latitude, location.longitude)
    }
}
