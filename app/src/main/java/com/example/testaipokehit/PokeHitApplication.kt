package com.example.testaipokehit

import android.app.Application
import com.example.testaipokehit.di.AppContainer

class PokeHitApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}