package com.daklok.biblelockscreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.*


// Helpers

private fun hsvToColor(hue: Float, sat: Float, value: Float): Color {
    val f = { n: Float ->
        val k = (n + hue / 60f) % 6f
        value - value * sat * maxOf(0f, minOf(k, 4f - k, 1f))
    }
    return Color(f(5f), f(3f), f(1f))
}

private fun colorToHsv(color: Color): Triple<Float, Float, Float> {
    val r = color.red; val g = color.green; val b = color.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b); val d = max - min
    val h = when {
        d == 0f  -> 0f
        max == r -> 60f * (((g - b) / d + 6f) % 6f)
        max == g -> 60f * ((b - r) / d + 2f)
        else     -> 60f * ((r - g) / d + 4f)
    }
    return Triple(h, if (max == 0f) 0f else d / max, max)
}

private fun Color.toHex(): String = String.format("%06X", toArgb() and 0xFFFFFF)

private fun hexToColor(hex: String): Color? {
    if (hex.length != 6) return null
    return try {
        val v = hex.toLong(16)
        Color(
            red   = ((v shr 16) and 0xFF) / 255f,
            green = ((v shr  8) and 0xFF) / 255f,
            blue  =  (v         and 0xFF) / 255f
        )
    } catch (_: NumberFormatException) { null }
}

