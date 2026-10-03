package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.WarPadApplication
import com.example.input.GamepadInputManager
import com.example.model.ButtonShape
import com.example.model.GamepadPresets
import com.example.model.GamepadProfile
import com.example.ui.overlay.DualZoneOverlayHud
import com.example.ui.overlay.FloatingQuickBubble
import com.example.ui.overlay.SingleControlHost
import com.example.ui.theme.WarPadTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class GamepadOverlayService : Service() {
    private val TAG = "GamepadOverlayService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private lateinit var inputManager: GamepadInputManager

    // Individual control overlay views: each button and joystick has its own exact-size window.
    // ZERO full-screen invisible window in normal gameplay mode = ZERO touch blocking on background!
    private val controlViews = mutableMapOf<String, ComposeView>()
    private var bubbleView: ComposeView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var editorOverlayView: OverlayContainerView? = null
    private var isTemporaryHidden = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        inputManager = GamepadInputManager(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Kontroller Başlatılıyor..."))

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner = OverlayLifecycleOwner().apply {
            onCreate()
            onResume()
        }
        _isRunning.value = true
        loadCurrentProfile()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_EDIT -> {
                _isEditMode.value = !_isEditMode.value
                updateNotification()
                refreshOverlay()
            }
            ACTION_TOGGLE_VISIBILITY -> {
                toggleHudVisibility()
            }
            ACTION_SET_PROFILE -> {
                val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, 1L)
                switchProfile(profileId)
            }
        }
        if (Settings.canDrawOverlays(this)) {
            refreshOverlay()
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        inputManager.releaseAll()
        val metrics = resources.displayMetrics
        bubbleParams?.let { params ->
            val bubbleSizePx = (50 * metrics.density).roundToInt()
            params.x = params.x.coerceIn(0, (metrics.widthPixels - bubbleSizePx).coerceAtLeast(0))
            params.y = params.y.coerceIn(0, (metrics.heightPixels - bubbleSizePx).coerceAtLeast(0))
            bubbleView?.let {
                try {
                    windowManager?.updateViewLayout(it, params)
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating bubble layout on rotate", e)
                }
            }
        }
        refreshOverlay()
    }

    fun toggleHudVisibility() {
        inputManager.releaseAll()
        val newVis = !_isHudVisible.value
        _isHudVisible.value = newVis
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_HUD_VISIBLE, newVis).apply()
        updateNotification()
        refreshOverlay()
    }

    private fun getFallbackPreset(id: Long): GamepadProfile = when (id) {
        1L -> GamepadPresets.createWarThunderProfile()
        2L -> GamepadPresets.createXboxStandardProfile()
        3L -> GamepadPresets.createFpsProfile()
        else -> GamepadPresets.createWarThunderProfile()
    }

    fun switchProfile(profileId: Long) {
        inputManager.releaseAll()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(PREF_ACTIVE_PROFILE_ID, profileId).apply()
        serviceScope.launch {
            val repository = WarPadApplication.instance.repository
            var profile = repository.getProfile(profileId)
            if (profile == null) {
                profile = getFallbackPreset(profileId)
                repository.saveProfile(profile)
            }
            _currentProfile.value = profile
            inputManager.setProfile(profile)
            updateNotification()
            refreshOverlay()
        }
    }

    private fun loadCurrentProfile() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val profileId = prefs.getLong(PREF_ACTIVE_PROFILE_ID, 1L)
        _isHudVisible.value = prefs.getBoolean(PREF_HUD_VISIBLE, true)
        serviceScope.launch {
            val repository = WarPadApplication.instance.repository
            var profile = repository.getProfile(profileId)
            if (profile == null) {
                profile = getFallbackPreset(profileId)
                repository.saveProfile(profile)
            }
            _currentProfile.value = profile
            inputManager.setProfile(profile)
            updateNotification()
            refreshOverlay()
        }
    }

    fun refreshOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        val profile = _currentProfile.value ?: return
        val isEdit = _isEditMode.value
        val isVisible = _isHudVisible.value

        if (isTemporaryHidden) {
            removeAllControlViews()
            removeBubbleView()
            removeEditorView()
            return
        }

        if (isEdit) {
            removeAllControlViews()
            removeBubbleView()
            showEditorOverlay(profile)
        } else {
            removeEditorView()
            if (!isVisible) {
                removeAllControlViews()
                ensureBubbleView(isHudVisible = false)
            } else {
                ensureBubbleView(isHudVisible = true)
                updateIndividualControlWindows(profile)
            }
        }
    }

    private fun ensureBubbleView(isHudVisible: Boolean) {
        val wm = windowManager ?: return
        val owner = lifecycleOwner ?: return
        val density = resources.displayMetrics.density
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultX = (12 * density).roundToInt()
        val defaultY = (12 * density).roundToInt()
        val savedX = prefs.getInt(PREF_BUBBLE_X, defaultX)
        val savedY = prefs.getInt(PREF_BUBBLE_Y, defaultY)

        val params = bubbleParams ?: WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }.also { bubbleParams = it }

        if (bubbleView == null) {
            val cv = ComposeView(this).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setContent {
                    WarPadTheme {
                        val hudVis by _isHudVisible.collectAsState()
                        val curProf by _currentProfile.collectAsState()
                        FloatingQuickBubble(
                            isEditMode = false,
                            isHudVisible = hudVis,
                            activeProfileId = curProf?.id ?: 1L,
                            onToggleEditMode = {
                                _isEditMode.value = true
                                updateNotification()
                                refreshOverlay()
                            },
                            onToggleHudVisibility = {
                                toggleHudVisibility()
                            },
                            onSelectProfile = { profileId ->
                                switchProfile(profileId)
                            },
                            onAddNewKey = {
                                _isEditMode.value = true
                                updateNotification()
                                refreshOverlay()
                            },
                            onOpenApp = {
                                val openIntent = Intent(this@GamepadOverlayService, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                startActivity(openIntent)
                            },
                            onStopService = {
                                stopSelf()
                            },
                            onDragDelta = { dx, dy ->
                                val p = bubbleParams ?: return@FloatingQuickBubble
                                val screenW = resources.displayMetrics.widthPixels
                                val screenH = resources.displayMetrics.heightPixels
                                val bubbleSizePx = (50 * density).roundToInt()
                                p.x = (p.x + dx).roundToInt().coerceIn(0, screenW - bubbleSizePx)
                                p.y = (p.y + dy).roundToInt().coerceIn(0, screenH - bubbleSizePx)
                                try {
                                    wm.updateViewLayout(bubbleView, p)
                                    prefs.edit().putInt(PREF_BUBBLE_X, p.x).putInt(PREF_BUBBLE_Y, p.y).apply()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error dragging floating bubble", e)
                                }
                            }
                        )
                    }
                }
            }
            try {
                wm.addView(cv, params)
                bubbleView = cv
            } catch (e: Exception) {
                Log.e(TAG, "Error adding bubble view", e)
            }
        } else {
            try {
                wm.updateViewLayout(bubbleView, params)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating bubble view layout", e)
            }
        }
    }

    private fun removeBubbleView() {
        bubbleView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing bubble view", e)
            }
            bubbleView = null
        }
    }

    private fun updateIndividualControlWindows(profile: GamepadProfile) {
        val wm = windowManager ?: return
        val owner = lifecycleOwner ?: return
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val density = metrics.density

        val activeIds = mutableSetOf<String>()

        for (item in profile.controls) {
            if (!item.isVisible) continue
            activeIds.add(item.id)

            val isPill = item.shape == ButtonShape.PILL
            val widthPx = ((if (isPill) item.sizeDp * 1.35f else item.sizeDp) * density).roundToInt()
            val heightPx = (item.sizeDp * density).roundToInt()

            val posX = ((item.xPercent * screenWidth) - widthPx / 2f).roundToInt().coerceIn(0, screenWidth - widthPx)
            val posY = ((item.yPercent * screenHeight) - heightPx / 2f).roundToInt().coerceIn(0, screenHeight - heightPx)

            val params = WindowManager.LayoutParams(
                widthPx,
                heightPx,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = posX
                y = posY
            }

            val existingView = controlViews[item.id]
            if (existingView != null) {
                try {
                    wm.updateViewLayout(existingView, params)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating control window ${item.id}", e)
                }
            } else {
                val cv = ComposeView(this).apply {
                    setViewTreeLifecycleOwner(owner)
                    setViewTreeSavedStateRegistryOwner(owner)
                    setViewTreeViewModelStoreOwner(owner)
                    setContent {
                        WarPadTheme {
                            val curProf by _currentProfile.collectAsState()
                            SingleControlHost(
                                item = item,
                                profile = curProf ?: profile,
                                inputManager = inputManager
                            )
                        }
                    }
                }
                try {
                    wm.addView(cv, params)
                    controlViews[item.id] = cv
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding control window ${item.id}", e)
                }
            }
        }

        // Remove controls that are no longer active or visible
        val toRemove = controlViews.keys.filter { it !in activeIds }
        for (id in toRemove) {
            controlViews[id]?.let {
                try {
                    wm.removeView(it)
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing old control view $id", e)
                }
            }
            controlViews.remove(id)
        }
    }

    private fun removeAllControlViews() {
        val wm = windowManager ?: return
        for ((id, view) in controlViews) {
            try {
                wm.removeView(view)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing control view $id", e)
            }
        }
        controlViews.clear()
    }

    private fun showEditorOverlay(profile: GamepadProfile) {
        val wm = windowManager ?: return
        val owner = lifecycleOwner ?: return
        if (editorOverlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        val customOverlayView = OverlayContainerView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            composeView.setViewTreeLifecycleOwner(owner)
            composeView.setViewTreeSavedStateRegistryOwner(owner)
            composeView.setViewTreeViewModelStoreOwner(owner)
        }

        customOverlayView.composeView.setContent {
            WarPadTheme {
                val prof by _currentProfile.collectAsState()
                prof?.let { activeProf ->
                    DualZoneOverlayHud(
                        profile = activeProf,
                        inputManager = inputManager,
                        isEditMode = true,
                        isHudVisible = true,
                        onToggleEditMode = {
                            _isEditMode.value = false
                            updateNotification()
                            refreshOverlay()
                        },
                        onToggleHudVisibility = {
                            toggleHudVisibility()
                        },
                        onOpenApp = {
                            val openIntent = Intent(this@GamepadOverlayService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(openIntent)
                        },
                        onStopService = {
                            stopSelf()
                        },
                        onUpdateControlItem = { updatedItem ->
                            val cur = _currentProfile.value ?: return@DualZoneOverlayHud
                            val updatedList = cur.controls.map {
                                if (it.id == updatedItem.id) updatedItem else it
                            }
                            val updatedProf = cur.copy(controls = updatedList)
                            _currentProfile.value = updatedProf
                            serviceScope.launch {
                                WarPadApplication.instance.repository.saveProfile(updatedProf)
                            }
                        },
                        onAddControlItem = { newItem ->
                            val cur = _currentProfile.value ?: return@DualZoneOverlayHud
                            val updatedList = cur.controls + newItem
                            val updatedProf = cur.copy(controls = updatedList)
                            _currentProfile.value = updatedProf
                            serviceScope.launch {
                                WarPadApplication.instance.repository.saveProfile(updatedProf)
                            }
                        },
                        onDeleteControlItem = { deletedId ->
                            val cur = _currentProfile.value ?: return@DualZoneOverlayHud
                            val updatedList = cur.controls.filter { it.id != deletedId }
                            val updatedProf = cur.copy(controls = updatedList)
                            _currentProfile.value = updatedProf
                            serviceScope.launch {
                                WarPadApplication.instance.repository.saveProfile(updatedProf)
                            }
                        },
                        onUpdateProfile = { updatedProf ->
                            _currentProfile.value = updatedProf
                            inputManager.setProfile(updatedProf)
                            serviceScope.launch {
                                WarPadApplication.instance.repository.saveProfile(updatedProf)
                            }
                        }
                    )
                }
            }
        }

        try {
            wm.addView(customOverlayView, params)
            editorOverlayView = customOverlayView
        } catch (e: Exception) {
            Log.e(TAG, "Error adding editor overlay", e)
        }
    }

    private fun removeEditorView() {
        editorOverlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing editor overlay", e)
            }
            editorOverlayView = null
        }
    }

    fun setTemporaryHudHidden(hidden: Boolean) {
        isTemporaryHidden = hidden
        refreshOverlay()
        updateNotification()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FlexiPad Gamepad Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sanal gamepad arka plan servisi"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(status: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, GamepadOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleEditIntent = Intent(this, GamepadOverlayService::class.java).apply {
            action = ACTION_TOGGLE_EDIT
        }
        val pendingEdit = PendingIntent.getService(
            this, 2, toggleEditIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val editLabel = if (_isEditMode.value) "Kilitle (Oyna)" else "Düzenle"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FlexiPad: ${_currentProfile.value?.name ?: "Aktif"}")
            .setContentText(status)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_edit, editLabel, pendingEdit)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Durdur", pendingStop)
            .build()
    }

    private fun updateNotification() {
        val status = if (!_isHudVisible.value) {
            "Kontroller Gizlendi (Oyun Devam Ediyor)"
        } else if (_isEditMode.value) {
            "Düzenleme Modu Aktif (Tuşları Sürükle)"
        } else {
            "GeForce NOW Kontrolleri Aktif"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(status))
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        instance = null
        inputManager.destroy()
        removeAllControlViews()
        removeBubbleView()
        removeEditorView()
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "flexipad_service_channel"
        const val NOTIFICATION_ID = 1001

        const val PREFS_NAME = "flexipad_overlay_prefs"
        const val PREF_BUBBLE_X = "bubble_x"
        const val PREF_BUBBLE_Y = "bubble_y"
        const val PREF_HUD_VISIBLE = "hud_visible"
        const val PREF_ACTIVE_PROFILE_ID = "active_profile_id"
        const val PREF_ORIENTATION_MODE = "orientation_mode"

        const val ACTION_STOP = "com.example.flexipad.STOP"
        const val ACTION_TOGGLE_EDIT = "com.example.flexipad.TOGGLE_EDIT"
        const val ACTION_TOGGLE_VISIBILITY = "com.example.flexipad.TOGGLE_VISIBILITY"
        const val ACTION_SET_PROFILE = "com.example.flexipad.SET_PROFILE"
        const val EXTRA_PROFILE_ID = "profile_id"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _isEditMode = MutableStateFlow(false)
        val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

        private val _isHudVisible = MutableStateFlow(true)
        val isHudVisible: StateFlow<Boolean> = _isHudVisible.asStateFlow()

        private val _currentProfile = MutableStateFlow<GamepadProfile?>(null)
        val currentProfile: StateFlow<GamepadProfile?> = _currentProfile.asStateFlow()

        var instance: GamepadOverlayService? = null
            private set

        fun start(context: Context, profileId: Long = 1L) {
            val intent = Intent(context, GamepadOverlayService::class.java).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GamepadOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun toggleEdit(context: Context) {
            val intent = Intent(context, GamepadOverlayService::class.java).apply {
                action = ACTION_TOGGLE_EDIT
            }
            context.startService(intent)
        }

        fun toggleVisibility(context: Context) {
            val intent = Intent(context, GamepadOverlayService::class.java).apply {
                action = ACTION_TOGGLE_VISIBILITY
            }
            context.startService(intent)
        }
    }
}
