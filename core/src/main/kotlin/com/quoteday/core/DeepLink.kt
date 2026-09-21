package com.quoteday.core

/**
 * 위젯 탭과 알림 탭에서 앱 안의 화면으로 가는 URL 규약.
 *
 * - `quoteday://quote/<id>` : 명언 상세
 * - `quoteday://schedule/<id>` : 일정 상세
 * - `quoteday://today` : 오늘 탭
 *
 * iOS 와 같은 모양을 쓴다. 한쪽에서 만든 링크가 다른 쪽에서 열리지는 않지만,
 * 규약이 갈라지면 나중에 웹 링크를 붙일 때 두 벌을 만들어야 한다.
 */
sealed interface DeepLink {
    data class Quote(val id: String) : DeepLink
    data class Schedule(val id: String) : DeepLink
    data object Today : DeepLink

    val uri: String
        get() = when (this) {
            is Quote -> "$SCHEME://$QUOTE_HOST/$id"
            is Schedule -> "$SCHEME://$SCHEDULE_HOST/$id"
            Today -> "$SCHEME://$TODAY_HOST"
        }

    companion object {
        const val SCHEME = "quoteday"
        const val QUOTE_HOST = "quote"
        const val SCHEDULE_HOST = "schedule"
        const val TODAY_HOST = "today"

        /**
         * 잘못된 링크가 들어와도 null 만 돌려주고 죽지 않는다.
         *
         * 안드로이드의 `Uri` 를 쓰지 않는 이유는 이 함수가 `:core` 에 있어서다 —
         * 그래야 링크 규약을 안드로이드 없이 테스트할 수 있다.
         */
        fun parse(uri: String): DeepLink? {
            val trimmed = uri.trim()
            val separator = "://"
            val schemeEnd = trimmed.indexOf(separator)
            if (schemeEnd <= 0) return null
            if (!trimmed.substring(0, schemeEnd).equals(SCHEME, ignoreCase = true)) return null

            val rest = trimmed.substring(schemeEnd + separator.length)
            // 쿼리와 프래그먼트는 이 규약에 없다. 붙어 와도 무시한다.
            val path = rest.substringBefore('?').substringBefore('#')
            val parts = path.split('/').filter { it.isNotEmpty() }
            val host = parts.firstOrNull()?.lowercase() ?: return Today
            val value = parts.getOrNull(1)

            return when (host) {
                QUOTE_HOST -> value?.takeIf { it.isNotBlank() }?.let { Quote(it) }
                SCHEDULE_HOST -> value?.takeIf { it.isNotBlank() }?.let { Schedule(it) }
                TODAY_HOST -> Today
                else -> null
            }
        }
    }
}

/** 알림에 실어 보내는 값의 키. 문자열 오타를 막으려고 한곳에 모은다. */
object NotificationPayloadKey {
    const val DEEP_LINK = "deepLink"
    const val QUOTE_ID = "quoteID"
    const val SCHEDULE_ID = "scheduleID"
    const val CATEGORY = "category"
}

/**
 * 알림에 넣을 글.
 *
 * 화면 밖에서 만들어 두는 이유는 알림을 **미리** 걸어 두기 때문이다. 안드로이드는
 * 예약한 시각에 앱을 깨워 주지만, 그때 무엇을 보여 줄지는 지금 정해야 한다.
 */
data class NotificationContent(
    val title: String,
    val subtitle: String?,
    val body: String,
    val deepLink: String,
    val quoteId: String,
    val scheduleId: String?,
    val category: AppCategory?,
) {
    companion object {
        /** 일정 회차에 붙는 알림. */
        fun forOccurrence(
            occurrence: ScheduleOccurrence,
            service: QuoteService = QuoteService(),
            library: QuoteLibrary = QuoteLibrary.shared,
        ): NotificationContent {
            val quote = occurrence.resolvedQuote(service)
            val author = library.author(quote)
            val category = occurrence.category
            return NotificationContent(
                title = "${category.emoji} ${category.notificationLead}",
                subtitle = occurrence.displayTitle,
                body = "“${quote.text}”\n— ${author.displayName}",
                deepLink = DeepLink.Quote(quote.id).uri,
                quoteId = quote.id,
                scheduleId = occurrence.schedule.id,
                category = category,
            )
        }

        /** 매일 정해진 시각에 오는 오늘의 명언 알림. */
        fun forDailyQuote(
            date: java.time.LocalDate,
            preferred: AppCategory?,
            service: QuoteService = QuoteService(),
            library: QuoteLibrary = QuoteLibrary.shared,
        ): NotificationContent {
            val quote = service.quoteOfTheDay(date, preferred)
            val author = library.author(quote)
            return NotificationContent(
                title = "☀️ 오늘의 명언",
                subtitle = null,
                body = "“${quote.text}”\n— ${author.displayName}",
                deepLink = DeepLink.Quote(quote.id).uri,
                quoteId = quote.id,
                scheduleId = null,
                category = preferred,
            )
        }
    }
}
