package com.quoteday.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.quoteday.core.AppCategory
import com.quoteday.core.RecurrenceFrequency
import com.quoteday.core.RecurrenceRule
import com.quoteday.core.Schedule
import com.quoteday.core.ScheduleOccurrence
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * 일정 저장소.
 *
 * JSON 한 덩어리를 `SharedPreferences` 에 넣는다. Room 을 쓰지 않은 이유는
 * 두 가지다 — 일정은 많아야 수십 건이라 질의할 것이 없고, Room 을 쓰려면 KSP 가
 * 붙는데 이 저장소의 안드로이드 코드는 **CI 에서만** 컴파일된다. 빌드 배관이
 * 늘어날수록 한 번의 왕복에서 볼 수 있는 것이 줄어든다.
 *
 * 목록이 커져서 "이번 주 일정"을 질의로 뽑아야 할 때가 오면 그때 옮기면 된다.
 * 화면은 [occurrences] 만 보고 있어서 저장 방식이 바뀌어도 그대로다.
 */
class ScheduleStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

    private val items = mutableStateListOf<Schedule>().apply {
        addAll(load())
    }

    /** 저장된 일정 전부. 시작 시각 순서다. */
    val all: List<Schedule> get() = items

    fun schedule(id: String): Schedule? = items.firstOrNull { it.id == id }

    /** 그날 시작하는 회차들. 시간 순서로 돌려준다. */
    fun occurrences(on: LocalDate): List<ScheduleOccurrence> =
        items.mapNotNull { it.occurrence(on) }.sortedBy { it.start }

    /** `from` 뒤로 가장 먼저 오는 회차. 홈 화면의 "다음 일정"이 이 값을 쓴다. */
    fun nextOccurrence(from: LocalDateTime = LocalDateTime.now()): ScheduleOccurrence? =
        items.mapNotNull { it.nextOccurrence(from) }.minByOrNull { it.start }

    /**
     * 앞으로 다가오는 회차들. 알림을 미리 걸 때 쓴다.
     *
     * 반복 일정은 회차가 끝없이 나오므로 [limit] 으로 끊는다. 안드로이드가 걸어 둘
     * 수 있는 알람 수에도 한계가 있어서, 어차피 전부 걸 수는 없다.
     */
    fun upcomingOccurrences(
        from: LocalDateTime = LocalDateTime.now(),
        horizonDays: Long = 14,
        limit: Int = 50,
    ): List<ScheduleOccurrence> = items
        .flatMap { it.occurrences(from, from.plusDays(horizonDays), limit) }
        .sortedBy { it.start }
        .take(limit)

    // ------------------------------------------------------------ 쓰기

    fun add(
        title: String,
        start: LocalDateTime,
        end: LocalDateTime,
        category: AppCategory,
        memo: String = "",
        recurrence: RecurrenceRule = RecurrenceRule.none,
        isQuoteNotificationEnabled: Boolean = true,
    ): Schedule {
        val schedule = Schedule(
            id = UUID.randomUUID().toString(),
            title = title,
            start = start,
            end = end,
            category = category,
            memo = memo,
            recurrence = recurrence,
            isQuoteNotificationEnabled = isQuoteNotificationEnabled,
        )
        items += schedule
        sortAndPersist()
        return schedule
    }

    fun update(schedule: Schedule) {
        val index = items.indexOfFirst { it.id == schedule.id }
        if (index < 0) return
        items[index] = schedule
        sortAndPersist()
    }

    fun remove(id: String) {
        items.removeAll { it.id == id }
        persist()
    }

    private fun sortAndPersist() {
        val sorted = items.sortedBy { it.start }
        items.clear()
        items.addAll(sorted)
        persist()
    }

    private fun persist() {
        val json = Json.encodeToString(recordListSerializer, items.map { it.toRecord() })
        prefs.edit().putString(SCHEDULES, json).apply()
    }

    private fun load(): List<Schedule> {
        val stored = prefs.getString(SCHEDULES, null) ?: return emptyList()
        // 저장된 값이 깨졌다고 앱이 켜지지 않으면 안 된다. 못 읽으면 빈 목록으로 시작한다.
        return runCatching { Json.decodeFromString(recordListSerializer, stored) }
            .getOrNull()
            ?.mapNotNull { it.toSchedule() }
            ?.sortedBy { it.start }
            .orEmpty()
    }

    /**
     * 저장용 표현.
     *
     * `LocalDateTime` 을 그대로 직렬화할 수 없어서 ISO-8601 문자열로 적는다.
     * 사람이 읽을 수 있어서 저장된 값을 눈으로 확인하기도 쉽다.
     */
    @Serializable
    private data class ScheduleRecord(
        val id: String,
        val title: String,
        val start: String,
        val end: String,
        val category: String,
        val memo: String = "",
        val quoteSlug: String? = null,
        val recurrence: String = RecurrenceFrequency.NONE.rawValue,
        val recurrenceEnd: String? = null,
        val notify: Boolean = true,
    )

    private fun Schedule.toRecord() = ScheduleRecord(
        id = id,
        title = title,
        start = start.toString(),
        end = effectiveEnd.toString(),
        category = category.rawValue,
        memo = memo,
        quoteSlug = quoteSlug,
        recurrence = recurrence.frequency.rawValue,
        recurrenceEnd = recurrence.effectiveEndDate?.toString(),
        notify = isQuoteNotificationEnabled,
    )

    private fun ScheduleRecord.toSchedule(): Schedule? {
        val startAt = runCatching { LocalDateTime.parse(start) }.getOrNull() ?: return null
        val endAt = runCatching { LocalDateTime.parse(end) }.getOrNull() ?: startAt
        return Schedule(
            id = id,
            title = title,
            start = startAt,
            end = endAt,
            category = AppCategory.from(category),
            memo = memo,
            quoteSlug = quoteSlug,
            isQuoteNotificationEnabled = notify,
            recurrence = RecurrenceRule(
                frequency = RecurrenceFrequency.from(recurrence),
                endDate = recurrenceEnd?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            ),
        )
    }

    companion object {
        const val STORE_NAME = "quoteday.schedules"
        private const val SCHEDULES = "schedules.v1"
        private val recordListSerializer = ListSerializer(ScheduleRecord.serializer())
    }
}
