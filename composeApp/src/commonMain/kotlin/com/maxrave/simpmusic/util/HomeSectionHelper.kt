package com.maxrave.simpmusic.util

/**
 * Checks if a section title from YouTube Music corresponds to the "Listen again" shelf.
 */
fun isListenAgainSection(title: String, localizedListenAgain: String? = null): Boolean {
    if (title.isBlank()) return false
    if (localizedListenAgain != null && localizedListenAgain.isNotBlank() && title.equals(localizedListenAgain.trim(), ignoreCase = true)) {
        return true
    }
    val t = title.trim().lowercase()
    return t == "listen again" ||
        t.startsWith("listen again") ||
        t.contains("listen again") ||
        (t.contains("listen") && t.contains("again")) ||
        t.contains("nghe lại") ||
        t.contains("volver a escuchar") ||
        t.contains("escuchar de nuevo") ||
        t.contains("réécouter") ||
        t.contains("wieder anhören") ||
        t.contains("ouvir novamente") ||
        t.contains("слушать снова") ||
        t.contains("riascolta") ||
        t.contains("もう一度聴く") ||
        t.contains("다시 듣기") ||
        t.contains("重温") ||
        t.contains("再聽一次") ||
        t.contains("再次聆聽") ||
        t.contains("重新播放") ||
        t.contains("إعادة الاستماع") ||
        t.contains("tekrar dinle") ||
        t.contains("ฟังอีกครั้ง") ||
        t.contains("dengarkan lagi")
}

/**
 * Checks if a section title from YouTube Music corresponds to the "Quick picks" shelf.
 */
fun isQuickPicksSection(title: String, localizedQuickPicks: String? = null): Boolean {
    if (title.isBlank()) return false
    if (localizedQuickPicks != null && localizedQuickPicks.isNotBlank() && title.equals(localizedQuickPicks.trim(), ignoreCase = true)) {
        return true
    }
    val t = title.trim().lowercase()
    return t == "quick picks" ||
        t == "quick pick" ||
        t.startsWith("quick pick") ||
        t.contains("quick pick") ||
        t.contains("quick") ||
        t.contains("chọn nhanh") ||
        t.contains("sélection rapide") ||
        t.contains("selección rápida") ||
        t.contains("schnelle auswahl") ||
        t.contains("escolhas rápidas") ||
        t.contains("быстрый выбор") ||
        t.contains("scelte rapide") ||
        t.contains("クイック選択") ||
        t.contains("빠른 선곡") ||
        t.contains("快速选择") ||
        t.contains("快速挑選") ||
        t.contains("精選歌曲") ||
        t.contains("اختيارات سريعة") ||
        t.contains("hızlı seçim") ||
        t.contains("เลือกด่วน") ||
        t.contains("pilihan cepat")
}
