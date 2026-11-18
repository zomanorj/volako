package mg.volako.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Verifie sur un vrai telephone que les donnees de la v1 survivent au passage en v2
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VolakoDatabase::class.java,
    )

    @Test
    fun migration_1_vers_2_garde_les_operations() {
        // base v1 avec une operation, comme chez l'utilisateur
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO transactions (type, amount, fee, account, to_account, category, label,
                    counterparty_name, counterparty_phone, occurred_at, source, sms_reference, sms_balance)
                VALUES ('EXPENSE', 500, 0, 'MVOLA', NULL, 'credit', 'Credit YAS', NULL, NULL, 0, 'SMS', '123', 388)
                """.trimIndent(),
            )
        }

        // migre en v2 et verifie le schema (Room compare avec schemas/2.json)
        val db = helper.runMigrationsAndValidate(dbName, 2, true)

        db.query("SELECT amount, sms_reference FROM transactions").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals(500, c.getInt(0))
            assertEquals("123", c.getString(1))
        }
        // la nouvelle table existe et marche
        db.execSQL("INSERT INTO counterparty_labels (phone, label) VALUES ('0340000000', 'Telephone')")
        db.query("SELECT COUNT(*) FROM counterparty_labels").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
    }

    @Test
    fun migration_jusqu_a_3_garde_tout_et_ajoute_read_by() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO transactions (type, amount, fee, account, to_account, category, label,
                    counterparty_name, counterparty_phone, occurred_at, source, sms_reference, sms_balance)
                VALUES ('EXPENSE', 500, 0, 'MVOLA', NULL, 'credit', 'Credit YAS', NULL, NULL, 0, 'SMS', '123', 388)
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(dbName, 2, true).close()
        val db = helper.runMigrationsAndValidate(dbName, 3, true)

        // l'ancienne operation est la, read_by vide (elle date d'avant)
        db.query("SELECT amount, read_by FROM transactions").use { c ->
            c.moveToFirst()
            assertEquals(500, c.getInt(0))
            assertEquals(true, c.isNull(1))
        }
        db.execSQL(
            "INSERT INTO sms_templates (words, number_count, amount_slot, kind, created_at) " +
                "VALUES ('achat#offre', 3, 0, 'CREDIT_PURCHASE', 0)",
        )
    }
}
