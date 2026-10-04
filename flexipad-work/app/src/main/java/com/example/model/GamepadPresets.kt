package com.example.model

/**
 * Pre-configured layout presets for mobile and cloud gaming scenarios.
 * Strictly adheres to clean input names (e.g. A, B, X, Y, LB, RB, LT, RT, LS, RS)
 * without any action description clutter on button faces.
 */
object GamepadPresets {
    /**
     * Default Gaming Profile:
     * Dual-zone layout optimized for cloud gaming and Android action games.
     * Features independent dual analog joysticks (LS and RS) and tactical fire buttons.
     */
    fun createWarThunderProfile(): GamepadProfile {
        val controls = listOf(
            // --- LEFT ZONE (Movement & Left Analog) ---
            GamepadControlItem(
                id = "wt_left_stick",
                type = GamepadButtonType.JOYSTICK_LEFT,
                label = "LS",
                subLabel = "",
                xPercent = 0.14f,
                yPercent = 0.72f,
                sizeDp = 140f,
                opacity = 0.70f,
                colorHex = 0xFFFF9800, // Tactical Amber
                shape = ButtonShape.CIRCLE,
                zone = ControlZone.LEFT
            ),
            GamepadControlItem(
                id = "wt_dpad",
                type = GamepadButtonType.DPAD,
                label = "D-PAD",
                subLabel = "",
                xPercent = 0.14f,
                yPercent = 0.32f,
                sizeDp = 115f,
                opacity = 0.65f,
                colorHex = 0xFF4CAF50,
                shape = ButtonShape.ROUNDED,
                zone = ControlZone.LEFT
            ),
            GamepadControlItem(
                id = "wt_l1_smoke",
                type = GamepadButtonType.BUTTON_L1,
                label = "LB",
                subLabel = "",
                xPercent = 0.07f,
                yPercent = 0.12f,
                sizeDp = 62f,
                opacity = 0.75f,
                colorHex = 0xFF9E9E9E,
                zone = ControlZone.LEFT
            ),
            GamepadControlItem(
                id = "wt_l2_zoom",
                type = GamepadButtonType.BUTTON_L2,
                label = "LT",
                subLabel = "",
                xPercent = 0.20f,
                yPercent = 0.12f,
                sizeDp = 68f,
                opacity = 0.80f,
                colorHex = 0xFF2196F3,
                zone = ControlZone.LEFT
            ),

            // --- RIGHT ZONE (Aiming Right Analog & Firepower) ---
            GamepadControlItem(
                id = "wt_right_stick",
                type = GamepadButtonType.JOYSTICK_RIGHT,
                label = "RS",
                subLabel = "",
                xPercent = 0.70f,
                yPercent = 0.72f,
                sizeDp = 140f,
                opacity = 0.70f,
                colorHex = 0xFF00E676, // Electric Green
                shape = ButtonShape.CIRCLE,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "wt_r2_fire",
                type = GamepadButtonType.BUTTON_R2,
                label = "RT",
                subLabel = "",
                xPercent = 0.90f,
                yPercent = 0.68f,
                sizeDp = 82f,
                opacity = 0.85f,
                colorHex = 0xFFF44336, // Combat Red
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "wt_r1_mg",
                type = GamepadButtonType.BUTTON_R1,
                label = "RB",
                subLabel = "",
                xPercent = 0.90f,
                yPercent = 0.44f,
                sizeDp = 66f,
                opacity = 0.75f,
                colorHex = 0xFFFF5722,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "wt_btn_x_bino",
                type = GamepadButtonType.BUTTON_X,
                label = "X",
                subLabel = "",
                xPercent = 0.78f,
                yPercent = 0.48f,
                sizeDp = 58f,
                opacity = 0.75f,
                colorHex = 0xFF29B6F6,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "wt_btn_y_lock",
                type = GamepadButtonType.BUTTON_Y,
                label = "Y",
                subLabel = "",
                xPercent = 0.82f,
                yPercent = 0.32f,
                sizeDp = 58f,
                opacity = 0.75f,
                colorHex = 0xFFFFCA28,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "wt_btn_a_repair",
                type = GamepadButtonType.BUTTON_A,
                label = "A",
                subLabel = "",
                xPercent = 0.82f,
                yPercent = 0.88f,
                sizeDp = 58f,
                opacity = 0.75f,
                colorHex = 0xFF66BB6A,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "wt_btn_b_artillery",
                type = GamepadButtonType.BUTTON_B,
                label = "B",
                subLabel = "",
                xPercent = 0.92f,
                yPercent = 0.88f,
                sizeDp = 58f,
                opacity = 0.75f,
                colorHex = 0xFFEF5350,
                zone = ControlZone.RIGHT
            ),

            // Center Top (Menu / Select)
            GamepadControlItem(
                id = "wt_select_map",
                type = GamepadButtonType.BUTTON_SELECT,
                label = "SELECT",
                subLabel = "",
                xPercent = 0.44f,
                yPercent = 0.08f,
                sizeDp = 50f,
                opacity = 0.60f,
                colorHex = 0xFF78909C,
                zone = ControlZone.CENTER
            ),
            GamepadControlItem(
                id = "wt_start_menu",
                type = GamepadButtonType.BUTTON_START,
                label = "START",
                subLabel = "",
                xPercent = 0.56f,
                yPercent = 0.08f,
                sizeDp = 50f,
                opacity = 0.60f,
                colorHex = 0xFF78909C,
                zone = ControlZone.CENTER
            )
        )

        return GamepadProfile(
            id = 1L,
            name = "1. Düzen (Varsayılan)",
            description = "Çift analog joystick (LS ve RS), tetikler ve taktik aksiyon tuşları.",
            isPreset = true,
            controls = controls,
            globalOpacity = 0.70f,
            hapticFeedback = true,
            leftStickDeadzone = 0.04f,
            leftStickSensitivity = 1.0f,
            rightStickDeadzone = 0.04f,
            rightStickSensitivity = 1.0f,
            dynamicJoystickCenter = false,
            centerAimFreeZone = true
        )
    }

