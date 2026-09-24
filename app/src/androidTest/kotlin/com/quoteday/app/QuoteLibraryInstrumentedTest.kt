package com.quoteday.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quoteday.core.QuoteLibrary
import com.quoteday.core.QuoteService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * 명언 데이터가 **APK 안에서** 읽히는지 확인한다.
 *
 * `:core` 는 JSON 을 `getResourceAsStream` 으로 읽는다. JVM 에서는 잘 되지만
 * 안드로이드에서는 자원이 APK 안에 다르게 들어가므로, 여기서 처음 확인된다.
 * 이게 실패하면 앱은 첫 화면을 그리기도 전에 죽는다 — 그리고 단위 테스트는
 * 전부 통과한 채로 그렇게 된다.
 *
 * 단언은 `org.junit.Assert` 를 쓴다. `kotlin.test` 는 `:core` 의 단위 테스트에만
 * 들어 있고 여기에는 없다.
 */
@RunWith(AndroidJUnit4::class)
class QuoteLibraryInstrumentedTest {

    @Test
    fun quoteDataLoadsOnDevice() {
        val library = QuoteLibrary.shared
        assertEquals("명언 수가 다릅니다.", 201, library.count)
        assertEquals("인물 수가 다릅니다.", 116, library.authors.size)
        assertEquals("비하인드 수가 다릅니다.", 41, library.behindStories.size)
    }

    @Test
    fun quoteOfTheDayIsChosenOnDevice() {
        val presentation = QuoteService(QuoteLibrary.shared)
            .presentationOfTheDay(LocalDate.of(2026, 9, 21))
        assertEquals("voltaire-perfect-enemy-of-good", presentation.quote.slug)
        assertTrue(presentation.quote.text.isNotBlank())
        assertNotNull(presentation.author.displayName)
    }
}
