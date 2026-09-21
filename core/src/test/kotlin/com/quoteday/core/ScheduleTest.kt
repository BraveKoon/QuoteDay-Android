package com.quoteday.core

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 일정 회차와 검증 규칙. iOS `Tests/RecurrenceTests.swift` 의 뒷부분과 같은 것을 본다. */
class ScheduleTest {

    private fun at(
        year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0,
    ): LocalDateTime = LocalDateTime.of(year, month, day, hour, minute)

    private fun schedule(
        title: String = "아침 러닝",
        start: LocalDateTime = at(2026, 3, 2, 9),
        minutes: Long = 30,
        category: AppCategory = AppCategory.EXERCISE,
        recurrence: RecurrenceRule = RecurrenceRule.none,
        quoteSlug: String? = null,
    ) = Schedule(
        id = "schedule-1",
        title = title,
        start = start,
        end = start.plusMinutes(minutes),
        category = category,
        recurrence = recurrence,
        quoteSlug = quoteSlug,
    )

    // ------------------------------------------------------------ 회차

    @Test
    fun `a schedule occurs on later repeat days`() {
        val item = schedule(recurrence = RecurrenceRule(RecurrenceFrequency.WEEKLY))
        assertTrue(item.occurs(LocalDate.of(2026, 3, 9)))
        assertFalse(item.occurs(LocalDate.of(2026, 3, 10)))
    }

    @Test
    fun `an occurrence keeps the original duration`() {
        val item = schedule(
            title = "회의",
            minutes = 90,
            category = AppCategory.WORK,
            recurrence = RecurrenceRule(RecurrenceFrequency.DAILY),
        )
        val occurrence = item.occurrence(LocalDate.of(2026, 3, 5))
        assertNotNull(occurrence)
        assertEquals(90, java.time.Duration.between(occurrence.start, occurrence.end).toMinutes())
        assertEquals(9, occurrence.start.hour)
    }

    @Test
    fun `the next occurrence looks past the original start`() {
        val item = schedule(recurrence = RecurrenceRule(RecurrenceFrequency.DAILY))
        val next = item.nextOccurrence(after = at(2026, 3, 10, 12))
        assertEquals(at(2026, 3, 11, 9, 0), next?.start)
    }

    @Test
    fun `a non repeating schedule has no next occurrence in the past`() {
        assertNull(schedule().nextOccurrence(after = at(2026, 3, 3)))
    }

    // ------------------------------------------------------------ 알림 식별자

    @Test
    fun `occurrence notification identifiers are unique`() {
        val item = schedule(
            title = "물 마시기",
            category = AppCategory.DAILY,
            recurrence = RecurrenceRule(RecurrenceFrequency.DAILY),
        )
        val occurrences = item.occurrences(at(2026, 3, 2), at(2026, 3, 6))
        val identifiers = occurrences.map { it.notificationIdentifier }.toSet()
        assertEquals(occurrences.size, identifiers.size)
        assertTrue(identifiers.all { it.startsWith(item.notificationIdentifier) })
    }

    /** 반복이 없던 시절에 걸어 둔 알림과 어긋나면 안 된다. */
    @Test
    fun `a non repeating schedule keeps the plain notification identifier`() {
        val item = schedule()
        assertEquals(item.notificationIdentifier, item.firstOccurrence.notificationIdentifier)
    }

    // ------------------------------------------------------------ 명언 배정

    /** 매일 같은 일정에 늘 같은 문장이 뜨면 이틀이면 읽지 않게 된다. */
    @Test
    fun `each occurrence gets its own quote`() {
        val item = schedule(recurrence = RecurrenceRule(RecurrenceFrequency.DAILY))
        val quotes = item.occurrences(at(2026, 3, 2), at(2026, 3, 8, 23, 59)).map { it.resolvedQuote().slug }
        assertEquals(7, quotes.size)
        assertTrue(quotes.toSet().size > 1, "회차마다 같은 명언만 나옵니다.")
    }

    @Test
    fun `a pinned quote wins over the computed one`() {
        val pinned = QuoteLibrary.shared.quotes.first().slug
        val item = schedule(
            recurrence = RecurrenceRule(RecurrenceFrequency.DAILY),
            quoteSlug = pinned,
        )
        val quotes = item.occurrences(at(2026, 3, 2), at(2026, 3, 8, 23, 59)).map { it.resolvedQuote().slug }
        assertEquals(setOf(pinned), quotes.toSet())
    }

    /** 같은 회차는 앱을 다시 켜도 같은 명언이어야 한다. */
    @Test
    fun `the same occurrence always gets the same quote`() {
        val item = schedule(recurrence = RecurrenceRule(RecurrenceFrequency.DAILY))
        val first = item.occurrence(LocalDate.of(2026, 3, 5))?.resolvedQuote()?.slug
        val second = item.occurrence(LocalDate.of(2026, 3, 5))?.resolvedQuote()?.slug
        assertNotNull(first)
        assertEquals(first, second)
    }

    // ------------------------------------------------------------ 검증

    @Test
    fun `the validator rejects an empty title`() {
        val failure = ScheduleValidator.validate("   ", at(2026, 3, 2, 9), at(2026, 3, 2, 10))
        assertEquals(ScheduleValidator.Failure.EMPTY_TITLE, failure)
    }

    @Test
    fun `the validator rejects an end before the start`() {
        val failure = ScheduleValidator.validate("회의", at(2026, 3, 2, 10), at(2026, 3, 2, 9))
        assertEquals(ScheduleValidator.Failure.END_BEFORE_START, failure)
    }

    @Test
    fun `the validator rejects a repeat end before the start`() {
        val failure = ScheduleValidator.validate(
            "회의",
            at(2026, 3, 2, 9),
            at(2026, 3, 2, 10),
            RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2026, 3, 1)),
        )
        assertEquals(ScheduleValidator.Failure.RECURRENCE_END_BEFORE_START, failure)
    }

    /** 종료일은 날짜 단위라 첫 회차와 같은 날이면 통과시킨다. */
    @Test
    fun `the validator accepts a repeat end on the start day`() {
        val failure = ScheduleValidator.validate(
            "회의",
            at(2026, 3, 2, 9),
            at(2026, 3, 2, 10),
            RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2026, 3, 2)),
        )
        assertNull(failure)
    }

    @Test
    fun `an empty title falls back to the category name`() {
        assertEquals("운동", schedule(title = "  ").displayTitle)
    }
}
