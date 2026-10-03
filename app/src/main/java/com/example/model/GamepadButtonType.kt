package com.example.model

/**
 * All supported gamepad buttons, triggers, joysticks, and shortcuts.
 * Button defaultLabel strictly contains the input key name (e.g. A, B, X, Y, LB, RB, LT, RT).
 */
enum class GamepadButtonType(
    val defaultLabel: String,
    val description: String,
    val isAnalog: Boolean = false
) {
    // Left & Right Joysticks
    JOYSTICK_LEFT("LS", "Sol Analog Çubuk (LS)", isAnalog = true),
    JOYSTICK_RIGHT("RS", "Sağ Analog Çubuk (RS)", isAnalog = true),

    // Thumbstick clicks (L3 / R3)
    BUTTON_L3("L3", "Sol Çubuk Tıklama (L3)"),
    BUTTON_R3("R3", "Sağ Çubuk Tıklama (R3)"),

    // Face Buttons
    BUTTON_A("A", "A Butonu"),
    BUTTON_B("B", "B Butonu"),
    BUTTON_X("X", "X Butonu"),
    BUTTON_Y("Y", "Y Butonu"),

    // Shoulder Bumpers (L1, R1)
    BUTTON_L1("LB", "Sol Omuz Tuşu (LB / L1)"),
    BUTTON_R1("RB", "Sağ Omuz Tuşu (RB / R1)"),

    // Analog Triggers (L2, R2)
    BUTTON_L2("LT", "Sol Tetik (LT / L2)", isAnalog = true),
    BUTTON_R2("RT", "Sağ Tetik (RT / R2)", isAnalog = true),

    // D-Pad (Directional Pad)
    DPAD("D-PAD", "4 Yönlü D-Pad Pedi"),
    DPAD_UP("▲", "D-Pad Yukarı"),
    DPAD_DOWN("▼", "D-Pad Aşağı"),
    DPAD_LEFT("◀", "D-Pad Sol"),
    DPAD_RIGHT("▶", "D-Pad Sağ"),

    // Navigation & System
    BUTTON_START("START", "Menü / Start"),
    BUTTON_SELECT("SELECT", "Görünüm / Select / Back"),
    BUTTON_HOME("HOME", "Xbox / Guide / Home"),

    // Game Action Shortcuts (Always showing the mapped key label on button face)
    WT_MAIN_GUN("RT", "Ana Ateş / Eylem (RT)"),
    WT_ZOOM("LT", "Dürbün / Yakınlaştırma (LT)"),
    WT_BINOCULAR("X", "Serbest Kamera / Bakış (X)"),
    WT_SMOKE("LB", "Sis / Savunma (LB)"),
    WT_REPAIR("A", "Aksiyon / Tamir (A)"),
    WT_LOCK_TARGET("Y", "Hedef Takibi / Mesafe (Y)"),
    WT_ARTILLERY("B", "Destek / Topçu (B)")
}
