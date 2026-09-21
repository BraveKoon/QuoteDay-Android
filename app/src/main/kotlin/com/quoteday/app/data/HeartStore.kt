package com.quoteday.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.quoteday.core.HeartSyncAvailability
import com.quoteday.core.HeartSyncing
import com.quoteday.core.OfflineHeartSync

/**
 * 하트.
 *
 * 화면은 **곧바로** 바뀌고, 서버 반영은 뒤에서 따라온다. 하트는 누른 그 순간
 * 반응해야 하는 종류의 것이라, 왕복을 기다리면 앱이 굼떠 보인다.
 *
 * 지금 끼워진 통로는 [OfflineHeartSync] 다 — 하트는 이 기기 안에만 쌓인다.
 * 서버가 생기면 [sync] 만 바꾸면 되고, 아래 코드는 그대로다.
 */
class HeartStore(
    context: Context,
    private val sync: HeartSyncing = OfflineHeartSync(),
) {
    private val prefs = context.applicationContext
        .getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

    /** 내가 하트를 누른 명언. */
    private val mine: SnapshotStateMap<String, Boolean> = mutableStateMapOf<String, Boolean>().apply {
        prefs.getStringSet(MINE, emptySet()).orEmpty().forEach { put(it, true) }
    }

    /** 마지막으로 받아 온 전체 하트 수. */
    private val counts: SnapshotStateMap<String, Int> = mutableStateMapOf()

    fun isHearted(slug: String): Boolean = mine[slug] == true

    /**
     * 화면에 보여 줄 하트 수.
     *
     * 서버에서 받아 온 값이 있으면 그것을, 없으면 내가 누른 것만 센다. 0 과
     * "아직 모름"을 구분하지 않는다 — 어느 쪽이든 화면에는 하트 하나만 뜬다.
     */
    fun count(slug: String): Int = counts[slug] ?: if (isHearted(slug)) 1 else 0

    /** 하트를 켜거나 끈다. 이미 그 상태면 아무 일도 하지 않는다. */
    fun toggle(slug: String) {
        val next = !isHearted(slug)
        if (next) mine[slug] = true else mine.remove(slug)
        counts[slug] = (counts[slug] ?: 0) + if (next) 1 else -1
        if ((counts[slug] ?: 0) < 0) counts[slug] = 0
        persist()
    }

    suspend fun availability(): HeartSyncAvailability = sync.availability()

    /** 서버에서 최신 하트 수를 받아 온다. 실패하면 화면은 지금 값을 그대로 쓴다. */
    suspend fun refresh(slugs: List<String>) {
        if (slugs.isEmpty()) return
        runCatching { sync.counts(slugs) }.getOrNull()?.forEach { (slug, count) ->
            counts[slug] = count
        }
    }

    private fun persist() {
        prefs.edit().putStringSet(MINE, mine.filterValues { it }.keys.toSet()).apply()
    }

    companion object {
        const val STORE_NAME = "quoteday.hearts"
        private const val MINE = "hearts.mine.v1"
    }
}
