package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CombatRed
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ScopeCyan
import com.example.ui.theme.StealthBackground
import com.example.ui.theme.StealthCard
import com.example.ui.theme.TacticalAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechReportScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Teknik Fizibilite & Android Güvenlik Raporu",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("tech_report_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StealthBackground,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = StealthBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Executive Summary Alert
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1C0E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Bilgi",
                        tint = TacticalAmber,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dürüst Mühendislik Özeti",
                            fontWeight = FontWeight.Bold,
                            color = TacticalAmber,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Android işletim sistemi, sandbox güvenlik modeli nedeniyle standart uygulamaların başka bir pencereye (GeForce NOW gibi) izinsiz sanal tuş enjekte etmesini engeller. Aşağıda root olmadan gerçek çalışan yöntemleri ve teknik sınırları eksiksiz bulabilirsiniz.",
                            color = Color(0xFFE0E0E0),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Section 1: Android OS Constraint
            TechSectionCard(
                icon = Icons.Default.Security,
                iconColor = CombatRed,
                title = "1. Android'in INJECT_EVENTS Güvenlik Sınırı",
                badge = "Engellenen Yöntem",
                badgeColor = CombatRed,
                content = """
                    Android işletim sisteminde bir uygulamanın diğer uygulamalara doğrudan KeyEvent (örneğin KEYCODE_BUTTON_A) gönderebilmesi için 'android.permission.INJECT_EVENTS' izni gerekir.
                    
                    Bu izin 'signatureOrSystem' seviyesindedir. Yani yalnızca telefon üreticisinin ROM anahtarıyla imzalanmış sistem uygulamaları veya Root (Magisk/KernelSU) sahibi cihazlar bu izne doğrudan erişebilir. Normal Google Play uygulamaları sahte kodlarla bu izni taklit edemez.
                """.trimIndent()
            )

            // Section 2: Real Solution 1 (Accessibility Service Touch Emulation)
            TechSectionCard(
                icon = Icons.Default.CheckCircle,
                iconColor = ElectricGreen,
                title = "2. Gerçek Çözüm: Erişilebilirlik Servisi (Touch Gesture)",
                badge = "FlexiPad'de Aktif (Root İstemez)",
                badgeColor = ElectricGreen,
                content = """
                    Android 7.0+ ve 8.0+ (API 26+) resmi 'dispatchGesture' API'si ile sistem genelinde dokunma hareketleri enjekte edilebilir.
                    
                    FlexiPad, kullanıcının bastığı sanal tuşların ekran koordinatlarına gerçek DOWN -> HOLD (willContinue) -> UP dokunma hareketleri gönderir.
                    
                    GeForce NOW ve mobil oyunlar bu dokunmaları gerçek parmak teması olarak algılar. Root gerektirmez, yalnızca Android Erişilebilirlik ayarlarından izin verilmesi yeterlidir.
                """.trimIndent()
            )

            // Section 3: Real Solution 2 (Bluetooth HID)
            TechSectionCard(
                icon = Icons.Default.Bluetooth,
                iconColor = ScopeCyan,
                title = "3. Donanım Emülasyonu: Bluetooth HID API",
                badge = "FlexiPad'de Aktif (Root İstemez)",
                badgeColor = ElectricGreen,
                content = """
                    Android 9 (Pie) ile gelen resmi 'BluetoothHidDevice' API'si sayesinde telefonunuz fiziksel bir Bluetooth Gamepad (Xbox Gamepad) gibi davranabilir!
                    
                    FlexiPad standart bir Gamepad HID tanımlayıcısı (16 buton, 4 analog eksen, D-Pad hat switch) yükler. 
                    Cihazı GeForce NOW oynadığınız PC'ye, tablete veya destekleyen bir Android ana cihaza Bluetooth üzerinden bağladığınızda, sistem bunu gerçek bir fiziksel donanım olarak görür.
                    
                    Bu yöntem 100% resmidir, root gerektirmez ve en yüksek oyun uyumluluğunu sağlar.
                """.trimIndent()
            )

            // Section 4: Real Solution 3 (Shizuku / Wireless Debugging)
            TechSectionCard(
                icon = Icons.Default.Terminal,
                iconColor = TacticalAmber,
                title = "4. Sistem İçi Enjeksiyon: Shizuku / Kablosuz ADB",
                badge = "Mantis Pro Yöntemi (Root İstemez)",
                badgeColor = ElectricGreen,
                content = """
                    Piyasada 'Mantis Gamepad Pro' veya 'Panda Gamepad' gibi araçların kullandığı yöntemdir.
                    
                    Android 11 ve üzerinde yerleşik 'Kablosuz Hata Ayıklama (Wireless Debugging)' bulunur.
                    Shizuku veya yerel ADB oturumu açılarak shell (UID 2000) kullanıcısı yetkisi elde edilir. Shell kullanıcısı INJECT_EVENTS ve /dev/uinput yetkilerine sahiptir!
                    
                    FlexiPad içerisinde Shizuku köprüsü mevcuttur. Kullanıcı Shizuku uygulamasını bir kez yetkilendirdiğinde doğrudan sistem içine gamepad girdisi gönderilebilir.
                """.trimIndent()
            )

            // Section 5: Real Solution 4 (UDP Network Streaming)
            TechSectionCard(
                icon = Icons.Default.CloudQueue,
                iconColor = ElectricGreen,
                title = "5. Ağ / UDP Üzerinden Gamepad Akışı",
                badge = "0ms Gecikme (Host Modu)",
                badgeColor = ElectricGreen,
                content = """
                    GeForce NOW'u bilgisayarınızda veya yerel ağda oynuyorsanız:
                    
                    FlexiPad dokunmatik ekran hareketlerini anlık olarak yüksek hızlı UDP soket paketlerine dönüştürür.
                    PC tarafında ViGEmBus / vJoy veya Sunshine/Moonlight sanal kumanda sürücüsüyle birebir Xbox kontrolcüsü olarak oyuna iletilir.
                """.trimIndent()
            )

            // Section 6: Dual-Zone Optimized Overlay
            TechSectionCard(
                icon = Icons.Default.CheckCircle,
                iconColor = ElectricGreen,
                title = "6. FlexiPad Bireysel Pencere (Multi-Window) Mimarisi",
                badge = "Sıfır Ekran Engelleme",
                badgeColor = TacticalAmber,
                content = """
                    GeForce NOW'un mobil uygulamasında oyun oynarken en büyük sorun ekranın üzerindeki görünmez pencerelerin dokunmaları engellemesidir.
                    
                    FlexiPad:
                    1. Tam ekran görünmez pencere KULLANMAZ. Her tuş ve joystick için yalnızca kendi boyutunda mini pencereler açar.
                    2. Ekranın geri kalan tüm boş alanlarında ZERO overlay bulunur; oyun ekranına doğrudan dokunabilirsiniz.
                    3. FLAG_SPLIT_TOUCH ile çoklu parmak (multi-touch) eşzamanlı olarak farklı tuşlara ve her iki joysticke aynı anda basabilir.
                    4. Tuşlar gizlendiğinde tüm pencereler kaldırılır ve ekran tamamen serbest kalır.
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TechSectionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    badge: String,
    badgeColor: Color,
    content: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StealthCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.2f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = content,
                color = Color(0xFFC9D1D9),
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }
    }
}
