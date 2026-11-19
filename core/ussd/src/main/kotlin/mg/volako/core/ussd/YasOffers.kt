package mg.volako.core.ussd

// Les familles d'offres comme dans le menu Yas
enum class OfferFamily(val label: String) {
    MORA("M'Ora"),
    FIRST("First"),
    YELOW("Ye'low"),
    NET_WEEK("Net semaine"),
    NET_MONTH("Net mois"),
}

// Une offre Yas.
// selfPath  = choix apres le menu "offre pour moi" (#111*1*1*3*...)
// otherPath = choix apres le numero dans "offre pour un autre" (#111*1*1*5*NUMERO*...)
// Les deux menus ne sont pas ranges pareil. "0" = page suivante.
// airtimeCode = achat avec le credit telephonique (#322*..#), null si on ne le connait pas
data class YasOffer(
    val id: String,
    val family: OfferFamily,
    val name: String,
    val price: Long,
    val content: String,
    val selfPath: List<String>,
    val otherPath: List<String>,
    val airtimeCode: String? = null,
)

// Chemins MVola relus et testes sur le vrai reseau le 19/11/2026 (jusqu'au code secret).
// Codes #322 : feuille Yas + yas.mg (pas testes : ils achetent tout de suite).
// Si Yas change son menu, c'est ici seulement qu'il faut corriger.
object YasOffers {

    private fun p(path: String) = path.split("*")

    val all: List<YasOffer> = listOf(
        YasOffer("mora_500", OfferFamily.MORA, "M'Ora 500", 500, "500 Ar d'appel, 25 Mo, 25 SMS - 1 jour", p("1*1"), p("1*1"), "#322*20#"),
        YasOffer("mora_one", OfferFamily.MORA, "M'Ora One", 1_000, "1 000 Ar d'appel, 40 Mo, 40 SMS - 1 jour", p("1*2"), p("1*2"), "#322*21#"),
        YasOffer("mora_2000", OfferFamily.MORA, "M'Ora+ 2000", 2_000, "2 000 Ar d'appel, 100 Mo, 50 SMS - 2 jours", p("1*3"), p("1*3"), "#322*32#"),
        YasOffer("mora_5000", OfferFamily.MORA, "M'Ora+ 5000", 5_000, "5 000 Ar d'appel, 200 Mo, 100 SMS - 7 jours", p("1*4"), p("1*4"), "#322*35#"),
        YasOffer("mora_inter", OfferFamily.MORA, "M'Ora International", 5_000, "15 min vers l'international - 24 h", p("1*5"), p("1*5"), "#322*52#"),

        YasOffer("first_premium", OfferFamily.FIRST, "First Premium", 10_000, "10 000 Ar d'appel, 350 Mo, 100 SMS - 30 jours", p("2*1"), p("2*1"), "#322*91#"),
        YasOffer("first_premium_plus", OfferFamily.FIRST, "First Premium+", 15_000, "15 000 Ar d'appel, 500 Mo, 150 SMS - 30 jours", p("2*2"), p("2*2"), "#322*92#"),
        YasOffer("first_prestige", OfferFamily.FIRST, "First Prestige", 30_000, "30 000 Ar d'appel, 20 Go, 400 SMS - 30 jours", p("2*3"), p("2*3"), "#322*93#"),
        YasOffer("first_royal", OfferFamily.FIRST, "First Royal", 50_000, "50 000 Ar d'appel, 40 Go, 700 SMS - 30 jours", p("2*0*4"), p("2*4"), "#322*95#"),

        YasOffer("yelow_100", OfferFamily.YELOW, "Ye'low 100", 100, "20 Mo ou 20 SMS - 1 jour", p("3*1"), p("3*1"), "#322*67#"),
        YasOffer("yelow_sms", OfferFamily.YELOW, "Ye'low SMS", 200, "100 SMS Yas + 25 SMS tous reseaux - 24 h", p("3*2"), p("3*2"), "#322*62#"),
        YasOffer("yelow_200", OfferFamily.YELOW, "Ye'low 200", 200, "40 Mo ou 40 SMS - 24 h", p("3*0*6"), p("3*6"), "#322*63#"),
        YasOffer("yelow_500", OfferFamily.YELOW, "Ye'low 500", 500, "130 Mo - 24 h", p("3*3"), p("3*3"), "#322*65#"),
        YasOffer("yelow_up", OfferFamily.YELOW, "Ye'low Up", 500, "Voir le detail dans le menu Yas", p("3*0*9"), p("3*0*9")),
        YasOffer("yelow_1000", OfferFamily.YELOW, "Ye'low 1000", 1_000, "100 SMS ou 50 Mo jour + 50 Mo nuit - 30 jours", p("3*4"), p("3*4"), "#322*66#"),
        YasOffer("yelow_one", OfferFamily.YELOW, "Ye'low One", 1_000, "1 Go - 24 h", p("3*5"), p("3*5"), "#322*64#"),
        YasOffer("yelow_2000", OfferFamily.YELOW, "Ye'low 2000", 2_000, "450 Mo - 48 h", p("3*0*7"), p("3*0*7"), "#322*68#"),
        YasOffer("yelow_2500", OfferFamily.YELOW, "Ye'low 2500", 2_500, "2,4 Go - 24 h", p("3*0*8"), p("3*0*8"), "#322*69#"),

        YasOffer("net_week_3000", OfferFamily.NET_WEEK, "Net Week 3 000", 3_000, "700 Mo - 7 jours", p("0*4*1*1"), p("4*1*1"), "#322*70#"),
        YasOffer("net_week_5000", OfferFamily.NET_WEEK, "Net Week 5 000", 5_000, "1,2 Go - 7 jours", p("0*4*1*2"), p("4*1*2"), "#322*71#"),
        YasOffer("net_week_10000", OfferFamily.NET_WEEK, "Net Week 10 000", 10_000, "3 Go - 7 jours", p("0*4*1*3"), p("4*1*3"), "#322*73#"),

        YasOffer("net_month_15000", OfferFamily.NET_MONTH, "Net Month 15 000", 15_000, "2,5 Go - 30 jours", p("0*4*2*1"), p("4*2*1"), "#322*80#"),
        YasOffer("net_month_25000", OfferFamily.NET_MONTH, "Net Month 25 000", 25_000, "4,5 Go - 30 jours", p("0*4*2*2"), p("4*2*2"), "#322*8#"),
        YasOffer("net_month_30000", OfferFamily.NET_MONTH, "Net Month 30 000", 30_000, "30 Go - 30 jours", p("0*4*2*0*7"), p("4*2*0*7"), "#322*9#"),
        YasOffer("net_month_75000", OfferFamily.NET_MONTH, "Net Month 75 000", 75_000, "75 Go - 30 jours", p("0*4*2*3"), p("4*2*3"), "#322*13#"),
        YasOffer("net_month_125000", OfferFamily.NET_MONTH, "Net Month 125 000", 125_000, "29 Go - 30 jours", p("0*4*2*4"), p("4*2*4"), "#322*17#"),
        YasOffer("net_month_175000", OfferFamily.NET_MONTH, "Net Month 175 000", 175_000, "58 Go - 30 jours", p("0*4*2*0*5"), p("4*2*5"), "#322*19#"),
        YasOffer("net_month_200000", OfferFamily.NET_MONTH, "Net Month 200 000", 200_000, "250 Go - 30 jours", p("0*4*2*0*6"), p("4*2*0*6"), "#322*15#"),
    )

    fun byId(id: String): YasOffer? = all.firstOrNull { it.id == id }

    fun of(family: OfferFamily): List<YasOffer> = all.filter { it.family == family }.sortedBy { it.price }
}
