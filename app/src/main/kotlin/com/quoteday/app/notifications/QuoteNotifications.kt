package com.quoteday.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.quoteday.app.MainActivity
import com.quoteday.app.R
import com.quoteday.app.data.AppSettings
import com.quoteday.app.data.ScheduleStore
import com.quoteday.core.NotificationContent
import com.quoteday.core.QuoteLibrary
import com.quoteday.core.QuoteService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 알림을 미리 걸어 둔다.
 *
 * 안드로이드에는 iOS 의 `UNUserNotificationCenter` 처럼 "내용까지 맡아 주는"
 * 예약 알림이 없다. `AlarmManager` 로 시각을 예약하고, 그 시각에 깨어난
 * [QuoteAlarmReceiver] 가 알림을 만들어 띄운다.
 *
 * ## 정확한 시각에 대하여
 *
 * 안드로이드 12부터 정확한 알람은 권한이 필요하고, 14부터는 대부분의 앱에
 * 자동으로 주어지지 않는다. 그래서 **권한이 있으면 정확하게, 없으면 창(window)**
 * 으로 건다. 일정 알림이 몇 분 늦는 것은 아쉽지만, 알림이 아예 오지 않거나
 * 앱이 정책에 걸려 내려가는 것보다 낫다.
 */
class QuoteNotifications(
    private val context: Context,
    private val settings: AppSettings,
    private val schedules: ScheduleStore,
    private val quotes: QuoteService = QuoteService(),
    private val library: QuoteLibrary = QuoteLibrary.shared,
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** 알림 권한이 있는지. 없으면 걸어도 뜨지 않는다. */
    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "오늘의 명언",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "일정 시작과 매일 정해진 시각에 명언을 보냅니다."
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * 걸려 있던 알림을 모두 지우고 다시 건다.
     *
     * 일정 하나가 바뀌면 그 일정의 회차가 통째로 달라질 수 있어서(반복 규칙),
     * 부분 갱신보다 다시 거는 편이 정확하다. 수십 건이라 비용도 없다.
     */
    fun reschedule() {
        ensureChannel()
        cancelAll()

        var slot = 0
        if (settings.dailyQuoteEnabled) {
            slot = scheduleDailyQuote(slot)
        }
        scheduleUpcoming(slot)
    }

    private fun cancelAll() {
        for (index in 0 until MAX_ALARMS) {
            alarmManager.cancel(alarmIntent(index, flags = PendingIntent.FLAG_NO_CREATE) ?: continue)
        }
    }

    /** 다음 며칠 치 오늘의 명언 알림. */
    private fun scheduleDailyQuote(startSlot: Int): Int {
        var slot = startSlot
        val now = LocalDateTime.now()
        for (offset in 0 until DAILY_DAYS) {
            if (slot >= MAX_ALARMS) break
            val date = LocalDate.now().plusDays(offset.toLong())
            val at = date.atTime(settings.dailyQuoteHour, settings.dailyQuoteMinute)
            if (at <= now) continue
            val content = NotificationContent.forDailyQuote(
                date = date,
                preferred = settings.preferredCategory,
                service = quotes,
                library = library,
            )
            schedule(slot, at, content)
            slot += 1
        }
        return slot
    }

    /** 다가오는 일정 회차 알림. */
    private fun scheduleUpcoming(startSlot: Int) {
        var slot = startSlot
        val now = LocalDateTime.now()
        for (occurrence in schedules.upcomingOccurrences(now)) {
            if (slot >= MAX_ALARMS) break
            if (!occurrence.schedule.isQuoteNotificationEnabled) continue
            val content = NotificationContent.forOccurrence(occurrence, quotes, library)
            schedule(slot, occurrence.start, content)
            slot += 1
        }
    }

    private fun schedule(slot: Int, at: LocalDateTime, content: NotificationContent) {
        val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(context, QuoteAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, content.title)
            putExtra(EXTRA_SUBTITLE, content.subtitle)
            putExtra(EXTRA_BODY, content.body)
            putExtra(EXTRA_DEEP_LINK, content.deepLink)
            putExtra(EXTRA_SLOT, slot)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            slot,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canBeExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, WINDOW_MILLIS, pending)
        }
    }

    private fun alarmIntent(slot: Int, flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        slot,
        Intent(context, QuoteAlarmReceiver::class.java),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_ID = "quoteday.quotes"

        const val EXTRA_TITLE = "title"
        const val EXTRA_SUBTITLE = "subtitle"
        const val EXTRA_BODY = "body"
        const val EXTRA_DEEP_LINK = "deepLink"
        const val EXTRA_SLOT = "slot"

        /**
         * 한 번에 걸어 두는 알람 수.
         *
         * 안드로이드는 앱이 걸 수 있는 알람 수를 제한한다. 넉넉하게 잡아도 이
         * 정도면 2주 치가 들어가고, 앱을 열 때마다 다시 걸리므로 모자랄 일이 없다.
         */
        const val MAX_ALARMS = 64

        /** 오늘의 명언을 며칠 치 미리 걸어 둘지. */
        const val DAILY_DAYS = 7

        /** 정확한 알람을 쓸 수 없을 때의 허용 오차. */
        const val WINDOW_MILLIS = 10 * 60 * 1000L
    }
}

/** 예약한 시각에 깨어나 알림을 띄운다. */
class QuoteAlarmReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(QuoteNotifications.EXTRA_TITLE) ?: "오늘의 명언"
        val body = intent.getStringExtra(QuoteNotifications.EXTRA_BODY).orEmpty()
        val subtitle = intent.getStringExtra(QuoteNotifications.EXTRA_SUBTITLE)
        val deepLink = intent.getStringExtra(QuoteNotifications.EXTRA_DEEP_LINK)
        val slot = intent.getIntExtra(QuoteNotifications.EXTRA_SLOT, 0)

        val open = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = deepLink?.let(Uri::parse)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            slot,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, QuoteNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(subtitle ?: body.lineSequence().firstOrNull().orEmpty())
            // 명언은 한 줄에 다 들어가지 않는다. 펼쳤을 때 전문이 보이게 한다.
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // 권한 확인은 이 자리에 그대로 적는다. 다른 함수로 빼면 린트가 따라가지
        // 못해 MissingPermission 오류가 난다 — 그리고 그 경고는 옳다. 권한이
        // 없는 채로 notify 를 부르면 아무 일도 일어나지 않는데, 그 사실이
        // 어디에도 드러나지 않는다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // 권한이 있어도 시스템 쪽 사정으로 실패할 수 있다. 여기서 죽으면
        // 시스템이 앱을 문제 삼으므로 조용히 넘어간다.
        runCatching {
            NotificationManagerCompat.from(context).notify(slot, notification)
        }
    }
}

/**
 * 다시 켜진 뒤 알람을 다시 건다.
 *
 * 기기를 껐다 켜면 걸어 둔 알람이 모두 사라진다. 이 수신기가 없으면 재부팅한
 * 사용자에게는 그 뒤로 알림이 오지 않는다 — 그리고 아무도 그 사실을 모른다.
 */
class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? com.quoteday.app.QuoteDayApplication ?: return
        app.notifications.reschedule()
    }
}
