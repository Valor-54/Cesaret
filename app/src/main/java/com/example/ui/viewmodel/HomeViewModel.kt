package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.WarPadApplication
import com.example.model.GamepadPresets
import com.example.model.GamepadProfile
import com.example.model.InputMode
import com.example.service.FlexiPadAccessibilityService
import com.example.service.GamepadOverlayService
import com.example.shizuku.ConnectionTestResult
import com.example.shizuku.ShizukuInputBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

sealed class ShizukuConnectionState(val title: String, val message: String) {
    object Idle : ShizukuConnectionState(
        title = "Kontrol Edilmedi",
        message = "Shizuku durumunu kontrol etmek ve bağlanmak için butona dokunun."
    )
    object NotInstalled : ShizukuConnectionState(
        title = "Shizuku kurulu değil.",
        message = "Cihazınızda Shizuku uygulaması bulunamadı. Lütfen Shizuku uygulamasını yükleyin."
    )
    object ServiceNotRunning : ShizukuConnectionState(
        title = "Shizuku servisi çalışmıyor.",
        message = "Shizuku yüklü ancak servis çalışmıyor. Shizuku uygulamasını açıp Kablosuz Hata Ayıklama (Wireless Debugging) veya ADB ile servisi başlatın."
    )
    object PermissionPending : ShizukuConnectionState(
        title = "İzin bekleniyor.",
        message = "Shizuku yetkilendirme penceresi açıldı. Lütfen FlexiPad'e izin verin."
    )
    object PermissionDenied : ShizukuConnectionState(
        title = "İzin reddedildi.",
        message = "Shizuku izni reddedildi. Sistem düzeyinde kontrol için Shizuku izni gereklidir."
    )
    object PermissionGranted : ShizukuConnectionState(
        title = "Shizuku izni verildi.",
        message = "İzin onaylandı. InputUserService köprüsüne bağlanılıyor ve test ediliyor..."
    )
    object UserServiceSuccess : ShizukuConnectionState(
        title = "InputUserService bağlantısı başarılı.",
        message = "Shizuku bağlı. InputUserService köprüsü doğrulandı ve sistem düzeyinde girdi enjeksiyonu aktif."
    )
    data class UserServiceFailed(val reason: String) : ShizukuConnectionState(
        title = "InputUserService bağlantısı başarısız.",
        message = reason.ifBlank { "InputUserService köprüsüne bağlanılamadı veya ping yanıtı alınamadı." }
    )
}

enum class ScreenOrientationOption(val title: String, val activityOrientation: Int) {
    AUTO("Otomatik (Sensör)", ActivityInfo.SCREEN_ORIENTATION_SENSOR),
    LANDSCAPE("Yatay (Landscape)", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE),
    PORTRAIT("Dikey (Portrait)", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as WarPadApplication).repository

    val allProfiles: StateFlow<List<GamepadProfile>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedProfile = MutableStateFlow<GamepadProfile?>(null)
    val selectedProfile: StateFlow<GamepadProfile?> = _selectedProfile.asStateFlow()

    private val _canDrawOverlays = MutableStateFlow(false)
    val canDrawOverlays: StateFlow<Boolean> = _canDrawOverlays.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _orientationOption = MutableStateFlow(ScreenOrientationOption.AUTO)
    val orientationOption: StateFlow<ScreenOrientationOption> = _orientationOption.asStateFlow()

    private val _shizukuState = MutableStateFlow<ShizukuConnectionState>(ShizukuConnectionState.Idle)
    val shizukuState: StateFlow<ShizukuConnectionState> = _shizukuState.asStateFlow()

    private val _isCheckingShizuku = MutableStateFlow(false)
    val isCheckingShizuku: StateFlow<Boolean> = _isCheckingShizuku.asStateFlow()

