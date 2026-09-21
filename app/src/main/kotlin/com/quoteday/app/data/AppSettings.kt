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
 *
 * 값마다 **private 상태 + 공개 프로퍼티** 한 쌍으로 두었다. Compose 가 다시
 * 그리도록 상태가 필요하고, 저장은 세터가 맡는다. `var x by mutableStateOf(...)
 * private set` 옆에 `fun setX(...)` 를 두는 모양은 쓸 수 없다 — 둘 다 JVM 에서
 * `setX` 가 되어 "Platform declaration clash" 로 컴파일이 죽는다.
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

    private var dailyQuoteEnabledState by mutableStateOf(prefs.getBoolean(DAILY_ENABLED, false))
    private var dailyQuoteHourState by mutableStateOf(prefs.getInt(DAILY_HOUR, 8))
    private var dailyQuoteMinuteState by mutableStateOf(prefs.getInt(DAILY_MINUTE, 0))
    private var preferredCategoryState by mutableStateOf(
        prefs.getString(PREFERRED_CATEGORY, null)?.let { stored ->
            AppCategory.entries.firstOrNull { it.rawValue == stored }
        }
    )
    private var appearanceState by mutableStateOf(Appearance.from(prefs.getString(APPEARANCE, null)))
    private var hasAskedForNotificationsState by mutableStateOf(
        prefs.getBoolean(ASKED_NOTIFICATIONS, false)
    )

    var dailyQuoteEnabled: Boolean
        get() = dailyQuoteEnabledState
        set(value) {
            dailyQuoteEnabledState = value
            prefs.edit().putBoolean(DAILY_ENABLED, value).apply()
        }

    val dailyQuoteHour: Int get() = dailyQuoteHourState
    val dailyQuoteMinute: Int get() = dailyQuoteMinuteState

    /** 시와 분은 늘 함께 바뀌므로 하나로 묶어 둔다. */
    fun setDailyQuoteTime(hour: Int, minute: Int) {
        dailyQuoteHourState = hour.coerceIn(0, 23)
        dailyQuoteMinuteState = minute.coerceIn(0, 59)
        prefs.edit()
            .putInt(DAILY_HOUR, dailyQuoteHourState)
            .putInt(DAILY_MINUTE, dailyQuoteMinuteState)
            .apply()
    }

    /** null 은 "카테고리를 가리지 않음"이다. */
    var preferredCategory: AppCategory?
        get() = preferredCategoryState
        set(value) {
            preferredCategoryState = value
            prefs.edit().apply {
                if (value == null) {
                    remove(PREFERRED_CATEGORY)
                } else {
                    putString(PREFERRED_CATEGORY, value.rawValue)
                }
            }.apply()
        }

    var appearance: Appearance
        get() = appearanceState
        set(value) {
            appearanceState = value
            prefs.edit().putString(APPEARANCE, value.rawValue).apply()
        }

    /** 알림 권한을 이미 물어봤는지. 같은 것을 두 번 묻지 않으려고 남긴다. */
    var hasAskedForNotifications: Boolean
        get() = hasAskedForNotificationsState
        set(value) {
            hasAskedForNotificationsState = value
            prefs.edit().putBoolean(ASKED_NOTIFICATIONS, value).apply()
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
