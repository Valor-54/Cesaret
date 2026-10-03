package com.example.model

/**
 * Describes the active input injection or transmission mode.
 */
enum class InputMode(
    val title: String,
    val subtitle: String,
    val requiresRoot: Boolean,
    val description: String
) {
    OVERLAY_HUD(
        title = "Şeffaf HUD & Dokunmatik Katman",
        subtitle = "Sıfır Gecikme, Doğrudan Dokunma Ekranı",
        requiresRoot = false,
        description = "GeForce NOW ve oyunlar üzerinde şeffaf butonlar oluşturur. Çift dokunma ve ekran üzeri kontroller için en stabil moddur."
    ),
    BLUETOOTH_HID(
        title = "Bluetooth HID Gamepad (Gerçek Donanım)",
        subtitle = "Root İstemez, Android 9+ Standart API",
        requiresRoot = false,
        description = "Telefonunuzu gerçek bir fiziksel Bluetooth Xbox gamepad gibi tanıtır. Android'in BluetoothHidDevice API'si üzerinden gerçek HID raporları gönderir."
    ),
    SHIZUKU_ADB(
        title = "Shizuku / Kablosuz ADB Köprüsü",
        subtitle = "Root İstemez, Kablosuz Hata Ayıklama",
        requiresRoot = false,
        description = "Android 11+ Kablosuz Hata Ayıklama veya Shizuku üzerinden doğrudan sistem düzeyinde input komutları (input keyevent / uinput) enjekte eder."
    ),
    UDP_NETWORK(
        title = "Düşük Gecikmeli UDP Ağ Yayını",
        subtitle = "PC / Host / Sunucu Bağlantısı (0ms)",
        requiresRoot = false,
        description = "GeForce NOW oynadığınız bilgisayara veya yerel sunucuya (ViGEmBus / Sunshine / vJoy) yüksek hızlı UDP paketleri gönderir."
    )
}
