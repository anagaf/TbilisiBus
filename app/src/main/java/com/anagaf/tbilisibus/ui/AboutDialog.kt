package com.anagaf.tbilisibus.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anagaf.tbilisibus.R

@Composable
internal fun AboutDialog(
    onDismissed: () -> Unit
) {
    AlertDialog(
        title = {
            Text(text = stringResource(R.string.app_name))
        },
        confirmButton = {},
        dismissButton = {
            Button(
                onClick = {
                    onDismissed()
                }
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        text = { Text(text = "(c)Andrei Agafonov") },
        onDismissRequest = {}
    )
}