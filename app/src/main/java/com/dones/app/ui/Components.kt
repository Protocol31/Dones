package com.dones.app.ui

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.graphics.RectangleShape
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

private val ButtonShape = CutCornerShape(14.dp)
private val CardShape = StoneShape

/** Piedra oscura para tarjetas: la textura media con un velo negro. */
private val CardTint = Color.Black.copy(alpha = .38f)

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
                .stoneBackdrop()
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
                        .stone(RectangleShape, tint = Color.Black.copy(alpha = .45f), bevel = 2.dp, elevation = 0.dp)
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
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
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
fun Eyebrow(text: String, icon: ImageVector? = null, color: Color = Ergos.AccentText, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

// ---------- Adornos ----------

/** Línea con rombo al centro y dos puntos: el separador de Ergos. */
@Composable
fun Ornament(modifier: Modifier = Modifier, color: Color = Ergos.Gold, width: Dp = 200.dp) {
    Canvas(modifier.width(width).height(14.dp)) {
        val s = size.width / 240f
        val y = size.height / 2f
        val stroke = 1.dp.toPx()
        drawLine(color.copy(alpha = .7f), Offset(0f, y), Offset(96 * s, y), stroke)
        drawLine(color.copy(alpha = .7f), Offset(144 * s, y), Offset(size.width, y), stroke)
        drawCircle(color, 2.4f * s, Offset(104 * s, y))
        drawCircle(color, 2.4f * s, Offset(136 * s, y))
        val r = 7f * s
        drawPath(
            Path().apply {
                moveTo(120 * s, y - r); lineTo(120 * s + r, y); lineTo(120 * s, y + r); lineTo(120 * s - r, y); close()
            },
            color,
        )
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
            Eyebrow(label, icon = icon, color = Ergos.Gold, modifier = Modifier.weight(1f))
            if (count != null) Text(count, style = MaterialTheme.typography.labelMedium, color = Ergos.Ink2)
        }
        // Ranura tallada en la piedra que se llena de poción violeta.
        Box(
            Modifier
                .fillMaxWidth()
                .height(18.dp)
                .stone(CutCornerShape(6.dp), dark = true, inset = true, bevel = 2.dp, elevation = 0.dp)
                .padding(3.5.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CutCornerShape(3.dp))
                    .background(PotionBrush),
            )
        }
    }
}

// ---------- Tarjetas ----------

/** Tarjeta de Ergos: fondo de superficie, borde fino y, si se quiere, esquina ornamental. */
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
    val base = modifier
        .fillMaxWidth()
        .stone(CardShape, tint = if (background == Ergos.Surface) CardTint else background.copy(alpha = .6f))
        .then(if (border != Ergos.Line) Modifier.border(1.5.dp, border, CardShape) else Modifier)
    Box(if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base) {
        if (ornament) CornerOrnament()
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** Fila grande para tocar: cuadro violeta con ícono, título, detalle y flecha (las "puertas" de Ergos). */
@Composable
fun DoorCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    highlighted: Boolean = false,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .stone(CardShape, tint = if (highlighted) Ergos.VioletDeep.copy(alpha = .45f) else CardTint)
            .then(if (highlighted) Modifier.border(2.dp, Ergos.AccentText, CardShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        if (highlighted) CornerOrnament(30.dp)
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Cuadro tallado en la piedra, relleno de poción.
            Box(
                Modifier
                    .size(58.dp)
                    .stone(CutCornerShape(9.dp), dark = true, inset = true, bevel = 2.dp, elevation = 0.dp)
                    .padding(5.dp)
                    .clip(CutCornerShape(5.dp))
                    .background(PotionBrush),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Ergos.Ink)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = Ergos.Muted)
            }
            Icon(ErgosIcons.ChevronRight, contentDescription = null, tint = Ergos.AccentText, modifier = Modifier.size(24.dp))
        }
    }
}

/** Cita bíblica como etiqueta que se toca para leerla: "📖 Efesios 4:11-14 · Leer ›". */
@Composable
fun RefChip(text: String, onClick: (() -> Unit)? = null) {
    val title = com.dones.core.Bible.parse(text)?.title ?: text
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .stone(StoneShapeSmall, tint = Ergos.VioletDeep.copy(alpha = .4f), bevel = 2.dp, elevation = 3.dp)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "Leer el pasaje", role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(ErgosIcons.Book, contentDescription = null, tint = Ergos.AccentText, modifier = Modifier.size(22.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, color = Ergos.Ink)
        if (onClick != null) {
            Text("· Leer", style = MaterialTheme.typography.labelMedium, color = Ergos.AccentText)
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
    // Losa de piedra con la poción dentro de la ranura.
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = BigTouch)
            .stone(ButtonShape, bevel = 3.dp, elevation = if (enabled) 8.dp else 2.dp)
            .padding(5.dp)
            .clip(CutCornerShape(9.dp))
            .then(if (enabled) Modifier.background(PotionBrush) else Modifier.background(Color(0xFF1E1C1A)))
            .drawBehind {
                if (enabled) drawRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = .30f), Color.Transparent), endY = size.height * .5f),
                )
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) { ButtonContent(text, icon, trailing, if (enabled) Color.White else Ergos.Muted) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, icon: ImageVector? = null, trailing: ImageVector? = null) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = BigTouch)
            .stone(ButtonShape, bevel = 3.dp, tint = Color.Black.copy(alpha = .15f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { ButtonContent(text, icon, trailing, Ergos.Ink) }
}

/** Opción de respuesta: ancho completo, con un círculo que se marca con ✓ al elegirla (no solo el color). */
@Composable
fun ChoiceButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = BigTouch)
            .stone(
                ButtonShape,
                inset = selected,
                bevel = 3.dp,
                tint = if (selected) Ergos.VioletDeep.copy(alpha = .62f) else Color.Black.copy(alpha = .15f),
            )
            .then(if (selected) Modifier.border(2.dp, Ergos.VioletHi, ButtonShape) else Modifier)
            .semantics {
                this.selected = selected
                role = Role.RadioButton
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hueco redondo tallado; al elegirlo se llena de poción con ✓ (no solo el color).
        Box(
            Modifier
                .size(32.dp)
                .stone(CircleShape, dark = true, inset = true, bevel = 2.dp, elevation = 0.dp)
                .then(if (selected) Modifier.padding(4.dp).clip(CircleShape).background(PotionBrush) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(ErgosIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = Ergos.Ink, modifier = Modifier.weight(1f))
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
                    .background(if (i + 1 <= current) Ergos.Violet else Ergos.Line),
            )
        }
    }
}

