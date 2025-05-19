package network.bisq.mobile.presentation.ui.components.atoms.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun AnimatedBadge(
    content: @Composable RowScope.() -> Unit
) {

    val transition = rememberInfiniteTransition(label = "badgePulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 5600 // 600ms pulse + 5000ms delay
                1.2f at 300 // halfway scale up
                1f at 600 // back to normal (end of pulse)
                1f at 5600 // hold until next repeat
            },
            repeatMode = RepeatMode.Restart
        ), label = "scale"
    )

    val rotateX by transition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 5600 // 600ms pulse + 5000ms delay
                10f at 300 // halfway scale up
                0f at 600 // back to normal (end of pulse)
                0f at 5600 // hold until next repeat
            },
            repeatMode = RepeatMode.Restart
        ), label = "rotateX"
    )

    Badge(
        containerColor = BisqTheme.colors.warningHover,
        contentColor = BisqTheme.colors.dark_grey20,
        modifier = Modifier
            .offset(y = (-4).dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotateX
            }
    ) {
        content()
    }
}