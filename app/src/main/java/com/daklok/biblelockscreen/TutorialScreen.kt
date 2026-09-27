package com.daklok.biblelockscreen



import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.daklok.biblelockscreen.strings.AppStrings
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Motion vocabulary — Material 3 Expressive emphasized curves
// ─────────────────────────────────────────────────────────────────────────────

/** M3 emphasized decelerate: fast start, long gentle tail. Entrances. */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** M3 emphasized (standard) accelerate+decelerate. Moves between points. */
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Smooth sine loop for breathing effects (no velocity kick at the ends). */
private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

/** Classic fast-out for press-ins and bursts. */
private val EaseOutCubic = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)

private fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun lerpRect(a: Rect, b: Rect, t: Float): Rect = Rect(
    left = lerpF(a.left, b.left, t),
    top = lerpF(a.top, b.top, t),
    right = lerpF(a.right, b.right, t),
    bottom = lerpF(a.bottom, b.bottom, t)
)

// ─────────────────────────────────────────────────────────────────────────────
// 1. Target registry
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Global spotlight-target registry shared by the whole app (all screens are
 * in this package, so no DI is needed).
 *
 * - [_bounds] maps a target key to its current on-screen (root) rect, kept
 *   fresh by [tutorialTarget] as layouts move.
 * - [revealRequest] is written by the walkthrough engine on every step
 *   change: it names the target key that should be brought into view.
 *   Scrollable containers that own below-the-fold targets observe it and
 *   scroll accordingly (see MainActivity's reveal LaunchedEffect and the
 *   matching one in WallpaperScreen).
 */
object TutorialTargets {
    internal val _bounds: SnapshotStateMap<String, Rect> = mutableStateMapOf()
    private val _requesters = mutableMapOf<String, BringIntoViewRequester>()

    var revealRequest by mutableStateOf<String?>(null)
        private set

    fun requestReveal(key: String?) {
        if (revealRequest != key) revealRequest = key
    }

    fun rectOf(key: String?): Rect? = key?.let { _bounds[it] }

    /** The [BringIntoViewRequester] attached to [key] by [tutorialTarget],
     *  created on first use so it exists even before the target itself has
     *  composed for the first time. */
    fun requesterFor(key: String): BringIntoViewRequester =
        _requesters.getOrPut(key) { BringIntoViewRequester() }

    /**
     * Scrolls the target registered under [key] into view, using Compose's
     * own bring-into-view coordination with whichever scrollable ancestor
     * owns it — this replaces an earlier approach that manually computed a
     * pixel offset from two [Rect]s read at a single point in time, which
     * was prone to reading a stale/absent rect right after a modal (like
     * the full-screen editor) closed and silently doing nothing. A no-op if
     * [key] is null or was never attached to a composable.
     */
    suspend fun reveal(key: String?) {
        key?.let { _requesters[it] }?.bringIntoView()
    }
}

/**
 * Marks a composable as a walkthrough spotlight target. Attach it to the
 * element a tutorial step points at, e.g.
 *
 *     Button(
 *         modifier = Modifier.fillMaxWidth().tutorialTarget("tutorial_generate"),
 *         ...
 *     )
 *
 * Registers/unregisters automatically as the element enters/leaves the
 * layout, and keeps the rect up to date while it moves (scrolls, page
 * transitions, ...).
 */
fun Modifier.tutorialTarget(key: String): Modifier =
    this
        .bringIntoViewRequester(TutorialTargets.requesterFor(key))
        .onGloballyPositioned { coords ->
            val bounds = coords.boundsInRoot()
            if (bounds.isEmpty || !bounds.isFinite) {
                TutorialTargets._bounds.remove(key)
            } else {
                TutorialTargets._bounds[key] = bounds
            }
        }

// ─────────────────────────────────────────────────────────────────────────────
// 2. Action bus
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Shared between MainActivity's real UI (which reports interactions) and
 * the walkthrough overlay (which consumes them). When the user performs the
 * exact action the current step is waiting for, the tour advances on the
 * spot — that's what makes this a "takes you through the app" tutorial
 * rather than a slideshow.
 */
class InteractiveTutorialController {
    var lastAction by mutableStateOf<Pair<String, Long>?>(null)
        private set