    /**
     * Standard Xbox Style Gamepad profile with complete dual analog sticks,
     * diamond face buttons (A, B, X, Y), bumpers, triggers, and D-Pad.
     */
    fun createXboxStandardProfile(): GamepadProfile {
        val controls = listOf(
            // Left Stick & D-Pad
            GamepadControlItem(
                id = "xb_ls",
                type = GamepadButtonType.JOYSTICK_LEFT,
                label = "LS",
                subLabel = "",
                xPercent = 0.15f,
                yPercent = 0.42f,
                sizeDp = 135f,
                opacity = 0.70f,
                colorHex = 0xFF3DDC84,
                shape = ButtonShape.CIRCLE,
                isVisible = true,
                zone = ControlZone.LEFT
            ),
            GamepadControlItem(
                id = "xb_dpad",
                type = GamepadButtonType.DPAD,
                label = "D-PAD",
                subLabel = "",
                xPercent = 0.26f,
                yPercent = 0.75f,
                sizeDp = 115f,
                opacity = 0.65f,
                colorHex = 0xFF9E9E9E,
                isVisible = true,
                zone = ControlZone.LEFT
            ),
            GamepadControlItem(
                id = "xb_lb",
                type = GamepadButtonType.BUTTON_L1,
                label = "LB",
                subLabel = "",
                xPercent = 0.12f,
                yPercent = 0.14f,
                sizeDp = 65f,
                opacity = 0.75f,
                colorHex = 0xFF607D8B,
                isVisible = true,
                zone = ControlZone.LEFT
            ),
            GamepadControlItem(
                id = "xb_lt",
                type = GamepadButtonType.BUTTON_L2,
                label = "LT",
                subLabel = "",
                xPercent = 0.24f,
                yPercent = 0.14f,
                sizeDp = 68f,
                opacity = 0.75f,
                colorHex = 0xFF455A64,
                isVisible = true,
                zone = ControlZone.LEFT
            ),
            GamepadControlItem(
                id = "xb_l3",
                type = GamepadButtonType.BUTTON_L3,
                label = "L3",
                subLabel = "",
                xPercent = 0.05f,
                yPercent = 0.42f,
                sizeDp = 48f,
                opacity = 0.60f,
                colorHex = 0xFF78909C,
                isVisible = true,
                zone = ControlZone.LEFT
            ),

            // Right Stick & Face Buttons (A, B, X, Y)
            GamepadControlItem(
                id = "xb_rs",
                type = GamepadButtonType.JOYSTICK_RIGHT,
                label = "RS",
                subLabel = "",
                xPercent = 0.74f,
                yPercent = 0.72f,
                sizeDp = 135f,
                opacity = 0.70f,
                colorHex = 0xFF3DDC84,
                shape = ButtonShape.CIRCLE,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "xb_btn_y",
                type = GamepadButtonType.BUTTON_Y,
                label = "Y",
                subLabel = "",
                xPercent = 0.85f,
                yPercent = 0.35f,
                sizeDp = 58f,
                opacity = 0.75f,
                colorHex = 0xFFFFCA28,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "xb_btn_x",
                type = GamepadButtonType.BUTTON_X,
                label = "X",
                subLabel = "",
                xPercent = 0.77f,
                yPercent = 0.45f,
                sizeDp = 58f,
                opacity = 0.75f,
                colorHex = 0xFF29B6F6,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "xb_btn_b",
                type = GamepadButtonType.BUTTON_B,
                label = "B",
                subLabel = "",
                xPercent = 0.93f,
                yPercent = 0.45f,
                sizeDp = 58f,
                opacity = 0.75f,
                colorHex = 0xFFEF5350,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "xb_btn_a",
                type = GamepadButtonType.BUTTON_A,
                label = "A",
                subLabel = "",
                xPercent = 0.85f,
                yPercent = 0.55f,
                sizeDp = 58f,
                opacity = 0.75f,
                colorHex = 0xFF66BB6A,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "xb_rb",
                type = GamepadButtonType.BUTTON_R1,
                label = "RB",
                subLabel = "",
                xPercent = 0.88f,
                yPercent = 0.14f,
                sizeDp = 65f,
                opacity = 0.75f,
                colorHex = 0xFF607D8B,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "xb_rt",
                type = GamepadButtonType.BUTTON_R2,
                label = "RT",
                subLabel = "",
                xPercent = 0.76f,
                yPercent = 0.14f,
                sizeDp = 68f,
                opacity = 0.75f,
                colorHex = 0xFF455A64,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "xb_r3",
                type = GamepadButtonType.BUTTON_R3,
                label = "R3",
                subLabel = "",
                xPercent = 0.95f,
                yPercent = 0.72f,
                sizeDp = 48f,
                opacity = 0.60f,
                colorHex = 0xFF78909C,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),

            // Center
            GamepadControlItem(
                id = "xb_back",
                type = GamepadButtonType.BUTTON_SELECT,
                label = "SELECT",
                subLabel = "",
                xPercent = 0.42f,
                yPercent = 0.16f,
                sizeDp = 50f,
                opacity = 0.60f,
                colorHex = 0xFF546E7A,
                isVisible = true,
                zone = ControlZone.CENTER
            ),
            GamepadControlItem(
                id = "xb_home",
                type = GamepadButtonType.BUTTON_HOME,
                label = "XBOX",
                subLabel = "",
                xPercent = 0.50f,
                yPercent = 0.12f,
                sizeDp = 52f,
                opacity = 0.75f,
                colorHex = 0xFF2E7D32,
                isVisible = true,
                zone = ControlZone.CENTER
            ),
            GamepadControlItem(
                id = "xb_start",
                type = GamepadButtonType.BUTTON_START,
                label = "START",
                subLabel = "",
                xPercent = 0.58f,
                yPercent = 0.16f,
                sizeDp = 50f,
                opacity = 0.60f,
                colorHex = 0xFF546E7A,
                isVisible = true,
                zone = ControlZone.CENTER
            )
        )

        return GamepadProfile(
            id = 2L,
            name = "2. Düzen (Xbox Çift Analog)",
            description = "Eksiksiz klasik Xbox düzeni. Çift bağımsız analog çubuk, tetikler ve tam D-Pad.",
            isPreset = true,
            controls = controls,
            globalOpacity = 0.70f,
            hapticFeedback = true,
            leftStickDeadzone = 0.04f,
            leftStickSensitivity = 1.0f,
            rightStickDeadzone = 0.04f,
            rightStickSensitivity = 1.0f,
            dynamicJoystickCenter = false,
            centerAimFreeZone = false
        )
    }

