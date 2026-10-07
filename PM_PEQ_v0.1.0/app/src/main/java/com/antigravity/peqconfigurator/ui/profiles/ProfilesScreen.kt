package com.antigravity.peqconfigurator.ui.profiles

import com.antigravity.peqconfigurator.ui.theme.TextPrimary
import com.antigravity.peqconfigurator.ui.theme.TextSecondary
import com.antigravity.peqconfigurator.ui.theme.TextMuted

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.peqconfigurator.domain.eq.EqProfile
import com.antigravity.peqconfigurator.domain.eq.FilterType
import com.antigravity.peqconfigurator.domain.profile.ExternalEqParser
import com.antigravity.peqconfigurator.ui.i18n.LocalStrings
import com.antigravity.peqconfigurator.ui.theme.DarkCardBorder
import com.antigravity.peqconfigurator.ui.theme.DarkSurfaceVariant
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfilesScreen(
    profiles: List<EqProfile>,
    activeProfileId: String,
    activeProfileName: String = "",
    slotAssignments: Map<Int, String> = emptyMap(),
    onAssignSlot: (slot: Int, profileId: String) -> Unit = { _, _ -> },
    onLoadProfile: (EqProfile) -> Unit,
    onSaveProfile: (String) -> Unit,
    onDuplicateProfile: (EqProfile) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onImportApoProfile: (EqProfile) -> Unit,
    onRenameProfile: (EqProfile, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf<EqProfile?>(null) }
    var saveName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<EqProfile?>(null) }
    var renameText by remember { mutableStateOf("") }
    var importText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.profilesTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            Row {
                IconButton(onClick = { showImportDialog = true }, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = "Import APO Text",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = { showSaveDialog = true },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.saveCurrent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(profiles, key = { it.id }) { prof ->
                val isActive = prof.id == activeProfileId || (prof.name == activeProfileName && activeProfileName.isNotBlank())
                val assignedSlots = slotAssignments.filter { it.value == prof.id }.keys.sorted()
                ProfileItemCard(
                    profile = prof,
                    isActive = isActive,
                    assignedSlots = assignedSlots,
                    onAssignSlot = { slot -> onAssignSlot(slot, prof.id) },
                    onLoad = { onLoadProfile(prof) },
                    onDuplicate = { onDuplicateProfile(prof) },
                    onDelete = { onDeleteProfile(prof.id) },
                    onExport = { showExportDialog = prof },
                    onRename = { renameTarget = prof; renameText = prof.name }
                )
            }
        }
    }

    // Save Profile Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(strings.saveProfileTitle, color = TextPrimary) },
            text = {
                Column {
                    Text(strings.saveProfilePrompt, color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        singleLine = true,
                        placeholder = { Text(strings.saveProfilePlaceholder) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (saveName.isNotBlank()) {
                            onSaveProfile(saveName)
                            saveName = ""
                            showSaveDialog = false
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(strings.saveConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }, shapes = ButtonDefaults.shapes()) {
                    Text(strings.saveCancel, color = TextMuted)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Import APO Text Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Parametric EQ (APO)", color = TextPrimary) },
            text = {
                Column {
                    Text("Paste Equalizer APO text configuration:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("Preamp: -3.0 dB\nFilter 1: ON PK Fc 100 Hz Gain 2.0 dB Q 1.41") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isNotBlank()) {
                            val parsed = ExternalEqParser.parseApoText(importText)
                            onImportApoProfile(parsed)
                            importText = ""
                            showImportDialog = false
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (strings.profilesTitle == "プロファイル") "インポート" else "Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }, shapes = ButtonDefaults.shapes()) {
                    Text(strings.saveCancel, color = TextMuted)
                }
            },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // Rename Dialog
    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(if (strings.profilesTitle == "プロファイル") "プロファイル名の変更" else "Rename Profile", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            onRenameProfile(target, renameText)
                            renameTarget = null
                        }
                    },
                    shapes = ButtonDefaults.shapes()
                ) { Text(strings.rename, color = androidx.compose.material3.MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }, shapes = ButtonDefaults.shapes()) { Text(strings.saveCancel, color = TextMuted) }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Export Dialog
    showExportDialog?.let { profToExport ->
        val apoText = remember(profToExport) { ExternalEqParser.exportApoText(profToExport) }
        AlertDialog(
            onDismissRequest = { showExportDialog = null },
            title = { Text("Exported APO Configuration", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = apoText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { showExportDialog = null },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (strings.profilesTitle == "プロファイル") "閉じる" else "Close")
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileItemCard(
    profile: EqProfile,
    isActive: Boolean,
    assignedSlots: List<Int>,
    onAssignSlot: (Int) -> Unit,
    onLoad: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit
) {
    val strings = LocalStrings.current
    val borderColor = if (isActive) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
    val cardShape = RoundedCornerShape(14.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
            .border(if (isActive) 1.5.dp else 1.dp, borderColor, cardShape)
            .clickable { onLoad() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    if (isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            strings.active,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    for (slot in assignedSlots) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            strings.slotBadge(slot),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val activeBands = profile.bands.count { it.enabled }
                val shelfCount = profile.bands.count { it.enabled && (it.type == FilterType.LOW_SHELF || it.type == FilterType.HIGH_SHELF) }
                val detailStr = buildString {
                    append(String.format(Locale.US, "Preamp: %+.1f dB", profile.preamp))
                    append("  •  ${activeBands}/${profile.bands.size} Bands")
                    if (shelfCount > 0) {
                        append(" ($shelfCount Shelf)")
                    }
                }
                Text(
                    text = detailStr,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row {
                IconButton(onClick = onRename, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Filled.Edit, contentDescription = "Rename", tint = TextMuted)
                }
                IconButton(onClick = onExport, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Filled.Upload, contentDescription = "Export APO Text", tint = TextMuted)
                }
                IconButton(onClick = onDuplicate, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate", tint = TextMuted)
                }
                if (profile.name != "Default 10-Band PEQ" && profile.id != "default-10-band-peq") {
                    IconButton(onClick = onDelete, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = TextMuted)
                    }
                }
            }
        }

        // Slot assignment interactive row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                strings.assignToSlotTitle + ":",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
            for (slot in 1..3) {
                val isThisSlot = assignedSlots.contains(slot)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isThisSlot) androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        )
                        .border(
                            1.dp,
                            if (isThisSlot) androidx.compose.material3.MaterialTheme.colorScheme.primary
                            else androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onAssignSlot(slot) }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        strings.slotName(slot),
                        fontSize = 10.sp,
                        fontWeight = if (isThisSlot) FontWeight.Bold else FontWeight.Normal,
                        color = if (isThisSlot) androidx.compose.material3.MaterialTheme.colorScheme.primary else TextSecondary
                    )
                }
            }
        }
    }
}