    fun notifyAction(actionKey: String) {
        lastAction = actionKey to System.currentTimeMillis()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Step model
// ─────────────────────────────────────────────────────────────────────────────

/** Pointer animation the overlay draws over the spotlight hole. */
enum class TutorialGesture { NONE, TAP, SWIPE_LEFT, SCROLL }

/**
 * One coach-mark step.
 *
 * @param key           stable id, e.g. "generate"
 * @param title         card title (localized)
 * @param message       card body (localized)
 * @param targetKeys    which registered targets to spotlight; several keys
 *                      are unioned into one hole (e.g. pager + dots). Empty
 *                      list → centered card (welcome / finish).
 * @param waitForAction when set, this is a TASK step: the hole is fully
 *                      click-through, the Next button is replaced by a
 *                      "waiting" indicator, and the step advances only when
 *                      this action key is reported through
 *                      [InteractiveTutorialController.notifyAction]
 * @param gesture       the finger-pointer animation over the hole
 * @param condition     steps whose condition evaluates false are skipped
 *                      (evaluated live, so the tour adapts if e.g. a photo
 *                      gets picked mid-tour)
 */
class TutorialStep(
    val key: String,
    val title: String,
    val message: String,
    val targetKeys: List<String> = emptyList(),
    val waitForAction: String? = null,
    val gesture: TutorialGesture = TutorialGesture.NONE,
    val condition: (() -> Boolean)? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// 4. The overlay engine
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InteractiveWalkthroughTutorial(
    strings: AppStrings,
    controller: InteractiveTutorialController,
    steps: List<TutorialStep>,
    stepKeyState: MutableState<String?>,
    onFinished: (completed: Boolean) -> Unit
) {
    if (steps.isEmpty()) {
        LaunchedEffect(Unit) { onFinished(true) }
        return
    }

    val haptic = LocalHapticFeedback.current

    // ── Navigation state ─────────────────────────────────────────────────
    // The current step is tracked BY KEY (not by index) and lives in
    // stepKeyState — owned by MainScreen, OUTSIDE this overlay — so the
    // tour position survives the overlay unmounting while the user
    // finishes a task inside the fullscreen editor or the settings sheet.
    // The set of visible steps changes as conditions flip (photo picked
    // mid-tour etc.), so an index would go stale; a key never does.
    val currentKey = stepKeyState.value ?: steps.first().key
    val step = steps.firstOrNull { it.key == currentKey } ?: steps.first()

    fun nextKey(from: String): String? {
        val idx = steps.indexOfFirst { it.key == from }
        for (i in idx + 1 until steps.size) {
            if (steps[i].condition?.invoke() != false) return steps[i].key
        }
        return null
    }

    fun prevKey(from: String): String? {
        val idx = steps.indexOfLast { it.key == from }
        for (i in idx - 1 downTo 0) {
            if (steps[i].condition?.invoke() != false) return steps[i].key
        }
        return null
    }

    fun advance() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val n = nextKey(currentKey)
        if (n == null) onFinished(true) else stepKeyState.value = n
    }

    fun goBack() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val p = prevKey(currentKey)
        if (p == null) onFinished(false) else stepKeyState.value = p
    }

    // Auto-skip a step whose condition turned false while it was on screen.
    val stepActive = step.condition?.invoke() != false
    LaunchedEffect(step.key, stepActive) {
        if (!stepActive) advance()
    }

    // Back = one step back (or dismiss from the first step).
    BackHandler { goBack() }

    // Ask the app to scroll the target into view and restart the entrance
    // animations whenever the step changes. The three animations run
    // concurrently: the spotlight hole glides to the new target with a
    // spring, the card glides to its new position with emphasized easing,
    // and the card content fades/scales in.
    val enter = remember { Animatable(1f) }   // card content entrance
    val holeT = remember { Animatable(1f) }   // spotlight glide 0→1
    val cardT = remember { Animatable(1f) }   // card position glide 0→1
    var glideFromHole by remember { mutableStateOf<Rect?>(null) }
    var glideFromCardX by remember { mutableStateOf(Float.NaN) }
    var glideFromCardY by remember { mutableStateOf(Float.NaN) }
    // Last positions actually shown on screen — the next step's glide
    // origins. Updated after every recomposition via SideEffect.
    var lastShownHole by remember { mutableStateOf<Rect?>(null) }
    var lastCardX by remember { mutableStateOf(Float.NaN) }
    var lastCardY by remember { mutableStateOf(Float.NaN) }

    LaunchedEffect(step.key) {
        TutorialTargets.requestReveal(step.targetKeys.firstOrNull())
        coroutineScope {
            launch {
                glideFromHole = lastShownHole
                holeT.snapTo(0f)
                holeT.animateTo(
                    1f,
                    animationSpec = spring(
                        dampingRatio = 0.9f,   // a hint of expressive bounce
                        stiffness = 380f
                    )
                )
            }
            launch {
                glideFromCardX = lastCardX
                glideFromCardY = lastCardY
                cardT.snapTo(0f)
                cardT.animateTo(1f, animationSpec = tween(430, easing = EmphasizedEasing))
            }
            launch {
                enter.snapTo(0f)
                enter.animateTo(1f, animationSpec = tween(380, easing = EmphasizedDecelerate))
            }
        }
    }

    // Advance when the user performs the real action this step demonstrates.
    // This effect re-runs when the overlay remounts (editor / settings sheet
    // closed mid-task) — the pending action is then processed and the tour
    // moves on, so tasks that open a modal still complete correctly.
    val waitForAction = step.waitForAction
    LaunchedEffect(controller.lastAction) {
        val action = controller.lastAction ?: return@LaunchedEffect
        if (waitForAction != null && waitForAction == action.first) {
            advance()
        }
    }

    // The overlay's origin in root coordinates. Spotlight rects are
    // tracked in root space (boundsInRoot) while the input guard below is
    // laid out in the overlay's own space; this closes the gap. It is
    // (0,0) today, but deriving it keeps the math correct regardless of
    // where the overlay is mounted.
    val overlayPosInRoot = remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                overlayPosInRoot.value = coords.positionInRoot()
            }
    ) {
        val density = LocalDensity.current
        val W = constraints.maxWidth.toFloat()
        val H = constraints.maxHeight.toFloat()

        fun Dp.px(): Float = with(density) { this@px.toPx() }
        val statusTopPx = WindowInsets.statusBars.asPaddingValues()
            .calculateTopPadding().px()
        val holeCornerRadius = 26.dp.px()

        // ── Spotlight hole (live rect) ────────────────────────────────────
        // Union of the step's target rects, inflated a little and clamped
        // to the screen.
        val holeState = remember(step.targetKeys, W, H) {
            derivedStateOf {
                val rects = step.targetKeys.mapNotNull { TutorialTargets.rectOf(it) }
                if (rects.isEmpty()) {
                    null
                } else {
                    var union = rects.first()
                    rects.drop(1).forEach { union = unionHull(union, it) }
                    val clamped = union.inflate(10f).intersect(Rect(0f, 0f, W, H))
                    if (clamped.isEmpty) null else clamped
                }
            }
        }
        val liveHole = holeState.value
        val bigHole = liveHole != null && liveHole.height > H * 0.5f

        // ── Spotlight glide ───────────────────────────────────────────────
        // displayed hole = lerp(previous displayed hole → live hole, holeT).
        // While gliding, the live rect keeps moving (reveal scrolls etc.) —
        // the lerp chases it, and once settled (t = 1) the displayed rect
        // follows the live rect exactly. Centered steps glide the hole to a
        // zero-size rect at the screen center (it collapses away).
        val collapsedHole = Rect(Offset(W / 2f, H / 2f), Size.Zero)
        val holeGlideTarget = liveHole ?: collapsedHole
        val shownHole: Rect? = when {
            liveHole == null && glideFromHole == null -> null
            else -> {
                // No previous hole (first target after a centered card)?
                // Bloom out from the screen center instead of popping in.
                val from = glideFromHole ?: collapsedHole
                val t = holeT.value
                if (t >= 1f) holeGlideTarget else lerpRect(from, holeGlideTarget, t)
            }
        }

        // Tooltip card height, reported by the card itself (card placement).
        var cardHeightPx by remember { mutableStateOf(0f) }

        // ── Layer 2: dimmed scrim with the hole punched out ───────────────
        val scrimColor = MaterialTheme.colorScheme.scrim
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(color = scrimColor.copy(alpha = 0.82f))
            val h = shownHole
            if (h != null && h.width > 1f && h.height > 1f) {
                val radius = minOf(holeCornerRadius, h.width / 2f, h.height / 2f)
                drawRoundRect(
                    color = Color.Black, // anything — Clear erases it
                    topLeft = h.topLeft,
                    size = h.size,
                    cornerRadius = CornerRadius(radius),
                    blendMode = BlendMode.Clear
                )
            }
        }

        // ── Layer 3: breathing spotlight ring ────────────────────────────
        // Slow, sine-based breathing — alive but never noisy. Fades out
        // for centered (welcome / finish) steps.
        val ringPulse by rememberInfiniteTransition(label = "ringPulse")
            .animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1700, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ringPulse"
            )
        val ringAlpha by animateFloatAsState(
            targetValue = if (liveHole != null) 1f else 0f,
            animationSpec = tween(350, easing = EmphasizedDecelerate),
            label = "ringAlpha"
        )
        val ringColor = MaterialTheme.colorScheme.primary
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = ringAlpha }
        ) {
            val h = shownHole ?: return@Canvas
            if (h.width <= 1f || h.height <= 1f) return@Canvas
            val radius = minOf(holeCornerRadius, h.width / 2f, h.height / 2f)
            drawRoundRect(
                color = ringColor.copy(alpha = 0.62f + 0.33f * ringPulse),
                topLeft = h.topLeft,
                size = h.size,
                cornerRadius = CornerRadius(radius),
                style = Stroke(width = (2f + 1.2f * ringPulse).dp.px())
            )
            // Soft glow that expands outward and fades — gives the ring a
            // "breathing" expressive feel without being noisy.
            val grow = (5f + 9f * ringPulse).dp.px()
            drawRoundRect(
                color = ringColor.copy(alpha = 0.15f * (1f - ringPulse)),
                topLeft = Offset(h.left - grow, h.top - grow),
                size = Size(h.width + grow * 2f, h.height + grow * 2f),
                cornerRadius = CornerRadius(radius + grow),
                style = Stroke(width = (1.2f + 2.2f * ringPulse).dp.px())
            )
        }

        // ── Layer 3.5: input guard (geometry-based) ─────────────────────
        // Compose hit-tests a Box's children topmost-first and stops at the
        // first one that is hit, and pointerInput never shares hits with
        // siblings — a full-screen input layer would therefore swallow
        // EVERY touch on screen, no matter what its handlers consume. (That
        // is exactly why the previous "smart blocker" never let anything
        // through.) This layer works WITH that rule instead of fighting it:
        // invisible guard boxes cover the dimmed area AROUND the spotlight
        // hole, and NOTHING covers the hole itself — a touch inside it hits
        // no overlay node at all, so the hit test falls straight through to
        // the real UI underneath. The boxes track the animated (displayed)
        // hole, so what the user sees as the open area is always exactly
        // the area they can touch.
        val touchMinPx = with(density) { 48.dp.roundToPx() }
        val holeLocal: Rect? = shownHole?.let { sh ->
            val origin = overlayPosInRoot.value
            Rect(
                left = sh.left - origin.x,
                top = sh.top - origin.y,
                right = sh.right - origin.x,
                bottom = sh.bottom - origin.y
            )
        }
        HoleInputGuard(
            passThrough = liveHole != null,
            hole = holeLocal,
            minSizePx = touchMinPx
        )

        // ── Layer 4: animated finger pointer ─────────────────────────────
        if (waitForAction != null && liveHole != null &&
            step.gesture != TutorialGesture.NONE
        ) {
            // The pointer rides the gliding hole, so it lands on the
            // target together with the spotlight.
            val anchor = shownHole ?: liveHole
            FingerPointer(hole = anchor, gesture = step.gesture)
        }

        // ── Layer 5: the tooltip / centered card ─────────────────────────
        // Position:
        //  • centered steps (no targets) → Alignment.Center
        //  • big holes (whole pager / page) → card docks at the bottom
        //  • otherwise → below the hole, or above it when there's no room
        // The card GLIDES from its previous step's position with
        // emphasized easing while the new content fades in.
        //
        // NOTE: this used to number steps within the CONDITION-FILTERED
        // subset (e.g. skipping the verse-settings steps entirely while no
        // photo is set), which made the total jump around — "step 5 of 13"
        // once a photo was picked, "step 3 of 11" before that. Numbering
        // against the full, fixed step list instead keeps the total steady
        // (always 15 here) no matter which steps end up auto-skipped.
        val totalSteps = steps.size
        val stepNumber = (steps.indexOfFirst { it.key == currentKey }.coerceAtLeast(0) + 1)
            .coerceAtMost(totalSteps)
        val isWelcome = currentKey == steps.first().key
        val isDone = currentKey == steps.last().key
        val taskPending = waitForAction != null

        val cardWpx = 340.dp.px().coerceAtMost(W - 28.dp.px())
        val cardW = with(density) { cardWpx.toDp() }
        val gap = 16.dp.px()
        val belowFits = liveHole == null ||
                (liveHole.bottom + gap + cardHeightPx) < (H - 20.dp.px())
        val targetCardY: Float = when {
            liveHole == null -> 0f // centered — positioned via Alignment.Center
            // Big holes used to always dock the card at the BOTTOM of the
            // screen. For a TASK step (scroll/swipe) that's exactly where
            // the user's thumb needs to move — and since the card swallows
            // its own touches (inputSink), it silently ate the gesture
            // before it ever reached the real, scrollable/swipeable UI
            // underneath. That's what made the scroll/swipe steps feel
            // like they barely let you do anything. Docking the card at
            // the TOP instead keeps it out of the gesture's way entirely.
            bigHole && taskPending -> (statusTopPx + 12.dp.px())
            bigHole -> (H - cardHeightPx - 26.dp.px())
                .coerceAtLeast(statusTopPx)
            belowFits -> liveHole.bottom + gap
            else -> (liveHole.top - gap - cardHeightPx)
                .coerceAtLeast(statusTopPx)
        }
        val targetCardX = (W - cardWpx) / 2f

        val cardGlideValid = !glideFromCardX.isNaN() && !glideFromCardY.isNaN() &&
                liveHole != null
        val displayedCardX = if (cardGlideValid && cardT.value < 1f)
            lerpF(glideFromCardX, targetCardX, cardT.value) else targetCardX
        val displayedCardY = if (cardGlideValid && cardT.value < 1f)
            lerpF(glideFromCardY, targetCardY, cardT.value) else targetCardY
        // Remember the displayed hole + card position for the next step's
        // glide origins (runs after every recomposition, so animation
        // frames are captured too).
        SideEffect {
            lastShownHole = shownHole
            if (liveHole != null && cardHeightPx > 0f) {
                lastCardX = displayedCardX
                lastCardY = displayedCardY
            }
        }

        if (liveHole == null) {
            // Centered card (welcome / finish) — floats in the middle with
            // breathing blobs / confetti artwork behind it.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) FinishBurst() else WelcomeBlob()
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .width(340.dp)
                        // Swallow gestures that land on the card itself so
                        // they can't fall through to the real UI underneath
                        // (e.g. swiping the pager below the card). The card's
                        // own buttons are dispatched FIRST at the Main pass
                        // (children before parents), so they still work.
                        .inputSink()
                        .graphicsLayer {
                            alpha = enter.value
                            val s = 0.94f + 0.06f * enter.value
                            scaleX = s
                            scaleY = s
                            translationY = (1f - enter.value) * 24.dp.toPx()
                        }
                        .onGloballyPositioned { coords ->
                            cardHeightPx = coords.size.height.toFloat()
                        }
                ) {
                    CenteredCard(
                        title = step.title,
                        message = step.message,
                        stepLabel = if (totalSteps > 1)
                            String.format(
                                strings.tutorialStepOf, stepNumber, totalSteps
                            )
                        else null,
                        ctaLabel = if (isWelcome) strings.tutorialStartTour
                        else strings.tutorialGetStarted,
                        onCta = { advance() },
                        onSkip = if (isWelcome) ({ onFinished(false) }) else null,
                        skipLabel = strings.tutorialSkip
                    )
                }
            }
        } else {
            val cardBelowHole = !bigHole && belowFits
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            displayedCardX.roundToInt(),
                            displayedCardY.roundToInt()
                        )
                    }
                    .width(cardW)
                    // Swallow gestures that land on the card itself so they
                    // can't fall through to the pager underneath (the card's
                    // own buttons are dispatched first at the Main pass and
                    // keep working).
                    .inputSink()
                    .graphicsLayer {
                        alpha = enter.value
                        val s = 0.94f + 0.06f * enter.value
                        scaleX = s
                        scaleY = s
                        // Slide in from the direction of the hole.
                        translationY = (1f - enter.value) * 16.dp.toPx() *
                                (if (cardBelowHole) -1f else 1f)
                    }
                    .onGloballyPositioned { coords ->
                        cardHeightPx = coords.size.height.toFloat()
                    }
            ) {
                TooltipCard(
                    title = step.title,
                    message = step.message,
                    stepLabel = String.format(
                        strings.tutorialStepOf, stepNumber, totalSteps
                    ),
                    actionHint = when {
                        !taskPending -> null
                        step.gesture == TutorialGesture.SWIPE_LEFT ->
                            strings.tutorialSwipeHint
                        step.gesture == TutorialGesture.SCROLL ->
                            strings.tutorialScrollHint
                        else -> strings.tutorialTapHint
                    },
                    hintGesture = step.gesture,
                    taskPending = taskPending,
                    nextLabel = when {
                        taskPending -> null
                        isDone -> strings.tutorialGetStarted
                        else -> strings.tutorialNext
                    },
                    onNext = if (taskPending) null else ({ advance() }),
                    skipLabel = strings.tutorialSkip,
                    onSkip = { onFinished(false) }
                )
            }
        }

    }
}

