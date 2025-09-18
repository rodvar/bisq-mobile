@file:OptIn(ExperimentalComposeUiApi::class)

package network.bisq.mobile.presentation.ui.helpers

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString

actual fun AnnotatedString?.toClipEntry(): ClipEntry? {
    if (this == null) return null
    return ClipEntry.withPlainText(this.text)
}

actual suspend fun ClipEntry.readText(): String? = getPlainText()