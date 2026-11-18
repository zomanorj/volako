package mg.monargent.core.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mg.monargent.core.database.MonArgentDatabase
import mg.monargent.core.model.SmsReadMethod
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

// De bout en bout sur une vraie base Room (en memoire, rien n'est garde) :
// un format inconnu -> l'utilisateur le classe une fois -> les suivants sont lus seuls
@RunWith(AndroidJUnit4::class)
class LearningFlowTest {

    private lateinit var db: MonArgentDatabase
    private lateinit var repo: OfflineTransactionRepository
    private val t0 = LocalDateTime.of(2026, 11, 20, 8, 0)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, MonArgentDatabase::class.java)
            .build()
        repo = OfflineTransactionRepository(db.transactionDao(), db.pendingSmsDao(), db.counterpartyLabelDao(), db.smsTemplateDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun classer_une_fois_puis_lecture_automatique() = runBlocking {
        // un SMS connu pour avoir un solde de depart
        repo.recordSms("Achat de credit YAS reussi: 500 Ar. Bonus: 20 pourcent. Solde MVola : 10 000 Ar. Ref: 1", t0)

        // nouveau format que personne ne comprend ("Disponible" n'est pas un mot de solde connu)
        val a = "Operation 3 000 Ar OK pour 0340000007. Disponible 7 000 Ar. Code 7777777"
        val b = "Operation 1 500 Ar OK pour 0340000008. Disponible 5 500 Ar. Code 7777778"
        assertEquals(SmsRecordResult.PENDING, repo.recordSms(a, t0.plusMinutes(1)).result)
        assertEquals(SmsRecordResult.PENDING, repo.recordSms(b, t0.plusMinutes(2)).result)

        // l'utilisateur classe le premier
        val first = repo.observePendingSms().first().minBy { it.receivedAt }
        repo.classifyPendingSms(first.id, ClassifyChoice.EXPENSE, 3_000, "Paiement")

        // le deuxieme s'est range tout seul, et "A verifier" est vide
        assertEquals(0, repo.observePendingSms().first().size)
        val ops = repo.observeTransactions().first()
        assertEquals(3, ops.size)
        val learned = ops.filter { it.readBy == SmsReadMethod.TEMPLATE }
        assertEquals(2, learned.size)
        assertEquals(listOf(1_500L, 3_000L), learned.map { it.amount }.sorted())
        // le solde MVola suit les SMS
        assertEquals(5_500L, ops.maxBy { it.occurredAt }.smsBalance)

        // et un 3e SMS de la meme forme arrive en direct : lu tout de suite
        val c = "Operation 500 Ar OK pour 0340000009. Disponible 5 000 Ar. Code 7777779"
        assertEquals(SmsRecordResult.ADDED, repo.recordSms(c, t0.plusMinutes(3)).result)
    }

    @Test
    fun un_modele_ne_valide_pas_un_solde_faux() = runBlocking {
        repo.recordSms("Achat de credit YAS reussi: 500 Ar. Bonus: 20 pourcent. Solde MVola : 10 000 Ar. Ref: 1", t0)
        repo.recordSms("Operation 3 000 Ar OK pour 0340000007. Disponible 7 000 Ar. Code 7777777", t0.plusMinutes(1))
        val first = repo.observePendingSms().first().single()
        repo.classifyPendingSms(first.id, ClassifyChoice.EXPENSE, 3_000, "Paiement")

        // meme forme mais le solde ne colle pas (7 000 - 1 500 != 9 999) -> reste a verifier
        val wrong = "Operation 1 500 Ar OK pour 0340000008. Disponible 9 999 Ar. Code 7777778"
        assertEquals(SmsRecordResult.PENDING, repo.recordSms(wrong, t0.plusMinutes(2)).result)
    }
}
