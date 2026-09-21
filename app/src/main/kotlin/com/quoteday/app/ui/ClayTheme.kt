package com.quoteday.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoteday.core.AppCategory

/**
 * 클레이 테마.
 *
 * iOS 의 `Shared/Design/ClayTheme.swift` 와 **같은 색 값**을 쓴다. 두 앱이 나란히
 * 놓였을 때 같은 앱으로 보여야 한다.
 *
 * 표면은 모두 단색이다. 그라데이션·블러·광택으로 입체감을 만들지 않고, 배경과
 * 카드의 밝기 차이 + 얇은 구분선만으로 층을 나눈다.
 *
 * Material3 의 `MaterialTheme` 도 함께 채운다. 그래야 기본 제공 컴포넌트(스위치,
 * 슬라이더, 리플)가 팔레트 밖의 색을 쓰지 않는다.
 */

/** 카테고리와 강조 요소에 쓰는 파스텔 색. */
object ClayPalette {
    val periwinkleLight = Color(0xFFA9B7FF)
    val periwinkleDark = Color(0xFF6D7CD8)
    val mintLight = Color(0xFF9EE6C9)
    val mintDark = Color(0xFF53A98A)
    val apricotLight = Color(0xFFFFC9A0)
    val apricotDark = Color(0xFFC8875A)
    val lilacLight = Color(0xFFD5B6F5)
    val lilacDark = Color(0xFF9370C4)
    val coralLight = Color(0xFFFFAFA3)
    val coralDark = Color(0xFFC97466)
    val roseLight = Color(0xFFFFAFC9)
    val roseDark = Color(0xFFC97292)
    val skyLight = Color(0xFF9FD8F5)
    val skyDark = Color(0xFF5C9BC0)
    val lemonLight = Color(0xFFFFE08A)
    val lemonDark = Color(0xFFC7A44A)
    val sageLight = Color(0xFFBEDBA5)
    val sageDark = Color(0xFF7CA162)
    val pebbleLight = Color(0xFFCFD3E3)
    val pebbleDark = Color(0xFF7C8194)
}

/** 화면이 참조하는 색 토큰. 뷰에서 색을 하드코딩하지 않는다. */
@Immutable
data class ClayColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val separator: Color,
    val shadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    /** 파스텔 위에 얹는 글자색. */
    val textOnTint: Color,
    /** 강조색 위에 얹는 글자색. */
    val textOnAccent: Color,
    val accent: Color,
    val danger: Color,
    val isDark: Boolean,
) {
    /** 카테고리를 구분하는 파스텔 색. */
    fun tint(category: AppCategory): Color = when (category) {
        AppCategory.WORK -> pick(ClayPalette.periwinkleLight, ClayPalette.periwinkleDark)
        AppCategory.LEISURE -> pick(ClayPalette.mintLight, ClayPalette.mintDark)
        AppCategory.MEAL -> pick(ClayPalette.apricotLight, ClayPalette.apricotDark)
        AppCategory.STUDY -> pick(ClayPalette.lilacLight, ClayPalette.lilacDark)
        AppCategory.EXERCISE -> pick(ClayPalette.coralLight, ClayPalette.coralDark)
        AppCategory.HEALTH -> pick(ClayPalette.roseLight, ClayPalette.roseDark)
        AppCategory.RELATIONSHIP -> pick(ClayPalette.skyLight, ClayPalette.skyDark)
        AppCategory.GROWTH -> pick(ClayPalette.lemonLight, ClayPalette.lemonDark)
        AppCategory.DAILY -> pick(ClayPalette.sageLight, ClayPalette.sageDark)
        AppCategory.ETC -> pick(ClayPalette.pebbleLight, ClayPalette.pebbleDark)
    }

    private fun pick(light: Color, dark: Color): Color = if (isDark) dark else light
}

private val lightClayColors = ClayColors(
    background = Color(0xFFF3F4F8),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF7F8FB),
    surfaceSunken = Color(0xFFEDEFF5),
    separator = Color(0xFFDFE2EC),
    shadow = Color(0x1A2E3350),
    textPrimary = Color(0xFF2E3350),
    textSecondary = Color(0xFF6E7595),
    textOnTint = Color(0xFF2A2F49),
    textOnAccent = Color(0xFFFFFFFF),
    accent = Color(0xFF5A64D8),
    danger = Color(0xFFD65A5A),
    isDark = false,
)

private val darkClayColors = ClayColors(
    background = Color(0xFF121420),
    surface = Color(0xFF1C1F2C),
    surfaceRaised = Color(0xFF252938),
    surfaceSunken = Color(0xFF161822),
    separator = Color(0xFF333849),
    shadow = Color(0x1A000000),
    textPrimary = Color(0xFFF1F3FF),
    textSecondary = Color(0xFFB3B9D6),
    textOnTint = Color(0xFF151827),
    textOnAccent = Color(0xFFFFFFFF),
    accent = Color(0xFF99A2FF),
    danger = Color(0xFFE58686),
    isDark = true,
)

/** 모서리 반경. */
object ClayRadius {
    val card: Dp = 22.dp
    val hero: Dp = 28.dp
    val control: Dp = 16.dp
    val chip: Dp = 12.dp
    val tiny: Dp = 10.dp
}

/** 여백. */
object ClaySpacing {
    val xs: Dp = 6.dp
    val s: Dp = 12.dp
    val m: Dp = 18.dp
    val l: Dp = 26.dp
    val xl: Dp = 36.dp
}

private val LocalClayColors = staticCompositionLocalOf { lightClayColors }

/** 화면에서 색을 꺼내는 입구. `ClayTheme.colors.accent` 처럼 쓴다. */
object ClayTheme {
    val colors: ClayColors
        @Composable @ReadOnlyComposable get() = LocalClayColors.current
}

/**
 * 둥근 서체.
 *
 * iOS 는 `.rounded` 디자인을 쓰지만 안드로이드에는 대응하는 시스템 서체가 없다.
 * 서체 파일을 번들에 넣으면 APK 가 커지고 한글 글리프까지 담아야 해서, 기본
 * 서체를 쓰되 크기와 굵기를 iOS 쪽 단계에 맞춘다.
 */
private val clayTypography = Typography(
    displaySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp,
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp,
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp,
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp,
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp,
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp,
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp,
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp,
    ),
)

@Composable
fun QuoteDayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkClayColors else lightClayColors

    // 다이나믹 컬러(Material You)를 쓰지 않는다. 기기 배경색에 따라 팔레트가
    // 바뀌면 카테고리 색이 서로 구분되지 않고, iOS 와도 달라진다.
    val material = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.textOnAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceRaised,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.separator,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.textOnAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceRaised,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.separator,
            error = colors.danger,
        )
    }

    CompositionLocalProvider(LocalClayColors provides colors) {
        MaterialTheme(colorScheme = material, typography = clayTypography, content = content)
    }
}
