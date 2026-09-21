package com.quoteday.core

import java.text.Normalizer
import java.util.Locale

/**
 * 인물 이름을 비교하기 위한 정규화.
 *
 * 규칙 두 가지가 중요하다.
 *
 * **1. 구두점은 지우지 않고 공백으로 바꾼다.**
 * 지워 버리면 `C.S. Lewis` 가 `cslewis` 가 되어 `C. S. Lewis`(`c s lewis`)와
 * 달라진다. 공백으로 바꾸고 연속 공백을 접으면 둘 다 `c s lewis` 가 된다.
 *
 * **2. 발음 구별 기호를 뗀 뒤 다시 합친다(NFC).**
 * `NFD` 는 한글 음절도 자모로 쪼갠다 — `윈` 이 `ㅇ+ㅟ+ㄴ` 이 된다. 화면에는
 * 똑같이 보이지만 코드 포인트가 달라서 비교가 조용히 실패한다. Swift 의
 * `folding(.diacriticInsensitive)` 는 한글을 건드리지 않으므로, 여기서도
 * 마지막에 `NFC` 로 되돌려 같은 결과를 만든다.
 */
object NameKey {
    private val combiningMarks = Regex("\\p{Mn}+")

    fun normalize(name: String): String {
        val withoutMarks = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
        val recomposed = Normalizer.normalize(withoutMarks, Normalizer.Form.NFC)
            .lowercase(Locale.ROOT)

        val spaced = recomposed.map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
        return spaced.split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }
}
