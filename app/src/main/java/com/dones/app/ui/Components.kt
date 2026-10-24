package com.dones.app.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Altura mínima de los botones: 64 dp, más que el mínimo de Android (48 dp). */
val BigTouch = 64.dp

/** Resplandor violeta suave arriba, como en Ergos. */
private val NightGlow = Brush.radialGradient(
    colors = listOf(Ergos.Violet.copy(alpha = 0.22f), Color.Transparent),
    center = Offset(0f, 0f),
    radius = 1100f,
)

private val ButtonShape = RoundedCornerShape(14.dp)
private val CardShape = RoundedCornerShape(12.dp)

/** Esqueleto común: una sola tarea por pantalla, con desplazamiento si la letra es muy grande. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun Screen(
    modifier: Modifier = Modifier,
    top: (@Composable () -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Con el teclado abierto se esconden la barra de arriba y los botones de abajo,
    // así queda casi toda la pantalla para el texto y los espacios para escribir.
    val typing = WindowInsets.isImeVisible
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .violetFog()
                .safeDrawingPadding(),
        ) {
            if (!typing) top?.invoke()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                content = content,
            )
            if (bottom != null && !typing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Sin línea ni caja: la niebla se oscurece suave hacia los botones.
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Ergos.Night.copy(alpha = .85f))))
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = bottom,
                )
            }
        }
    }
}

// ---------- Texto ----------

@Composable
fun Title(text: String, modifier: Modifier = Modifier, center: Boolean = false) {
    // Títulos con la letra del logo, como en el mockup y los PDF.
    Text(
        frakturString(text),
        style = TextStyle(
            fontFamily = Fraktur, fontSize = 40.sp, lineHeight = 46.sp, color = Ergos.Ink,
            shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = .6f), Offset(0f, 3f), 6f),
        ),
        modifier = modifier,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
    )
}

@Composable
fun Body(text: String, modifier: Modifier = Modifier, center: Boolean = false, color: Color = Color.Unspecified) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier,
        color = color,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
    )
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier, center: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Ergos.Muted,
        modifier = modifier,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
    )
}

/** Etiqueta pequeña en mayúsculas, como los encabezados de tarjeta de Ergos. */
@Composable
fun Eyebrow(text: String, icon: ImageVector? = null, color: Color = Ergos.Gold, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, letterSpacing = 2.6.sp), color = color)
    }
}

// ---------- Adornos ----------

/** Línea con rombo al centro y dos puntos: el separador de Ergos. */
@Composable
fun Ornament(modifier: Modifier = Modifier, color: Color = Ergos.Gold, width: Dp = 110.dp) {
    // Línea fina — rombo — línea fina (el adorno del mockup).
    Canvas(modifier.width(width).height(12.dp)) {
        val y = size.height / 2f
        val c = size.width / 2f
        val r = 3.5.dp.toPx()
        val stroke = 1.dp.toPx()
        drawLine(color.copy(alpha = .75f), Offset(0f, y), Offset(c - r * 2.4f, y), stroke)
        drawLine(color.copy(alpha = .75f), Offset(c + r * 2.4f, y), Offset(size.width, y), stroke)
        drawPath(Path().apply { moveTo(c, y - r); lineTo(c + r, y); lineTo(c, y + r); lineTo(c - r, y); close() }, color)
    }
}

/**
 * Navegación de abajo, sin botones grandes (mockup): una línea de progreso fina, «PÁGINA 3 DE 117»,
 * y los enlaces «← Anterior» y «Siguiente →». Las zonas para tocar miden al menos 56 dp.
 */
