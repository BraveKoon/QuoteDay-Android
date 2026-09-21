package com.quoteday.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** 반복 주기. `rawValue` 는 저장되는 값이라 iOS 와 같아야 한다. */
enum class RecurrenceFrequency(val rawValue: String) {
    NONE("none"),
    DAILY("daily"),
    WEEKDAY("weekday"),
    WEEKLY("weekly"),
    BIWEEKLY("biweekly"),
    MONTHLY("monthly"),
    YEARLY("yearly");

    val title: String
        get() = when (this) {
            NONE -> "반복 안 함"; DAILY -> "매일"; WEEKDAY -> "주중 매일"; WEEKLY -> "매주"
            BIWEEKLY -> "2주마다"; MONTHLY -> "매월"; YEARLY -> "매년"
        }

    /** 선택 버튼처럼 좁은 자리에 넣는 짧은 이름. */
    val shortTitle: String
        get() = when (this) {
            NONE -> "안 함"; DAILY -> "매일"; WEEKDAY -> "주중"; WEEKLY -> "매주"
            BIWEEKLY -> "격주"; MONTHLY -> "매월"; YEARLY -> "매년"
        }

    companion object {
        /** 모르는 값이 들어오면 "반복 안 함"으로 떨어뜨린다. */
        fun from(rawValue: String?): RecurrenceFrequency =
            entries.firstOrNull { it.rawValue == rawValue } ?: NONE
    }
}

/**
 * 일정 하나의 반복 규칙.
 *
 * 반복 일정을 여러 행으로 복제해 저장하지 않고 이 규칙만 저장한 뒤, 화면에
 * 필요한 구간의 회차를 그때그때 계산한다. 그래서 "매일 · 종료 없음" 같은 규칙도
 * 저장 비용이 일정 한 건과 같다.
 *
 * 시각을 `LocalDateTime` 으로 다룬다. 절대 시각(epoch)으로 계산하면 서머타임이
 * 있는 지역에서 "매일 아침 8시"가 7시나 9시로 밀린다. 사람이 정한 일정은
 * 벽시계 시각이 기준이다.
 */
