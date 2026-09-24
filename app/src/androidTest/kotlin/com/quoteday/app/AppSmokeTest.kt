package com.quoteday.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quoteday.app.ui.AppTab
import com.quoteday.app.ui.tabTestTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 앱이 켜지고 다섯 탭이 모두 그려지는지 본다.
 *
 * 이 저장소에서 앱을 **실제로 켜 보는** 유일한 자리다. 컴파일과 단위 테스트가
 * 모두 통과해도 앱은 첫 프레임에서 죽을 수 있다 — 자원을 못 읽거나, 시작할 때
 * 거는 알람이 예외를 던지거나, 화면 하나가 빈 목록에서 넘어지거나.
 */
@RunWith(AndroidJUnit4::class)
class AppSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun 다섯_탭이_모두_열린다() {
        // 탭 바가 그려졌다는 것은 첫 프레임이 나왔다는 뜻이다.
        compose.onNodeWithTag(tabTestTag(AppTab.HOME)).assertIsDisplayed()

        for (tab in AppTab.entries) {
            compose.onNodeWithTag(tabTestTag(tab)).performClick()
            compose.waitForIdle()
            compose.onNodeWithTag(tabTestTag(tab)).assertIsDisplayed()
        }
    }

    /**
     * 설정 화면은 데이터 개수를 글자로 보여 준다. 이 줄이 뜬다는 것은 JSON 이
     * APK 안에서 읽혔다는 뜻이다 — 화면을 통해 확인하는 쪽이 더 믿을 만하다.
     */
    @Test
    fun 설정에_데이터_개수가_뜬다() {
        compose.onNodeWithTag(tabTestTag(AppTab.SETTINGS)).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("명언 201편 · 인물 116명 · 배경 41편").assertIsDisplayed()
    }

    @Test
    fun 챌린지_단계가_늘어선다() {
        compose.onNodeWithTag(tabTestTag(AppTab.CHALLENGE)).performClick()
        compose.waitForIdle()
        for (level in listOf("입문", "보통", "어려움", "매우 어려움", "극한")) {
            compose.onNodeWithText(level).assertIsDisplayed()
        }
    }
}
