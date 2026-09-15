package com.example.checkpoint.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.checkpoint.R
import com.example.checkpoint.ui.theme.CheckPointTheme
import com.example.checkpoint.ui.theme.spacing

private data class OnboardingSlideData(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: @Composable () -> Unit
)

@Composable
fun OnboardingScreen(
    showPermissionsStep: Boolean = true,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val slides = remember {
        listOf(
            OnboardingSlideData(
                titleRes = R.string.onboarding_slide_1_title,
                subtitleRes = R.string.onboarding_slide_1_subtitle,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ),
            OnboardingSlideData(
                titleRes = R.string.onboarding_slide_2_title,
                subtitleRes = R.string.onboarding_slide_2_subtitle,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ),
            OnboardingSlideData(
                titleRes = R.string.onboarding_slide_3_title,
                subtitleRes = R.string.onboarding_slide_3_subtitle,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ),
            OnboardingSlideData(
                titleRes = R.string.onboarding_slide_4_title,
                subtitleRes = R.string.onboarding_slide_4_subtitle,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        )
    }
    val pageCount = if (showPermissionsStep) 5 else 4
    val pagerState = rememberPagerState { pageCount }
    var permissionsRequested by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionsRequested = true
    }

    fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions.toTypedArray())
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.medium)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = MaterialTheme.spacing.small),
                pageSpacing = MaterialTheme.spacing.smallMedium,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = MaterialTheme.spacing.smallMedium
                )
            ) { page ->
                if (showPermissionsStep && page == pageCount - 1) {
                    val permissionStates = requiredPermissions.map { permission ->
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            permission
                        ) == PackageManager.PERMISSION_GRANTED
                        permission to granted
                    }
                    PermissionsSlideContent(
                        permissionStates = permissionStates,
                        onRequestPermissions = ::requestPermissions,
                        permissionsRequested = permissionsRequested
                    )
                } else {
                    val slideIndex = if (showPermissionsStep) page else page
                    IntroSlideContent(
                        title = stringResource(slides[slideIndex].titleRes),
                        subtitle = stringResource(slides[slideIndex].subtitleRes),
                        icon = slides[slideIndex].icon
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.medium),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pageCount) { index ->
                    val selected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 20.dp else 8.dp, 8.dp)
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(100)
                            )
                    )
                }
            }

            if (pagerState.currentPage == pageCount - 1) {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.spacing.extraLarge),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = stringResource(if (showPermissionsStep) R.string.onboarding_start_button else R.string.onboarding_done_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.spacing.extraLarge),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroSlideContent(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    outerVerticalPadding: Dp = MaterialTheme.spacing.medium
) {
    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = outerVerticalPadding),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(80.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        color = ringColor,
                        radius = size.minDimension / 2f
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PermissionsSlideContent(
    permissionStates: List<Pair<String, Boolean>>,
    onRequestPermissions: () -> Unit,
    permissionsRequested: Boolean,
    outerVerticalPadding: Dp = MaterialTheme.spacing.medium
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = outerVerticalPadding),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.large),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.onboarding_permissions_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))

            Text(
                text = stringResource(R.string.onboarding_permissions_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            PermissionRow(
                icon = Icons.Default.Place,
                label = stringResource(R.string.onboarding_permission_location),
                granted = permissionStates.any { it.first == Manifest.permission.ACCESS_FINE_LOCATION && it.second }
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            PermissionRow(
                icon = Icons.Default.Notifications,
                label = stringResource(R.string.onboarding_permission_notifications),
                granted = permissionStates.none { it.first == Manifest.permission.POST_NOTIFICATIONS } ||
                    permissionStates.any { it.first == Manifest.permission.POST_NOTIFICATIONS && it.second }
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    text = stringResource(
                        if (permissionsRequested) {
                            R.string.onboarding_permissions_retry
                        } else {
                            R.string.onboarding_permissions_button
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.smallMedium))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(
                if (granted) R.string.onboarding_permission_enabled else R.string.onboarding_permission_disabled
            ),
            style = MaterialTheme.typography.labelMedium,
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Preview(showSystemUi = true, name = "Onboarding Light")
@Composable
private fun OnboardingLightPreview() {
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        OnboardingScreen(showPermissionsStep = true, onFinish = {})
    }
}

@Preview(showSystemUi = true, name = "Onboarding Dark")
@Composable
private fun OnboardingDarkPreview() {
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        OnboardingScreen(showPermissionsStep = true, onFinish = {})
    }
}
