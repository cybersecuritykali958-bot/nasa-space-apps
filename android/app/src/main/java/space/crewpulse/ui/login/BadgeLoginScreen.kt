package space.crewpulse.ui.login

import android.nfc.NfcAdapter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import space.crewpulse.R
import space.crewpulse.data.BadgeRepository
import space.crewpulse.data.CrewMember
import space.crewpulse.data.model.Status
import space.crewpulse.ui.components.SpaceBackground
import space.crewpulse.ui.theme.CrewColors
import space.crewpulse.ui.theme.StatusColors
import space.crewpulse.ui.watch.phone

private sealed interface LoginState {
    data object Waiting : LoginState
    data object Reading : LoginState
    data class Granted(val member: CrewMember) : LoginState
    data object Denied : LoginState
}

/**
 * Sign-in with a base NFC badge, asked on every launch. A known badge greets the person
 * by name and role; an unknown card is refused. Demo buttons stand in for a real badge.
 */
@Composable
fun BadgeLoginScreen(badges: BadgeRepository, onSignedIn: () -> Unit) {
    var state by remember { mutableStateOf<LoginState>(LoginState.Waiting) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val nfcOn = remember { NfcAdapter.getDefaultAdapter(context)?.isEnabled == true }

    LaunchedEffect(Unit) {
        badges.scans.collect { id ->
            if (state != LoginState.Waiting) return@collect
            state = LoginState.Reading
            delay(900)
            val member = badges.signIn(id)
            state = if (member != null) LoginState.Granted(member) else LoginState.Denied
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    LaunchedEffect(state) {
        if (state == LoginState.Denied) {
            delay(2_500)
            state = LoginState.Waiting
        }
    }

    Box(Modifier.fillMaxSize()) {
        SpaceBackground(if (state == LoginState.Denied) Status.RED else Status.GREEN)
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            BadgeTapArt(state, Modifier.size(240.dp))
            AnimatedContent(
                state,
                transitionSpec = { (fadeIn(tween(350)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(200)) },
                label = "login",
            ) { s -> Message(s, nfcOn) }
            Box(Modifier.weight(1f, fill = false))
            when (state) {
                is LoginState.Granted -> Button(onClick = onSignedIn, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.login_continue))
                }
                LoginState.Waiting -> DemoBadges(badges)
                else -> Unit
            }
        }
    }
}

@Composable
private fun Message(state: LoginState, nfcOn: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state) {
            LoginState.Waiting -> {
                Title(stringResource(R.string.login_title))
                Body(stringResource(R.string.login_tap))
                Body(stringResource(if (nfcOn) R.string.login_only_staff else R.string.login_nfc_off))
            }
            LoginState.Reading -> Title(stringResource(R.string.login_reading))
            is LoginState.Granted -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp).padding(end = 6.dp))
                    Title(stringResource(R.string.login_greeting, greeting(), state.member.name))
                }
                Text(roleLabel(state.member.role), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Body(stringResource(R.string.login_badge_id, state.member.badgeId))
            }
            LoginState.Denied -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Block, null, tint = StatusColors.red, modifier = Modifier.size(28.dp).padding(end = 6.dp))
                    Title(stringResource(R.string.login_denied))
                }
                Body(stringResource(R.string.login_denied_hint))
            }
        }
    }
}

@Composable
private fun DemoBadges(badges: BadgeRepository) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { badges.onScan("CP-0001") }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.login_demo_crew)) }
        OutlinedButton(onClick = { badges.onScan("UID:04A1B2C3") }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.login_demo_unknown)) }
        Text(stringResource(R.string.login_demo_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Phone with NFC waves; the badge card glides toward it, then snaps on when read. */
@Composable
private fun BadgeTapArt(state: LoginState, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "nfc")
    val hover by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1_800), RepeatMode.Reverse), label = "hover")
    val wave by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1_400, easing = LinearEasing)), label = "wave")
    val accent = when (state) {
        LoginState.Denied -> StatusColors.red
        else -> CrewColors.teal
    }
    val docked = state != LoginState.Waiting
    Canvas(modifier) {
        val c = center
        val phoneH = size.minDimension * 0.62f
        phone(c + Offset(-size.minDimension * 0.12f, 0f), phoneH, accent)
        // NFC waves from the phone toward the badge.
        for (i in 0 until 3) {
            val f = (wave + i / 3f) % 1f
            val r = size.minDimension * (0.12f + 0.22f * f)
            drawArc(
                accent.copy(alpha = 0.6f * (1f - f)), -40f, 80f, useCenter = false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                style = Stroke(4f, cap = StrokeCap.Round),
            )
        }
        // Badge card.
        val gap = if (docked) 0f else size.minDimension * (0.06f + 0.08f * hover)
        val cardW = size.minDimension * 0.38f
        val cardH = cardW * 0.63f
        val tl = Offset(c.x + size.minDimension * 0.02f + gap, c.y - cardH / 2)
        drawRoundRect(accent.copy(alpha = 0.18f), tl, Size(cardW, cardH), CornerRadius(14f))
        drawRoundRect(accent, tl, Size(cardW, cardH), CornerRadius(14f), style = Stroke(3f))
        drawCircle(accent.copy(alpha = 0.7f), cardH * 0.18f, tl + Offset(cardW * 0.25f, cardH * 0.45f))
        drawLine(accent.copy(alpha = 0.7f), tl + Offset(cardW * 0.5f, cardH * 0.38f), tl + Offset(cardW * 0.85f, cardH * 0.38f), 5f, StrokeCap.Round)
        drawLine(accent.copy(alpha = 0.4f), tl + Offset(cardW * 0.5f, cardH * 0.58f), tl + Offset(cardW * 0.75f, cardH * 0.58f), 5f, StrokeCap.Round)
        if (state is LoginState.Granted) {
            drawCircle(accent.copy(alpha = 0.35f * (1f - wave)), size.minDimension * (0.3f + 0.2f * wave), c, style = Stroke(4f))
        }
    }
}

@Composable
internal fun roleLabel(role: String): String = stringResource(
    when (role) {
        "commander" -> R.string.role_commander
        "cmo" -> R.string.role_cmo
        "engineer" -> R.string.role_engineer
        else -> R.string.role_staff
    },
)

@Composable
private fun greeting(): String = stringResource(
    when (java.time.LocalTime.now().hour) {
        in 5..11 -> R.string.greeting_morning
        in 12..17 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    },
)

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}
