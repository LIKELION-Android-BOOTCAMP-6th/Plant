package com.a32b.plant

import android.app.Application
import android.content.Context

class PlantApplication : Application(){
    companion object{
        lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}