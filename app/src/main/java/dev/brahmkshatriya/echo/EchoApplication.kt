package dev.brahmkshatriya.echo

import android.app.Application
import com.google.android.material.color.DynamicColors

class EchoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}