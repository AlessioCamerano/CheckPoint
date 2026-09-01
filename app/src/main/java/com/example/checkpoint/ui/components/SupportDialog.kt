package com.example.checkpoint.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.checkpoint.ui.theme.CheckPointTheme

@Composable
fun SupportDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(com.example.checkpoint.R.string.support_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Text(
                text = stringResource(com.example.checkpoint.R.string.support_message, "example@gmail.com"),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.example.checkpoint.R.string.action_close), color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Preview(name = "Support Dialog Dark", showSystemUi = true)
@Composable
fun SupportDialogDarkPreview() {
    CheckPointTheme(darkTheme = true, dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SupportDialog(onDismiss = {})
        }
    }
}

@Preview(name = "Support Dialog Light", showSystemUi = true)
@Composable
fun SupportDialogLightPreview() {
    CheckPointTheme(darkTheme = false, dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SupportDialog(onDismiss = {})
        }
    }
}
