package network.bisq.mobile.presentation.ui.components.organisms

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.composeModels.PagerViewItem
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@Composable
fun BisqPagerView(pagerState: PagerState, pageItems: List<PagerViewItem>) {

    CompositionLocalProvider(values = arrayOf()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically),
        ) {
            HorizontalPager(
                pageSpacing = 56.dp,
                contentPadding = PaddingValues(horizontal = 36.dp),
                pageSize = PageSize.Fill,
                verticalAlignment = Alignment.CenterVertically,
                state = pagerState
            ) { index ->
                pageItems.getOrNull(
                    index % (pageItems.size)
                )?.let { item ->
                    PagerSingleItem(
                        image = item.image,
                        title = item.title,
                        desc = item.desc,
                        index = index,
                    )
                }
            }
            PagerLineIndicator(pagerState = pagerState)
        }
    }

}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun PagerSingleItem(
    title: String,
    image: DrawableResource,
    desc: String,
    index: Int
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(color = BisqTheme.colors.dark_grey30)
            .padding(vertical = 56.dp)
    ) {
        Image(painterResource(image), title, modifier = Modifier.size(120.dp))
        BisqGap.V4()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BisqText.h4Regular(title, textAlign = TextAlign.Center)
            BisqGap.V2()
            BisqText.largeRegularGrey(
                text = desc,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PagerLineIndicator(pagerState: PagerState) {
    Box(
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pagerState.pageCount) {
                Box(
                    modifier = Modifier
                        .size(width = 76.dp, height = 2.dp)
                        .background(color = BisqTheme.colors.mid_grey20)
                )
            }
        }
        Box(
            Modifier
                .slidingLineTransition(
                    pagerState,
                    76f * LocalDensity.current.density
                )
                .size(width = 76.dp, height = 3.dp)
                .background(
                    color = BisqTheme.colors.primary,
                    shape = RoundedCornerShape(4.dp),
                )
        )
    }
}

fun Modifier.slidingLineTransition(pagerState: PagerState, distance: Float) =
    graphicsLayer {
        val scrollPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
        translationX = scrollPosition * distance
    }
