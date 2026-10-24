package com.dones.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import android.graphics.BitmapFactory
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
import androidx.compose.ui.text.font.FontWeight
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
        FogBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .background(NightGlow)
                .safeDrawingPadding(),
        ) {
            // (la niebla va detrás de todo, ver FogBackdrop)
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
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Ergos.Night.copy(alpha = .78f))))
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = bottom,
                )
            }
        }
    }
}

/** Niebla violeta de fondo: las mismas texturas de la batalla, teñidas de violeta y muy suaves. */
@Composable
fun FogBackdrop() {
    val context = LocalContext.current
    val fog: ImageBitmap? = remember {
        runCatching {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            context.assets.open("batalla-fondo/niebla-1.jpg").use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
        }.getOrNull()
    }
    if (fog != null) {
        Image(
            bitmap = fog,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(Ergos.Violet, BlendMode.Modulate),
            modifier = Modifier.fillMaxSize().alpha(.38f),
        )
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
fun Ornament(modifier: Modifier = Modifier, color: Color = Ergos.Violet, width: Dp = 200.dp) {
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
        Box(Modifier.fillMaxWidth().height(3.dp).background(Ergos.Violet.copy(alpha = .25f))) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(Ergos.Violet),
            )
        }
    }
}

// ---------- Tarjetas ----------

/** Bloque de contenido sin recuadro: solo aire y una línea muy fina debajo. */
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
        .drawBehind {
            drawLine(Ergos.Violet.copy(alpha = .22f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        }
    Box(if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base) {
        Column(Modifier.padding(vertical = padding), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** Rombo con un ícono dentro, en trazo fino. */
@Composable
private fun DiamondIcon(icon: ImageVector, size: Dp = 52.dp, tint: Color = Ergos.AccentText) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val k = this.size.width / 64f
            val d = Path().apply { moveTo(32 * k, 3 * k); lineTo(61 * k, 32 * k); lineTo(32 * k, 61 * k); lineTo(3 * k, 32 * k); close() }
            drawPath(d, Ergos.Violet.copy(alpha = .14f))
            drawPath(d, tint.copy(alpha = .8f), style = Stroke(1.3f * k))
        }
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * .42f))
    }
}

/** Fila grande para tocar, sin recuadro: ícono en rombo, título, detalle y flecha, con una línea fina debajo. */
@Composable
fun DoorCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    highlighted: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .drawBehind {
                drawLine(Ergos.Violet.copy(alpha = if (highlighted) .55f else .22f), Offset(0f, size.height), Offset(size.width, size.height), (if (highlighted) 1.5.dp else 1.dp).toPx())
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DiamondIcon(icon, tint = if (highlighted) Ergos.Gold else Ergos.AccentText)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ergos.Ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = Ergos.Muted)
        }
        Icon(ErgosIcons.ChevronRight, contentDescription = null, tint = Ergos.AccentText, modifier = Modifier.size(24.dp))
    }
}

/** Cita bíblica como enlace sin cápsula: "📖 Efesios 4:11-14 · Leer ›". */
@Composable
fun RefChip(text: String, onClick: (() -> Unit)? = null) {
    val title = com.dones.core.Bible.parse(text)?.title ?: text
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "Leer el pasaje", role = Role.Button, onClick = onClick) else Modifier),
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
    val fill = if (enabled) Brush.horizontalGradient(listOf(Ergos.ButtonBottom, Ergos.ButtonTop))
    else Brush.horizontalGradient(listOf(Ergos.Surface2, Ergos.Surface2))
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = BigTouch)
            .clip(ButtonShape)
            .background(fill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { ButtonContent(text, icon, trailing, if (enabled) Color.White else Ergos.Muted) }
}

/** Botón secundario: solo texto violeta claro, sin recuadro. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, icon: ImageVector? = null, trailing: ImageVector? = null) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = BigTouch)
            .clip(ButtonShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { ButtonContent(text, icon, trailing, Ergos.AccentText) }
}

/** Opción de respuesta: fila con un rombo que se llena al elegirla y una línea fina debajo. */
@Composable
fun ChoiceButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = BigTouch)
            .drawBehind {
                drawLine(
                    if (selected) Ergos.Violet.copy(alpha = .8f) else Ergos.Violet.copy(alpha = .22f),
                    Offset(0f, size.height), Offset(size.width, size.height),
                    (if (selected) 2.dp else 1.dp).toPx(),
                )
            }
            .semantics {
                this.selected = selected
                role = Role.RadioButton
            }
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val r = size.width / 2f
                val d = Path().apply {
                    moveTo(r, 0f); lineTo(size.width, r); lineTo(r, size.height); lineTo(0f, r); close()
                }
                if (selected) drawPath(d, Ergos.Violet)
                drawPath(d, if (selected) Ergos.VioletHi else Ergos.LineStrong, style = Stroke(2.dp.toPx()))
            }
            if (selected) Icon(ErgosIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Ergos.Ink else Ergos.Ink2,
            modifier = Modifier.weight(1f),
        )
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

