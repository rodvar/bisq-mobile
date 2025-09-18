package network.bisq.mobile.presentation.ui.helpers

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString

expect fun AnnotatedString?.toClipEntry(): ClipEntry?

expect suspend fun ClipEntry.readText(): String?