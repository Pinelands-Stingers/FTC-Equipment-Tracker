package za.org.team25310.ftcequipmenttracker

import android.app.Application
import com.google.firebase.FirebaseApp

class FtcApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