/** Smallest rect covering both inputs (Rect has no union operator). */
private fun unionHull(a: Rect, b: Rect): Rect = Rect(
    left = minOf(a.left, b.left),
    top = minOf(a.top, b.top),
    right = maxOf(a.right, b.right),
    bottom = maxOf(a.bottom, b.bottom)
)

/** Where degenerate (unused) guard boxes are parked: far off-screen, so
 *  their minimum-touch-target inflation can never reach the screen. */
private const val GuardParkedFarAway = -1_000_000

/**
 * Consumes every pointer event that lands on it, at the Main pass.
 *
 * Used by the walkthrough's input guard boxes (they sit above the dimmed UI
 * and eat stray touches on it) and by the tutorial cards (a card swallows
 * gestures that land on itself so they can't fall through to the pager
 * underneath; the card's own buttons are its children and are dispatched
 * before the card at the Main pass, so they keep working).
 */
private fun Modifier.inputSink(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
        }
    }
}

/**
 * The walkthrough's input guard — the piece that makes the spotlight hole
 * genuinely interactive. Five invisible [inputSink] boxes are laid out
 * around the spotlight hole:
 *
 *  • passThrough = true (a spotlight hole exists): four boxes cover the
 *    screen except the hole. The hole itself is covered by NOTHING — a
 *    touch there hits no overlay node at all, so the hit test falls
 *    through to the real UI, and taps, swipes and scrolls behave exactly
 *    as they would without the tutorial.
 *  • passThrough = false (centered welcome/finish cards, or a target that
 *    is momentarily missing): one box covers everything. The overlay's
 *    card is composed AFTER this guard, so it is hit-tested first and its
 *    own buttons stay clickable.
 *
 * A guard box is only emitted when it is at least [minSizePx] (the system
 * minimum touch target, 48dp) in BOTH dimensions — anything smaller would
 * get its touch bounds INFLATED by up to half of that on every side
 * (NodeCoordinator.distanceInMinimumTouchTarget) and could swallow touches
 * just inside the hole, so a sliver that narrow is left unblocked instead.
 * Degenerate boxes are parked far off-screen for the same reason: a 0×0
 * box parked on-screen would inflate around its own corner.
 */
