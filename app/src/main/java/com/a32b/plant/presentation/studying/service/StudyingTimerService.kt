package com.a32b.plant.presentation.studying.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.a32b.plant.R
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StudyingTimerService : Service() {

    companion object {
        private val _elapsed = MutableStateFlow(0L)
        val elapsed: StateFlow<Long> = _elapsed

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_ACCUMULATED = "EXTRA_ACCUMULATED"

        private const val NOTIFICATION_ID = 9001
        const val CHANNEL_ID = "studying_timer_channel"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var segmentStart = 0L
    private var accumulatedMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                accumulatedMs = intent.getLongExtra(EXTRA_ACCUMULATED, 0L)
                startTimer()
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_STOP -> {
                stopTimer()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        _elapsed.value = accumulatedMs
        startForeground(NOTIFICATION_ID, buildNotification(accumulatedMs))
        segmentStart = SystemClock.elapsedRealtime()
        timerJob?.cancel()
        timerJob = scope.launch {
            var lastNotificationMs = accumulatedMs
            while (true) {
                delay(200)
                val ms = accumulatedMs + (SystemClock.elapsedRealtime() - segmentStart)
                _elapsed.value = ms
                if (ms - lastNotificationMs >= 1_000L) {
                    lastNotificationMs = ms
                    updateNotification(ms)
                }
            }
        }
    }

    private fun pauseTimer() {
        accumulatedMs += SystemClock.elapsedRealtime() - segmentStart
        timerJob?.cancel()
        _elapsed.value = accumulatedMs
        updateNotification(accumulatedMs)
    }

    private fun stopTimer() {
        timerJob?.cancel()
        accumulatedMs = 0L
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildNotification(ms: Long) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Plant 학습 중")
            .setContentText(TimeFormatter.formatToDigitalClock(ms))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    // 알림 탭 시 기존 MainActivity를 재사용해 학습 화면으로 복귀시킨다.
    // 싱글 액티비티 구조라 SINGLE_TOP만으로 Compose NavController 백스택이 그대로 유지된다.
    private val contentIntent: PendingIntent by lazy {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun updateNotification(ms: Long) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(ms))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "학습 타이머",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "학습 중 경과 시간을 표시합니다"
            setSound(null, null)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
