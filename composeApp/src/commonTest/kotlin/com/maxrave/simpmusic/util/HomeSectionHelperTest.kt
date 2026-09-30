package com.maxrave.simpmusic.util

import kotlin.test.Test
import kotlin.test.assertFalse
import com.maxrave.domain.extension.isVideoContent
import kotlin.test.assertTrue

class HomeSectionHelperTest {

    @Test
    fun testListenAgainSectionMatching() {
        // English variations
        assertTrue(isListenAgainSection("Listen again"))
        assertTrue(isListenAgainSection("listen again"))
        assertTrue(isListenAgainSection("LISTEN AGAIN"))
        assertTrue(isListenAgainSection("Listen again for today"))

        // International translations
        assertTrue(isListenAgainSection("Nghe lại"))
        assertTrue(isListenAgainSection("Volver a escuchar"))
        assertTrue(isListenAgainSection("Réécouter"))
        assertTrue(isListenAgainSection("Wieder anhören"))
        assertTrue(isListenAgainSection("Ouvir novamente"))
        assertTrue(isListenAgainSection("Слушать снова"))
        assertTrue(isListenAgainSection("Riascolta"))
        assertTrue(isListenAgainSection("もう一度聴く"))
        assertTrue(isListenAgainSection("다시 듣기"))
        assertTrue(isListenAgainSection("重温"))
        assertTrue(isListenAgainSection("再聽一次"))
        assertTrue(isListenAgainSection("再次聆聽"))
        assertTrue(isListenAgainSection("إعادة الاستماع"))
        assertTrue(isListenAgainSection("Tekrar dinle"))
        assertTrue(isListenAgainSection("ฟังอีกครั้ง"))
        assertTrue(isListenAgainSection("Dengarkan lagi"))

        // Localized parameter matching
        assertTrue(isListenAgainSection("Custom Listen Again Title", localizedListenAgain = "Custom Listen Again Title"))

        // Non-matching titles
        assertFalse(isListenAgainSection("Quick picks"))
        assertFalse(isListenAgainSection("Mixed for you"))
        assertFalse(isListenAgainSection("Albums for you"))
        assertFalse(isListenAgainSection("Similar to Taylor Swift"))
        assertFalse(isListenAgainSection("Trending"))
        assertFalse(isListenAgainSection(""))
    }

    @Test
    fun testQuickPicksSectionMatching() {
        // English variations
        assertTrue(isQuickPicksSection("Quick picks"))
        assertTrue(isQuickPicksSection("quick picks"))
        assertTrue(isQuickPicksSection("Quick pick"))
        assertTrue(isQuickPicksSection("QUICK PICKS"))
        assertTrue(isQuickPicksSection("Quick picks for you"))

        // International translations
        assertTrue(isQuickPicksSection("Chọn nhanh"))
        assertTrue(isQuickPicksSection("Chọn nhanh đài phát"))
        assertTrue(isQuickPicksSection("Sélection rapide"))
        assertTrue(isQuickPicksSection("Selección rápida"))
        assertTrue(isQuickPicksSection("Schnelle Auswahl"))
        assertTrue(isQuickPicksSection("Escolhas rápidas"))
        assertTrue(isQuickPicksSection("Быстрый выбор"))
        assertTrue(isQuickPicksSection("Scelte rapide"))
        assertTrue(isQuickPicksSection("クイック選択"))
        assertTrue(isQuickPicksSection("빠른 선곡"))
        assertTrue(isQuickPicksSection("快速选择"))
        assertTrue(isQuickPicksSection("快速挑選"))
        assertTrue(isQuickPicksSection("精選歌曲"))
        assertTrue(isQuickPicksSection("اختيارات سريعة"))
        assertTrue(isQuickPicksSection("Hızlı seçim"))
        assertTrue(isQuickPicksSection("เลือกด่วน"))
        assertTrue(isQuickPicksSection("Pilihan cepat"))

        // Localized parameter matching
        assertTrue(isQuickPicksSection("Custom Quick Picks Title", localizedQuickPicks = "Custom Quick Picks Title"))

        // Non-matching titles
        assertFalse(isQuickPicksSection("Listen again"))
        assertFalse(isQuickPicksSection("Nghe lại"))
        assertFalse(isQuickPicksSection("Mixed for you"))
        assertFalse(isQuickPicksSection("Albums for you"))
        assertFalse(isQuickPicksSection("Similar to Taylor Swift"))
        assertFalse(isQuickPicksSection(""))
    }
}


    @Test
    fun testContentSongVsVideoClassifier() {
        val songThumb =
            com.maxrave.domain.data.model.searchResult.songs.Thumbnail(120, "https://lh3.googleusercontent.com/w120-h120", 120)
        val videoThumb =
            com.maxrave.domain.data.model.searchResult.songs.Thumbnail(180, "https://i.ytimg.com/vi/x/hq720.jpg", 320)
        fun content(videoType: String?, thumb: com.maxrave.domain.data.model.searchResult.songs.Thumbnail) =
            com.maxrave.domain.data.model.home.Content(
                album = null,
                artists = null,
                description = null,
                isExplicit = null,
                playlistId = null,
                browseId = null,
                thumbnails = listOf(thumb),
                title = "t",
                videoId = "id",
                views = null,
                videoType = videoType,
            )
        assertFalse(content("MUSIC_VIDEO_TYPE_ATV", songThumb).isVideoContent())
        assertTrue(content("MUSIC_VIDEO_TYPE_OMV", videoThumb).isVideoContent())
        assertFalse(content(null, songThumb).isVideoContent())
        assertTrue(content(null, videoThumb).isVideoContent())
    }

