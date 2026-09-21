package com.quoteday.core

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 반복 규칙의 회차 계산 검증. iOS `Tests/RecurrenceTests.swift` 와 같은 것을 본다.
 *
 * 반복 일정은 저장된 행이 한 건뿐이라, 화면·알림·위젯이 보는 모든 값이 이
 * 계산에서 나온다. 그래서 경계(종료일, 짧은 달, 주말)를 집중적으로 본다.
 */
class RecurrenceTest {

    private fun at(
        year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0,
    ): LocalDateTime = LocalDateTime.of(year, month, day, hour, minute)

    // ------------------------------------------------------------ 기본 동작

    @Test
    fun `no repeat yields only the anchor`() {
        val anchor = at(2026, 3, 2, 9)
        val starts = RecurrenceRule.none.occurrenceStarts(anchor, at(2026, 3, 1), at(2026, 3, 31))
        assertEquals(listOf(anchor), starts)
    }

    @Test
    fun `an anchor outside the window is not returned`() {
        val anchor = at(2026, 3, 2, 9)
        val starts = RecurrenceRule.none.occurrenceStarts(anchor, at(2026, 4, 1), at(2026, 4, 30))
        assertTrue(starts.isEmpty())
    }

    @Test
    fun `daily fills every day of the window`() {
        val anchor = at(2026, 3, 2, 9)
        val starts = RecurrenceRule(RecurrenceFrequency.DAILY)
            .occurrenceStarts(anchor, at(2026, 3, 2), at(2026, 3, 8, 23, 59))
        assertEquals(7, starts.size)
        assertEquals(anchor, starts.first())
        // 시각은 첫 회차와 같아야 한다.
        assertTrue(starts.all { it.hour == 9 })
    }

    @Test
    fun `weekday skips saturday and sunday`() {
        // 2026-03-02 는 월요일.
        val anchor = at(2026, 3, 2, 8)
        val starts = RecurrenceRule(RecurrenceFrequency.WEEKDAY)
            .occurrenceStarts(anchor, at(2026, 3, 2), at(2026, 3, 15, 23, 59))
        assertEquals(10, starts.size, "2주면 평일은 10일이다.")
        assertTrue(starts.none { it.dayOfWeek.value >= 6 })
    }

    @Test
    fun `weekly keeps the same weekday`() {
        val anchor = at(2026, 3, 3, 19)
        val starts = RecurrenceRule(RecurrenceFrequency.WEEKLY)
            .occurrenceStarts(anchor, at(2026, 3, 1), at(2026, 3, 31, 23, 59))
        assertEquals(5, starts.size)
        assertTrue(starts.all { it.dayOfWeek == anchor.dayOfWeek })
    }

    @Test
    fun `biweekly steps two weeks`() {
        val anchor = at(2026, 3, 3, 19)
        val starts = RecurrenceRule(RecurrenceFrequency.BIWEEKLY)
            .occurrenceStarts(anchor, at(2026, 3, 1), at(2026, 4, 30, 23, 59))
        assertEquals(listOf(3, 17, 31, 14, 28), starts.map { it.dayOfMonth })
    }

    @Test
    fun `monthly repeats on the same day`() {
        val anchor = at(2026, 1, 15, 7)
        val starts = RecurrenceRule(RecurrenceFrequency.MONTHLY)
            .occurrenceStarts(anchor, at(2026, 1, 1), at(2026, 12, 31, 23, 59))
        assertEquals(12, starts.size)
        assertTrue(starts.all { it.dayOfMonth == 15 })
    }

    /** 2월은 28일까지지만, 다음 달은 다시 31일로 돌아와야 한다. */
    @Test
    fun `monthly from the thirty first falls back inside short months`() {
        val anchor = at(2026, 1, 31, 7)
        val starts = RecurrenceRule(RecurrenceFrequency.MONTHLY)
            .occurrenceStarts(anchor, at(2026, 1, 1), at(2026, 4, 30, 23, 59))
        assertEquals(listOf(31, 28, 31, 30), starts.map { it.dayOfMonth })
    }

    @Test
    fun `yearly repeats on the same date`() {
        val anchor = at(2026, 5, 5, 12)
        val starts = RecurrenceRule(RecurrenceFrequency.YEARLY)
            .occurrenceStarts(anchor, at(2026, 1, 1), at(2029, 12, 31, 23, 59))
        assertEquals(listOf(2026, 2027, 2028, 2029), starts.map { it.year })
    }

