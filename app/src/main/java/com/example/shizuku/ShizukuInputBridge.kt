package com.example.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.util.Log
import com.example.WarPadApplication
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku

sealed class ConnectionTestResult {
    object Success : ConnectionTestResult()
    object ServiceNotRunning : ConnectionTestResult()
    object PermissionRequired : ConnectionTestResult()
    data class BindFailed(val error: String) : ConnectionTestResult()
    data class Timeout(val error: String) : ConnectionTestResult()
    data class PingFailed(val error: String) : ConnectionTestResult()
}

class ShizukuInputBridge(customPackageName: String? = null) {

    companion object {
        private const val TAG = "FlexiPadShizuku"

        val instance: ShizukuInputBridge by lazy { ShizukuInputBridge() }

        private const val DESCRIPTOR =
            "com.example.shizuku.IInputUserService"

        private const val TRANSACTION_EXECUTE =
            IBinder.FIRST_CALL_TRANSACTION

        private const val TRANSACTION_PING =
            IBinder.FIRST_CALL_TRANSACTION + 1

        private const val TRANSACTION_TOUCH =
            IBinder.FIRST_CALL_TRANSACTION + 2

        const val TOUCH_DOWN = 0
        const val TOUCH_MOVE = 1
        const val TOUCH_UP = 2
    }

    private val packageName: String = customPackageName ?: try {
        WarPadApplication.instance.packageName
    } catch (_: Throwable) {
        "com.aistudio.flexipad.wtov"
    }

    @Volatile
    private var remote: IBinder? = null

    @Volatile
    var connected: Boolean = false
        private set

    var onConnectionStateChanged: ((Boolean) -> Unit)? = null

    private val serviceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(
                packageName,
                InputUserService::class.java.name
            )
        )
            .daemon(true)
            .processNameSuffix("input")
            .version(2)
            .tag("flexipad-input")

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            remote = service
            connected = service != null

            Log.i(
                TAG,
                "Shizuku UserService bağlandı: $connected"
            )
            onConnectionStateChanged?.invoke(connected)
        }

        override fun onServiceDisconnected(
            name: ComponentName?
        ) {
            remote = null
            connected = false

            Log.w(
                TAG,
                "Shizuku UserService bağlantısı kesildi"
            )
            onConnectionStateChanged?.invoke(false)
        }
    }

    fun start(): Boolean {
        return try {

            if (!Shizuku.pingBinder()) {
                Log.w(TAG, "Shizuku çalışmıyor")
                return false
            }

            if (
                Shizuku.checkSelfPermission() !=
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Shizuku izni yok")
                return false
            }

            Shizuku.bindUserService(
                serviceArgs,
                connection
            )

            true

        } catch (e: Throwable) {

            Log.e(
                TAG,
                "UserService başlatılamadı",
                e
            )

            false
        }
    }

    suspend fun connectAndTest(timeoutMs: Long = 3500L): ConnectionTestResult {
        try {
            if (!Shizuku.pingBinder()) {
                return ConnectionTestResult.ServiceNotRunning
            }
        } catch (_: Throwable) {
            return ConnectionTestResult.ServiceNotRunning
        }

        val hasPermission = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }

        if (!hasPermission) {
            return ConnectionTestResult.PermissionRequired
        }

        if (connected && ping()) {
            return ConnectionTestResult.Success
        }

        val bindStarted = start()
        if (!bindStarted) {
            return ConnectionTestResult.BindFailed("Shizuku.bindUserService çağrısı başarısız oldu")
        }

        val startTime = System.currentTimeMillis()
        while (!connected && (System.currentTimeMillis() - startTime) < timeoutMs) {
            delay(50)
        }

        if (!connected) {
            return ConnectionTestResult.Timeout("InputUserService bağlantısı zaman aşımına uğradı (${timeoutMs}ms)")
        }

        val pingSuccess = ping()
        return if (pingSuccess) {
            ConnectionTestResult.Success
        } else {
            ConnectionTestResult.PingFailed("InputUserService yanıt vermiyor (Ping başarısız)")
        }
    }

    fun stop() {

        try {

            Shizuku.unbindUserService(
                serviceArgs,
                connection,
                true
            )

        } catch (e: Throwable) {

            Log.w(
                TAG,
                "UserService durdurulurken hata",
                e
            )
        }

        remote = null
        connected = false
    }

    fun ping(): Boolean {

        val binder = remote ?: return false

        return try {

            val data = Parcel.obtain()
            val reply = Parcel.obtain()

            try {

                data.writeInterfaceToken(
                    DESCRIPTOR
                )

                binder.transact(
                    TRANSACTION_PING,
                    data,
                    reply,
                    0
                )

                reply.readException()

                reply.readInt() == 1

            } finally {

                data.recycle()
                reply.recycle()
            }

        } catch (e: RemoteException) {

            Log.e(
                TAG,
                "Ping başarısız",
                e
            )

            false
        }
    }

    fun execute(command: String): Int {

        val binder = remote ?: return -100

        return try {

            val data = Parcel.obtain()
            val reply = Parcel.obtain()

            try {

                data.writeInterfaceToken(
                    DESCRIPTOR
                )

                data.writeString(command)

                binder.transact(
                    TRANSACTION_EXECUTE,
                    data,
                    reply,
                    0
                )

                reply.readException()

                reply.readInt()

            } finally {

                data.recycle()
                reply.recycle()
            }

        } catch (e: Throwable) {

            Log.e(
                TAG,
                "Shizuku komutu çalıştırılamadı",
                e
            )

            -101
        }
    }

    fun sendTouch(
        pointerId: Int,
        action: Int,
        x: Float,
        y: Float
    ): Boolean {

        val binder = remote ?: return false

        return try {

            val data = Parcel.obtain()
            val reply = Parcel.obtain()

            try {

                data.writeInterfaceToken(
                    DESCRIPTOR
                )

                data.writeInt(pointerId)
                data.writeInt(action)
                data.writeFloat(x)
                data.writeFloat(y)

                binder.transact(
                    TRANSACTION_TOUCH,
                    data,
                    reply,
                    0
                )

                reply.readException()

                reply.readInt() == 1

            } finally {

                data.recycle()
                reply.recycle()
            }

        } catch (e: Throwable) {

            Log.e(
                TAG,
                "Touch gönderilemedi",
                e
            )

            false
        }
    }
}
