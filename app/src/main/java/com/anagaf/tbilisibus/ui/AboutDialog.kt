package com.anagaf.tbilisibus.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anagaf.tbilisibus.BuildConfig
import com.anagaf.tbilisibus.R


val kFontSize = 16.sp

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
                Text(text = stringResource(R.string.ok))
            }
        },
        text = {
            val spacerSize = 12.dp
            Column {
                Text(fontSize = kFontSize, text = "Version ${BuildConfig.VERSION_NAME}")
                Spacer(modifier = Modifier.size(spacerSize))
                Text(fontSize = kFontSize, text = "Developed by Andrei Agafonov")
                Spacer(modifier = Modifier.size(spacerSize))
                IconsByText()
            }
        },
        onDismissRequest = {}
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun IconsByText() {
    val icon8Tag = "icon8"
    val text = buildAnnotatedString {
        append("Icons by ")
        withAnnotation(icon8Tag, "annotation") {
            append(
                AnnotatedString(
                    "Icon8",
                    spanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
                )
            )
        }
    }
    val uriHandler = LocalUriHandler.current
    ClickableText(text = text, style = TextStyle(fontSize = kFontSize)) {
        text.getStringAnnotations(it, it).firstOrNull()?.tag?.let { tag ->
            if (tag == icon8Tag) {
                uriHandler.openUri("https://icons8.com/")
            }
        }
    }
}

@Preview
@Composable
fun AboutDialogPreview() {
    AboutDialog {}
}
