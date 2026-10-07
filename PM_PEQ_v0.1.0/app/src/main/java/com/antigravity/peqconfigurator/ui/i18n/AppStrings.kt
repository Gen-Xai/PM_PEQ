package com.antigravity.peqconfigurator.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.antigravity.peqconfigurator.data.AppLanguage
import java.util.Locale

interface Strings {
    // Navigation
    val navPeq: String
    val navDevice: String
    val navProfiles: String
    val navSettings: String

    // Status
    val statusConnected: String
    val statusConnecting: String
    val statusError: String
    val statusDisconnected: String
    val statusEdited: String
    val slot: String
    fun slotName(slot: Int): String

    // PEQ Screen
    val freqResponseTitle: String
    fun activeBandsCount(active: Int, total: Int): String
    val preampTitle: String
    val peq10BandTitle: String
    fun bandSelected(index: Int): String
    val syncTitle: String
    val resetBtn: String
    val pullDacBtn: String
    val saveFlashBtn: String
    val resetConfirmTitle: String
    val resetConfirmMsg: String
    val othersTitle: String
    val pinkNoiseTitle: String
    val pinkNoisePlaying: String
    val pinkNoiseStopped: String
    val volumeLabel: String
    val undo: String
    val redo: String
    val experimentalWarning: String
    val discardTitle: String
    fun discardMsg(slot: Int, device: String): String
    fun saveTitle(slot: Int): String
    fun saveMsg(slot: Int, device: String): String
    val cancel: String
    val ok: String

    // Band Editor
    val band: String
    val on: String
    val frequency: String
    val gain: String
    val level: String

    // Device Screen
    val deviceInfoTitle: String
    val noDacConnected: String
    val plugDacPrompt: String
    val disconnect: String
    val scanAndConnect: String
    val deviceCapabilitiesTitle: String
    val supportConfirmed: String
    val supportExperimental: String
    val supportUnsupported: String
    val cap10BandPeq: String
    val capPreampGain: String
    val capDacFilter: String
    val capGainMode: String
    val capBalance: String
    val capPresetSlots: String
    fun presetSlotsCount(count: Int): String
    val supported: String
    val notSupported: String
    val channelBalanceTitle: String
    val center: String

    // Profiles Screen
    val profilesTitle: String
    val saveCurrent: String
    val saveProfileTitle: String
    val saveProfilePrompt: String
    val saveProfilePlaceholder: String
    val saveConfirm: String
    val saveCancel: String
    val active: String
    val load: String
    val rename: String
    val delete: String
    val assignToSlotTitle: String
    fun slotBadge(slot: Int): String
    fun activeProfileLabel(slot: Int, name: String): String

    // Settings Screen
    val settingsTitle: String
    val appearanceCategory: String
    val themeModeTitle: String
    val themeSystem: String
    val themeDark: String
    val themeLight: String
    val colorDynamic: String
    val colorCyan: String
    val colorGreen: String
    val colorViolet: String
    val colorAmber: String
    val colorBlue: String
    val languageCategory: String
    val langSystem: String
    val langJa: String
    val langEn: String
    val diagnosticsCategory: String
    val debugLoggingTitle: String
    val logConsoleTitle: String
    val clearLogs: String
    val aboutCategory: String
    val aboutText: String
}

class EnglishStrings : Strings {
    override val navPeq = "PEQ"
    override val navDevice = "Device"
    override val navProfiles = "Profiles"
    override val navSettings = "Settings"

    override val statusConnected = "Connected"
    override val statusConnecting = "Connecting..."
    override val statusError = "Error"
    override val statusDisconnected = "No DAC Connected"
    override val statusEdited = "Edited"
    override val slot = "Slot"
    override fun slotName(slot: Int) = "Slot $slot"

    override val freqResponseTitle = "FREQUENCY RESPONSE"
    override fun activeBandsCount(active: Int, total: Int) = "$active / $total Active"
    override val preampTitle = "PREAMP GAIN"
    override val peq10BandTitle = "PEQ SETTINGS"
    override fun bandSelected(index: Int) = "BAND $index Selected"
    override val syncTitle = "HARDWARE SYNC"
    override val resetBtn = "Reset"
    override val pullDacBtn = "Pull_DAC"
    override val saveFlashBtn = "Save_Flash"
    override val resetConfirmTitle = "Reset EQ Settings?"
    override val resetConfirmMsg = "Reset all 10 bands to initial flat defaults?"
    override val othersTitle = "OTHERS"
    override val pinkNoiseTitle = "Pink Noise Generator"
    override val pinkNoisePlaying = "Playing"
    override val pinkNoiseStopped = "Stopped"
    override val volumeLabel = "Volume"
    override val undo = "Undo"
    override val redo = "Redo"
    override val experimentalWarning = "EXPERIMENTAL protocol profile — verified for Protocol Max"
    override val discardTitle = "Discard unsaved changes?"
    override fun discardMsg(slot: Int, device: String) = "Pulling Slot $slot from $device will overwrite current editor state."
    override fun saveTitle(slot: Int) = "Save Slot $slot to flash?"
    override fun saveMsg(slot: Int, device: String) = "Persists current settings to the hardware memory of $device."
    override val cancel = "Cancel"
    override val ok = "OK"