data class RecurrenceRule(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
    /** 마지막 반복 날짜(그날 포함). null 이면 종료 없이 계속 반복한다. */
    val endDate: LocalDate? = null,
) {
    /** 반복하지 않는 일정에 종료일이 남아 있으면 혼란만 준다. */
    val effectiveEndDate: LocalDate? = if (frequency == RecurrenceFrequency.NONE) null else endDate

    val isRepeating: Boolean get() = frequency != RecurrenceFrequency.NONE

    /**
     * `anchor` 를 첫 회차로 삼아 `from..to` 안에 들어오는 시작 시각들.
     *
     * 창(window)이 아무리 멀리 있어도 첫 후보 위치를 계산으로 건너뛰기 때문에
     * 순회 비용은 저장된 일정의 나이가 아니라 창 크기에만 비례한다.
     *
     * @param limit 최대 개수. 1 을 주면 "다음 회차"만 값싸게 찾을 수 있다.
     */
    fun occurrenceStarts(
        anchor: LocalDateTime,
        from: LocalDateTime,
        to: LocalDateTime,
        limit: Int = DEFAULT_LIMIT,
    ): List<LocalDateTime> {
        if (limit <= 0 || from > to || anchor > to) return emptyList()

        // 종료일은 그날 **끝**까지 포함한다.
        val repeatEnd = effectiveEndDate?.plusDays(1)?.atStartOfDay()
        val end = if (repeatEnd != null && repeatEnd < to) repeatEnd else to
        if (anchor > end) return emptyList()

        return when (frequency) {
            RecurrenceFrequency.NONE -> if (anchor >= from) listOf(anchor) else emptyList()
            RecurrenceFrequency.DAILY, RecurrenceFrequency.WEEKDAY ->
                dailyStarts(anchor, from, end, limit)
            RecurrenceFrequency.WEEKLY ->
                steppedStarts(anchor, from, end, limit, 7.0) { anchor.plusDays(it * 7L) }
            RecurrenceFrequency.BIWEEKLY ->
                steppedStarts(anchor, from, end, limit, 14.0) { anchor.plusDays(it * 14L) }
            // 31일에 시작한 일정은 짧은 달에서 그 달의 마지막 날로 당겨진다.
            RecurrenceFrequency.MONTHLY ->
                steppedStarts(anchor, from, end, limit, 31.0) { anchor.plusMonths(it.toLong()) }
            RecurrenceFrequency.YEARLY ->
                steppedStarts(anchor, from, end, limit, 366.0) { anchor.plusYears(it.toLong()) }
        }
    }

    /** 이 시각 뒤의 첫 회차. 없으면 null. */
    fun nextOccurrence(
        anchor: LocalDateTime,
        after: LocalDateTime,
        within: LocalDateTime = after.plusYears(2),
    ): LocalDateTime? = occurrenceStarts(anchor, after, within, limit = 1).firstOrNull()

    /** "매주 화요일 · 3월 1일까지" 처럼 규칙을 한 줄로 설명한다. */
    fun summary(anchor: LocalDateTime): String {
        if (!isRepeating) return RecurrenceFrequency.NONE.title

        var text = frequency.title
        when (frequency) {
            RecurrenceFrequency.WEEKLY, RecurrenceFrequency.BIWEEKLY ->
                text += " ${weekdayName(anchor.dayOfWeek)}"
            RecurrenceFrequency.MONTHLY -> text += " ${anchor.dayOfMonth}일"
            RecurrenceFrequency.YEARLY -> text += " ${anchor.monthValue}월 ${anchor.dayOfMonth}일"
            else -> Unit
        }
        effectiveEndDate?.let { text += " · ${it.monthValue}월 ${it.dayOfMonth}일까지" }
        return text
    }

    /** 매일 / 주중 매일: 창 안의 날짜를 하루씩 훑으며 같은 시각을 만든다. */
    private fun dailyStarts(
        anchor: LocalDateTime,
        from: LocalDateTime,
        end: LocalDateTime,
        limit: Int,
    ): List<LocalDateTime> {
        val time = anchor.toLocalTime()
        val lastDay = end.toLocalDate()
        var day = maxOf(anchor, from).toLocalDate()
        val result = mutableListOf<LocalDateTime>()

        while (day <= lastDay && result.size < limit) {
            val candidate = day.atTime(time)
            if (candidate >= anchor && candidate >= from && candidate <= end &&
                (frequency != RecurrenceFrequency.WEEKDAY || isWeekday(candidate))
            ) {
                result += candidate
            }
            day = day.plusDays(1)
        }
        return result
    }

    /**
     * 매주 / 격주 / 매월 / 매년: `anchor` 로부터 n 번째 회차를 직접 만든다.
     *
     * 이전 회차가 아니라 항상 `anchor` 기준으로 계산해서, 31일 → 28일처럼 한 번
     * 당겨진 날짜가 그대로 굳지 않게 한다.
     */
    private inline fun steppedStarts(
        anchor: LocalDateTime,
        from: LocalDateTime,
        end: LocalDateTime,
        limit: Int,
        approximatePeriodDays: Double,
        candidate: (Int) -> LocalDateTime,
    ): List<LocalDateTime> {
        var index = 0
        if (from > anchor) {
            // 주기를 넉넉하게 잡아 실제 회차보다 앞선 위치에서 시작한다(건너뛰지 않도록).
            val days = ChronoUnit.DAYS.between(anchor, from).toDouble()
            val periods = (days / approximatePeriodDays).toInt() - 1
            index = maxOf(0, minOf(periods, MAX_SKIP))
        }

        val result = mutableListOf<LocalDateTime>()
        var steps = 0
        while (result.size < limit && steps < MAX_STEPS) {
            steps += 1
            val date = candidate(index)
            index += 1
            if (date > end) break
            if (date < from || date < anchor) continue
            result += date
        }
        return result
    }

    private fun isWeekday(date: LocalDateTime): Boolean =
        date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY

    private fun weekdayName(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "월요일"; DayOfWeek.TUESDAY -> "화요일"; DayOfWeek.WEDNESDAY -> "수요일"
        DayOfWeek.THURSDAY -> "목요일"; DayOfWeek.FRIDAY -> "금요일"; DayOfWeek.SATURDAY -> "토요일"
        DayOfWeek.SUNDAY -> "일요일"
    }

    companion object {
        val none = RecurrenceRule()

        const val DEFAULT_LIMIT = 400

        /** 건너뛰기 계산이 이상한 값을 내도 순회가 폭주하지 않게 막는 상한. */
        const val MAX_SKIP = 1_000_000
        const val MAX_STEPS = 10_000
    }
}
