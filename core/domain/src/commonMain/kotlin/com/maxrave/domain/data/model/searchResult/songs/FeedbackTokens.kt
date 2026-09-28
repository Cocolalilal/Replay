package com.maxrave.domain.data.model.searchResult.songs

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackTokens(
    val add: String? = null,
    val remove: String? = null,
    /** YT Music "Not interested" algorithm feedback token. */
    val notInterested: String? = null,
    /** YT Music "Don't recommend artist/channel" feedback token. */
    val dontRecommend: String? = null,
) {
    fun anyAlgorithmToken(): Boolean = !notInterested.isNullOrBlank() || !dontRecommend.isNullOrBlank()
}
