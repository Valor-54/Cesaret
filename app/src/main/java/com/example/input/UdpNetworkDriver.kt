package com.example.input

import android.util.Log
import com.example.model.GamepadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Sends ultra-low latency UDP gamepad packets to a target endpoint
 * (e.g. PC running Sunshine, Moonlight, ViGEmBus, or local host).
 */
class UdpNetworkDriver {
    private val TAG = "UdpNetworkDriver"
    private var socket: DatagramSocket? = null
    private var targetHost: String = "127.0.0.1"
    private var targetPort: Int = 8889
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isRunning = false

    fun start(host: String = "127.0.0.1", port: Int = 8889) {
        targetHost = host
        targetPort = port
        try {
            socket = DatagramSocket()
            isRunning = true
            Log.d(TAG, "UDP driver started on target: $targetHost:$targetPort")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create DatagramSocket", e)
        }
    }

    fun sendState(state: GamepadState) {
        if (!isRunning || socket == null) return
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "gamepad")
                    put("ts", state.timestamp)
                    put("lx", (state.leftStickX * 100).toInt())
                    put("ly", (state.leftStickY * 100).toInt())
                    put("rx", (state.rightStickX * 100).toInt())
                    put("ry", (state.rightStickY * 100).toInt())
                    put("lt", (state.l2Trigger * 100).toInt())
                    put("rt", (state.r2Trigger * 100).toInt())
                    put("buttons", state.pressedButtons.map { it.name })
                }.toString()
                val bytes = json.toByteArray(Charsets.UTF_8)
                val address = InetAddress.getByName(targetHost)
                val packet = DatagramPacket(bytes, bytes.size, address, targetPort)
                socket?.send(packet)
            } catch (e: Exception) {
                // Silent catch on high frequency UDP
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            Log.e(TAG, "Socket close failed", e)
        }
    }
}
