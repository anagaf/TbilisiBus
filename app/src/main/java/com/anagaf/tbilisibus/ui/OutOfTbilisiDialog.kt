package com.anagaf.tbilisibus.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anagaf.tbilisibus.R

@Composable
internal fun OutOfTbilisiDialog(
    onMoveAccepted: () -> Unit,
    onDismissed: () -> Unit
) {
    AlertDialog(
        title = {
            Text(text = stringResource(R.string.app_name))
        },
        confirmButton = {
            Button(
                onClick = {
                    onMoveAccepted()
                },
            ) {
                Text(text = stringResource(R.string.go_to_tbilisi))
            }
        },
        dismissButton = {
            Button(
                onClick = { onDismissed() }
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        text = {
            Text(fontSize = kFontSize, text = stringResource(R.string.out_of_tbilisi))
        },
        onDismissRequest = {}
    )
}
