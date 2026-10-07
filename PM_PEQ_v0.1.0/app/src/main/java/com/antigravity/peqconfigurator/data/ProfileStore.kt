package com.antigravity.peqconfigurator.data

import android.content.Context
import com.antigravity.peqconfigurator.domain.eq.EqProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ProfileStore(private val context: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences("slot_profiles", Context.MODE_PRIVATE)
    private val profilesDir: File
        get() = File(context.filesDir, "profiles").apply { if (!exists()) mkdirs() }

    fun getSlotAssignments(): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        for (slot in 1..3) {
            val defaultId = when (slot) {
                1 -> EqProfile.DEFAULT_PROFILE_ID
                2 -> "slot-2-default"
                3 -> "slot-3-default"
                else -> "slot-$slot-default"
            }
            map[slot] = prefs.getString("slot_$slot", defaultId) ?: defaultId
        }
        return map
    }

    fun setSlotAssignment(slot: Int, profileId: String) {
        prefs.edit().putString("slot_$slot", profileId).apply()
        DiagnosticLogger.i("ProfileStore", "Assigned slot $slot to profile: $profileId")
    }

    suspend fun saveProfile(profile: EqProfile): Unit = withContext(Dispatchers.IO) {
        val file = File(profilesDir, "${profile.id}.json")
        val content = json.encodeToString(profile)
        file.writeText(content)
        DiagnosticLogger.i("ProfileStore", "Saved profile: ${profile.name} (${profile.id})")
    }

    suspend fun getAllProfiles(): List<EqProfile> = withContext(Dispatchers.IO) {
        val files = profilesDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
        val list = mutableListOf<EqProfile>()
        for (file in files) {
            try {
                val content = file.readText()
                val profile = json.decodeFromString<EqProfile>(content)
                list.add(profile)
            } catch (e: Exception) {
                DiagnosticLogger.e("ProfileStore", "Failed to load profile from ${file.name}: ${e.message}")
            }
        }

        val existingIds = list.map { it.id }.toSet()
        if (!existingIds.contains(EqProfile.DEFAULT_PROFILE_ID) && list.none { it.name == EqProfile.DEFAULT_PROFILE_NAME || it.name == "Slot 1" }) {
            val def1 = EqProfile(id = EqProfile.DEFAULT_PROFILE_ID, name = "Slot 1", presetSlot = 1)
            saveProfile(def1)
            list.add(def1)
        }
        if (!existingIds.contains("slot-2-default") && list.none { it.name == "Slot 2" || it.id == "slot-2-default" }) {
            val def2 = EqProfile(id = "slot-2-default", name = "Slot 2", presetSlot = 2)
            saveProfile(def2)
            list.add(def2)
        }
        if (!existingIds.contains("slot-3-default") && list.none { it.name == "Slot 3" || it.id == "slot-3-default" }) {
            val def3 = EqProfile(id = "slot-3-default", name = "Slot 3", presetSlot = 3)
            saveProfile(def3)
            list.add(def3)
        }

        val normalized = list.map { prof ->
            when {
                prof.id == EqProfile.DEFAULT_PROFILE_ID || prof.name == "Default 10-Band PEQ" || prof.name == "設定1" -> {
                    prof.copy(name = "Slot 1", id = EqProfile.DEFAULT_PROFILE_ID)
                }
                prof.id == "slot-2-default" || prof.name == "設定2" -> {
                    prof.copy(name = "Slot 2", id = "slot-2-default")
                }
                prof.id == "slot-3-default" || prof.name == "設定3" -> {
                    prof.copy(name = "Slot 3", id = "slot-3-default")
                }
                else -> prof
            }
        }
        normalized.sortedBy { it.name }
    }

    suspend fun deleteProfile(id: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(profilesDir, "$id.json")
        if (file.exists()) {
            val deleted = file.delete()
            DiagnosticLogger.i("ProfileStore", "Deleted profile: $id (success=$deleted)")
            deleted
        } else false
    }

    suspend fun duplicateProfile(profile: EqProfile): EqProfile = withContext(Dispatchers.IO) {
        val copy = profile.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${profile.name} (Copy)"
        )
        saveProfile(copy)
        copy
    }
}
