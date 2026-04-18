package com.aegisinput.app

import android.app.Application

class AegisInputApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Rime engine on app startup
    }
}
