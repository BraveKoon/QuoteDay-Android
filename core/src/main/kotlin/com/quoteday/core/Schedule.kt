package com.quoteday.core

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 앱이 관리하는 일정 하나.
 *
 * 저장은 `:app` 의 Room 이 맡고, 여기 있는 것은 **규칙**뿐이다. 회차 계산과
 * 명언 배정은 저장소를 몰라도 되는 순수한 계산이라, 안드로이드 없이 테스트된다.
 */
data class Schedule(
    /** 알림 식별자와 딥링크의 기준. */
    val id: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val category: AppCategory,
    val memo: String = "",
    val isQuoteNotificationEnabled: Boolean = true,
    /** 이 일정에 못 박아 둔 명언. 비어 있으면 카테고리에서 결정적으로 계산한다. */
    val quoteSlug: String? = null,
    val recurrence: RecurrenceRule = RecurrenceRule.none,
) {
    /** 종료가 시작보다 빠른 일정은 만들지 않는다. */
    val effectiveEnd: LocalDateTime = if (end < start) start else end

    val isRecurring: Boolean get() = recurrence.isRepeating

    /** 화면에 보여 줄 제목. 비어 있으면 카테고리 이름으로 대신한다. */
    val displayTitle: String get() = title.trim().ifEmpty { category.title }

    val duration: Duration get() = Duration.between(start, effectiveEnd)

    /** 하루를 거의 다 차지하면 "종일"처럼 다룬다. */
    val isAllDayLike: Boolean get() = duration.toHours() >= 23

    /**
     * 알림에 쓰는 식별자.
     *
     * 반복 회차는 이 값을 접두사로 삼아 뒤에 시작 시각을 덧붙인다. 접두사를
     * 공유해야 일정 하나에 딸린 알림을 한 번에 지울 수 있다.
     */
    val notificationIdentifier: String get() = "schedule-$id"

    /** `from..to` 안에 들어오는 회차들. 반복하지 않으면 최대 1건. */
    fun occurrences(
        from: LocalDateTime,
        to: LocalDateTime,
        limit: Int = RecurrenceRule.DEFAULT_LIMIT,
    ): List<ScheduleOccurrence> {
        val length = duration
        return recurrence.occurrenceStarts(start, from, to, limit)
            .map { ScheduleOccurrence(this, it, it.plus(length)) }
    }

    /** 그날 시작하는 회차. 없으면 null. */
    fun occurrence(on: LocalDate): ScheduleOccurrence? = occurrences(
        from = on.atStartOfDay(),
        to = on.plusDays(1).atStartOfDay().minusNanos(1),
        limit = 1,
    ).firstOrNull()

    fun occurs(on: LocalDate): Boolean = occurrence(on) != null

    /**
     * `after` 뒤에 가장 먼저 시작하는 회차.
     *
     * @param horizonDays 이 기간 안에서만 찾는다. 종료 없는 반복도 무한히 뒤지지 않게 한다.
     */
    fun nextOccurrence(
        after: LocalDateTime,
        horizonDays: Long = 400,
    ): ScheduleOccurrence? = occurrences(
        from = after.plusNanos(1),
        to = after.plusDays(horizonDays),
        limit = 1,
    ).firstOrNull()

    /** 저장된 시작 시각 그대로의 첫 회차. */
    val firstOccurrence: ScheduleOccurrence get() = ScheduleOccurrence(this, start, effectiveEnd)
}

/**
 * 반복 일정의 "한 회차".
 *
 * 회차마다 행을 만들지 않고 원본 한 건에서 계산해 낸다. 화면·알림·위젯이 이 값을
 * 통해 일정을 다루므로, 반복 여부와 상관없이 같은 코드로 처리된다.
 */
data class ScheduleOccurrence(
    val schedule: Schedule,
    val start: LocalDateTime,
    val end: LocalDateTime,
) {
    /** 원본 식별자 + 시작 시각. 같은 일정의 다른 회차와 절대 겹치지 않는다. */
    val id: String get() = "${schedule.id}@$start"

    val displayTitle: String get() = schedule.displayTitle
    val category: AppCategory get() = schedule.category
    val isRecurring: Boolean get() = schedule.isRecurring

    /**
     * 알림 센터에 등록할 때 쓰는 식별자.
     *
     * 반복하지 않는 일정은 원본과 같은 값을 써서, 반복이 없던 시절에 걸어 둔
     * 알림과 어긋나지 않게 한다.
     */
    val notificationIdentifier: String
        get() = if (isRecurring) {
            "${schedule.notificationIdentifier}@${start.toLocalDate()}T${start.toLocalTime()}"
        } else {
            schedule.notificationIdentifier
        }

    /**
     * 이 회차에 붙는 명언.
     *
     * 명언을 못 박아 두지 않았다면 회차마다 다른 명언이 붙는다 — 매일 같은 일정에
     * 늘 같은 문장이 뜨면 이틀이면 읽지 않게 된다.
     */
    fun resolvedQuote(service: QuoteService = QuoteService()): Quote {
        schedule.quoteSlug?.let { slug ->
            service.quoteBySlug(slug)?.let { return it }
        }
        return service.quote(category, seed = "${schedule.id}:$start")
    }
}

/** 일정을 저장하기 전에 보는 규칙. 화면과 테스트가 함께 쓴다. */
object ScheduleValidator {

    enum class Failure(val message: String) {
        EMPTY_TITLE("일정 제목을 입력해 주세요."),
        END_BEFORE_START("종료 시간이 시작 시간보다 빠릅니다."),
        TOO_FAR_IN_PAST("너무 과거의 날짜입니다. 다시 확인해 주세요."),
        RECURRENCE_END_BEFORE_START("반복 종료일이 시작 날짜보다 빠릅니다."),
    }

    /** 1900년 이전은 명백한 입력 실수로 본다. 과거 일정 기록 자체는 허용한다. */
    private val earliestAllowed: LocalDateTime = LocalDateTime.of(1900, 1, 1, 0, 0)

    fun validate(
        title: String,
        start: LocalDateTime,
        end: LocalDateTime,
        recurrence: RecurrenceRule = RecurrenceRule.none,
    ): Failure? {
        if (title.trim().isEmpty()) return Failure.EMPTY_TITLE
        if (end < start) return Failure.END_BEFORE_START
        if (start < earliestAllowed) return Failure.TOO_FAR_IN_PAST
        // 종료일은 날짜 단위라 첫 회차와 같은 날이면 통과시킨다.
        val repeatEnd = recurrence.effectiveEndDate
        if (repeatEnd != null && repeatEnd < start.toLocalDate()) {
            return Failure.RECURRENCE_END_BEFORE_START
        }
        return null
    }
}
