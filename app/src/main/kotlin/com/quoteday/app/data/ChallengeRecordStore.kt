package com.quoteday.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import com.quoteday.core.ChallengeDifficulty
import com.quoteday.core.ChallengeMode
import com.quoteday.core.ChallengeResult
import com.quoteday.core.ChallengeScore
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * 챌린지 기록.
 *
 * 모드·단계마다 **가장 많이 맞힌 수**만 남긴다. 판마다 쌓아 두지 않는 이유는
 * 보여 줄 데가 없기 때문이다 — 화면에 뜨는 것은 최고 기록과 그것을 합친 총점뿐이다.
 *
 * iOS 는 이 기록을 시즌(분기)별로도 나눠 두고 랭킹에 올린다. 안드로이드는
 * 랭킹을 아직 넣지 않았으므로 통산 기록 하나만 둔다. 랭킹을 넣을 때 시즌 기록을
 * 더하면 되고, 통산 기록은 그때도 그대로 쓰인다.
 */
class ChallengeRecordStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

    private val records = mutableStateMapOf<String, Int>().apply {
        val stored = prefs.getString(RECORDS, null) ?: return@apply
        runCatching { json.decodeFromString(recordSerializer, stored) }
            .getOrNull()
            ?.let { putAll(it) }
    }

    /** 이 단계에서 가장 많이 맞힌 문제 수. 한 번도 안 했으면 0. */
    fun best(mode: ChallengeMode, difficulty: ChallengeDifficulty): Int =
        records[key(mode, difficulty)] ?: 0

    fun hasPlayed(mode: ChallengeMode, difficulty: ChallengeDifficulty): Boolean =
        records.containsKey(key(mode, difficulty))

    /** 모든 단계의 최고 기록을 합친 점수. */
    val total: Int get() = ChallengeScore.total { mode, difficulty -> best(mode, difficulty) }

    /**
     * 한 판의 결과를 넣는다.
     *
     * @return 기록을 새로 썼는지가 채워진 결과. 결과 화면이 이 값으로 축하 문구를 정한다.
     */
    fun record(result: ChallengeResult): ChallengeResult {
        val key = key(result.mode, result.difficulty)
        val previous = records[key]
        val isNewRecord = previous == null || result.correctCount > previous
        if (isNewRecord) {
            records[key] = result.correctCount
            persist()
        }
        return result.copy(isNewRecord = isNewRecord)
    }

    private fun persist() {
        prefs.edit().putString(RECORDS, json.encodeToString(recordSerializer, records.toMap())).apply()
    }

    private fun key(mode: ChallengeMode, difficulty: ChallengeDifficulty): String =
        "${mode.rawValue}:${difficulty.level}"

    companion object {
        const val STORE_NAME = "quoteday.challenge"
        private const val RECORDS = "challenge.records.v1"
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * 직렬화기를 손으로 지정한다.
         *
         * `json.encodeToString(맵)` 처럼 부르면 `encodeToString(직렬화기, 값)` 쪽으로
         * 붙어 버려서 "No value passed for parameter 'value'" 로 죽는다. 타입만 보고
         * 알아서 찾아 주는 쪽은 별도 확장 함수라 import 가 필요하고, 그것이 빠졌는지
         * 여부가 컴파일 오류로만 드러난다. 이렇게 적어 두면 헷갈릴 일이 없다.
         */
        private val recordSerializer = MapSerializer(String.serializer(), Int.serializer())
    }
}
