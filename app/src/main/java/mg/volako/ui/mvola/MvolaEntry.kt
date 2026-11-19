package mg.volako.ui.mvola

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import mg.volako.MainActivity
import mg.volako.R
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.model.Favorite
import mg.volako.core.model.FavoriteKind
import mg.volako.core.ussd.YasOffers

// Ce que l'app doit ouvrir quand on arrive par un raccourci de l'icone
sealed interface MvolaEntry {
    data object Send : MvolaEntry
    data object Withdraw : MvolaEntry
    data object Credit : MvolaEntry
    data class OpenFavorite(val id: Long) : MvolaEntry

    companion object {
        // memes actions que dans res/xml/shortcuts.xml
        const val ACTION_SEND = "mg.volako.action.MVOLA_SEND"
        const val ACTION_WITHDRAW = "mg.volako.action.MVOLA_WITHDRAW"
        const val ACTION_CREDIT = "mg.volako.action.MVOLA_CREDIT"
        const val ACTION_FAVORITE = "mg.volako.action.MVOLA_FAVORITE"
        const val EXTRA_FAVORITE_ID = "favorite_id"

        fun from(intent: Intent?): MvolaEntry? = when (intent?.action) {
            ACTION_SEND -> Send
            ACTION_WITHDRAW -> Withdraw
            ACTION_CREDIT -> Credit
            ACTION_FAVORITE -> intent.getLongExtra(EXTRA_FAVORITE_ID, -1).takeIf { it > 0 }?.let(::OpenFavorite)
            else -> null
        }
    }
}

// Les favoris les plus recents apparaissent aussi dans l'appui long sur l'icone
object FavoriteShortcuts {

    private const val MAX_FAVORITES = 2
    private const val ID_PREFIX = "fav_"

    fun publish(context: Context, favorites: List<Favorite>) {
        val shortcuts = favorites.take(MAX_FAVORITES).mapIndexed { index, fav ->
            ShortcutInfoCompat.Builder(context, ID_PREFIX + fav.id)
                .setShortLabel(fav.shortLabel(context))
                .setLongLabel(fav.longLabel(context))
                .setIcon(IconCompat.createWithResource(context, R.drawable.shortcut_favorite))
                .setRank(index)
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(MvolaEntry.ACTION_FAVORITE)
                        .putExtra(MvolaEntry.EXTRA_FAVORITE_ID, fav.id),
                )
                .build()
        }
        // remplace tous les raccourcis dynamiques : un favori supprime disparait aussi d'ici
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    // court (environ 10 caracteres conseilles par la doc) : la personne ou l'offre
    private fun Favorite.shortLabel(context: Context): String = when (kind) {
        FavoriteKind.OFFER_ME, FavoriteKind.OFFER_AIRTIME -> offerName() ?: context.getString(R.string.fav_section)
        FavoriteKind.CREDIT_ME -> context.getString(R.string.shortcut_credit_short)
        else -> name ?: phone ?: context.getString(R.string.fav_section)
    }

    // long (25 caracteres max) : "Envoyer 50 000 Ar · Elyse"
    private fun Favorite.longLabel(context: Context): String {
        val a = formatAriary(amount ?: 0)
        val action = when (kind) {
            FavoriteKind.SEND -> context.getString(R.string.fav_send, a)
            FavoriteKind.CREDIT_ME, FavoriteKind.CREDIT_OTHER -> context.getString(R.string.fav_credit_me, a)
            FavoriteKind.WITHDRAW_AGENT -> context.getString(R.string.fav_withdraw, a)
            FavoriteKind.OFFER_ME, FavoriteKind.OFFER_AIRTIME -> return offerName() ?: context.getString(R.string.fav_section)
            FavoriteKind.OFFER_OTHER -> offerName() ?: context.getString(R.string.fav_section)
        }
        val who = name ?: phone
        return (if (who != null && kind != FavoriteKind.CREDIT_ME) "$action · $who" else action).take(25)
    }

    private fun Favorite.offerName(): String? = offerId?.let(YasOffers::byId)?.name
}
