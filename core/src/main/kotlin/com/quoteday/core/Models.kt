package com.quoteday.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 일정과 명언이 함께 쓰는 단일 카테고리.
 *
 * `rawValue` 는 iOS 와 같아야 한다. 위젯 설정과 저장된 값이 그 문자열을 쓴다.
 */
@Serializable
enum class AppCategory(val rawValue: String) {
    @SerialName("work") WORK("work"),
    @SerialName("leisure") LEISURE("leisure"),
    @SerialName("meal") MEAL("meal"),
    @SerialName("study") STUDY("study"),
    @SerialName("exercise") EXERCISE("exercise"),
    @SerialName("health") HEALTH("health"),
    @SerialName("relationship") RELATIONSHIP("relationship"),
    @SerialName("growth") GROWTH("growth"),
    @SerialName("daily") DAILY("daily"),
    @SerialName("etc") ETC("etc");

    val emoji: String
        get() = when (this) {
            WORK -> "💼"; LEISURE -> "🎮"; MEAL -> "🍽"; STUDY -> "📚"; EXERCISE -> "🏃"
            HEALTH -> "❤️"; RELATIONSHIP -> "👥"; GROWTH -> "💡"; DAILY -> "🏠"; ETC -> "⭐"
        }

    val title: String
        get() = when (this) {
            WORK -> "직장"; LEISURE -> "여가"; MEAL -> "식사"; STUDY -> "학업"; EXERCISE -> "운동"
            HEALTH -> "건강"; RELATIONSHIP -> "인간관계"; GROWTH -> "자기계발"; DAILY -> "일상"; ETC -> "기타"
        }

    val displayName: String get() = "$emoji $title"

    /** 알림 제목에 쓰는 짧은 안내. */
    val notificationLead: String
        get() = when (this) {
            WORK -> "일에 집중할 시간이에요"
            LEISURE -> "즐거운 시간을 보내세요"
            MEAL -> "맛있는 식사 하세요"
            STUDY -> "지금은 공부할 시간이에요"
            EXERCISE -> "몸을 움직일 시간이에요"
            HEALTH -> "나를 돌볼 시간이에요"
            RELATIONSHIP -> "함께할 시간이에요"
            GROWTH -> "한 걸음 더 나아갈 시간이에요"
            DAILY -> "오늘 하루를 챙겨요"
            ETC -> "잠시 숨을 고르세요"
        }

    /**
     * 명언이 모자랄 때 끌어올 이웃 카테고리.
     * 마지막 폴백(전체 풀)은 [QuoteService] 가 처리하므로 여기엔 가까운 것만 둔다.
     */
    val related: List<AppCategory>
        get() = when (this) {
            WORK -> listOf(GROWTH, DAILY)
            LEISURE -> listOf(RELATIONSHIP, DAILY)
            MEAL -> listOf(HEALTH, DAILY)
            STUDY -> listOf(GROWTH, WORK)
            EXERCISE -> listOf(HEALTH, GROWTH)
            HEALTH -> listOf(EXERCISE, DAILY)
            RELATIONSHIP -> listOf(LEISURE, DAILY)
            GROWTH -> listOf(STUDY, WORK)
            DAILY -> listOf(GROWTH, HEALTH)
            ETC -> listOf(DAILY, GROWTH)
        }

    companion object {
        fun from(rawValue: String): AppCategory =
            entries.firstOrNull { it.rawValue == rawValue } ?: ETC

        /** 카테고리 지정 없이 명언만 고를 때의 기본 순서. */
        val selectableForQuotes: List<AppCategory> get() = entries.filter { it != ETC }
    }
}

/** 명언 한 편. */
@Serializable
data class Quote(
    /** 사람이 읽을 수 있는 고유 키. 출시 뒤에는 바꾸지 않는다 — 딥링크의 기준이다. */
    val slug: String,
    val text: String,
    val originalText: String? = null,
    val authorId: String,
    val category: AppCategory,
    val secondaryCategories: List<AppCategory> = emptyList(),
) {
    /** slug 로부터 만든 안정적 식별자. iOS 와 같은 값이 나온다. */
    val id: String get() = StableHash.stableUuid("quote:$slug")

    val categories: List<AppCategory> get() = listOf(category) + secondaryCategories

    fun matches(category: AppCategory): Boolean = category in categories
}

/** 명언을 남긴 인물. */
@Serializable
data class Author(
    val id: String,
    val name: String,
    val koreanName: String? = null,
    val birthYear: Int? = null,
    val deathYear: Int? = null,
    val occupation: String,
    val nationality: String,
    val biography: String,
    val achievements: List<String> = emptyList(),
    val era: String? = null,
    val notableWorks: List<String> = emptyList(),
) {
    /**
     * 화면과 보기에 쓰는 이름. 한국어 표기가 있으면 그쪽이다.
     *
     * iOS 와 같은 규칙을 써야 한다. 챌린지의 "누가 말했을까"는 이 값을 보기로
     * 늘어놓는데, 한쪽은 "윈스턴 처칠", 다른 쪽은 "Winston Churchill" 이면
     * 같은 앱이 플랫폼마다 다른 문제를 내는 꼴이 된다.
     */
    val displayName: String get() = koreanName ?: name

    val lifespan: String?
        get() = when {
            birthYear == null -> null
            deathYear == null -> "$birthYear~"
            else -> "$birthYear–$deathYear"
        }
}

/** 명언 + 인물을 함께 넘기는 표시용 묶음. */
data class QuotePresentation(val quote: Quote, val author: Author) {
    val id: String get() = quote.id

    /** 공유 시트나 알림 본문에 쓰는 한 줄. */
    val shareText: String get() = "“${quote.text}” — ${author.name}"
}

/** 명언이 나온 배경. 출처를 확인한 것만 있다. */
@Serializable
data class BehindStory(
    val quoteSlug: String,
    val occasion: String,
    val context: String,
    val takeaway: String? = null,
    val source: String,
)

/** 외국어 이름의 한국어 표기. */
@Serializable
data class ForeignName(
    val english: String,
    val korean: String,
    val occupation: String? = null,
    val nationality: String? = null,
)
