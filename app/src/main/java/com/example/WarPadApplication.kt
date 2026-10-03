package com.example

import android.app.Application
import com.example.data.GamepadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WarPadApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var repository: GamepadRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = GamepadRepository.create(this)
        applicationScope.launch {
            repository.ensureDefaultPresets()
        }
    }

    companion object {
        lateinit var instance: WarPadApplication
            private set
    }
}
