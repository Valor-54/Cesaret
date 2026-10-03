package com.example.data

import android.content.Context
import com.example.model.GamepadPresets
import com.example.model.GamepadProfile
import com.example.model.InputMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GamepadRepository(private val dao: GamepadProfileDao) {

    val allProfiles: Flow<List<GamepadProfile>> = dao.getAllProfiles().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun ensureDefaultPresets() = withContext(Dispatchers.IO) {
        if (dao.getCount() == 0) {
            val wt = GamepadPresets.createWarThunderProfile()
            val xb = GamepadPresets.createXboxStandardProfile()
            val fps = GamepadPresets.createFpsProfile()
            dao.insertProfile(wt.toEntity())
            dao.insertProfile(xb.toEntity())
            dao.insertProfile(fps.toEntity())
        }
    }

    suspend fun getProfile(id: Long): GamepadProfile? = withContext(Dispatchers.IO) {
        dao.getProfileById(id)?.toModel()
    }

    suspend fun saveProfile(profile: GamepadProfile): Long = withContext(Dispatchers.IO) {
        val entity = profile.toEntity()
        val resultId = dao.insertProfile(entity)
        if (resultId > 0) resultId else profile.id
    }

    suspend fun deleteProfile(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteProfileById(id)
    }

    suspend fun duplicateProfile(profile: GamepadProfile): Long = withContext(Dispatchers.IO) {
        val copy = profile.copy(
            id = 0L,
            name = "${profile.name} (Kopya)",
            isPreset = false,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertProfile(copy.toEntity())
    }

    companion object {
        fun create(context: Context): GamepadRepository {
            val db = AppDatabase.getDatabase(context)
            return GamepadRepository(db.gamepadProfileDao())
        }
    }
}

fun GamepadProfileEntity.toModel(): GamepadProfile {
    val mode = try {
        InputMode.valueOf(inputMode)
    } catch (e: Exception) {
        InputMode.OVERLAY_HUD
    }

    val parsedControls = ControlJsonParser.fromJson(controlsJson).map { item ->
        val cleanLabel = when (item.label.trim()) {
            "Ateş", "Ateş Et", "ATEŞ ET", "Ates", "FIRE", "Fire" -> "RT"
            "Dürbün", "Dürbün (ADS)", "Nişan / Zoom", "ZOOM", "Zoom" -> "LT"
            "Sis", "Bomba", "SMOKE", "GRENADE" -> "LB"
            "Makineli", "Makineli Tüfek", "MG" -> "RB"
            "Tamir", "Zıpla", "JUMP", "Jump", "Kullan", "Onayla" -> "A"
            "Topçu", "Çömel", "CROUCH", "Crouch", "İptal", "Geri" -> "B"
            "Gözlem", "Doldur", "RELOAD", "Reload", "Serbest Kamera" -> "X"
            "Kilit", "Silah Değiş", "SWITCH", "Hedef" -> "Y"
            "Koşma", "SPRINT", "Sprint", "Hareket" -> "LS"
            "HARİTA", "Harita", "MAP", "Map" -> "SELECT"
            "MENÜ", "Menü", "MENU", "Menu" -> "START"
            else -> item.label
        }
        item.copy(label = cleanLabel, subLabel = "")
    }

    return GamepadProfile(
        id = id,
        name = name,
        description = description,
        isPreset = isPreset,
        controls = parsedControls,
        globalOpacity = globalOpacity,
        hapticFeedback = hapticFeedback,
        leftStickDeadzone = leftStickDeadzone,
        leftStickSensitivity = leftStickSensitivity,
        leftStickMaxTravel = leftStickMaxTravel,
        rightStickDeadzone = rightStickDeadzone,
        rightStickSensitivity = rightStickSensitivity,
        rightStickMaxTravel = rightStickMaxTravel,
        dynamicJoystickCenter = dynamicJoystickCenter,
        inputMode = mode,
        centerAimFreeZone = centerAimFreeZone,
        updatedAt = updatedAt
    )
}

fun GamepadProfile.toEntity(): GamepadProfileEntity {
    return GamepadProfileEntity(
        id = id,
        name = name,
        description = description,
        isPreset = isPreset,
        controlsJson = ControlJsonParser.toJson(controls),
        globalOpacity = globalOpacity,
        hapticFeedback = hapticFeedback,
        leftStickDeadzone = leftStickDeadzone,
        leftStickSensitivity = leftStickSensitivity,
        leftStickMaxTravel = leftStickMaxTravel,
        rightStickDeadzone = rightStickDeadzone,
        rightStickSensitivity = rightStickSensitivity,
        rightStickMaxTravel = rightStickMaxTravel,
        dynamicJoystickCenter = dynamicJoystickCenter,
        inputMode = inputMode.name,
        centerAimFreeZone = centerAimFreeZone,
        updatedAt = updatedAt
    )
}
