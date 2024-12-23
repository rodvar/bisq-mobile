package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqTheme

enum class BisqButtonType {
    Default,
    Outline,
    Clear
}

/**
 * Either pass
 *  - iconOnly for Icon only button (or)
 *  - textComponent for button with custom styled text (or)
 *  - text for regular button
 */
@Composable
fun BisqButton(
    text: String? = "Button",
    onClick: () -> Unit,
    color: Color = BisqTheme.colors.light1,
    backgroundColor: Color = BisqTheme.colors.primary,
    padding: PaddingValues = PaddingValues(horizontal = 48.dp, vertical = 4.dp),
    iconOnly: (@Composable () -> Unit)? = null,
    leftIcon: (@Composable () -> Unit)? = null,
    rightIcon: (@Composable () -> Unit)? = null,
    textComponent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    disabled: Boolean = false,
    isLoading: Boolean = false,
    border: BorderStroke? = null,
    type: BisqButtonType = BisqButtonType.Default
) {

    val enabled = !disabled && !isLoading

    val finalBackgroundColor = when (type) {
        BisqButtonType.Default -> backgroundColor
        BisqButtonType.Outline -> Color.Transparent
        BisqButtonType.Clear -> Color.Transparent
    }

    val finalBorder = when (type) {
        BisqButtonType.Default -> border
        BisqButtonType.Outline -> BorderStroke(1.dp, BisqTheme.colors.primary)
        BisqButtonType.Clear -> null
    }

    val finalContentColor = color

    Button(
        onClick = { onClick() },
        contentPadding = if (iconOnly != null) PaddingValues(horizontal = 0.dp, vertical = 0.dp) else padding,
        colors = ButtonColors(
            containerColor = finalBackgroundColor,
            disabledContainerColor = finalBackgroundColor.copy(alpha = 0.5f),
            contentColor = finalContentColor,
            disabledContentColor = finalContentColor.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(cornerRadius),
        enabled = enabled,
        border = finalBorder,
        modifier = modifier
    ) {
        if (iconOnly == null && text == null && textComponent == null) {
            BisqText.baseMedium("Error: Pass either text or customText or icon")
        }

        if (iconOnly != null) {
            iconOnly()
        } else if (text != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = BisqTheme.colors.light1,
                        strokeWidth = 2.dp
                    )
                    BisqGap.HHalf()
                }
                if (leftIcon != null) leftIcon()
                if (leftIcon != null) Spacer(modifier = Modifier.width(10.dp))
                if (textComponent != null) {
                    textComponent()
                } else {
                    BisqText.baseMedium(
                        text = text,
                        color = BisqTheme.colors.light1,
                    )
                }
                if (rightIcon != null) Spacer(modifier = Modifier.width(10.dp))
                if (rightIcon != null) rightIcon()
            }
        }
    }
}
