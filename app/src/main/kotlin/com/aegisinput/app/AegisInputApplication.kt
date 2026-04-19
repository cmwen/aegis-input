package com.aegisinput.app

import android.app.Application
import com.aegisinput.engine.RimeBridge

class AegisInputApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RimeBridge.initialize(applicationContext)
    }
}
