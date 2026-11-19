package mg.volako.core.ussd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YasOffersTest {

    @Test
    fun `chaque offre a un id unique`() {
        val ids = YasOffers.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `deux offres ne tombent jamais sur le meme choix du menu`() {
        val paths = YasOffers.all.map { it.menuPath }
        assertEquals(paths.size, paths.toSet().size)
    }

    @Test
    fun `chaque famille a des offres triees par prix`() {
        OfferFamily.entries.forEach { family ->
            val prices = YasOffers.of(family).map { it.price }
            assertTrue("famille vide : $family", prices.isNotEmpty())
            assertEquals(prices.sorted(), prices)
        }
    }
}
