package com.quoteday.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quoteday.core.AppCategory

/**
 * 사용자 설정.
 *
 * `SharedPreferences` 에 담는다. DataStore 를 쓰지 않은 이유는 위젯과 알림
 * 스케줄러가 **코루틴 밖에서** 이 값을 읽어야 하기 때문이다. 설정은 몇 개의
 * 짧은 값뿐이라 동기 읽기로 잃는 것이 없다.
 *
 * 키 문자열은 iOS 와 같은 것을 쓴다. 두 앱이 같은 저장소를 공유하지는 않지만,
 * 한쪽을 고칠 때 다른 쪽에서 같은 이름을 찾을 수 있어야 한다.
 */
class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

    enum class Appearance(val rawValue: String) {
        SYSTEM("system"), LIGHT("light"), DARK("dark");

        val title: String
            get() = when (this) {
                SYSTEM -> "시스템"; LIGHT -> "라이트"; DARK -> "다크"
            }

        companion object {
            fun from(rawValue: String?): Appearance =
                entries.firstOrNull { it.rawValue == rawValue } ?: SYSTEM
        }
    }

    // Compose 가 다시 그리도록 상태로 들고, 쓸 때 곧바로 저장한다.
    var dailyQuoteEnabled: Boolean by mutableStateOf(prefs.getBoolean(DAILY_ENABLED, false))
        private set
    var dailyQuoteHour: Int by mutableStateOf(prefs.getInt(DAILY_HOUR, 8))
        private set
    var dailyQuoteMinute: Int by mutableStateOf(prefs.getInt(DAILY_MINUTE, 0))
        private set
    var preferredCategory: AppCategory? by mutableStateOf(
        prefs.getString(PREFERRED_CATEGORY, null)?.let { stored ->
            AppCategory.entries.firstOrNull { it.rawValue == stored }
        }
    )
        private set
    var appearance: Appearance by mutableStateOf(Appearance.from(prefs.getString(APPEARANCE, null)))
        private set
    var hasAskedForNotifications: Boolean by mutableStateOf(prefs.getBoolean(ASKED_NOTIFICATIONS, false))
        private set

    fun setDailyQuoteEnabled(value: Boolean) {
        dailyQuoteEnabled = value
        prefs.edit().putBoolean(DAILY_ENABLED, value).apply()
    }

    fun setDailyQuoteTime(hour: Int, minute: Int) {
        dailyQuoteHour = hour.coerceIn(0, 23)
        dailyQuoteMinute = minute.coerceIn(0, 59)
        prefs.edit()
            .putInt(DAILY_HOUR, dailyQuoteHour)
            .putInt(DAILY_MINUTE, dailyQuoteMinute)
            .apply()
    }

    /** null 은 "카테고리를 가리지 않음"이다. */
    fun setPreferredCategory(value: AppCategory?) {
        preferredCategory = value
        prefs.edit().apply {
            if (value == null) remove(PREFERRED_CATEGORY) else putString(PREFERRED_CATEGORY, value.rawValue)
        }.apply()
    }

    fun setAppearance(value: Appearance) {
        appearance = value
        prefs.edit().putString(APPEARANCE, value.rawValue).apply()
    }

    fun markNotificationsAsked() {
        hasAskedForNotifications = true
        prefs.edit().putBoolean(ASKED_NOTIFICATIONS, true).apply()
    }

    companion object {
        const val STORE_NAME = "quoteday.settings"

        private const val DAILY_ENABLED = "settings.dailyQuote.enabled"
        private const val DAILY_HOUR = "settings.dailyQuote.hour"
        private const val DAILY_MINUTE = "settings.dailyQuote.minute"
        private const val PREFERRED_CATEGORY = "settings.preferredCategory"
        private const val APPEARANCE = "settings.appearance"
        private const val ASKED_NOTIFICATIONS = "settings.hasRequestedNotifications"
    }
}
