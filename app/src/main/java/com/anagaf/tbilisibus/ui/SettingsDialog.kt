package com.anagaf.tbilisibus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.RadioButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.anagaf.tbilisibus.R

@Composable
internal fun SettingsDialog(
    uiAlignment: UiAlignment,
    onConfirmed: (UiAlignment) -> Unit,
    onDismissed: () -> Unit
) {
    val newUiAlignment: MutableState<UiAlignment> = remember {
        mutableStateOf(uiAlignment)
    }
    AlertDialog(
        title = {
            Text(
                text = stringResource(R.string.settings),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(R.string.buttons_alignment),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,

                    ) {
                    RadioButton(
                        onClick = { newUiAlignment.value = UiAlignment.Right },
                        selected = (newUiAlignment.value == UiAlignment.Right),
                    )
                    Text(
                        text = stringResource(R.string.right),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        onClick = { newUiAlignment.value = UiAlignment.Left },
                        selected = (newUiAlignment.value == UiAlignment.Left),
                    )
                    Text(
                        text = stringResource(R.string.left),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmed(newUiAlignment.value)
                },
            ) {
                Text(text = stringResource(R.string.ok))
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    onDismissed()
                }
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        onDismissRequest = {}
    )
}

@Preview
@Composable
fun SettingsDialogPreview() {
    SettingsDialog(
        uiAlignment = UiAlignment.Right,
        onConfirmed = {},
        onDismissed = {})
}
