package com.quoteday.core

/**
 * 실행할 때마다 값이 바뀌지 않는 결정적 해시.
 *
 * `String.hashCode()` 를 쓰면 안 된다. JVM 마다, 실행마다 같다는 보장이 없고
 * 무엇보다 **iOS 와 다른 값이 나온다.** 같은 날 같은 명언이 두 플랫폼에서
 * 나와야 하므로, iOS 의 `StableHash` 와 비트 단위로 같은 계산을 한다.
 *
 * Kotlin 의 `Long` 은 부호가 있지만 덧셈·곱셈·XOR 은 2의 보수에서 부호 없는 연산과
 * 결과가 같다. 나눗셈과 비교만 다르므로 그 두 곳에서만 부호 없는 연산을 쓴다.
 */
object StableHash {

    /** FNV-1a 64bit. */
    fun fnv1a(string: String): Long {
        var hash = -0x340d631b7bdddcdbL          // 0xcbf29ce484222325
        for (byte in string.toByteArray(Charsets.UTF_8)) {
            hash = hash xor (byte.toLong() and 0xFF)
            hash *= 0x100000001B3L
        }
        return hash
    }

    /** splitmix64 — 낮은 비트까지 고르게 섞어 준다. */
    fun mix(seed: Long): Long {
        var z = seed + -0x61c8864680b583ebL      // 0x9E3779B97F4A7C15
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L  // 0xBF58476D1CE4E5B9
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L  // 0x94D049BB133111EB
        return z xor (z ushr 31)
    }

    /** 문자열 seed 로부터 `0 until count` 범위의 결정적 인덱스를 만든다. */
    fun index(seed: String, count: Int): Int {
        if (count <= 0) return 0
        // 부호 없는 나머지여야 iOS 와 같은 값이 나온다.
        return java.lang.Long.remainderUnsigned(mix(fnv1a(seed)), count.toLong()).toInt()
    }

    /**
     * 같은 seed 에 대해 항상 같은 UUID 문자열.
     *
     * 알림과 위젯 딥링크가 이 값을 담고 다니므로 앱을 다시 켜도 같아야 한다.
     * iOS 의 `UUID(stableSeed:)` 와 같은 바이트를 만든다.
     */
    fun stableUuid(seed: String): String {
        val bytes = ByteArray(16)
        var state = fnv1a(seed)
        for (chunk in 0 until 2) {
            state = mix(state + chunk.toLong())
            for (byte in 0 until 8) {
                bytes[chunk * 8 + byte] = ((state ushr (byte * 8)) and 0xFF).toByte()
            }
        }
        // RFC 4122 version 4 / variant 10xx.
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

        val hex = bytes.joinToString("") { "%02x".format(it) }
        return buildString {
            append(hex, 0, 8); append('-')
            append(hex, 8, 12); append('-')
            append(hex, 12, 16); append('-')
            append(hex, 16, 20); append('-')
            append(hex, 20, 32)
        }.uppercase()
    }
}