@Composable
private fun BoxScope.HoleInputGuard(
    passThrough: Boolean,
    hole: Rect?,
    minSizePx: Int
) {
    Layout(
        content = {
            Box(Modifier.inputSink())
            Box(Modifier.inputSink())
            Box(Modifier.inputSink())
            Box(Modifier.inputSink())
            Box(Modifier.inputSink())
        },
        modifier = Modifier.matchParentSize()
    ) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight

        data class Band(val x: Int, val y: Int, val bw: Int, val bh: Int)
        fun band(x: Int, y: Int, bw: Int, bh: Int): Band =
            if (bw >= minSizePx && bh >= minSizePx) Band(x, y, bw, bh)
            else Band(GuardParkedFarAway, GuardParkedFarAway, 0, 0)
        val parked = Band(GuardParkedFarAway, GuardParkedFarAway, 0, 0)

        val bands: List<Band> = if (!passThrough || hole == null) {
            // Full block — the overlay's own card floats above this.
            listOf(band(0, 0, w, h)) + List(4) { parked }
        } else {
            val holeLeft = hole.left.roundToInt().coerceIn(0, w)
            val holeTop = hole.top.roundToInt().coerceIn(0, h)
            val holeRight = hole.right.roundToInt().coerceIn(0, w)
            val holeBottom = hole.bottom.roundToInt().coerceIn(0, h)
            listOf(
                band(0, 0, w, holeTop),                              // above
                band(0, holeBottom, w, h - holeBottom),              // below
                band(0, holeTop, holeLeft, holeBottom - holeTop),    // left
                band(holeRight, holeTop, w - holeRight, holeBottom - holeTop), // right
                parked
            )
        }
        val placeables = bands.mapIndexed { i, b ->
            measurables[i].measure(Constraints.fixed(b.bw, b.bh))
        }
        layout(w, h) {
            bands.forEachIndexed { i, b ->
                placeables[i].place(b.x, b.y)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Cards
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Coach-mark tooltip for spotlighted steps. Task steps (taskPending) have no
 * Next button — only the user performing the highlighted action moves the
 * tour forward — so the button row shows a quiet "waiting" spinner instead.
 */
@Composable
private fun TooltipCard(
    title: String,
    message: String,
    stepLabel: String,
    actionHint: String?,
    hintGesture: TutorialGesture,
    taskPending: Boolean,
    nextLabel: String?,
    onNext: (() -> Unit)?,
    skipLabel: String,
    onSkip: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        StepChip(stepLabel)
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionHint != null) {
            Spacer(Modifier.height(12.dp))
            ActionHintRow(text = actionHint, gesture = hintGesture)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSkip) { Text(skipLabel) }
            Spacer(Modifier.width(8.dp))
            if (taskPending) {
                WaitingIndicator()
            } else if (nextLabel != null && onNext != null) {
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(nextLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** "The tour is waiting for you" chip shown in place of Next on task steps. */
@Composable
private fun WaitingIndicator() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                trackColor = Color.Transparent
            )
        }
    }
}

/** Tap / swipe / scroll pill + hint text, with a nudging direction icon. */
@Composable
private fun ActionHintRow(text: String, gesture: TutorialGesture) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val nudge by rememberInfiniteTransition(label = "hintNudge")
            .animateFloat(
                initialValue = 0f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "hintNudge"
            )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(28.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (gesture) {
                    TutorialGesture.SWIPE_LEFT -> Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { translationX = -nudge }
                    )
                    TutorialGesture.SCROLL -> Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { translationY = nudge }
                    )
                    else -> Icon(
                        imageVector = Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Big centered card for the welcome and finish steps. */
@Composable
private fun CenteredCard(
    title: String,
    message: String,
    stepLabel: String?,
    ctaLabel: String,
    onCta: () -> Unit,
    onSkip: (() -> Unit)?,
    skipLabel: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (stepLabel != null) {
            StepChip(stepLabel)
            Spacer(Modifier.height(12.dp))
        }
        // Sparkle badge — the finish variant draws its own artwork behind
        // the card (see FinishBurst), so only welcome uses this one.
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (onSkip != null) {
                TextButton(onClick = onSkip) { Text(skipLabel) }
                Spacer(Modifier.width(8.dp))
            }
            Button(
                onClick = onCta,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text(ctaLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StepChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Finger pointer
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The "look, do THIS" pointer: a finger icon in a colored circle that
 * presses toward the hole's center (TAP), glides left across it
 * (SWIPE_LEFT) or glides upward through it (SCROLL — scrolling down means
 * the finger moves up), with a ripple or a fading motion trail. Looping,
 * so the user's eye is continuously pulled to the right place.
 *
 * The icon is centered in its circular badge via contentAlignment on the
 * fixed-size badge Box itself (a wrap-content child Box would ignore its
 * own contentAlignment and park the icon top-start).
 */
@Composable
private fun FingerPointer(hole: Rect, gesture: TutorialGesture) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val density = LocalDensity.current
    fun Dp.px(): Float = with(density) { this@px.toPx() }

    val cycle by rememberInfiniteTransition(label = "finger")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                // Tap runs a touch slower so the press reads clearly.
                animation = tween(
                    if (gesture == TutorialGesture.TAP) 1350 else 1500,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "cycle"
        )

    when (gesture) {
        TutorialGesture.TAP -> {
            // Keyframed press → hold → spring-back release → ripple + rest.
            // Value AND velocity are continuous at every segment boundary,
            // which is what keeps the loop from feeling mechanical.
            val pressInEnd = 0.24f
            val holdEnd = 0.46f
            val releaseEnd = 0.68f
            val press: Float = when {
                cycle < pressInEnd ->
                    EaseOutCubic.transform(cycle / pressInEnd)
                cycle < holdEnd -> 1f
                cycle < releaseEnd -> {
                    val u = (cycle - holdEnd) / (releaseEnd - holdEnd)
                    1f - EaseOutCubic.transform(u)
                }
                else -> 0f
            }
            // Tiny scale pop as the finger springs back up.
            val pop = if (cycle in holdEnd..releaseEnd) {
                val u = (cycle - holdEnd) / (releaseEnd - holdEnd)
                (sin(PI * u).toFloat()) * 0.05f
            } else 0f
            val badgeScale = 1f - 0.13f * press + pop
            val cx = hole.center.x
            val cy = hole.center.y + 6.dp.px() * press

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Soft halo under the badge — contracts while pressed.
                drawCircle(
                    color = primary.copy(alpha = 0.10f + 0.10f * (1f - press)),
                    radius = 36.dp.toPx() * (1f - 0.25f * press),
                    center = Offset(cx, cy)
                )
                // Ripple fires on release and expands out as it fades.
                if (cycle >= releaseEnd) {
                    val ru = ((cycle - releaseEnd) / (1f - releaseEnd))
                        .coerceIn(0f, 1f)
                    drawCircle(
                        color = primary.copy(alpha = 0.42f * (1f - ru)),
                        radius = (12.dp.toPx()) + 26.dp.toPx() * EaseOutCubic.transform(ru),
                        center = Offset(cx, cy)
                    )
                }
            }
            PointerBadge(
                center = Offset(cx, cy),
                badgeSize = 54.dp,
                iconSize = 26.dp,
                tint = primary,
                iconTint = onPrimary,
                scale = badgeScale
            )
        }
        TutorialGesture.SWIPE_LEFT -> {
            // One long leftward glide per cycle: accelerate out of the
            // start, decelerate into the end (emphasized), with a fading
            // gradient trail and trailing chevrons behind the finger.
            val minRange = 60.dp.px()
            val maxRange = 150.dp.px()
            val range = (hole.width * 0.36f).coerceIn(minRange, maxRange)
            val startX = hole.center.x + range
            val endX = hole.center.x - range
            val p = cycle
            val x = startX + (endX - startX) * EmphasizedEasing.transform(p)
            // Gentle upward arc mid-glide + lean into the motion.
            val bob = -sin(p * PI).toFloat() * 5.dp.px()
            val cy = hole.center.y
            val rot = -12f - 7f * sin(p * PI).toFloat()
            val alpha = when {
                p < 0.10f -> p / 0.10f
                p > 0.86f -> (1f - p) / 0.14f
                else -> 1f
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Gradient trail fading away behind the finger.
                val t0 = Offset(x + 26.dp.toPx(), cy + bob)
                val t1 = Offset(x + 84.dp.toPx(), cy + bob)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.38f * alpha),
                            primary.copy(alpha = 0f)
                        ),
                        startX = t0.x,
                        endX = t1.x
                    ),
                    start = t0,
                    end = t1,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawChevron(
                    cx = x + 50.dp.toPx(), cy = cy + bob,
                    half = 7.dp.toPx(),
                    alpha = 0.55f * alpha * (1f - p * 0.35f),
                    color = primary, up = false
                )
                drawChevron(
                    cx = x + 76.dp.toPx(), cy = cy + bob,
                    half = 6.dp.toPx(),
                    alpha = 0.35f * alpha * (1f - p * 0.35f),
                    color = primary, up = false
                )
            }
            PointerBadge(
                center = Offset(x, cy + bob),
                badgeSize = 54.dp,
                iconSize = 26.dp,
                tint = primary,
                iconTint = onPrimary,
                rotation = rot,
                alpha = alpha
            )
        }
        TutorialGesture.SCROLL -> {
            // Scrolling DOWN = the finger glides UP the screen. Same
            // emphasized glide as the swipe, but vertical, with the trail
            // and chevrons stacked below the finger pointing up.
            val minRange = 44.dp.px()
            val maxRange = 105.dp.px()
            val range = (hole.height * 0.15f).coerceIn(minRange, maxRange)
            val startY = hole.center.y + range
            val endY = hole.center.y - range
            val p = cycle
            val y = startY + (endY - startY) * EmphasizedEasing.transform(p)
            val cx = hole.center.x
            val alpha = when {
                p < 0.10f -> p / 0.10f
                p > 0.86f -> (1f - p) / 0.14f
                else -> 1f
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val t0 = Offset(cx, y + 26.dp.toPx())
                val t1 = Offset(cx, y + 84.dp.toPx())
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.38f * alpha),
                            primary.copy(alpha = 0f)
                        ),
                        startY = t0.y,
                        endY = t1.y
                    ),
                    start = t0,
                    end = t1,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawChevron(
                    cx = cx, cy = y + 50.dp.toPx(),
                    half = 7.dp.toPx(),
                    alpha = 0.55f * alpha * (1f - p * 0.35f),
                    color = primary, up = true
                )
                drawChevron(
                    cx = cx, cy = y + 74.dp.toPx(),
                    half = 6.dp.toPx(),
                    alpha = 0.35f * alpha * (1f - p * 0.35f),
                    color = primary, up = true
                )
            }
            PointerBadge(
                center = Offset(cx, y),
                badgeSize = 54.dp,
                iconSize = 26.dp,
                tint = primary,
                iconTint = onPrimary,
                rotation = -6f,
                alpha = alpha
            )
        }
        TutorialGesture.NONE -> Unit
    }
}

