package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.extension.shimmer
import com.maxrave.simpmusic.ui.theme.LocalAppColors

@Composable
fun HomeHeroShimmer() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(LocalAppColors.current.shimmerBackground)
            .shimmer(),
    )
}

@Composable
fun HomeItemShimmer() {
    Column(Modifier.padding(vertical = 8.dp)) {
        Box(
            Modifier
                .width(180.dp)
                .height(24.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(LocalAppColors.current.shimmerBackground)
                .shimmer(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            repeat(3) {
                PlaylistShimmer()
            }
        }
    }
}

@Composable
fun PlaylistShimmer() {
    Column(
        Modifier.width(150.dp),
    ) {
        Box(
            Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(LocalAppColors.current.shimmerBackground)
                .shimmer(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            Modifier
                .width(120.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(LocalAppColors.current.shimmerBackground)
                .shimmer(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            Modifier
                .width(80.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(LocalAppColors.current.shimmerBackground)
                .shimmer(),
        )
    }
}

@Composable
fun QuickPicksShimmerItem() {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LocalAppColors.current.shimmerBackground)
                .shimmer(),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            Modifier.weight(1f),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LocalAppColors.current.shimmerBackground)
                    .shimmer(),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.45f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LocalAppColors.current.shimmerBackground)
                    .shimmer(),
            )
        }
    }
}

@Composable
fun QuickPicksShimmer() {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(140.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(LocalAppColors.current.shimmerBackground)
                    .shimmer(),
            )
            Box(
                Modifier
                    .width(72.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LocalAppColors.current.shimmerBackground)
                    .shimmer(),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        repeat(4) {
            QuickPicksShimmerItem()
        }
    }
}

@Composable
fun HomeShimmer() {
    Column(
        Modifier.padding(vertical = 12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
    ) {
        HomeHeroShimmer()
        QuickPicksShimmer()
        HomeItemShimmer()
    }
}

@Composable
fun ShimmerSearchItem() {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail shimmer
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(LocalAppColors.current.shimmerBackground)
                .shimmer()
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Text content shimmer
        Column {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LocalAppColors.current.shimmerBackground)
                    .shimmer()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LocalAppColors.current.shimmerBackground)
                    .shimmer()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LocalAppColors.current.shimmerBackground)
                    .shimmer()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShimmerSearchItemPreview() {
    ShimmerSearchItem()
}
