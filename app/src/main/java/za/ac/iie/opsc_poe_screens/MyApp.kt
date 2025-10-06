package za.ac.iie.opsc_poe_screens

import android.app.Application
import androidx.room.Room

class MyApp : Application() {
    companion object {
        lateinit var db: AppDatabase
    }

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "finance_db"
        ).build()
    }
}