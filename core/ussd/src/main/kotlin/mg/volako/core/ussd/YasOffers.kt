package mg.volako.core.ussd

// Les familles d'offres comme dans le menu Yas
enum class OfferFamily(val label: String) {
    MORA("M'Ora"),
    FIRST("First"),
    YELOW("Ye'low"),
    NET_WEEK("Net semaine"),
    NET_MONTH("Net mois"),
}

// Une offre achetable via MVola.
// menuPath = les choix a taper apres le menu "Offre" (#111*1*1*3*...), page suivante = "0"
data class YasOffer(
    val id: String,
    val family: OfferFamily,
    val name: String,
    val price: Long,
    val content: String,
    val menuPath: List<String>,
)

// Catalogue relu sur le vrai menu MVola le 19/11/2026.
// Si Yas change son menu, c'est ici seulement qu'il faut corriger.
object YasOffers {

    val all: List<YasOffer> = listOf(
        YasOffer("mora_500", OfferFamily.MORA, "M'Ora 500", 500, "500 Ar d'appel, 25 Mo, 25 SMS - 1 jour", listOf("1", "1")),
        YasOffer("mora_one", OfferFamily.MORA, "M'Ora One", 1_000, "1 000 Ar d'appel, 40 Mo, 40 SMS - 1 jour", listOf("1", "2")),
        YasOffer("mora_2000", OfferFamily.MORA, "M'Ora+ 2000", 2_000, "2 000 Ar d'appel, 100 Mo, 50 SMS - 2 jours", listOf("1", "3")),
        YasOffer("mora_5000", OfferFamily.MORA, "M'Ora+ 5000", 5_000, "5 000 Ar d'appel, 200 Mo, 100 SMS - 7 jours", listOf("1", "4")),
        YasOffer("mora_inter", OfferFamily.MORA, "M'Ora International", 5_000, "15 min vers l'international - 24 h", listOf("1", "5")),

        YasOffer("first_premium", OfferFamily.FIRST, "First Premium", 10_000, "10 000 Ar d'appel, 350 Mo, 100 SMS - 30 jours", listOf("2", "1")),
        YasOffer("first_premium_plus", OfferFamily.FIRST, "First Premium+", 15_000, "15 000 Ar d'appel, 500 Mo, 150 SMS - 30 jours", listOf("2", "2")),
        YasOffer("first_prestige", OfferFamily.FIRST, "First Prestige", 30_000, "30 000 Ar d'appel, 20 Go, 400 SMS - 30 jours", listOf("2", "3")),
        YasOffer("first_royal", OfferFamily.FIRST, "First Royal", 50_000, "50 000 Ar d'appel, 40 Go, 700 SMS - 30 jours", listOf("2", "0", "4")),

        YasOffer("yelow_100", OfferFamily.YELOW, "Ye'low 100", 100, "20 Mo ou 20 SMS - 1 jour", listOf("3", "1")),
        YasOffer("yelow_sms", OfferFamily.YELOW, "Ye'low SMS", 200, "100 SMS Yas + 25 SMS tous reseaux - 24 h", listOf("3", "2")),
        YasOffer("yelow_200", OfferFamily.YELOW, "Ye'low 200", 200, "40 Mo ou 40 SMS - 24 h", listOf("3", "0", "6")),
        YasOffer("yelow_500", OfferFamily.YELOW, "Ye'low 500", 500, "130 Mo - 24 h", listOf("3", "3")),
        YasOffer("yelow_up", OfferFamily.YELOW, "Ye'low Up", 500, "Voir le detail dans le menu Yas", listOf("3", "0", "9")),
        YasOffer("yelow_1000", OfferFamily.YELOW, "Ye'low 1000", 1_000, "100 SMS ou 50 Mo jour + 50 Mo nuit - 30 jours", listOf("3", "4")),
        YasOffer("yelow_one", OfferFamily.YELOW, "Ye'low One", 1_000, "1 Go - 24 h", listOf("3", "5")),
        YasOffer("yelow_2000", OfferFamily.YELOW, "Ye'low 2000", 2_000, "450 Mo - 48 h", listOf("3", "0", "7")),
        YasOffer("yelow_2500", OfferFamily.YELOW, "Ye'low 2500", 2_500, "2,4 Go - 24 h", listOf("3", "0", "8")),

        YasOffer("net_week_3000", OfferFamily.NET_WEEK, "Net Week 3 000", 3_000, "700 Mo - 7 jours", listOf("0", "4", "1", "1")),
        YasOffer("net_week_5000", OfferFamily.NET_WEEK, "Net Week 5 000", 5_000, "1,2 Go - 7 jours", listOf("0", "4", "1", "2")),
        YasOffer("net_week_10000", OfferFamily.NET_WEEK, "Net Week 10 000", 10_000, "3 Go - 7 jours", listOf("0", "4", "1", "3")),

        YasOffer("net_month_15000", OfferFamily.NET_MONTH, "Net Month 15 000", 15_000, "2,5 Go - 30 jours", listOf("0", "4", "2", "1")),
        YasOffer("net_month_25000", OfferFamily.NET_MONTH, "Net Month 25 000", 25_000, "4,5 Go - 30 jours", listOf("0", "4", "2", "2")),
        YasOffer("net_month_75000", OfferFamily.NET_MONTH, "Net Month 75 000", 75_000, "75 Go - 30 jours", listOf("0", "4", "2", "3")),
        YasOffer("net_month_125000", OfferFamily.NET_MONTH, "Net Month 125 000", 125_000, "29 Go - 30 jours", listOf("0", "4", "2", "4")),
    )

    fun byId(id: String): YasOffer? = all.firstOrNull { it.id == id }

    fun of(family: OfferFamily): List<YasOffer> = all.filter { it.family == family }.sortedBy { it.price }
}
