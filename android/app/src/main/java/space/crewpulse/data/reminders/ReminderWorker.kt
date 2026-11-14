package space.crewpulse.data.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import space.crewpulse.CrewPulseApp
import space.crewpulse.R
import space.crewpulse.data.Profile
import space.crewpulse.domain.Reminders

/**
 * Periodically checks the adaptive measurement plan for the user's own data and posts a reminder
 * when something is due. Does nothing for the demo profile or without notification permission.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as CrewPulseApp).container
        val now = System.currentTimeMillis()
        val report = container.reportBuilder.build(Profile.ME, now)
        if (report.systems.isEmpty()) return Result.success() // no own data yet
        val message = report.plan?.let { Reminders.message(it, now) } ?: return Result.success()
        notify(applicationContext, message.title, message.text)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "measurement-reminders"
        private const val CHANNEL_ID = "measurements"
        private const val NOTIFICATION_ID = 1001

        /** Safe to call on every app start: keeps the existing schedule. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun notify(context: Context, title: String, text: String) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return // the UI asks for the permission; without it we stay silent
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Measurement reminders", NotificationManager.IMPORTANCE_DEFAULT),
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
