package com.quoteday.app

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quoteday.app.ui.AppTab
import com.quoteday.app.ui.tabTestTag
import com.quoteday.core.AppCategory
import com.quoteday.core.RecurrenceFrequency
import com.quoteday.core.RecurrenceRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * 스토어에 올릴 화면을 찍는다.
 *
 * 테스트라기보다 **찍는 도구**다. 이 저장소에는 안드로이드 기기가 없고 화면을
 * 눈으로 볼 방법도 없어서, 에뮬레이터에서 찍어 내보내는 것이 유일한 길이다.
 * 화면이 깨져 있으면 여기서 찍힌 그림으로 드러난다.
 *
 * 찍은 파일은 앱의 외부 파일 폴더에 쌓이고, 워크플로가 `adb pull` 로 가져간다.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app: QuoteDayApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext as QuoteDayApplication

    @Test
    fun captureStoreScreenshots() {
        seedSchedules()

        capture("01-today")

        compose.onNodeWithTag(tabTestTag(AppTab.SCHEDULE)).performClick()
        capture("02-schedule")

        compose.onNodeWithTag(tabTestTag(AppTab.QUOTES)).performClick()
        capture("03-quotes")

        compose.onNodeWithTag(tabTestTag(AppTab.CHALLENGE)).performClick()
        capture("04-challenge")

        // 문제 화면이 목록보다 앱을 잘 보여 준다. 보통 단계는 제한 시간이 없어서
        // 화면이 가만히 있고, 찍는 동안 시간이 흐르지 않는다.
        compose.onNodeWithText("보통").performClick()
        capture("05-challenge-quiz")

        compose.onNodeWithTag(tabTestTag(AppTab.SETTINGS)).performClick()
        capture("06-settings")
    }

    /**
     * 일정 화면은 비어 있으면 안내 문구만 나온다. 스토어에서 볼 사람에게는
     * 그 화면이 앱을 설명하지 못하므로, 그럴듯한 일정 몇 개를 넣어 둔다.
     */
    private fun seedSchedules() {
        val store = app.schedules
        store.all.map { it.id }.forEach(store::remove)

        val today = LocalDate.now()
        store.add(
            title = "아침 러닝",
            start = today.atTime(7, 0),
            end = today.atTime(7, 40),
            category = AppCategory.EXERCISE,
            recurrence = RecurrenceRule(RecurrenceFrequency.WEEKDAY),
        )
        store.add(
            title = "팀 회의",
            start = today.atTime(10, 30),
            end = today.atTime(11, 30),
            category = AppCategory.WORK,
        )
        store.add(
            title = "저녁 산책",
            start = today.atTime(19, 0),
            end = today.atTime(19, 30),
            category = AppCategory.HEALTH,
            recurrence = RecurrenceRule(RecurrenceFrequency.DAILY),
        )
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        // 화면이 자리를 잡을 틈을 준다. 애니메이션이 끝나기 전에 찍으면
        // 카드가 반쯤 투명한 채로 남는다.
        Thread.sleep(600)

        val bitmap: Bitmap = InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .takeScreenshot()

        val directory = File(app.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        FileOutputStream(File(directory, "$name.png")).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
    }
}
