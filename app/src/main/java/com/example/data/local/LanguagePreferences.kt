package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

class LanguagePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("green_route_language_prefs", Context.MODE_PRIVATE)

    var currentLanguageCode: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    fun getLocale(): Locale {
        return when (currentLanguageCode) {
            "hi" -> Locale("hi", "IN")
            "bn" -> Locale("bn", "IN")
            else -> Locale("en", "US")
        }
    }

    companion object {
        private const val KEY_LANGUAGE = "selected_language"

        fun applyLocale(context: Context, languageCode: String): Context {
            val locale = when (languageCode) {
                "hi" -> Locale("hi", "IN")
                "bn" -> Locale("bn", "IN")
                else -> Locale("en", "US")
            }
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }
            return context.createConfigurationContext(config)
        }
    }
}
