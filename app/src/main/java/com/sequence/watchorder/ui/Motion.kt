package com.sequence.watchorder.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * One switch for every timed animation in the app.
 *
 * Snapshot tests render a single frame, so an entrance animation would be captured at its first
 * frame, half faded. [com.sequence.watchorder.ScreenshotTest] turns motion off and the screens
 * render settled. Nothing here loops, so the app never animates while it is idle.
 */
object Motion {
    var enabled: Boolean = true
}

/**
 * Remembers which entrance animations have already played, keyed by a stable name.
 *
 * List items are disposed when they scroll away. Without this, every scroll back up would replay
 * the entrance and the content would flicker. A key that has already played stays settled.
 */
class EnterTracker {
    private val played = mutableSetOf<String>()

    fun claim(key: String): Boolean = played.add(key)
}

/**
 * Press feedback for tappable cards: a small, quick shrink while the finger is down.
 *
 * The same [InteractionSource] is handed to the card itself, so the ripple and this scale react to
 * exactly the same gesture.
 */
@Composable
fun Modifier.pressScale(
    source: InteractionSource,
    pressedScale: Float = 0.966f,
): Modifier {
    if (!Motion.enabled) return this
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 900f),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Fades and lifts a section into place once, when a screen is first shown.
 *
 * [index] staggers neighbours so the eye is led down the page instead of everything arriving at
 * once. The delay is capped so a section further down never feels slow to appear.
 */
@Composable
fun Modifier.enterUp(
    tracker: EnterTracker,
    key: String,
    index: Int = 0,
    lift: Dp = 16.dp,
): Modifier {
    if (!Motion.enabled) return this
    // Claiming inside remember() means a disposed-and-recomposed item does not replay.
    val play = remember(tracker, key) { tracker.claim(key) }
    if (!play) return this

    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 340,
                delayMillis = (index * 55).coerceAtMost(180),
                easing = FastOutSlowInEasing,
            ),
        )
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * lift.toPx()
    }
}

/** One screen section, faded and lifted into place as a unit. */
@Composable
fun EnterSection(
    tracker: EnterTracker,
    key: String,
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth().enterUp(tracker, key, index)) {
        content()
    }
}

/** Which way a section change travels. */
enum class SectionAxis {
    /** Sibling pages, e.g. tabs: the new section arrives from the side. */
    HORIZONTAL,

    /** Content inside one page, e.g. a genre filter: the new section rises into place. */
    VERTICAL,
}

/**
 * Swaps one section for another with a shared-axis move instead of a hard cut.
 *
 * The incoming section slides in the direction of travel and settles from 98% scale, while the
 * outgoing one leaves in the opposite direction. [direction] returns 1 for forward, -1 for back and
 * 0 to cross-fade in place, so tab order, list/grid and detail navigation all read correctly.
 *
 * With [Motion] off the target renders directly, so snapshot tests still capture settled frames.
 */
@Composable
fun <T> SectionSwitcher(
    target: T,
    modifier: Modifier = Modifier,
    axis: SectionAxis = SectionAxis.VERTICAL,
    direction: (initial: T, target: T) -> Int = { _, _ -> 1 },
    content: @Composable (T) -> Unit,
) {
    if (!Motion.enabled) {
        // The modifier still applies: otherwise a settled frame would lose padding or weight.
        Box(modifier) { content(target) }
        return
    }

    val density = LocalDensity.current

    AnimatedContent(
        targetState = target,
        modifier = modifier,
        transitionSpec = {
            val dir = direction(initialState, targetState).coerceIn(-1, 1)
            when (axis) {
                SectionAxis.HORIZONTAL -> {
                    // Exactly the mpvEx tab change (also the Material 3 Expressive motion in
                    // Google's Phone app): a fixed 48dp slide, 250ms in and out, fading in over
                    // the full 250ms and out over the first 125ms. No scale, so sibling pages
                    // read as peers rather than one sinking under the other.
                    val slide = with(density) { 48.dp.roundToPx() }
                    val move = tween<IntOffset>(durationMillis = 250, easing = FastOutSlowInEasing)
                    (
                        slideInHorizontally(move) { slide * dir } +
                            fadeIn(tween(durationMillis = 250, easing = FastOutSlowInEasing))
                        ) togetherWith (
                        slideOutHorizontally(move) { -slide * dir } +
                            fadeOut(tween(durationMillis = 125, easing = FastOutSlowInEasing))
                        )
                }

                SectionAxis.VERTICAL -> {
                    val enterMove = tween<IntOffset>(durationMillis = 360, easing = FastOutSlowInEasing)
                    val exitMove = tween<IntOffset>(durationMillis = 260, easing = FastOutSlowInEasing)
                    (
                        slideInVertically(enterMove) { height -> (height / 16) * dir } +
                            fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing))
                        ) togetherWith (
                        slideOutVertically(exitMove) { height -> (-height / 20) * dir } +
                            fadeOut(tween(durationMillis = 160))
                        )
                }
            }
        },
        label = "section",
    ) { state ->
        // A hair of scale on the way in gives in-page moves depth; sibling pages do not scale.
        val settle by transition.animateFloat(
            transitionSpec = { tween(durationMillis = 360, easing = FastOutSlowInEasing) },
            label = "sectionSettle",
        ) { phase -> if (phase == EnterExitState.Visible) 1f else 0.985f }
        val scale = if (axis == SectionAxis.HORIZONTAL) 1f else settle
        Box(
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        ) {
            content(state)
        }
    }
}
