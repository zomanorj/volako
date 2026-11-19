package mg.volako.core.domain

import mg.volako.core.model.PhoneContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ContactSearchTest {

    private val at = LocalDateTime.of(2026, 11, 19, 10, 0)
    private val recents = listOf(
        RecentContact("0342045485", "AndriamaharoElyse", 5, at),
        RecentContact("0341541054", "Toky lalaina", 2, at),
    )
    private val contacts = listOf(
        PhoneContact("Élyse", "0342045485"),
        PhoneContact("Maman", "0381112233"),
        PhoneContact("Ando", "0321234567"),  // Orange
        PhoneContact("Bako", "0349998877"),
    )
    private val yasOnly = { p: String -> p.startsWith("034") || p.startsWith("038") }

    @Test
    fun `sans recherche les recents d'abord avec le nom du repertoire`() {
        val r = ContactSearch.search("", recents, contacts, yasOnly)
        assertEquals(listOf("Élyse", "Toky lalaina"), r.suggestions.map { it.name })
        // les contacts : sans les recents deja affiches, sans Orange, par ordre alphabetique
        assertEquals(listOf("Bako", "Maman"), r.contacts.map { it.name })
    }

    @Test
    fun `recherche par nom sans accent ni majuscule`() {
        val r = ContactSearch.search("ELY", recents, contacts, yasOnly)
        assertEquals(listOf("0342045485"), r.suggestions.map { it.phone })
        assertTrue(r.contacts.isEmpty())
        assertEquals(listOf("Maman"), ContactSearch.search("mam", recents, contacts, yasOnly).contacts.map { it.name })
    }

    @Test
    fun `recherche par chiffres, meme partiels ou en +261`() {
        assertEquals(listOf("0349998877"), ContactSearch.search("9998", recents, contacts, yasOnly).contacts.map { it.phone })
        assertEquals(listOf("0342045485"), ContactSearch.search("+261 34 20", recents, contacts, yasOnly).suggestions.map { it.phone })
        assertEquals(listOf("0381112233"), ContactSearch.search("038 11", recents, contacts, yasOnly).contacts.map { it.phone })
    }

    @Test
    fun `un numero recent absent du repertoire garde le nom du SMS`() {
        assertEquals("Toky lalaina", ContactSearch.search("toky", recents, contacts, yasOnly).suggestions.single().name)
    }

    @Test
    fun `limites respectees`() {
        val many = (10..99).map { PhoneContact("P$it", "03400000$it") }
        val r = ContactSearch.search("", emptyList(), many, yasOnly, maxContacts = 20)
        assertEquals(20, r.contacts.size)
    }

    @Test
    fun `un contact sans nom passe a la fin sans faux nom`() {
        val list = listOf(PhoneContact("+261349998877", "0349998877"), PhoneContact("Zoe", "0341112233"))
        val r = ContactSearch.search("", emptyList(), list, yasOnly)
        assertEquals(listOf("Zoe", null), r.contacts.map { it.name })
        assertEquals(false, ContactSearch.namesByPhone(list).containsKey("0349998877"))
    }
}
