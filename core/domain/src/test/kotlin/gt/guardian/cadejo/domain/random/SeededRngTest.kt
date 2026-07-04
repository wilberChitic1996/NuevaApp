package gt.guardian.cadejo.domain.random

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeededRngTest {

    @Test
    fun `same seed produces identical sequence`() {
        val a = SeededRng(42)
        val b = SeededRng(42)
        repeat(1_000) {
            assertEquals(a.nextLong(), b.nextLong())
        }
    }

    @Test
    fun `different seeds diverge`() {
        val a = SeededRng(1)
        val b = SeededRng(2)
        val seqA = List(50) { a.nextLong() }
        val seqB = List(50) { b.nextLong() }
        assertNotEquals(seqA, seqB)
    }

    @Test
    fun `nextInt stays within bound`() {
        val rng = SeededRng(7)
        repeat(10_000) {
            val v = rng.nextInt(13)
            assertTrue("value $v out of range", v in 0 until 13)
        }
    }

    @Test
    fun `nextIntInRange respects inclusive bounds`() {
        val rng = SeededRng(99)
        repeat(10_000) {
            val v = rng.nextIntInRange(-4, 4)
            assertTrue("value $v out of range", v in -4..4)
        }
    }

    @Test
    fun `state snapshot reproduces the following draws`() {
        val rng = SeededRng(123)
        repeat(10) { rng.nextLong() }
        val snapshot = rng.state
        val expected = List(20) { rng.nextLong() }

        val resumed = SeededRng(snapshot)
        val actual = List(20) { resumed.nextLong() }
        assertEquals(expected, actual)
    }
}