@Composable
fun PageNav(
    progress: Float? = null,
    label: String? = null,
    onPrevious: (() -> Unit)? = null,
    nextLabel: String = "Siguiente",
    onNext: (() -> Unit)? = null,
    nextEnabled: Boolean = true,
) {
    val caps = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 2.sp)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Arriba: la línea de progreso y «PÁGINA 3 DE 117»; abajo, los dos enlaces con espacio de sobra.
        if (progress != null || label != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (progress != null) {
                    Box(Modifier.weight(1f).height(2.dp).background(Ergos.Line.copy(alpha = .7f))) {
                        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(2.dp).background(Ergos.Violet))
                    }
                } else Spacer(Modifier.weight(1f))
                label?.let { Text(it.uppercase(), style = caps, color = Ergos.Muted) }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (onPrevious != null) NavLink("Anterior", ErgosIcons.ArrowLeft, before = true, color = Ergos.Ink2, onClick = onPrevious)
            Spacer(Modifier.weight(1f))
            if (onNext != null) {
                NavLink(nextLabel, ErgosIcons.ArrowRight, before = false, color = if (nextEnabled) Ergos.AccentText else Ergos.Muted.copy(alpha = .5f), enabled = nextEnabled, onClick = onNext)
            }
        }
    }
}

@Composable
private fun NavLink(text: String, icon: ImageVector, before: Boolean, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (before) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = color)
        if (!before) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
}

/** Ícono dentro del marco en rombo de Ergos. */
@Composable
fun IconFrame(icon: ImageVector, size: Dp = 76.dp, tint: Color = Ergos.AccentText, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val k = this.size.width / 64f
            fun diamond(a: Float) = Path().apply {
                moveTo(32 * k, a * k); lineTo((64 - a) * k, 32 * k); lineTo(32 * k, (64 - a) * k); lineTo(a * k, 32 * k); close()
            }
            drawPath(diamond(3f), Ergos.Violet.copy(alpha = .16f))
            drawPath(diamond(3f), tint, style = Stroke(1.4f * k))
            drawPath(diamond(9f), tint.copy(alpha = .55f), style = Stroke(.8f * k))
            // Ondas arriba y abajo.
            val wave = Path().apply {
                moveTo(24 * k, 4.5f * k)
                cubicTo(26.5f * k, 6.7f * k, 29.2f * k, 6.9f * k, 32 * k, 4.5f * k)
                cubicTo(34.8f * k, 6.9f * k, 37.5f * k, 6.7f * k, 40 * k, 4.5f * k)
            }
            drawPath(wave, tint.copy(alpha = .8f), style = Stroke(1f * k, cap = StrokeCap.Round))
            scale(1f, -1f, pivot = Offset(32 * k, 32 * k)) {
                drawPath(wave, tint.copy(alpha = .8f), style = Stroke(1f * k, cap = StrokeCap.Round))
            }
        }
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * .42f))
    }
}

/** Esquina ornamental arriba a la derecha de una tarjeta. */
@Composable
fun BoxScope.CornerOrnament(size: Dp = 34.dp) {
    Image(
        rememberVectorPainter(ErgosIcons.OrnamentCorner),
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(size)
            .graphicsLayer { scaleX = -1f },
    )
}

// ---------- Encabezados ----------

/** Encabezado de pantalla: marco con ícono, etiqueta, título serif y adorno. */
@Composable
fun PageHeader(
    title: String,
    icon: ImageVector? = null,
    eyebrow: String? = null,
    subtitle: String? = null,
    center: Boolean = true,
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) IconFrame(icon)
        if (eyebrow != null) Eyebrow(eyebrow)
        Title(title, center = center)
        Ornament(width = if (center) 200.dp else 160.dp)
        if (subtitle != null) Body(subtitle, center = center, color = Ergos.Ink2)
    }
}

