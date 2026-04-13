package com.topit.ecotrace

import android.app.Application
import com.topit.ecotrace.di.AppComponent
import com.topit.ecotrace.di.DaggerAppComponent
import com.yandex.mapkit.MapKitFactory

class EcoTraceApp : Application() {
    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey(BuildConfig.YANDEX_MAPS_API_KEY)
        MapKitFactory.initialize(this)
        appComponent = DaggerAppComponent.factory().create(this)
    }
}