    override val band = "BAND"
    override val on = "ON"
    override val frequency = "FREQUENCY"
    override val gain = "GAIN"
    override val level = "LEVEL"

    override val deviceInfoTitle = "USB Device Information"
    override val noDacConnected = "No USB DAC Connected"
    override val plugDacPrompt = "Plug in USB DAC OTG cable to configure"
    override val disconnect = "Disconnect"
    override val scanAndConnect = "Scan & Connect USB DAC"
    override val deviceCapabilitiesTitle = "Device Capabilities"
    override val supportConfirmed = "CONFIRMED"
    override val supportExperimental = "EXPERIMENTAL"
    override val supportUnsupported = "UNSUPPORTED"
    override val cap10BandPeq = "10-Band PEQ"
    override val capPreampGain = "Preamp Gain Control"
    override val capDacFilter = "DAC Filter Selector"
    override val capGainMode = "Gain Mode (High/Low)"
    override val capBalance = "Hardware Channel Balance"
    override val capPresetSlots = "Preset Slots"
    override fun presetSlotsCount(count: Int) = "$count Slots"
    override val supported = "Supported"
    override val notSupported = "Not Supported"
    override val channelBalanceTitle = "Channel Balance L/R"
    override val center = "Center"

    override val profilesTitle = "Profiles"
    override val saveCurrent = "Save"
    override val saveProfileTitle = "Save Profile"
    override val saveProfilePrompt = "Enter a name for this PEQ profile:"
    override val saveProfilePlaceholder = "e.g. Warm Bass Boost"
    override val saveConfirm = "Save"
    override val saveCancel = "Cancel"
    override val active = "ACTIVE"
    override val load = "Load"
    override val rename = "Rename"
    override val delete = "Delete"
    override val assignToSlotTitle = "Assign to Slot"
    override fun slotBadge(slot: Int) = "Slot $slot"
    override fun activeProfileLabel(slot: Int, name: String) = "Slot $slot • $name"

    override val settingsTitle = "App Settings & Diagnostics"
    override val appearanceCategory = "APPEARANCE & THEME"
    override val themeModeTitle = "Theme Mode"
    override val themeSystem = "System"
    override val themeDark = "Dark"
    override val themeLight = "Light"
    override val colorDynamic = "System UI (Material You)"
    override val colorCyan = "Cyberpunk Cyan"
    override val colorGreen = "Android Green"
    override val colorViolet = "Deep Violet"
    override val colorAmber = "Sunset Amber"
    override val colorBlue = "Ocean Blue"
    override val languageCategory = "LANGUAGE"
    override val langSystem = "System Default"
    override val langJa = "日本語 (Japanese)"
    override val langEn = "English"
    override val diagnosticsCategory = "DIAGNOSTICS & LOGGING"
    override val debugLoggingTitle = "Diagnostic USB Logging"
    override val logConsoleTitle = "USB & Protocol Log Console"
    override val clearLogs = "Clear Logs"
    override val aboutCategory = "ABOUT"
    override val aboutText = "PM_PEQ v0.1.0"
}

class JapaneseStrings : Strings {
    override val navPeq = "PEQ"
    override val navDevice = "デバイス"
    override val navProfiles = "プロファイル"
    override val navSettings = "設定"

    override val statusConnected = "接続中"
    override val statusConnecting = "接続試行中..."
    override val statusError = "エラー"
    override val statusDisconnected = "DAC未接続"
    override val statusEdited = "編集中"
    override val slot = "Slot"
    override fun slotName(slot: Int) = "Slot $slot"

