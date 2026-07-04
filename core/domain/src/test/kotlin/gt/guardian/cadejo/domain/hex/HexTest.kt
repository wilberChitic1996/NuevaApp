package gt.guardian.cadejo.domain.hex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HexTest {
    @Test
    fun `cube coordinate always sums to zero`() {
        for (q in -5..5) {
            for (r in -5..5) {
                val h = Hex(q, r)
                assertEquals(0, h.q + h.r + h.s)
            }
        }
    }

    @Test
    fun `packing round-trips q and r including negatives`() {
        val h = Hex(-7, 12)
        assertEquals(-7, h.q)
        assertEquals(12, h.r)
    }

    @Test
    fun `every hex has exactly six distinct neighbors at distance one`() {
        val h = Hex(2, -1)
        val neighbors = h.neighbors()
        assertEquals(6, neighbors.size)
        assertEquals(6, neighbors.toSet().size)
        assertTrue(neighbors.all { it.distanceTo(h) == 1 })
    }

    @Test
    fun `distance is symmetric and zero to self`() {
        val a = Hex(0, 0)
        val b = Hex(3, -2)
        assertEquals(0, a.distanceTo(a))
        assertEquals(a.distanceTo(b), b.distanceTo(a))
        assertEquals(3, a.distanceTo(b))
    }

    @Test
    fun `disc of radius n contains 3n squared plus 3n plus 1 hexes`() {
        // Known closed form for a filled hex of radius n.
        for (n in 0..5) {
            val expected = 3 * n * n + 3 * n + 1
            assertEquals(expected, Hex.disc(Hex.ORIGIN, n).size)
        }
    }

    @Test
    fun `plus and minus are inverses`() {
        val a = Hex(4, -3)
        val delta = Hex(-1, 2)
        assertEquals(a, (a + delta) - delta)
    }
}
