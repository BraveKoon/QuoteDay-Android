package com.quoteday.core

import kotlin.test.Test
import kotlin.test.assertEquals

class NameKeyTest {

    /** 구두점을 지우지 않고 공백으로 바꾸는 이유가 바로 이것이다. */
    @Test
    fun `initials with and without spaces match`() {
        assertEquals(NameKey.normalize("C.S. Lewis"), NameKey.normalize("C. S. Lewis"))
        assertEquals("c s lewis", NameKey.normalize("C.S. Lewis"))
    }

    @Test
    fun `accents and case are ignored`() {
        assertEquals(NameKey.normalize("Antoine de Saint-Exupéry"),
                     NameKey.normalize("ANTOINE DE SAINT EXUPERY"))
        assertEquals("albert camus", NameKey.normalize("  Albert   Camus  "))
    }

    @Test
    fun `korean names survive normalization`() {
        assertEquals("윈스턴 처칠", NameKey.normalize("윈스턴 처칠"))
    }
}
