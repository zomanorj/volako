package mg.volako.core.ussd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MgNumberTest {

    @Test
    fun `accepte les formats courants`() {
        assertEquals("0342045485", MgNumber.parse("0342045485")?.digits)
        assertEquals("0342045485", MgNumber.parse("034 20 454 85")?.digits)
        assertEquals("0342045485", MgNumber.parse("(034) 20-454-85")?.digits)
        assertEquals("0342045485", MgNumber.parse("+261 34 20 454 85")?.digits)
        assertEquals("0381234567", MgNumber.parse("038 12 345 67")?.digits)
    }

    @Test
    fun `refuse les autres operateurs et les mauvaises longueurs`() {
        assertNull(MgNumber.parse("0321234567")) // Orange
        assertNull(MgNumber.parse("0331234567")) // Airtel
        assertNull(MgNumber.parse("034204548"))
        assertNull(MgNumber.parse(""))
    }

    @Test
    fun `autres operateurs seulement avec parseAny`() {
        assertEquals(Operator.ORANGE, MgNumber.parseAny("032 12 345 67")!!.operator)
        assertEquals(Operator.ORANGE, MgNumber.parseAny("0371234567")!!.operator)
        assertEquals(Operator.AIRTEL, MgNumber.parseAny("+261 33 12 345 67")!!.operator)
        assertEquals(Operator.YAS, MgNumber.parseAny("0342045485")!!.operator)
        assertNull(MgNumber.parseAny("0391234567"))
        assertNull(MgNumber.parse("0321234567"))
    }

    @Test
    fun `affichage comme Yas`() {
        assertEquals("034 20 454 85", MgNumber.parse("0342045485")!!.pretty())
    }

    @Test
    fun `mise en forme pendant la saisie`() {
        assertEquals("", MgNumber.formatPartial(""))
        assertEquals("03", MgNumber.formatPartial("03"))
        assertEquals("034 2", MgNumber.formatPartial("0342"))
        assertEquals("034 20 454", MgNumber.formatPartial("03420454"))
        assertEquals("034 20 454 85", MgNumber.formatPartial("034204548599"))
    }
}
