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
        t.contains("start radio") ||
        t.contains("radio from a song") ||
        t.contains("start a radio") ||
        t.contains("chọn nhanh") ||
        t.contains("bắt đầu danh sách phát từ bài hát") ||
        t.contains("bắt đầu radio") ||
        t.contains("sélection rapide") ||
        t.contains("lancer une radio") ||
        t.contains("selección rápida") ||
        t.contains("inicia una radio") ||
        t.contains("schnelle auswahl") ||
        t.contains("radio starten") ||
        t.contains("escolhas rápidas") ||
        t.contains("inicie uma rádio") ||
        t.contains("быстрый выбор") ||
        t.contains("радио по треку") ||
        t.contains("запустить радио") ||
        t.contains("scelte rapide") ||
        t.contains("avvia una radio") ||
        t.contains("クイック選択") ||
        t.contains("ラジオを開始") ||
        t.contains("빠른 선곡") ||
        t.contains("뮤직 스테이션 시작") ||
        t.contains("快速选择") ||
        t.contains("快速挑選") ||
        t.contains("精選歌曲") ||
        t.contains("開始播放電台") ||
        t.contains("开始播放电台") ||
        t.contains("اختيارات سريعة") ||
        t.contains("بدء محطة راديو") ||
        t.contains("hızlı seçim") ||
        t.contains("radyo başlat") ||
        t.contains("เลือกด่วน") ||
        t.contains("เริ่มเล่นวิทยุ") ||
        t.contains("pilihan cepat") ||
        t.contains("mulai radio")
}
