package mg.volako.core.domain

import mg.volako.core.model.Account
import mg.volako.core.model.Category
import mg.volako.core.model.Transaction
import mg.volako.core.model.TransactionSource
import mg.volako.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class CsvExportTest {

    private val t = Transaction(
        type = TransactionType.EXPENSE, amount = 81_900, fee = 819, account = Account.MVOLA,
        category = Category.OTHER, label = "Chargeur; cache \"ecran\"", counterpartyName = "Toky",
        counterpartyPhone = "0340000003", occurredAt = LocalDateTime.of(2026, 11, 16, 10, 54),
        source = TransactionSource.SMS, smsReference = "4", smsBalance = 64_913,
    )

    @Test
    fun `entete, BOM et une ligne par operation`() {
        val lines = CsvExport.toCsv(listOf(t)).lines().filter { it.isNotEmpty() }
        assertTrue(lines[0].startsWith("﻿date;compte;type"))
        assertEquals(2, lines.size)
    }

    @Test
    fun `les valeurs avec point-virgule ou guillemets sont protegees`() {
        val row = CsvExport.toCsv(listOf(t)).lines()[1]
        assertEquals(
            "2026-11-16 10:54;MVOLA;EXPENSE;other;\"Chargeur; cache \"\"ecran\"\"\";Toky;0340000003;81900;819;64913;4;SMS",
            row,
        )
    }
}
