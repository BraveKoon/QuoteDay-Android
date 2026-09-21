package com.quoteday.core

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeepLinkTest {

    @Test
    fun `a link survives a round trip`() {
        val quote = DeepLink.Quote("ABC-123")
        assertEquals(quote, DeepLink.parse(quote.uri))
        val schedule = DeepLink.Schedule("s-1")
        assertEquals(schedule, DeepLink.parse(schedule.uri))
        assertEquals(DeepLink.Today, DeepLink.parse(DeepLink.Today.uri))
    }

    @Test
    fun `a broken link returns null instead of crashing`() {
        assertNull(DeepLink.parse(""))
        assertNull(DeepLink.parse("quoteday"))
        assertNull(DeepLink.parse("https://example.com/quote/1"))
        assertNull(DeepLink.parse("quoteday://unknown/1"))
        assertNull(DeepLink.parse("quoteday://quote/"))
    }

    @Test
    fun `the scheme is matched without case`() {
        assertEquals(DeepLink.Quote("x"), DeepLink.parse("QuoteDay://QUOTE/x"))
    }

    @Test
    fun `a query string is ignored`() {
        assertEquals(DeepLink.Quote("x"), DeepLink.parse("quoteday://quote/x?from=widget"))
    }
}

class NotificationContentTest {

    private val library = QuoteLibrary.shared
    private val service = QuoteService(library)

    @Test
    fun `an occurrence notification carries the schedule and the quote`() {
        val start = LocalDateTime.of(2026, 3, 2, 9, 0)
        val schedule = Schedule(
            id = "s-1",
            title = "아침 러닝",
            start = start,
            end = start.plusMinutes(30),
            category = AppCategory.EXERCISE,
            recurrence = RecurrenceRule(RecurrenceFrequency.DAILY),
        )
        val occurrence = schedule.occurrence(LocalDate.of(2026, 3, 5))!!
        val content = NotificationContent.forOccurrence(occurrence, service, library)

        assertEquals("아침 러닝", content.subtitle)
        assertTrue(content.title.contains(AppCategory.EXERCISE.notificationLead))
        assertEquals("s-1", content.scheduleId)
        assertEquals(DeepLink.Quote(content.quoteId), DeepLink.parse(content.deepLink))
        assertTrue(content.body.contains("—"), content.body)
    }

    /** 알림 본문의 명언은 화면에 뜨는 오늘의 명언과 같아야 한다. */
    @Test
    fun `the daily notification matches the quote on screen`() {
        val date = LocalDate.of(2026, 9, 21)
        val content = NotificationContent.forDailyQuote(date, preferred = null, service, library)
        val onScreen = service.quoteOfTheDay(date)
        assertEquals(onScreen.id, content.quoteId)
        assertTrue(content.body.contains(onScreen.text))
        assertNull(content.scheduleId)
    }
}
