package com.topit.ecotrace

import android.app.Application
import android.util.Log
import com.topit.ecotrace.di.AppComponent
import com.topit.ecotrace.di.DaggerAppComponent
import com.yandex.mapkit.MapKitFactory

class EcoTraceApp : Application() {
    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.YANDEX_MAPS_API_KEY.isBlank()) {
            Log.e(TAG, "yandex.maps.api.key is missing in local.properties, the map will not load")
        } else {
            MapKitFactory.setApiKey(BuildConfig.YANDEX_MAPS_API_KEY)
        }
        MapKitFactory.initialize(this)
        appComponent = DaggerAppComponent.factory().create(this)
    }

    private companion object {
        const val TAG = "EcoTraceApp"
    }
}
