package za.ac.iie.opsc_poe_screens

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.database.database

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // This is the correct place to set up persistence.
        // It runs once when the app is first created, before any other code.
        FirebaseRepository.initPersistence()
    }
}