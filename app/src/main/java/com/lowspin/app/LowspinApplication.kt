package com.lowspin.app

import android.app.Application
import com.lowspin.app.di.AppContainer

class LowspinApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