    override val freqResponseTitle = "周波数応答グラフ"
    override fun activeBandsCount(active: Int, total: Int) = "$active / $total バンド有効"
    override val preampTitle = "プリアンプゲイン"
    override val peq10BandTitle = "PEQ設定"
    override fun bandSelected(index: Int) = "BAND $index 選択中"
    override val syncTitle = "ハードウェア同期"
    override val resetBtn = "Reset"
    override val pullDacBtn = "Pull_DAC"
    override val saveFlashBtn = "Save_Flash"
    override val resetConfirmTitle = "EQ設定のリセット"
    override val resetConfirmMsg = "全10バンドを初期状態（フラット）にリセットしますか？"
    override val othersTitle = "OTHERS"
    override val pinkNoiseTitle = "ピンクノイズ再生"
    override val pinkNoisePlaying = "再生中"
    override val pinkNoiseStopped = "停止中"
    override val volumeLabel = "音量"
    override val undo = "元に戻す"
    override val redo = "やり直す"
    override val experimentalWarning = "EXPERIMENTALプロトコル (Protocol Max対応)"
    override val discardTitle = "未保存の変更を破棄しますか？"
    override fun discardMsg(slot: Int, device: String) = "Slot $slot を $device から読み込むと、現在の編集内容が上書きされます。"
    override fun saveTitle(slot: Int) = "Slot $slot を本体Flashに保存しますか？"
    override fun saveMsg(slot: Int, device: String) = "現在のEQ設定を $device 本体の不揮発性メモリに永続保存します。"
    override val cancel = "キャンセル"
    override val ok = "OK"

    override val band = "バンド"
    override val on = "ON"
    override val frequency = "周波数"
    override val gain = "ゲイン"
    override val level = "レベル"

    override val deviceInfoTitle = "USBデバイス情報"
    override val noDacConnected = "USB DACが接続されていません"
    override val plugDacPrompt = "USB DACをOTGケーブルで接続してください"
    override val disconnect = "切断"
    override val scanAndConnect = "USB DACをスキャン・接続"
    override val deviceCapabilitiesTitle = "デバイス対応機能"
    override val supportConfirmed = "動作確認済"
    override val supportExperimental = "実験的プロトコル"
    override val supportUnsupported = "非対応"
    override val cap10BandPeq = "10バンド PEQ"
    override val capPreampGain = "プリアンプゲイン調整"
    override val capDacFilter = "DACフィルター切替"
    override val capGainMode = "ゲインモード (High/Low)"
    override val capBalance = "ハードウェア左右バランス"
    override val capPresetSlots = "プリセットスロット"
    override fun presetSlotsCount(count: Int) = "$count スロット"
    override val supported = "対応"
    override val notSupported = "非対応"
    override val channelBalanceTitle = "左右チャンネルバランス"
    override val center = "中央"

    override val profilesTitle = "プロファイル"
    override val saveCurrent = "保存"
    override val saveProfileTitle = "プロファイルの保存"
    override val saveProfilePrompt = "PEQプロファイルの名前を入力してください:"
    override val saveProfilePlaceholder = "例: Warm Bass Boost"
    override val saveConfirm = "保存"
    override val saveCancel = "キャンセル"
    override val active = "ACTIVE"
    override val load = "読込"
    override val rename = "リネーム"
    override val delete = "削除"
    override val assignToSlotTitle = "スロットに割り当て"
    override fun slotBadge(slot: Int) = "Slot $slot"
    override fun activeProfileLabel(slot: Int, name: String) = "Slot $slot • $name"

    override val settingsTitle = "アプリ設定と診断"
    override val appearanceCategory = "外観とテーマ"
    override val themeModeTitle = "テーマモード"
    override val themeSystem = "システム"
    override val themeDark = "ダーク"
    override val themeLight = "ライト"
    override val colorDynamic = "システムUI (Material You)"
    override val colorCyan = "サイバーパンク・シアン"
    override val colorGreen = "Androidグリーン"
    override val colorViolet = "ディープ・バイオレット"
    override val colorAmber = "サンセット・アンバー"
    override val colorBlue = "オーシャン・ブルー"
    override val languageCategory = "言語設定"
    override val langSystem = "システム標準"
    override val langJa = "日本語"
    override val langEn = "English (英語)"
    override val diagnosticsCategory = "診断とUSB通信ログ"
    override val debugLoggingTitle = "詳細USB通信ログ"
    override val logConsoleTitle = "USBプロトコル・ログコンソール"
    override val clearLogs = "ログ消去"
    override val aboutCategory = "アプリ情報"
    override val aboutText = "PM_PEQ v0.1.0"
}

val LocalStrings = staticCompositionLocalOf<Strings> { EnglishStrings() }

fun getStringsForLanguage(lang: AppLanguage): Strings {
    return when (lang) {
        AppLanguage.EN -> EnglishStrings()
        AppLanguage.JA -> JapaneseStrings()
        AppLanguage.SYSTEM -> {
            val systemLang = Locale.getDefault().language
            if (systemLang.startsWith("ja")) JapaneseStrings() else EnglishStrings()
        }
    }
}