/**
 * The finger badge. `contentAlignment = Alignment.Center` on the FIXED-SIZE
 * badge is what keeps the icon dead-center of the circle.
 */
@Composable
private fun PointerBadge(
    center: Offset,
    badgeSize: Dp,
    iconSize: Dp,
    tint: Color,
    iconTint: Color,
    scale: Float = 1f,
    rotation: Float = 0f,
    alpha: Float = 1f
) {
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (center.x - badgeSize.toPx() / 2f).roundToInt(),
                    (center.y - badgeSize.toPx() / 2f).roundToInt()
                )
            }
            .size(badgeSize)
            .graphicsLayer {
                rotationZ = rotation
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(tint),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.TouchApp,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** A single chevron — points left (up = false) or up (up = true). */
private fun DrawScope.drawChevron(
    cx: Float,
    cy: Float,
    half: Float,
    alpha: Float,
    color: Color,
    up: Boolean
) {
    val path = Path().apply {
        if (up) {
            moveTo(cx - half, cy + half)
            lineTo(cx, cy - half * 0.7f)
            moveTo(cx, cy - half * 0.7f)
            lineTo(cx + half, cy + half)
        } else {
            moveTo(cx + half, cy - half)
            lineTo(cx - half * 0.7f, cy)
            moveTo(cx - half * 0.7f, cy)
            lineTo(cx + half, cy + half)
        }
    }
    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(
            width = 4.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Card backdrops
// ─────────────────────────────────────────────────────────────────────────────

/** Soft pulsing blobs behind the welcome card. */
@Composable
private fun WelcomeBlob() {
    val blobScale by rememberInfiniteTransition(label = "blob")
        .animateFloat(
            initialValue = 0.94f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blobScale"
        )
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.5f }
    ) {
        val c = center
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = 0.22f),
                    primary.copy(alpha = 0f)
                ),
                center = Offset(c.x - 120f, c.y - 60f),
                radius = 240f * blobScale
            ),
            radius = 240f * blobScale,
            center = Offset(c.x - 120f, c.y - 60f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondary.copy(alpha = 0.18f),
                    secondary.copy(alpha = 0f)
                ),
                center = Offset(c.x + 130f, c.y + 90f),
                radius = 200f / blobScale
            ),
            radius = 200f / blobScale,
            center = Offset(c.x + 130f, c.y + 90f)
        )
    }
}

