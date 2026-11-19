package mg.volako.core.ussd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YasNumberTest {

    @Test
    fun `accepte les formats courants`() {
        assertEquals("0342045485", YasNumber.parse("0342045485")?.digits)
        assertEquals("0342045485", YasNumber.parse("034 20 454 85")?.digits)
        assertEquals("0342045485", YasNumber.parse("(034) 20-454-85")?.digits)
        assertEquals("0342045485", YasNumber.parse("+261 34 20 454 85")?.digits)
        assertEquals("0381234567", YasNumber.parse("038 12 345 67")?.digits)
    }

    @Test
    fun `refuse les autres operateurs et les mauvaises longueurs`() {
        assertNull(YasNumber.parse("0321234567")) // Orange
        assertNull(YasNumber.parse("0331234567")) // Airtel
        assertNull(YasNumber.parse("034204548"))
        assertNull(YasNumber.parse(""))
    }

    @Test
    fun `affichage comme Yas`() {
        assertEquals("034 20 454 85", YasNumber.parse("0342045485")!!.pretty())
    }

    @Test
    fun `mise en forme pendant la saisie`() {
        assertEquals("", YasNumber.formatPartial(""))
        assertEquals("03", YasNumber.formatPartial("03"))
        assertEquals("034 2", YasNumber.formatPartial("0342"))
        assertEquals("034 20 454", YasNumber.formatPartial("03420454"))
        assertEquals("034 20 454 85", YasNumber.formatPartial("034204548599"))
    }

    @Test
    fun `debut qui ne peut pas etre Yas`() {
        assertEquals(false, YasNumber.cannotBeYas("0"))
        assertEquals(false, YasNumber.cannotBeYas("03"))
        assertEquals(false, YasNumber.cannotBeYas("038"))
        assertEquals(true, YasNumber.cannotBeYas("032"))
        assertEquals(true, YasNumber.cannotBeYas("4"))
    }
}
