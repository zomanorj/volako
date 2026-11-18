package mg.monargent.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mg.monargent.R
import mg.monargent.core.designsystem.component.IconBadge
import mg.monargent.core.designsystem.theme.MonArgentTheme
import mg.monargent.core.designsystem.theme.Spacing
import mg.monargent.ui.SmsPermission
import mg.monargent.core.designsystem.R as DesignR

// Ecran du premier lancement : on explique, on demande la permission SMS, puis on importe
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionResult(granted)
    }

    val askPermission = {
        if (SmsPermission.isGranted(context)) viewModel.startImport() else launcher.launch(SmsPermission.READ)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Spacer(Modifier.weight(1f))
            when (val s = state) {
                OnboardingState.Intro -> Intro(onAllow = askPermission, onLater = viewModel::finish)

                OnboardingState.Denied -> {
                    // si Android ne veut plus afficher la demande, il faut passer par les reglages
                    val blocked = activity != null &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, SmsPermission.READ)
                    Message(DesignR.drawable.ic_lock, MonArgentTheme.finance.leak, R.string.onboarding_denied)
                    if (blocked) {
                        Button(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()),
                            )
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.onboarding_open_settings))
                        }
                    }
                    Button(onClick = askPermission, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_allow))
                    }
                    TextButton(onClick = viewModel::finish, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_later))
                    }
                }

                OnboardingState.Importing -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    CircularProgressIndicator(Modifier.size(32.dp))
                    Text(stringResource(R.string.onboarding_importing), style = MaterialTheme.typography.titleMedium)
                }

                is OnboardingState.Done -> {
                    IconBadge(DesignR.drawable.ic_check_circle, MonArgentTheme.finance.income, size = 64.dp)
                    Text(stringResource(R.string.onboarding_done_title), style = MaterialTheme.typography.headlineSmall)
                    if (s.report.total == 0) {
                        Text(stringResource(R.string.onboarding_done_none), style = MaterialTheme.typography.bodyLarge)
                    } else {
                        Text(stringResource(R.string.onboarding_done_added, s.report.added), style = MaterialTheme.typography.bodyLarge)
                        if (s.report.pending > 0) {
                            Text(
                                stringResource(R.string.onboarding_done_pending, s.report.pending),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MonArgentTheme.finance.leak,
                            )
                        }
                    }
                    Button(onClick = viewModel::finish, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_continue))
                    }
                }

                OnboardingState.Error -> {
                    Message(DesignR.drawable.ic_sync, MaterialTheme.colorScheme.error, R.string.onboarding_error)
                    Button(onClick = viewModel::startImport, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_allow))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Intro(onAllow: () -> Unit, onLater: () -> Unit) {
    IconBadge(DesignR.drawable.ic_sms, MaterialTheme.colorScheme.primary, size = 64.dp)
    Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
    Text(
        stringResource(R.string.onboarding_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(DesignR.drawable.ic_lock), null, Modifier.size(18.dp), tint = MonArgentTheme.finance.income)
        Text(stringResource(R.string.onboarding_privacy), style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.size(Spacing.sm))
    Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onboarding_allow))
    }
    TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onboarding_later))
    }
}

@Composable
private fun Message(icon: Int, color: androidx.compose.ui.graphics.Color, text: Int) {
    IconBadge(icon, color, size = 64.dp)
    Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
}
