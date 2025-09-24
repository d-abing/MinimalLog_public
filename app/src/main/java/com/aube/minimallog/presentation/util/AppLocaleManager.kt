package com.aube.minimallog.presentation.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLocaleManager {
    fun apply(languageTag: String?) {
        val tags = languageTag?.takeIf { it.isNotBlank() } ?: "" // "" = 시스템 기본
        val localeList = LocaleListCompat.forLanguageTags(tags)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}