    /**
     * FPS & Fast Shooter profile with dual analog joysticks and clean tactical controls.
     */
    fun createFpsProfile(): GamepadProfile {
        val controls = listOf(
            GamepadControlItem(
                id = "fps_ls",
                type = GamepadButtonType.JOYSTICK_LEFT,
                label = "LS",
                subLabel = "",
                xPercent = 0.15f,
                yPercent = 0.70f,
                sizeDp = 140f,
                opacity = 0.70f,
                colorHex = 0xFF00E676,
                shape = ButtonShape.CIRCLE,
                isVisible = true,
                zone = ControlZone.LEFT
            ),
            GamepadControlItem(
                id = "fps_rs",
                type = GamepadButtonType.JOYSTICK_RIGHT,
                label = "RS",
                subLabel = "",
                xPercent = 0.74f,
                yPercent = 0.70f,
                sizeDp = 140f,
                opacity = 0.70f,
                colorHex = 0xFF00E676,
                shape = ButtonShape.CIRCLE,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "fps_lt",
                type = GamepadButtonType.BUTTON_L2,
                label = "LT",
                subLabel = "",
                xPercent = 0.18f,
                yPercent = 0.25f,
                sizeDp = 76f,
                opacity = 0.80f,
                colorHex = 0xFF00B0FF,
                isVisible = true,
                zone = ControlZone.LEFT
            ),
            GamepadControlItem(
                id = "fps_rt",
                type = GamepadButtonType.BUTTON_R2,
                label = "RT",
                subLabel = "",
                xPercent = 0.86f,
                yPercent = 0.65f,
                sizeDp = 85f,
                opacity = 0.85f,
                colorHex = 0xFFFF1744,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "fps_a",
                type = GamepadButtonType.BUTTON_A,
                label = "A",
                subLabel = "",
                xPercent = 0.88f,
                yPercent = 0.42f,
                sizeDp = 64f,
                opacity = 0.75f,
                colorHex = 0xFFFFD600,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "fps_b",
                type = GamepadButtonType.BUTTON_B,
                label = "B",
                subLabel = "",
                xPercent = 0.94f,
                yPercent = 0.52f,
                sizeDp = 60f,
                opacity = 0.70f,
                colorHex = 0xFFFF9100,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "fps_x",
                type = GamepadButtonType.BUTTON_X,
                label = "X",
                subLabel = "",
                xPercent = 0.80f,
                yPercent = 0.52f,
                sizeDp = 60f,
                opacity = 0.70f,
                colorHex = 0xFF00E5FF,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "fps_y",
                type = GamepadButtonType.BUTTON_Y,
                label = "Y",
                subLabel = "",
                xPercent = 0.86f,
                yPercent = 0.28f,
                sizeDp = 60f,
                opacity = 0.70f,
                colorHex = 0xFFE040FB,
                isVisible = true,
                zone = ControlZone.RIGHT
            ),
            GamepadControlItem(
                id = "fps_l1",
                type = GamepadButtonType.BUTTON_L1,
                label = "LB",
                subLabel = "",
                xPercent = 0.08f,
                yPercent = 0.25f,
                sizeDp = 60f,
                opacity = 0.70f,
                colorHex = 0xFF76FF03,
                isVisible = true,
                zone = ControlZone.LEFT
            ),
            GamepadControlItem(
                id = "fps_r1",
                type = GamepadButtonType.BUTTON_R1,
                label = "RB",
                subLabel = "",
                xPercent = 0.92f,
                yPercent = 0.25f,
                sizeDp = 60f,
                opacity = 0.70f,
                colorHex = 0xFF76FF03,
                isVisible = true,
                zone = ControlZone.RIGHT
            )
        )

        return GamepadProfile(
            id = 3L,
            name = "3. Düzen (FPS Nişan)",
            description = "Birinci şahıs nişancı oyunları için çift analog, geniş tetikler ve hızlı aksiyon tuşları.",
            isPreset = true,
            controls = controls,
            globalOpacity = 0.70f,
            hapticFeedback = true,
            leftStickDeadzone = 0.04f,
            leftStickSensitivity = 1.0f,
            rightStickDeadzone = 0.04f,
            rightStickSensitivity = 1.0f,
            dynamicJoystickCenter = false,
            centerAimFreeZone = true
        )
    }
}