/** Progreso con palabras y barra: "BLOQUE 2 DE 5 · 14 de 25". */
@Composable
fun ProgressHeader(label: String, fraction: Float, icon: ImageVector? = null, count: String? = null) {
    Column(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(label, icon = icon, color = Ergos.AccentText, modifier = Modifier.weight(1f))
            if (count != null) Text(count, style = MaterialTheme.typography.labelMedium, color = Ergos.Ink2)
        }
        // Línea fina con brillo en vez de barra gruesa.
        Box(Modifier.fillMaxWidth().height(2.dp).background(Ergos.Line.copy(alpha = .6f))) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(2.dp)
                    .shadow(6.dp, RoundedCornerShape(1.dp), ambientColor = Ergos.VioletHi, spotColor = Ergos.VioletHi)
                    .background(Brush.horizontalGradient(listOf(Ergos.Violet, Ergos.VioletHi))),
            )
        }
    }
}

// ---------- Tarjetas ----------

/**
 * Bloque de contenido sin caja (estilo de la hoja 6): solo el texto sobre la niebla.
 * Con [ornament], arriba lleva la línea fina con un rombo que separa los bloques.
 */
@Composable
fun ErgosCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    ornament: Boolean = false,
    border: Color = Ergos.Line,
    background: Color = Ergos.Surface,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier.fillMaxWidth()
    Column(
        (if (onClick != null) base.clip(CardShape).clickable(role = Role.Button, onClick = onClick) else base).padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (ornament) DiamondRule()
        content()
    }
}

/** Línea fina con un rombo al centro: separa bloques sin dibujar cajas. */
@Composable
fun DiamondRule(color: Color = Ergos.Violet, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val y = size.height / 2
        val c = size.width / 2
        val r = 5.dp.toPx()
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, color.copy(alpha = .7f)), endX = c - r * 2), Offset(0f, y), Offset(c - r * 2.2f, y), 1.dp.toPx())
        drawLine(Brush.horizontalGradient(listOf(color.copy(alpha = .7f), Color.Transparent), startX = c + r * 2), Offset(c + r * 2.2f, y), Offset(size.width, y), 1.dp.toPx())
        drawPath(Path().apply { moveTo(c, y - r); lineTo(c + r, y); lineTo(c, y + r); lineTo(c - r, y); close() }, color)
    }
}

/** Fila grande para tocar, sin caja: ícono en un rombo, título, detalle, flecha y una línea fina debajo. */
@Composable
fun DoorCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    highlighted: Boolean = false,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .clip(CardShape)
                .then(if (highlighted) Modifier.background(Brush.horizontalGradient(listOf(Ergos.Violet.copy(alpha = .22f), Color.Transparent))) else Modifier)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconFrame(icon, size = 58.dp, tint = if (highlighted) Ergos.VioletHi else Ergos.AccentText)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (highlighted) Color.White else Ergos.Ink)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = Ergos.Muted)
            }
            Icon(ErgosIcons.ChevronRight, contentDescription = null, tint = Ergos.AccentText, modifier = Modifier.size(24.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Ergos.Violet.copy(alpha = .5f), Color.Transparent))))
    }
}

/** Cita bíblica como enlace, sin cápsula: "📖 Efesios 4:11-14 · Leer ›". */
@Composable
fun RefChip(text: String, onClick: (() -> Unit)? = null) {
    val title = com.dones.core.Bible.parse(text)?.title ?: text
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "Leer el pasaje", role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(ErgosIcons.Book, contentDescription = null, tint = Ergos.AccentText, modifier = Modifier.size(22.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = Ergos.Ink)
        if (onClick != null) {
            Text("· Leer", style = MaterialTheme.typography.labelLarge, color = Ergos.AccentText)
            Icon(ErgosIcons.ChevronRight, contentDescription = null, tint = Ergos.AccentText, modifier = Modifier.size(18.dp))
        }
    }
}