    // ------------------------------------------------------------ 종료일

    @Test
    fun `repeat stops at the end date inclusive`() {
        val anchor = at(2026, 3, 2, 9)
        val rule = RecurrenceRule(RecurrenceFrequency.DAILY, LocalDate.of(2026, 3, 5))
        val starts = rule.occurrenceStarts(anchor, at(2026, 3, 1), at(2026, 3, 31))
        assertEquals(4, starts.size, "종료일 당일까지 포함한다.")
        assertEquals(5, starts.last().dayOfMonth)
    }

    @Test
    fun `the end date is dropped when not repeating`() {
        val rule = RecurrenceRule(RecurrenceFrequency.NONE, LocalDate.of(2026, 3, 5))
        assertNull(rule.effectiveEndDate, "반복하지 않는 일정에는 종료일을 남기지 않는다.")
    }

    // ------------------------------------------------------------ 오래된 일정

    /** 2년 전에 시작한 매일 반복도 창 안의 회차를 정확히 만들어야 한다. */
    @Test
    fun `an old anchor still resolves inside a far window`() {
        val anchor = at(2024, 1, 1, 6, 30)
        val starts = RecurrenceRule(RecurrenceFrequency.DAILY)
            .occurrenceStarts(anchor, at(2026, 6, 10), at(2026, 6, 12, 23, 59))
        assertEquals(3, starts.size)
        assertEquals(at(2026, 6, 10, 6, 30), starts.first())
    }

    @Test
    fun `an old anchor does not skip weekly occurrences`() {
        val anchor = at(2024, 1, 2, 20)
        val starts = RecurrenceRule(RecurrenceFrequency.WEEKLY)
            .occurrenceStarts(anchor, at(2026, 6, 1), at(2026, 6, 30, 23, 59))
        assertEquals(5, starts.size, "6월에는 화요일이 5번 있다.")
        assertTrue(starts.all { it.dayOfWeek == anchor.dayOfWeek })
    }

    @Test
    fun `an old anchor does not skip monthly occurrences`() {
        val anchor = at(2020, 3, 9, 7)
        val starts = RecurrenceRule(RecurrenceFrequency.MONTHLY)
            .occurrenceStarts(anchor, at(2026, 6, 1), at(2026, 8, 31, 23, 59))
        assertEquals(listOf(at(2026, 6, 9, 7, 0), at(2026, 7, 9, 7, 0), at(2026, 8, 9, 7, 0)), starts)
    }

    @Test
    fun `the limit stops generation`() {
        val anchor = at(2026, 3, 2, 9)
        val starts = RecurrenceRule(RecurrenceFrequency.DAILY)
            .occurrenceStarts(anchor, at(2026, 3, 1), at(2027, 3, 1), limit = 3)
        assertEquals(3, starts.size)
    }

    // ------------------------------------------------------------ 다음 회차

    @Test
    fun `the next occurrence looks past the original start`() {
        val anchor = at(2026, 3, 2, 9)
        val next = RecurrenceRule(RecurrenceFrequency.WEEKLY)
            .nextOccurrence(anchor, after = at(2026, 3, 10))
        assertEquals(at(2026, 3, 16, 9, 0), next)
    }

    @Test
    fun `a non repeating schedule has no next occurrence in the past`() {
        val anchor = at(2026, 3, 2, 9)
        assertNull(RecurrenceRule.none.nextOccurrence(anchor, after = at(2026, 3, 3)))
    }

    // ------------------------------------------------------------ 설명 문구

    @Test
    fun `the summary mentions the frequency and the end`() {
        val anchor = at(2026, 3, 3, 19)
        val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2026, 5, 1))
        val summary = rule.summary(anchor)
        assertTrue(summary.contains("매주"), summary)
        assertTrue(summary.contains("화요일"), summary)
        assertTrue(summary.contains("5월 1일까지"), summary)
        assertEquals("반복 안 함", RecurrenceRule.none.summary(anchor))
    }

    @Test
    fun `an unknown stored frequency degrades to no repeat`() {
        assertEquals(RecurrenceFrequency.NONE, RecurrenceFrequency.from("고장난값"))
        assertEquals(RecurrenceFrequency.NONE, RecurrenceFrequency.from(null))
        assertEquals(RecurrenceFrequency.WEEKLY, RecurrenceFrequency.from("weekly"))
    }
}
