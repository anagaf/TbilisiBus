package com.anagaf.tbilisibus.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.anagaf.tbilisibus.R

@Composable
internal fun RouteNumberDialog(
    onConfirmed: (number: Int) -> Unit,
    onDismissed: () -> Unit
) {
    var number by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var textFieldLoaded by remember { mutableStateOf(false) }

    AlertDialog(
        title = {
            Text(text = stringResource(R.string.choose_route))
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmed(number.toInt())
                },
                enabled = number.isNotEmpty()
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
        text = {
            TextField(
                value = number,
                onValueChange = {
                    if (it.isEmpty() || (it.length <= 3 && it.toIntOrNull() != null)) {
                        number = it
                    }
                },
                label = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        // it's necessary to make sure that the text field has been initialised
                        // before requesting the focus (see https://stackoverflow.com/a/75104192)
                        if (!textFieldLoaded) {
                            focusRequester.requestFocus()
                            textFieldLoaded = true // stop cyclic recompositions
                        }
                    }
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        onDismissRequest = {}
    )
}
