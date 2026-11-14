package space.crewpulse

import android.app.Application
import space.crewpulse.data.reminders.ReminderWorker

class CrewPulseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderWorker.schedule(this)
    }
}
