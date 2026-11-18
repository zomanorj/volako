package mg.volako.core.domain

import mg.volako.core.model.Transaction
import java.time.format.DateTimeFormatter

// Export de toutes les operations en CSV, lisible dans Excel / Google Sheets.
// Separateur ";" et BOM UTF-8 : sinon Excel en francais melange les colonnes et les accents.
object CsvExport {

    private val HEADER = listOf(
        "date", "compte", "type", "categorie", "libelle", "personne", "telephone",
        "montant", "frais", "solde_mvola_sms", "reference", "source",
    )

    private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun toCsv(transactions: List<Transaction>): String = buildString {
        append('﻿')
        appendLine(HEADER.joinToString(";"))
        transactions.sortedWith(Ledger.chronological).forEach { t ->
            appendLine(
                listOf(
                    t.occurredAt.format(DATE),
                    t.account.name,
                    t.type.name,
                    t.category.key,
                    t.label,
                    t.counterpartyName,
                    t.counterpartyPhone,
                    t.amount.toString(),
                    t.fee.toString(),
                    t.smsBalance?.toString(),
                    t.smsReference,
                    t.source.name,
                ).joinToString(";") { escape(it.orEmpty()) },
            )
        }
    }

    // regle CSV : si la valeur contient ; " ou un retour a la ligne, on l'entoure de guillemets
    private fun escape(value: String): String =
        if (value.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