/**
 * Finish artwork: a self-drawing check badge with a two-wave confetti
 * burst of rotating paper slips. Draw progress runs once when the step
 * appears.
 */
@Composable
private fun FinishBurst() {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(1500, easing = EmphasizedDecelerate))
    }
    val p = progress.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.85f },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val c = center
            val checkProgress = (p / 0.55f).coerceIn(0f, 1f)
            val burstProgress = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f)

            // Badge circle — soft halo fills in behind the stroke ring.
            drawCircle(
                color = primary.copy(alpha = 0.12f + 0.10f * (1f - p)),
                radius = 64.dp.toPx()
            )
            drawCircle(
                color = primary,
                radius = 44.dp.toPx(),
                style = Stroke(width = 3.5.dp.toPx())
            )

            // Self-drawing checkmark.
            if (checkProgress > 0f) {
                val check = Path().apply {
                    moveTo(c.x - 18.dp.toPx(), c.y + 1.dp.toPx())
                    lineTo(c.x - 5.dp.toPx(), c.y + 14.dp.toPx())
                    lineTo(c.x + 20.dp.toPx(), c.y - 13.dp.toPx())
                }
                val measure = PathMeasure().apply { setPath(check, false) }
                val partial = Path()
                measure.getSegment(0f, measure.length * checkProgress, partial, true)
                drawPath(
                    path = partial,
                    color = primary,
                    style = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Confetti — two staggered waves of rotating paper slips that
            // burst outward, drift slightly down and fade.
            if (burstProgress > 0f) {
                val colors = listOf(primary, secondary, tertiary)
                val total = 14
                for (i in 0 until total) {
                    val wave = if (i < 8) 0 else 1
                    val bp = ((burstProgress - wave * 0.22f) / 0.78f)
                        .coerceIn(0f, 1f)
                    if (bp <= 0f) continue
                    val ease = EaseOutCubic.transform(bp)
                    val angle = (i.toFloat() / total) * (2.0 * PI).toFloat() +
                            wave * 0.24f
                    val dist = (70f + (i % 3) * 26f) * ease + 46.dp.toPx()
                    val px = c.x + cos(angle) * dist
                    val py = c.y + sin(angle) * dist + bp * bp * 18.dp.toPx()
                    val alpha = (1f - bp).coerceIn(0f, 1f) * 0.9f
                    val len = 3.6.dp.toPx() + (i % 2) * 1.6.dp.toPx()
                    val wide = 1.8.dp.toPx()
                    rotate(angle + bp * 0.9f, pivot = Offset(px, py)) {
                        drawRoundRect(
                            color = colors[i % colors.size].copy(alpha = alpha),
                            topLeft = Offset(px - len / 2f, py - wide / 2f),
                            size = Size(len, wide),
                            cornerRadius = CornerRadius(wide / 2f)
                        )
                    }
                }
            }
        }
    }
}