    private val SHIZUKU_REQUEST_CODE = 8008

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                _shizukuState.value = ShizukuConnectionState.PermissionGranted
                viewModelScope.launch {
                    bindAndTestUserService()
                }
            } else {
                _shizukuState.value = ShizukuConnectionState.PermissionDenied
            }
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        viewModelScope.launch {
            checkShizukuStatus(autoConnect = true)
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _shizukuState.value = ShizukuConnectionState.ServiceNotRunning
        ShizukuInputBridge.instance.stop()
    }

    val isOverlayRunning: StateFlow<Boolean> = GamepadOverlayService.isRunning

    init {
        checkOverlayPermission()
        checkAccessibilityPermission()

        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionListener)
        } catch (e: Throwable) {
            Log.w("HomeViewModel", "Shizuku listener registration failed", e)
        }

        checkShizukuStatus(autoConnect = true)

        val prefs = application.getSharedPreferences(GamepadOverlayService.PREFS_NAME, Context.MODE_PRIVATE)
        val savedOrientationName = prefs.getString(GamepadOverlayService.PREF_ORIENTATION_MODE, ScreenOrientationOption.AUTO.name)
        _orientationOption.value = ScreenOrientationOption.entries.firstOrNull { it.name == savedOrientationName } ?: ScreenOrientationOption.AUTO

        viewModelScope.launch {
            repository.allProfiles.collect { list ->
                if (_selectedProfile.value == null && list.isNotEmpty()) {
                    val activeId = prefs.getLong(GamepadOverlayService.PREF_ACTIVE_PROFILE_ID, 1L)
                    val matching = list.firstOrNull { it.id == activeId } ?: list.first()
                    _selectedProfile.value = matching
                }
            }
        }
    }

    fun checkOverlayPermission() {
        _canDrawOverlays.value = Settings.canDrawOverlays(getApplication())
    }

    fun checkAccessibilityPermission() {
        val context: Context = getApplication()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val myService = "${context.packageName}/${FlexiPadAccessibilityService::class.java.name}"
        val isEnabled = enabledServices.contains(myService) || FlexiPadAccessibilityService.isConnected()
        _isAccessibilityEnabled.value = isEnabled
    }

    fun setOrientationOption(option: ScreenOrientationOption) {
        _orientationOption.value = option
        val prefs = getApplication<Application>().getSharedPreferences(GamepadOverlayService.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(GamepadOverlayService.PREF_ORIENTATION_MODE, option.name).apply()
    }

    fun selectProfile(profile: GamepadProfile) {
        _selectedProfile.value = profile
        if (isOverlayRunning.value) {
            GamepadOverlayService.start(getApplication(), profile.id)
        }
    }

    fun startControls(context: Context) {
        val profile = _selectedProfile.value ?: GamepadPresets.createWarThunderProfile()
        GamepadOverlayService.start(context, profile.id)
    }

    fun stopControls(context: Context) {
        GamepadOverlayService.stop(context)
    }

    fun toggleEditMode(context: Context) {
        GamepadOverlayService.toggleEdit(context)
    }

    fun updateSelectedProfile(updated: GamepadProfile) {
        _selectedProfile.value = updated
        viewModelScope.launch {
            repository.saveProfile(updated)
            if (isOverlayRunning.value) {
                GamepadOverlayService.start(getApplication(), updated.id)
            }
        }
    }

    fun setInputMode(mode: InputMode) {
        val cur = _selectedProfile.value ?: return
        updateSelectedProfile(cur.copy(inputMode = mode))
    }

    fun setGlobalOpacity(opacity: Float) {
        val cur = _selectedProfile.value ?: return
        val updatedControls = cur.controls.map { it.copy(opacity = opacity) }
        updateSelectedProfile(cur.copy(globalOpacity = opacity, controls = updatedControls))
    }

    fun setJoystickSensitivity(sensitivity: Float) {
        val cur = _selectedProfile.value ?: return
        updateSelectedProfile(cur.copy(
            leftStickSensitivity = sensitivity,
            rightStickSensitivity = sensitivity
        ))
    }

    fun setJoystickDeadzone(deadzone: Float) {
        val cur = _selectedProfile.value ?: return
        updateSelectedProfile(cur.copy(
            leftStickDeadzone = deadzone,
            rightStickDeadzone = deadzone
        ))
    }

    fun setHapticEnabled(enabled: Boolean) {
        val cur = _selectedProfile.value ?: return
        updateSelectedProfile(cur.copy(hapticFeedback = enabled))
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            val cur = _selectedProfile.value ?: return@launch
            val fresh = when {
                cur.name.contains("Xbox", ignoreCase = true) -> GamepadPresets.createXboxStandardProfile()
                cur.name.contains("FPS", ignoreCase = true) -> GamepadPresets.createFpsProfile()
                else -> GamepadPresets.createWarThunderProfile()
            }.copy(id = cur.id)
            repository.saveProfile(fresh)
            _selectedProfile.value = fresh
        }
    }

    fun duplicateCurrentProfile() {
        viewModelScope.launch {
            val cur = _selectedProfile.value ?: return@launch
            repository.duplicateProfile(cur)
        }
    }

    fun deleteCurrentProfile() {
        val cur = _selectedProfile.value ?: return
        if (cur.isPreset) return // Don't delete built-in presets
        viewModelScope.launch {
            repository.deleteProfile(cur.id)
            _selectedProfile.value = allProfiles.value.firstOrNull()
        }
    }

    fun launchGeForceNow(context: Context): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.nvidia.geforcenow")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                val playStoreIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.nvidia.geforcenow")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(playStoreIntent)
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun connectShizuku() {
        val context: Context = getApplication()
        viewModelScope.launch {
            _isCheckingShizuku.value = true

            // 1. Shizuku uygulamasının kurulu olup olmadığını kontrol et
            val isInstalled = isShizukuInstalled(context)
            if (!isInstalled) {
                _shizukuState.value = ShizukuConnectionState.NotInstalled
                _isCheckingShizuku.value = false
                return@launch
            }

            // 2. Shizuku servisinin başlatılmış olup olmadığını kontrol et
            val isServiceRunning = try {
                Shizuku.pingBinder()
            } catch (e: Throwable) {
                false
            }

            if (!isServiceRunning) {
                _shizukuState.value = ShizukuConnectionState.ServiceNotRunning
                _isCheckingShizuku.value = false
                return@launch
            }

            // 3. Uygulamanın Shizuku iznine sahip olup olmadığını kontrol et
            val hasPermission = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Throwable) {
                false
            }

            if (!hasPermission) {
                // 4. İzin yoksa Shizuku'nun resmi izin isteme mekanizmasını kullan
                _shizukuState.value = ShizukuConnectionState.PermissionPending
                try {
                    if (Shizuku.isPreV11()) {
                        _shizukuState.value = ShizukuConnectionState.PermissionDenied
                    } else {
                        Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                    }
                } catch (e: Throwable) {
                    Log.e("HomeViewModel", "Shizuku requestPermission failed", e)
                    _shizukuState.value = ShizukuConnectionState.PermissionDenied
                }
                _isCheckingShizuku.value = false
                return@launch
            }

            // 5. İzin zaten verilmiş -> InputUserService bağlantısı ve gerçek ping testi
            _shizukuState.value = ShizukuConnectionState.PermissionGranted
            bindAndTestUserService()
        }
    }

    private suspend fun bindAndTestUserService() {
        _isCheckingShizuku.value = true
        try {
            val result = ShizukuInputBridge.instance.connectAndTest(timeoutMs = 3500L)
            when (result) {
                is ConnectionTestResult.Success -> {
                    _shizukuState.value = ShizukuConnectionState.UserServiceSuccess
                }
                is ConnectionTestResult.ServiceNotRunning -> {
                    _shizukuState.value = ShizukuConnectionState.ServiceNotRunning
                }
                is ConnectionTestResult.PermissionRequired -> {
                    _shizukuState.value = ShizukuConnectionState.PermissionDenied
                }
                is ConnectionTestResult.Timeout -> {
                    _shizukuState.value = ShizukuConnectionState.UserServiceFailed(result.error)
                }
                is ConnectionTestResult.PingFailed -> {
                    _shizukuState.value = ShizukuConnectionState.UserServiceFailed(result.error)
                }
                is ConnectionTestResult.BindFailed -> {
                    _shizukuState.value = ShizukuConnectionState.UserServiceFailed(result.error)
                }
            }
        } catch (e: Throwable) {
            _shizukuState.value = ShizukuConnectionState.UserServiceFailed("Bağlantı hatası: ${e.localizedMessage ?: e.message}")
        } finally {
            _isCheckingShizuku.value = false
        }
    }

    fun checkShizukuStatus(autoConnect: Boolean = false) {
        val context: Context = getApplication()
        viewModelScope.launch {
            val isInstalled = isShizukuInstalled(context)
            if (!isInstalled) {
                _shizukuState.value = ShizukuConnectionState.NotInstalled
                return@launch
            }

            val isRunning = try {
                Shizuku.pingBinder()
            } catch (e: Throwable) {
                false
            }

            if (!isRunning) {
                _shizukuState.value = ShizukuConnectionState.ServiceNotRunning
                return@launch
            }

            val hasPermission = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Throwable) {
                false
            }

            if (!hasPermission) {
                if (_shizukuState.value !is ShizukuConnectionState.PermissionPending &&
                    _shizukuState.value !is ShizukuConnectionState.PermissionDenied) {
                    _shizukuState.value = ShizukuConnectionState.PermissionPending
                }
                return@launch
            }

            // İzin mevcut
            if (ShizukuInputBridge.instance.connected && ShizukuInputBridge.instance.ping()) {
                _shizukuState.value = ShizukuConnectionState.UserServiceSuccess
                return@launch
            }

            if (autoConnect) {
                _shizukuState.value = ShizukuConnectionState.PermissionGranted
                bindAndTestUserService()
            } else {
                _shizukuState.value = ShizukuConnectionState.PermissionGranted
            }
        }
    }

    private fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                context.packageManager.resolveContentProvider("moe.shizuku.privileged.api.provider", 0) != null
            } catch (_: Exception) {
                false
            }
        }
    }

    fun openShizukuApp(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun openShizukuPlayStore(context: Context) {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://shizuku.rikka.app/")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        } catch (_: Throwable) {}
    }
}
