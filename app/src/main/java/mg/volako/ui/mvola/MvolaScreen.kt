package mg.volako.ui.mvola

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mg.volako.R
import mg.volako.core.designsystem.component.IconTile
import mg.volako.core.designsystem.component.SectionHeader
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.designsystem.theme.Sizes
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.domain.RecentContact
import mg.volako.core.ussd.MvolaUssd
import mg.volako.core.ussd.UssdOperation
import mg.volako.core.ussd.UssdRequest
import mg.volako.core.ussd.UssdResult
import mg.volako.core.designsystem.R as DesignR

// les fenetres qu'on peut ouvrir depuis l'onglet
private enum class MvolaSheet { SEND, WITHDRAW, CREDIT, OFFER }

// Onglet MVola : les operations courantes en un appui, sans taper les longs codes #111*...
// L'app prepare le code, l'utilisateur relit, puis tape son code secret dans la fenetre Yas.
@Composable
fun MvolaScreen(
    contentPadding: PaddingValues,
    mvolaBalance: Long,
    entry: MvolaEntry? = null,
    onEntryHandled: () -> Unit = {},
    viewModel: MvolaViewModel = viewModel(factory = MvolaViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launch = rememberUssdLauncher()
    val autofillOn = rememberAutofillEnabled()
    var showAutofillDialog by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf<MvolaSheet?>(null) }
    var openFavoriteId by rememberSaveable { mutableStateOf<Long?>(null) }

    // raccourci de l'icone : on ouvre directement la bonne fenetre (jamais de lancement automatique)
    LaunchedEffect(entry) {
        when (entry) {
            MvolaEntry.Send -> sheet = MvolaSheet.SEND
            MvolaEntry.Withdraw -> sheet = MvolaSheet.WITHDRAW
            MvolaEntry.Credit -> sheet = MvolaSheet.CREDIT
            is MvolaEntry.OpenFavorite -> openFavoriteId = entry.id
            null -> return@LaunchedEffect
        }
        onEntryHandled()
    }

    // recents propres a chaque operation + tout le repertoire du telephone
    val picker = { recents: List<RecentContact> ->
        PeoplePicker(recents, state.contacts, state.contactsAllowed, onContactsAllowed = viewModel::onContactsPermissionChanged)
    }

    val launchAndClose = { request: UssdRequest ->
        sheet = null
        openFavoriteId = null
        launch(request)
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xl)
            .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.mvola_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.mvola_balance, formatAriary(mvolaBalance)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.favorites.isNotEmpty()) {
            SectionHeader(stringResource(R.string.fav_section))
            FavoritesList(state.favorites, onOpen = { openFavoriteId = it.id })
        }

        SectionHeader(stringResource(R.string.mvola_section_money))
        TileRow {
            ActionTile(
                icon = DesignR.drawable.ic_send_money,
                title = stringResource(R.string.mvola_send_short),
                subtitle = stringResource(R.string.mvola_send_sub),
                onClick = { sheet = MvolaSheet.SEND },
                // l'action la plus utilisee : mise en avant
                emphasized = true,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            ActionTile(
                icon = DesignR.drawable.ic_local_atm,
                title = stringResource(R.string.mvola_withdraw),
                subtitle = stringResource(R.string.mvola_withdraw_sub),
                onClick = { sheet = MvolaSheet.WITHDRAW },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        SectionHeader(stringResource(R.string.mvola_section_buy))
        TileRow {
            ActionTile(
                icon = DesignR.drawable.ic_sim_card,
                title = stringResource(R.string.mvola_credit),
                subtitle = stringResource(R.string.mvola_credit_sub),
                onClick = { sheet = MvolaSheet.CREDIT },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            ActionTile(
                icon = DesignR.drawable.ic_signal_cellular_alt,
                title = stringResource(R.string.mvola_offer),
                subtitle = stringResource(R.string.mvola_offer_sub),
                onClick = { sheet = MvolaSheet.OFFER },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        SectionHeader(stringResource(R.string.mvola_section_check))
        ActionRow(
            icon = DesignR.drawable.ic_smartphone,
            title = stringResource(R.string.mvola_credit_balance),
            subtitle = stringResource(R.string.mvola_credit_balance_sub),
            // gratuit et sans argent en jeu : on lance directement, sans resume
            onClick = { (MvolaUssd.build(UssdOperation.CreditBalance) as? UssdResult.Ready)?.let { launch(it.request) } },
        )

        // un reglage, pas une consultation : sa propre section
        SectionHeader(stringResource(R.string.mvola_section_settings))
        // bien visible : on voit tout de suite si le remplissage auto est actif
        ActionRow(
            icon = if (autofillOn) DesignR.drawable.ic_task_alt else DesignR.drawable.ic_autorenew,
            title = stringResource(R.string.autofill_row_title),
            subtitle = stringResource(if (autofillOn) R.string.autofill_row_on else R.string.autofill_row_off),
            onClick = { showAutofillDialog = true },
        )

        Spacer(Modifier.height(Spacing.sm))
        SecretNote(stringResource(R.string.mvola_secret_note))
    }

    when (sheet) {
        MvolaSheet.SEND -> SendSheet(mvolaBalance, picker(state.people), launchAndClose, viewModel::addFavorite, onDismiss = { sheet = null })
        MvolaSheet.WITHDRAW -> WithdrawSheet(mvolaBalance, picker(state.agents), launchAndClose, viewModel::addFavorite, onDismiss = { sheet = null })
        MvolaSheet.CREDIT -> CreditSheet(mvolaBalance, picker(state.creditTargets), launchAndClose, viewModel::addFavorite, onDismiss = { sheet = null })
        MvolaSheet.OFFER -> OfferSheet(mvolaBalance, picker(state.creditTargets), launchAndClose, viewModel::addFavorite, onDismiss = { sheet = null })
        null -> Unit
    }

    if (showAutofillDialog) AutofillDialog(onDismiss = { showAutofillDialog = false })

    // on retrouve le favori par son id (il peut avoir ete supprime entre temps)
    state.favorites.firstOrNull { it.id == openFavoriteId }?.let { fav ->
        FavoriteSheet(
            favorite = fav,
            onLaunch = launchAndClose,
            onDelete = {
                viewModel.deleteFavorite(fav.id)
                openFavoriteId = null
            },
            onDismiss = { openFavoriteId = null },
        )
    }
}

// grosse tuile pour les actions principales (cible bien plus grande que 48 dp)
@Composable
private fun ActionTile(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (emphasized) colors.primaryContainer else colors.surfaceContainerLow
    val onContainer = if (emphasized) colors.onPrimaryContainer else colors.onSurface
    val onContainerVariant = if (emphasized) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = container,
        modifier = modifier.heightIn(min = 128.dp),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconTile(
                icon,
                tint = if (emphasized) colors.onPrimaryContainer else colors.primary,
                container = if (emphasized) colors.surface.copy(alpha = 0.35f) else colors.surfaceContainerHigh,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = onContainer)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainerVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

// deux tuiles cote a cote, toujours de la meme hauteur (IntrinsicSize.Max + fillMaxHeight)
@Composable
private fun TileRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Spacing.md), content = content)
}

// ligne simple pour les consultations
@Composable
private fun ActionRow(@DrawableRes icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .heightIn(min = Sizes.listItemMinHeight)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            IconTile(icon, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                painterResource(DesignR.drawable.ic_chevron_right),
                null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