private fun DrawScope.drawColorWheel(value: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r  = minOf(cx, cy)
    val ringCount  = 28
    val sliceCount = 360

    for (h in 0 until sliceCount) {
        for (ring in 0 until ringCount) {
            val sat   = (ring + 0.5f) / ringCount
            val inner = ring.toFloat()       / ringCount * r
            val outer = (ring + 1).toFloat() / ringCount * r
            drawArc(
                color      = hsvToColor(h.toFloat(), sat, value),
                startAngle = h - 0.7f,
                sweepAngle = 1.4f,
                useCenter  = true,
                topLeft    = Offset(cx - outer, cy - outer),
                size       = Size(outer * 2f, outer * 2f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Presets
// ─────────────────────────────────────────────────────────────────────────────

private val PRESETS = listOf(
    Color.White,
    Color.Black,
    Color(0xFFCFDEF3.toInt()),   // soft blue-white
    Color(0xFFFFF8E7.toInt()),   // warm cream
    Color(0xFFAAAAAA.toInt()),   // mid gray
    Color(0xFFFFB347.toInt()),   // golden
    Color(0xFF87CEEB.toInt()),   // sky blue
    Color(0xFFDDA0DD.toInt()),   // plum
)

@Composable
fun ColorPickerDialog(
    initialColor: Color = Color.White,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val (initH, initS, initV) = colorToHsv(initialColor)

    var hueState   by remember { mutableStateOf(initH) }
    var satState   by remember { mutableStateOf(initS) }
    var valueState by remember { mutableStateOf(initV) }

    val currentColor by remember(hueState, satState, valueState) {
        derivedStateOf { hsvToColor(hueState, satState, valueState) }
    }


    var hexText by remember { mutableStateOf(initialColor.toHex()) }
    var rText   by remember { mutableStateOf((initialColor.red   * 255).toInt().toString()) }
    var gText   by remember { mutableStateOf((initialColor.green * 255).toInt().toString()) }
    var bText   by remember { mutableStateOf((initialColor.blue  * 255).toInt().toString()) }


    var wheelDriving by remember { mutableStateOf(false) }
    LaunchedEffect(hueState, satState, valueState) {
        if (wheelDriving) {
            val c = currentColor
            hexText = c.toHex()
            rText   = (c.red   * 255).toInt().toString()
            gText   = (c.green * 255).toInt().toString()
            bText   = (c.blue  * 255).toInt().toString()
        }
    }

    fun setHsv(h: Float, s: Float, v: Float, fromWheel: Boolean = false) {
        wheelDriving = fromWheel
        hueState   = h
        satState   = s
        valueState = v
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape         = RoundedCornerShape(20.dp),
            tonalElevation = 4.dp,
            modifier      = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier            = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // Title
                Text(
                    text       = "Verse color",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                //Wheel + right column
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .pointerInput(valueState) {
                                val sz = size.width.toFloat()
                                val cx = sz / 2f; val r = sz / 2f
                                fun handle(pos: Offset) {
                                    val dx = pos.x - cx; val dy = pos.y - cx
                                    val dist = sqrt(dx * dx + dy * dy).coerceAtMost(r)
                                    val h = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
                                    setHsv(h, dist / r, valueState, fromWheel = true)
                                }
                                detectTapGestures { handle(it) }
                            }
                            .pointerInput(valueState) {
                                val sz = size.width.toFloat()
                                val cx = sz / 2f; val r = sz / 2f
                                detectDragGestures { change, _ ->
                                    val pos = change.position
                                    val dx  = pos.x - cx; val dy = pos.y - cx
                                    val dist = sqrt(dx * dx + dy * dy).coerceAtMost(r)
                                    val h = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
                                    setHsv(h, dist / r, valueState, fromWheel = true)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawColorWheel(valueState)
                            // cursor
                            val cx2  = size.width  / 2f
                            val cy2  = size.height / 2f
                            val r2   = minOf(cx2, cy2)
                            val ang  = hueState * PI.toFloat() / 180f
                            val dotX = cx2 + satState * r2 * cos(ang)
                            val dotY = cy2 + satState * r2 * sin(ang)
                            drawCircle(Color.White,        radius = 10f, center = Offset(dotX, dotY))
                            drawCircle(currentColor,       radius = 7f,  center = Offset(dotX, dotY))
                        }
                    }

                    Column(
                        modifier            = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Swatch
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(currentColor)
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(10.dp)
                                )
                        )
                        // Verse preview
                        Text(
                            text     = "\"For God so loved...\"",
                            fontSize = 11.sp,
                            color    = currentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                //Brightness
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text      = "Brightness",
                        fontSize  = 12.sp,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier  = Modifier.width(72.dp)
                    )
                    Slider(
                        value         = valueState,
                        onValueChange = { setHsv(hueState, satState, it, fromWheel = true) },
                        valueRange    = 0f..1f,
                        modifier      = Modifier.weight(1f)
                    )
                    Text(
                        text     = "${(valueState * 100).toInt()}%",
                        fontSize = 12.sp,
                        modifier = Modifier.width(36.dp)
                    )
                }

                //Hex input
                OutlinedTextField(
                    value         = hexText,
                    onValueChange = { raw ->
                        val clean = raw
                            .filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
                            .uppercase()
                            .take(6)
                        hexText = clean
                        if (clean.length == 6) {
                            hexToColor(clean)?.let { c ->
                                val (h, s, v) = colorToHsv(c)
                                setHsv(h, s, v)
                                rText = (c.red   * 255).toInt().toString()
                                gText = (c.green * 255).toInt().toString()
                                bText = (c.blue  * 255).toInt().toString()
                            }
                        }
                    },
                    label         = { Text("Hex") },
                    prefix        = { Text("#") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier      = Modifier.fillMaxWidth()
                )

                //RGB inputs
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    @Composable
                    fun RgbField(label: String, text: String, onUpdate: (String) -> Unit) {
                        OutlinedTextField(
                            value         = text,
                            onValueChange = onUpdate,
                            label         = { Text(label) },
                            singleLine    = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier      = Modifier.weight(1f)
                        )
                    }

                    RgbField("R", rText) { v ->
                        rText = v.filter { it.isDigit() }.take(3)
                        val r2 = rText.toIntOrNull()?.coerceIn(0, 255) ?: return@RgbField
                        val g2 = gText.toIntOrNull()?.coerceIn(0, 255) ?: return@RgbField
                        val b2 = bText.toIntOrNull()?.coerceIn(0, 255) ?: return@RgbField
                        val c  = Color(r2 / 255f, g2 / 255f, b2 / 255f)
                        val (h, s, bv) = colorToHsv(c)
                        setHsv(h, s, bv)
                        hexText = c.toHex()
                    }
                    RgbField("G", gText) { v ->
                        gText = v.filter { it.isDigit() }.take(3)
                        val r2 = rText.toIntOrNull()?.coerceIn(0, 255) ?: return@RgbField
                        val g2 = gText.toIntOrNull()?.coerceIn(0, 255) ?: return@RgbField
                        val b2 = bText.toIntOrNull()?.coerceIn(0, 255) ?: return@RgbField
                        val c  = Color(r2 / 255f, g2 / 255f, b2 / 255f)
                        val (h, s, bv) = colorToHsv(c)
                        setHsv(h, s, bv)
                        hexText = c.toHex()
                    }
                    RgbField("B", bText) { v ->
                        bText = v.filter { it.isDigit() }.take(3)
                        val r2 = rText.toIntOrNull()?.coerceIn(0, 255) ?: return@RgbField
                        val g2 = gText.toIntOrNull()?.coerceIn(0, 255) ?: return@RgbField
                        val b2 = bText.toIntOrNull()?.coerceIn(0, 255) ?: return@RgbField
                        val c  = Color(r2 / 255f, g2 / 255f, b2 / 255f)
                        val (h, s, bv) = colorToHsv(c)
                        setHsv(h, s, bv)
                        hexText = c.toHex()
                    }
                }

                //Presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    PRESETS.forEach { preset ->
                        val selected = currentColor.toHex() == preset.toHex()
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(preset)
                                .border(
                                    width  = if (selected) 2.dp else 0.5.dp,
                                    color  = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape  = CircleShape
                                )
                                .pointerInput(preset) {
                                    detectTapGestures {
                                        val (h, s, v) = colorToHsv(preset)
                                        setHsv(h, s, v, fromWheel = true)
                                        hexText = preset.toHex()
                                        rText   = (preset.red   * 255).toInt().toString()
                                        gText   = (preset.green * 255).toInt().toString()
                                        bText   = (preset.blue  * 255).toInt().toString()
                                    }
                                }
                        )
                    }
                }

                //Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onColorSelected(currentColor); onDismiss() }) { Text("Apply") }
                }
            }
        }
    }
}