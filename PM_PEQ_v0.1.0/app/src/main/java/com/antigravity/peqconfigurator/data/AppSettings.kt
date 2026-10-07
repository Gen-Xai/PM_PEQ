package com.antigravity.peqconfigurator.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val id: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light")
}

enum class ColorPalette(val id: String, val primaryHex: Long) {
    SYSTEM_DYNAMIC("dynamic", 0xFF6750A4),
    CYBERPUNK_CYAN("cyan", 0xFF00E5FF),
    ANDROID_GREEN("green", 0xFF00E676),
    DEEP_VIOLET("violet", 0xFF9D4EDD),
    SUNSET_AMBER("amber", 0xFFFFB300),
    OCEAN_BLUE("blue", 0xFF2979FF)
}

enum class AppLanguage(val code: String) {
    SYSTEM("system"),
    JA("ja"),
    EN("en")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorPalette: ColorPalette = ColorPalette.CYBERPUNK_CYAN,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val isDebugEnabled: Boolean = false
)

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeModeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.id) ?: ThemeMode.SYSTEM.id
        val themeMode = ThemeMode.entries.firstOrNull { it.id == themeModeStr } ?: ThemeMode.SYSTEM

        val paletteStr = prefs.getString("color_palette", ColorPalette.CYBERPUNK_CYAN.id) ?: ColorPalette.CYBERPUNK_CYAN.id
        val palette = ColorPalette.entries.firstOrNull { it.id == paletteStr } ?: ColorPalette.CYBERPUNK_CYAN

        val langStr = prefs.getString("language", AppLanguage.SYSTEM.code) ?: AppLanguage.SYSTEM.code
        val lang = AppLanguage.entries.firstOrNull { it.code == langStr } ?: AppLanguage.SYSTEM

        val debug = prefs.getBoolean("debug_enabled", false)

        return AppSettings(
            themeMode = themeMode,
            colorPalette = palette,
            language = lang,
            isDebugEnabled = debug
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.id).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setColorPalette(palette: ColorPalette) {
        prefs.edit().putString("color_palette", palette.id).apply()
        _settings.value = _settings.value.copy(colorPalette = palette)
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString("language", lang.code).apply()
        _settings.value = _settings.value.copy(language = lang)
    }

    fun setDebugEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("debug_enabled", enabled).apply()
        _settings.value = _settings.value.copy(isDebugEnabled = enabled)
    }
}
