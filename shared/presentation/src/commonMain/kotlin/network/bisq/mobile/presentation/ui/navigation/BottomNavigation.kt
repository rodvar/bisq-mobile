package network.bisq.mobile.presentation.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.animations.AnimatedBadge
import network.bisq.mobile.presentation.ui.composeModels.BottomNavigationItem
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun BottomNavigation(
    items: List<BottomNavigationItem>,
    currentRoute: String,
    unreadTradeCount: Int,
    showAnimation: Boolean,
    onItemClick: (BottomNavigationItem) -> Unit
) {

    val MY_TRADES_TAB_INDEX = 2

    NavigationBar(
        containerColor = BisqTheme.colors.backgroundColor
    ) {
        items.forEachIndexed { index, navigationItem ->
            NavigationBarItem(
                colors = NavigationBarItemColors(
                    selectedIndicatorColor = BisqTheme.colors.backgroundColor,
                    selectedIconColor = BisqTheme.colors.primary,
                    selectedTextColor = BisqTheme.colors.primary,
                    unselectedIconColor = Color.White,
                    unselectedTextColor = Color.White,
                    disabledIconColor = Color.Red,
                    disabledTextColor = Color.Red
                ),
                interactionSource = remember { MutableInteractionSource() },
                selected = currentRoute == navigationItem.route,
                onClick = { onItemClick(navigationItem) },
                icon = {

                    val icon = @Composable {
                        Image(
                            painter = painterResource(navigationItem.icon),
                            contentDescription = "",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(
                                color = if (navigationItem.route == currentRoute) BisqTheme.colors.primary else Color.White
                            )
                        )
                    }

                    if (index == MY_TRADES_TAB_INDEX && unreadTradeCount > 0) {
                        BadgedBox(
                            badge = {
                                AnimatedBadge(showAnimation = showAnimation) {
                                    BisqText.xsmallLight(
                                        unreadTradeCount.toString(),
                                        textAlign = TextAlign.Center,
                                        color = BisqTheme.colors.dark_grey20
                                    )
                                }
                            }
                        ) {
                            icon()
                        }
                    } else {
                        icon()
                    }
                },
                label = {
                    BisqText.baseRegular(
                        text = navigationItem.title.i18n(),
                        color = if (navigationItem.route == currentRoute) BisqTheme.colors.primary else BisqTheme.colors.white,
                    )
                }
            )
        }
    }
}