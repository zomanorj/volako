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
        // dans chacun des deux menus (pour moi / pour un autre)
        val self = YasOffers.all.map { it.selfPath }
        assertEquals(self.size, self.toSet().size)
        val other = YasOffers.all.map { it.otherPath }
        assertEquals(other.size, other.toSet().size)
        val airtime = YasOffers.all.mapNotNull { it.airtimeCode }
        assertEquals(airtime.size, airtime.toSet().size)
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