// ---------- Botones ----------

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, trailing: ImageVector?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = color, textAlign = TextAlign.Center)
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            Icon(trailing, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailing: ImageVector? = null,
) {
    val fill = if (enabled) Brush.verticalGradient(listOf(Ergos.ButtonTop, Ergos.ButtonBottom))
    else Brush.verticalGradient(listOf(Ergos.Surface2, Ergos.Surface2))
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = BigTouch)
            .then(if (enabled) Modifier.shadow(14.dp, ButtonShape, ambientColor = Ergos.Violet, spotColor = Ergos.Violet) else Modifier)
            .clip(ButtonShape)
            .background(fill)
            .drawBehind {
                // Brillo fino arriba, como el botón principal de Ergos.
                if (enabled) drawRoundRect(
                    Color.White.copy(alpha = .28f),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(1.dp.toPx()),
                    alpha = .6f,
                )
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { ButtonContent(text, icon, trailing, if (enabled) Color.White else Ergos.Muted) }
}

/** Botón secundario sin borde: texto violeta claro con su ícono. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, icon: ImageVector? = null, trailing: ImageVector? = null) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = BigTouch)
            .clip(ButtonShape)
            .background(Ergos.Violet.copy(alpha = .1f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { ButtonContent(text, icon, trailing, Ergos.AccentText) }
}

/**
 * Opción de respuesta, sin caja: un rombo que se llena al elegirla (no solo el color),
 * el texto, y una línea fina debajo. La elegida brilla en violeta.
 */
@Composable
fun ChoiceButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = BigTouch)
                .clip(ButtonShape)
                .then(if (selected) Modifier.background(Brush.horizontalGradient(listOf(Ergos.Violet.copy(alpha = .35f), Ergos.Violet.copy(alpha = .05f)))) else Modifier)
                .semantics {
                    this.selected = selected
                    role = Role.RadioButton
                }
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize()) {
                    val c = size.width / 2
                    val r = size.width * .42f
                    val d = Path().apply { moveTo(c, c - r); lineTo(c + r, c); lineTo(c, c + r); lineTo(c - r, c); close() }
                    if (selected) drawPath(d, Brush.verticalGradient(listOf(Ergos.VioletHi, Ergos.ButtonBottom)))
                    else drawPath(d, Ergos.Muted, style = Stroke(1.6.dp.toPx()))
                }
                if (selected) Icon(ErgosIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else Ergos.Ink, modifier = Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Ergos.Violet.copy(alpha = .35f), Color.Transparent))))
    }
}

/** Enlace de texto con ícono (por defecto "‹ Volver"). */
@Composable
fun BackLink(
    text: String = "Volver",
    onClick: () -> Unit,
    icon: ImageVector? = ErgosIcons.ChevronLeft,
    trailing: ImageVector? = null,
    color: Color = Ergos.AccentText,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides color) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = color)
            if (trailing != null) Icon(trailing, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

/** Mensaje centrado con ícono en marco, para pantallas de "listo". */
@Composable
fun CenteredMessage(title: String, body: String, icon: ImageVector = ErgosIcons.Sparkle) {
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconFrame(icon, size = 96.dp, tint = Ergos.Gold)
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Ornament()
        Body(body, center = true, color = Ergos.Ink2)
    }
}

/** Ícono pequeño de estado (✓ verde / ✗ rojo) en un círculo. */
@Composable
fun StatusDot(ok: Boolean, size: Dp = 28.dp) {
    val color = if (ok) Ergos.Positive else Ergos.Negative
    Box(
        Modifier.size(size).clip(CircleShape).background(color.copy(alpha = .18f)).border(1.5.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (ok) ErgosIcons.Check else ErgosIcons.Close, contentDescription = if (ok) "Correcto" else "Incorrecto", tint = color, modifier = Modifier.size(size * .6f))
    }
}

/** Para listas de pasos del onboarding: "Dato 2 de 4" con cuatro puntitos. */
@Composable
fun StepDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("DATO $current DE $total", style = MaterialTheme.typography.labelSmall, color = Ergos.Gold)
        Spacer(Modifier.width(4.dp))
        repeat(total) { i ->
            Box(
                Modifier
                    .size(if (i + 1 == current) 12.dp else 9.dp)
                    .clip(CircleShape)
                    .background(if (i + 1 <= current) Ergos.Gold else Ergos.Line),
            )
        }
    }
}

