package com.maxrave.kotlinytmusicscraper.models

import com.maxrave.kotlinytmusicscraper.models.response.LikeStatus
import kotlinx.serialization.Serializable

@Serializable
data class Menu(
    val menuRenderer: MenuRenderer,
) {
    @Serializable
    data class MenuRenderer(
        val items: List<Item>,
        val topLevelButtons: List<TopLevelButton>?,
    ) {
        @Serializable
        data class Item(
            val menuNavigationItemRenderer: MenuNavigationItemRenderer? = null,
            val menuServiceItemRenderer: MenuServiceItemRenderer? = null,
            val toggleMenuServiceItemRenderer: ToggleMenuServiceItemRenderer? = null,
        ) {
            @Serializable
            data class MenuNavigationItemRenderer(
                val text: Runs,
                val icon: Icon,
                val navigationEndpoint: NavigationEndpoint,
            )

            @Serializable
            data class MenuServiceItemRenderer(
                val text: Runs,
                val icon: Icon,
                val serviceEndpoint: NavigationEndpoint,
            )

            @Serializable
            data class ToggleMenuServiceItemRenderer(
                val defaultText: Runs? = null,
                val defaultIcon: Icon? = null,
                val defaultServiceEndpoint: NavigationEndpoint? = null,
                val toggledText: Runs? = null,
                val toggledIcon: Icon? = null,
                val toggledServiceEndpoint: NavigationEndpoint? = null,
                val isToggled: Boolean? = null,
            )
        }

        @Serializable
        data class TopLevelButton(
            val buttonRenderer: ButtonRenderer?,
            val likeButtonRenderer: LikeButtonRenderer? = null,
        ) {
            @Serializable
            data class LikeButtonRenderer(
                val likeStatus: String,
                val likesAllowed: Boolean,
            ) {
                fun toLikeStatus(): LikeStatus =
                    if (likesAllowed) {
                        when (likeStatus) {
                            "LIKE" -> LikeStatus.LIKE
                            "DISLIKE" -> LikeStatus.DISLIKE
                            else -> LikeStatus.INDIFFERENT
                        }
                    } else {
                        LikeStatus.INDIFFERENT
                    }
            }

            @Serializable
            data class ButtonRenderer(
                val icon: Icon,
                val navigationEndpoint: NavigationEndpoint,
            )
        }
    }
}

/** Extract library / algorithm feedback tokens from a YT Music item menu. */
fun Menu.extractFeedbackTokens(): SongItemFeedbackTokens {
    var add: String? = null
    var remove: String? = null
    var notInterested: String? = null
    var dontRecommend: String? = null

    fun runsText(runs: Runs?): String =
        runs?.runs?.joinToString("") { it.text }.orEmpty()

    for (item in menuRenderer.items) {
        val service = item.menuServiceItemRenderer
        if (service != null) {
            val icon = service.icon.iconType
            val label = runsText(service.text).lowercase()
            val token = service.serviceEndpoint.resolveFeedbackToken()
            if (token != null) {
                when {
                    icon == "NOT_INTERESTED" || label.contains("not interested") ->
                        notInterested = token
                    label.contains("don't recommend") ||
                        label.contains("dont recommend") ||
                        label.contains("do not recommend") ||
                        (icon == "REMOVE" && (label.contains("recommend") || label.contains("artist") || label.contains("channel"))) ->
                        dontRecommend = token
                    icon == "REMOVE_FROM_HISTORY" ->
                        if (notInterested == null) notInterested = token
                    label.contains("recommend") && dontRecommend == null ->
                        dontRecommend = token
                }
            }
        }

        val toggle = item.toggleMenuServiceItemRenderer
        if (toggle != null) {
            val icon =
                toggle.defaultIcon?.iconType
                    ?: toggle.toggledIcon?.iconType
            when (icon) {
                "BOOKMARK_BORDER", "BOOKMARK", "LIBRARY_ADD", "LIBRARY_SAVED", "LIBRARY_REMOVE" -> {
                    val defaultTok = toggle.defaultServiceEndpoint?.resolveFeedbackToken()
                    val toggledTok = toggle.toggledServiceEndpoint?.resolveFeedbackToken()
                    when (icon) {
                        "BOOKMARK_BORDER", "LIBRARY_ADD" -> {
                            add = defaultTok
                            remove = toggledTok
                        }
                        else -> {
                            add = toggledTok
                            remove = defaultTok
                        }
                    }
                }
            }
        }
    }

    return SongItemFeedbackTokens(
        add = add,
        remove = remove,
        notInterested = notInterested,
        dontRecommend = dontRecommend,
    )
}
