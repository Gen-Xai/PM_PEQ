package com.antigravity.peqconfigurator.ui.peq

import com.antigravity.peqconfigurator.ui.theme.TextPrimary
import com.antigravity.peqconfigurator.ui.theme.TextSecondary
import com.antigravity.peqconfigurator.ui.theme.TextMuted

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreampControl(
    preampDb: Double,
    onPreampChange: (Double) -> Unit,
    minGain: Double = -12.0,
    maxGain: Double = 12.0,
    modifier: Modifier = Modifier
) {
    val fieldText = String.format(Locale.US, "%.1f", preampDb)
    var textValue by remember { mutableStateOf(fieldText) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(fieldText, focused) { if (!focused) textValue = fieldText }

    val quickSteps = listOf(-6.0, -3.0, -1.0, -0.5, 0.0, 0.5, 1.0)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Main Slider & Value Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.width(90.dp)) {
                Text(
                    text = "LEVEL",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = String.format(Locale.US, "%+.1f dB", preampDb),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            val sliderColors = SliderDefaults.colors(
                thumbColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                activeTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                inactiveTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
            )
            Slider(
                value = preampDb.toFloat().coerceIn(minGain.toFloat(), maxGain.toFloat()),
                onValueChange = { newDb ->
                    onPreampChange((newDb * 10f).roundToInt() / 10.0)
                },
                valueRange = minGain.toFloat()..maxGain.toFloat(),
                colors = sliderColors,
                track = { sliderState ->
                    SliderDefaults.CenteredTrack(
                        sliderState = sliderState,
                        colors = sliderColors
                    )
                },
                modifier = Modifier.weight(1f)
            )

            // Direct input textfield with signed decimal support
            OutlinedTextField(
                value = textValue,
                onValueChange = { str ->
                    textValue = str
                    str.toDoubleOrNull()?.let { newDb ->
                        if (newDb in minGain..maxGain) onPreampChange(newDb)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                modifier = Modifier
                    .width(76.dp)
                    .onFocusChanged { focused = it.isFocused },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }

        // Quick Step Pill Buttons (Material 3 Expressive)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickSteps.forEach { step ->
                val isCurrent = kotlin.math.abs(preampDb - step) < 0.05
                val pillBg = if (isCurrent) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                val textCol = if (isCurrent) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary else TextSecondary

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(pillBg)
                        .clickable { onPreampChange(step) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (step == 0.0) "0" else String.format(Locale.US, "%+.1f", step).replace(".0", ""),
                        color = textCol,
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

