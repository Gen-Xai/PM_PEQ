package com.antigravity.peqconfigurator.ui.settings

import com.antigravity.peqconfigurator.ui.theme.TextPrimary
import com.antigravity.peqconfigurator.ui.theme.TextSecondary
import com.antigravity.peqconfigurator.ui.theme.TextMuted

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.peqconfigurator.data.AppLanguage
import com.antigravity.peqconfigurator.data.AppSettings
import com.antigravity.peqconfigurator.data.ColorPalette
import com.antigravity.peqconfigurator.data.LogEntry
import com.antigravity.peqconfigurator.data.LogLevel
import com.antigravity.peqconfigurator.data.ThemeMode
import com.antigravity.peqconfigurator.ui.components.ExpressiveRibbon
import com.antigravity.peqconfigurator.ui.i18n.LocalStrings
import com.antigravity.peqconfigurator.ui.theme.DarkBackground
import com.antigravity.peqconfigurator.ui.theme.DarkCardBorder
import com.antigravity.peqconfigurator.ui.theme.DarkSurface
import com.antigravity.peqconfigurator.ui.theme.EmeraldGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onColorPaletteChanged: (ColorPalette) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    isDebugEnabled: Boolean,
    logs: List<LogEntry>,
    onToggleDebug: (Boolean) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var appearanceExpanded by remember { mutableStateOf(true) }
    var languageExpanded by remember { mutableStateOf(true) }
    var diagnosticsExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = strings.settingsTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // 1. Appearance & Theme Color Ribbon
        ExpressiveRibbon(
            title = strings.appearanceCategory,
            icon = Icons.Filled.Palette,
            badgeText = when (appSettings.themeMode) {
                ThemeMode.SYSTEM -> strings.themeSystem
                ThemeMode.DARK -> strings.themeDark
                ThemeMode.LIGHT -> strings.themeLight
            },
            badgeColor = MaterialTheme.colorScheme.primary,
            isExpanded = appearanceExpanded,
            onToggleExpand = { appearanceExpanded = !appearanceExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Theme Mode Selector (System, Dark, Light)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.themeModeTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    com.antigravity.peqconfigurator.ui.components.ExpressiveSingleChoiceGroup(
                        options = listOf(
                            ThemeMode.SYSTEM to strings.themeSystem,
                            ThemeMode.DARK to strings.themeDark,
                            ThemeMode.LIGHT to strings.themeLight
                        ),
                        selected = appSettings.themeMode,
                        onSelect = onThemeModeChanged
                    )
                }

                // Accent Color Palette Selector Grid
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                    val palettes = listOf(
                        ColorPalette.SYSTEM_DYNAMIC to (strings.colorDynamic + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) " (Monet)" else ""),
                        ColorPalette.CYBERPUNK_CYAN to strings.colorCyan,
                        ColorPalette.ANDROID_GREEN to strings.colorGreen,
                        ColorPalette.DEEP_VIOLET to strings.colorViolet,
                        ColorPalette.SUNSET_AMBER to strings.colorAmber,
                        ColorPalette.OCEAN_BLUE to strings.colorBlue
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        palettes.forEach { (palette, name) ->
                            val isSelected = appSettings.colorPalette == palette
                            val previewColor = Color(palette.primaryHex)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else DarkSurface)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else DarkCardBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onColorPaletteChanged(palette) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Color Dot
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(previewColor)
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = name,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Language Ribbon
        ExpressiveRibbon(
            title = strings.languageCategory,
            icon = Icons.Filled.Language,
            badgeText = when (appSettings.language) {
                AppLanguage.SYSTEM -> strings.langSystem
                AppLanguage.JA -> "日本語"
                AppLanguage.EN -> "English"
            },
            badgeColor = MaterialTheme.colorScheme.primary,
            isExpanded = languageExpanded,
            onToggleExpand = { languageExpanded = !languageExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                com.antigravity.peqconfigurator.ui.components.ExpressiveSingleChoiceGroup(
                    options = listOf(
                        AppLanguage.SYSTEM to strings.langSystem,
                        AppLanguage.JA to strings.langJa,
                        AppLanguage.EN to strings.langEn
                    ),
                    selected = appSettings.language,
                    onSelect = onLanguageChanged
                )
            }
        }

        // 3. Diagnostics & USB Logging Ribbon
        ExpressiveRibbon(
            title = strings.diagnosticsCategory,
            icon = Icons.Filled.BugReport,
            badgeText = "${logs.size} Logs",
            badgeColor = EmeraldGreen,
            isExpanded = diagnosticsExpanded,
            onToggleExpand = { diagnosticsExpanded = !diagnosticsExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.debugLoggingTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))

                    Switch(
                        checked = isDebugEnabled,
                        onCheckedChange = onToggleDebug,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkCardBorder
                        )
                    )
                }

                // Log Console
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.logConsoleTitle} (${logs.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )

                        OutlinedButton(
                            onClick = onClearLogs,
                            shapes = ButtonDefaults.shapes(),
                            border = BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Text(strings.clearLogs, fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val listState = rememberLazyListState()
                    val logNestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                // Consume remaining scroll so outer screen verticalScroll is never triggered
                                return available
                            }

                            override suspend fun onPostFling(
                                consumed: Velocity,
                                available: Velocity
                            ): Velocity {
                                // Consume remaining velocity so outer screen verticalScroll never flings
                                return available
                            }
                        }
                    }

                    LaunchedEffect(logs.size) {
                        if (logs.isNotEmpty()) {
                            listState.animateScrollToItem(logs.size - 1)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .nestedScroll(logNestedScrollConnection)
                            .background(DarkBackground.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (logs.isEmpty()) {
                            item {
                                Text(
                                    text = if (strings.aboutCategory == "アプリ情報") "ログはありません" else "No logs yet",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            items(logs) { entry ->
                                val levelColor = when (entry.level) {
                                    LogLevel.DEBUG -> TextMuted
                                    LogLevel.INFO -> EmeraldGreen
                                    LogLevel.WARN -> Color(0xFFFFB300)
                                    LogLevel.ERROR -> Color(0xFFFF5252)
                                }

                                Text(
                                    text = "[${entry.timestamp}] [${entry.category}] ${entry.message}",
                                    color = levelColor,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. About Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = strings.aboutText,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}
