package com.dones.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import com.dones.app.R
import kotlinx.coroutines.launch
import kotlin.math.min

/*
 * Intro de la app de la iglesia (Ergos · Memorias vivas), con el logo de la
 * Iglesia Anglicana San Andrés y el nombre del curso: "Llamados".
 *
 * En Android 13 o superior se usa la misma luz de Ergos (rayos, halo, destellos,
 * estallido y el logo hecho de luz). En teléfonos más antiguos, o si la luz no
 * se puede dibujar, se usa la versión sencilla de Ergos: resplandor + logo.
 */

private const val SHADER_SECONDS = 8.4f   // igual que Ergos
private const val TEXT_AT = 5.5f           // aparece "Llamados"
private const val LITE_SECONDS = 4.6f

/** Alto ÷ ancho del logo (viewBox 303 × 295). */
private const val LOGO_RATIO = 295f / 303f

private val EaseOutExpo = CubicBezierEasing(.16f, 1f, .3f, 1f)
private val Silver = Color(0xFFC9CBD6)

@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val finish by rememberUpdatedState(onFinished)
    var ended by remember { mutableStateOf(false) }
    val end: () -> Unit = { if (!ended) { ended = true; finish() } }
    val light = remember {
        if (Build.VERSION.SDK_INT >= 33) runCatching { LightIntro.create(context) }.getOrNull() else null
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { end() } },
    ) {
        if (light != null && Build.VERSION.SDK_INT >= 33) ShaderIntro(light, end) else LiteIntro(end)
        SkipButton(end, Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun SkipButton(onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .navigationBarsPadding()
            .padding(18.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = .06f))
            .border(1.dp, Color.White.copy(alpha = .28f), RoundedCornerShape(3.dp))
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            "SALTAR INTRO",
            color = Color.White,
            style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 2.sp),
        )
    }
}

/** "LLAMADOS" y debajo, más pequeño, de qué se trata el curso. [elapsed] = segundos desde que empieza a aparecer. */
@Composable
private fun IntroTitle(elapsed: Float, modifier: Modifier = Modifier) {
    val a = EaseOutExpo.transform((elapsed / 1.6f).coerceIn(0f, 1f))
    val b = EaseOutExpo.transform(((elapsed - .5f) / 1.2f).coerceIn(0f, 1f))
    val spacing = 1.2f + (.5f - 1.2f) * a
    val wordSize = 30f
    val density = LocalDensity.current
    // El espacio entre letras también queda después de la última: se compensa a la izquierda.
    val pad = with(density) { (wordSize * spacing).sp.toDp() }
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "LLAMADOS",
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            style = TextStyle(
                fontFamily = Cormorant, fontWeight = FontWeight.SemiBold,
                fontSize = wordSize.sp, letterSpacing = spacing.em, textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .padding(start = pad)
                .graphicsLayer { alpha = a; translationY = (1f - a) * 10.dp.toPx() },
        )
        Text(
            "un curso para conocer tus dones espirituales",
            color = Silver,
            style = TextStyle(
                fontFamily = Geist, fontSize = 18.sp, lineHeight = 26.sp,
                letterSpacing = .04.em, textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .width(320.dp)
                .graphicsLayer { alpha = b; translationY = (1f - b) * 8.dp.toPx() },
        )
    }
}

// ---------- La luz de Ergos (Android 13+) ----------

@RequiresApi(33)
@Composable
private fun ShaderIntro(light: LightIntro, end: () -> Unit) {
    var t by remember { mutableFloatStateOf(0f) }
    val onEnd by rememberUpdatedState(end)
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            t = (now - start) / 1e9f
            if (t > SHADER_SECONDS) { onEnd(); break }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Mismo encuadre que Ergos: luz un poco arriba del centro, logo bajo ella y el texto debajo del logo.
        val aspect = constraints.maxWidth.toFloat() / constraints.maxHeight.toFloat()
        val logoH = min(.56f, .78f * aspect * LOGO_RATIO)
        val scale = logoH / (.7f * LOGO_RATIO)
        val cy = .06f + logoH * .1f
        val lcy = cy - .18f * logoH
        val textTop = min(.88f, .5f - (lcy - logoH / 2f) + .03f)
        // Se dibuja a 2/3 de la resolución y se amplía: la luz es suave y así anda fluida en más teléfonos.
        val f = 1.5f
        Spacer(
            Modifier
                .size(maxWidth / f, maxHeight / f)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = f
                    scaleY = f
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithCache {
                    val brush = ShaderBrush(light.shader)
                    onDrawBehind {
                        light.shader.setFloatUniform("uRes", size.width, size.height)
                        light.shader.setFloatUniform("uT", t)
                        light.shader.setFloatUniform("uS", scale)
                        light.shader.setFloatUniform("uC", 0f, cy)
                        light.shader.setFloatUniform("uLC", 0f, lcy)
                        drawRect(brush)
                    }
                },
        )
        if (t > TEXT_AT) {
            IntroTitle(t - TEXT_AT, Modifier.padding(top = maxHeight * textTop))
        }
    }
}

@RequiresApi(33)
private class LightIntro(val shader: RuntimeShader) {
    companion object {
        fun create(context: Context): LightIntro {
            val shader = RuntimeShader(ERGOS_LIGHT)
            val logo = logoBitmap(context, 1024)
            // Ergos usa niveles de mipmap para el resplandor; aquí, copias reducidas del logo.
            val glow1 = halveTo(logo, 64)
            val glow2 = halveTo(glow1, 16)
            shader.setInputShader("uLogo", bitmapShader(logo))
            shader.setInputShader("uGlow1", bitmapShader(glow1))
            shader.setInputShader("uGlow2", bitmapShader(glow2))
            shader.setFloatUniform("uLogoSize", logo.width.toFloat(), logo.height.toFloat())
            shader.setFloatUniform("uGlow1Size", glow1.width.toFloat(), glow1.height.toFloat())
            shader.setFloatUniform("uGlow2Size", glow2.width.toFloat(), glow2.height.toFloat())
            return LightIntro(shader)
        }

        private fun bitmapShader(b: Bitmap) = BitmapShader(b, Shader.TileMode.DECAL, Shader.TileMode.DECAL).apply {
            filterMode = BitmapShader.FILTER_MODE_LINEAR
        }

        /** Logo blanco al 70 % del ancho, centrado en un cuadrado, como la textura de Ergos. */
        private fun logoBitmap(context: Context, px: Int): Bitmap {
            val bmp = createBitmap(px, px)
            val canvas = android.graphics.Canvas(bmp)
            val d = requireNotNull(context.getDrawable(R.drawable.logo_san_andres))
            val w = (px * .7f).toInt()
            val h = (w * LOGO_RATIO).toInt()
            d.setBounds((px - w) / 2, (px - h) / 2, (px + w) / 2, (px + h) / 2)
            d.draw(canvas)
            return bmp
        }

        /** Reduce a la mitad una y otra vez (cada paso promedia 2×2 píxeles): queda difuminado. */
        private fun halveTo(src: Bitmap, target: Int): Bitmap {
            var b = src
            while (b.width / 2 >= target) b = Bitmap.createScaledBitmap(b, b.width / 2, b.height / 2, true)
            return b
        }
    }
}

/** La luz de Ergos, traducida de WebGL a AGSL. */
private const val ERGOS_LIGHT = """
uniform float2 uRes; uniform float uT;
uniform float uS; uniform float2 uLC; uniform float2 uC;
uniform shader uLogo; uniform shader uGlow1; uniform shader uGlow2;
uniform float2 uLogoSize; uniform float2 uGlow1Size; uniform float2 uGlow2Size;

float h1(float n){ return fract(sin(n*127.1)*43758.5453); }
float h2(float2 p){ return fract(sin(dot(p, float2(127.1,311.7)))*43758.5453); }
float vnoise(float2 p){
  float2 i=floor(p); float2 f=fract(p); f=f*f*(3.-2.*f);
  return mix(mix(h2(i),h2(i+float2(1.,0.)),f.x), mix(h2(i+float2(0.,1.)),h2(i+float2(1.,1.)),f.x), f.y);
}
float2 rot(float2 v, float a){ float c=cos(a); float s=sin(a); return float2(c*v.x-s*v.y, s*v.x+c*v.y); }
float sq(float x){ return x*x; }

// La textura del logo: lp de 0 a 1, con y hacia arriba.
float logoA(float2 lp){ return uLogo.eval(float2(lp.x, 1.-lp.y)*uLogoSize).a; }
float glow1A(float2 lp){ return uGlow1.eval(float2(lp.x, 1.-lp.y)*uGlow1Size).a; }
float glow2A(float2 lp){ return uGlow2.eval(float2(lp.x, 1.-lp.y)*uGlow2Size).a; }

float rayField(float2 d, float t, float k){
  float a = vnoise(d*5.  + float2(t*.10, k));
  float b = vnoise(d*13. - float2(t*.18, 0.) + 3.7);
  float c = vnoise(d*36. + float2(0., t*.40) + 9.1);
  return a*b*(.55+.9*c);
}

float sparks(float2 p, float t, float speed, float layer){
  float r = length(p);
  float ang = atan(p.y, p.x)/6.2831853 + .5;
  float N = 70. + layer*46.;
  float cell = floor(ang*N);
  float f = fract(ang*N);
  float a1 = h1(cell + layer*31.);
  float a2 = h1(cell*1.7 + layer*13.);
  float a3 = h1(cell*3.1 + layer*7.);
  float lat = abs(f - (.5 + (a1-.5)*.5)) * (6.2831853*r/N);
  float head = pow(fract(t*speed*(.35+a2*.9) + a3), 1.6) * 1.5;
  float L = (.04 + .30*a1) * (.6 + speed*.9);
  float tail = clamp((head - r)/L, 0., 1.);
  float on = step(r, head) * (1.-tail);
  return on * smoothstep(.0035+.002*a2, 0., lat) * smoothstep(.0, .12, r) * (.4+.6*a3);
}

half4 main(float2 fc){
  float2 frag = float2(fc.x, uRes.y - fc.y);
  float2 uv = (frag - .5*uRes) / uRes.y;
  float t = uT;
  float2 p = uv - uC;
  float r = length(p);
  float2 dir = p / max(r, 1e-4);

  float grow  = smoothstep(.5, 3.4, t);
  float build = smoothstep(3.0, 4.25, t);
  float tb = t - 4.3;
  float after = smoothstep(4.3, 6.6, t);
  float calm = smoothstep(6.8, 8.2, t);
  float inten = (.10 + .90*grow) * (1. + 1.0*build) * (1. - .50*after) * (1. - .25*calm);

  float fog = vnoise(p*2.2 + float2(t*.05, 0.)) * vnoise(p*4.6 - float2(0., t*.07) + 4.);
  float3 col = mix(float3(.012,.008,.02), float3(.045,.02,.10), smoothstep(1.4, 0., r));
  col += float3(.42,.2,1.) * fog * exp(-r*1.7) * inten * .55;

  float speedUp = 1. + 2.6*build;
  float ca = .008 * (1. + 3.*build);
  float3 ray = float3(
    rayField(rot(dir,  ca), t*speedUp, 0.),
    rayField(rot(dir,  0.), t*speedUp, 0.),
    rayField(rot(dir, -ca), t*speedUp, 0.));
  ray = pow(ray*2.5, float3(2.7));
  ray = float3(ray.r*.7 + ray.g*.3, ray.g*.8, ray.b*.9 + ray.g*.1);
  float rayFall = exp(-r*(2.4 - grow*1.0)) / (1. + r*2.5);
  float3 rayCol = mix(float3(.62,.38,1.), float3(1.), exp(-r*3.2));
  col += ray * rayFall * rayCol * inten * 2.4;

  float core = (.010 / (r*r + .0035*(.35 + grow))) * (.30 + grow);
  col += core * mix(float3(1.), float3(.66,.42,1.), smoothstep(.02, .45, r)) * inten * .55;

  float streak = exp(-abs(p.y)*(150. - 70.*build)) * exp(-abs(p.x)*1.6) * (.45 + build*.9);
  col += streak * float3(.72,.66,1.) * inten * .9;
  float beam = exp(-abs(p.x)*(240. - 120.*build)) * exp(-max(p.y,0.)*1.1) * (.3 + build*.7);
  col += beam * float3(.85,.8,1.) * inten * .7;

  float2 e = rot(p, -.5); e.y *= 1.75;
  float er = length(e);
  float R0 = .30 + .10*grow + .12*build;
  float3 ring = float3(
    exp(-sq((er - R0*1.030)/.011)),
    exp(-sq((er - R0*1.000)/.011)),
    exp(-sq((er - R0*0.970)/.011)));
  float ringMask = .35 + .65*smoothstep(-.3, 1., dot(normalize(e + 1e-4), float2(.8,.3)));
  col += ring * ringMask * float3(.75,.7,1.) * (.25 + .6*grow) * (1. - after) * 1.2;

  float spkOn = smoothstep(.9, 1.8, t) * (1. - smoothstep(4.6, 5.8, t));
  float sp1 = sparks(p, t, .55 + build*.9, 0.);
  float sp2 = sparks(p, t, .8  + build*1.4, 1.);
  float sp3 = sparks(p, t, 1.1 + build*2.0, 2.);
  float spk = (sp1 + sp2*.9 + sp3*.8) * spkOn;
  col += spk * mix(float3(1.), float3(.7,.5,1.), h1(floor(atan(p.y,p.x)*30.))) * 1.6;

  float2 g = uv*34. + float2(0., -t*.5);
  float dust = step(.985, h2(floor(g))) * smoothstep(.5, 0., length(fract(g)-.5)) * (.4+.6*sin(t*3.+h2(floor(g))*40.));
  col += dust * float3(.8,.7,1.) * .35 * smoothstep(.3, 2., t);

  float Wv = tb * 1.55;
  float flash = (tb > 0.) ? exp(-tb*4.2) : 0.;
  col += flash * (1.7*exp(-r*1.1) + .6) * float3(1.);
  if (tb > 0.) {
    float3 sw = float3(
      exp(-sq((r - Wv*1.035)/.040)),
      exp(-sq((r - Wv)/.040)),
      exp(-sq((r - Wv*.965)/.040)));
    col += sw * float3(.9,.85,1.) * 2.2 * exp(-tb*.85);
  }

  float band = (tb > 0.) ? exp(-sq((r - Wv)/.16)) : 0.;
  float2 lp = (uv - uLC)/uS + .5;
  float2 disp = dir * band * .028 + dir * .0012;
  float aR = logoA(lp - disp*1.0);
  float aG = logoA(lp - disp*1.7);
  float aB = logoA(lp - disp*2.4);
  float reveal = (tb > 0.) ? 1. - smoothstep(Wv - .30, Wv, r) : 0.;
  float3 logoMix = float3(aR, aG, aB);
  float glowA = glow1A(lp) * .9 + glow2A(lp) * 1.3;
  float sweepPos = fract((t - 5.2)*.28);
  float sweep = exp(-sq((lp.x*.8 + lp.y*.6 - (sweepPos*2.2 - .4))/.06)) * smoothstep(5.0, 6.0, t);
  float3 body = float3(.97,.97,1.) + sweep*float3(.5,.3,.9);
  col += float3(.58,.30,1.) * glowA * reveal * (.8 + .2*sin(t*2.2)) * 1.25;

  col = float3(1.) - exp(-col * 1.15);
  col *= 1. - .55*dot(uv*float2(.8,1.), uv*float2(.8,1.));
  col += (h2(fc + fract(t)*97.) - .5) * .022;
  col = mix(col, clamp(body, 0., 1.), clamp(logoMix * reveal, 0., 1.));
  return half4(half3(max(col, 0.)), 1.);
}
"""

// ---------- Versión sencilla (Android 8 a 12) ----------

@Composable
private fun LiteIntro(end: () -> Unit) {
    val glowAlpha = remember { Animatable(0f) }
    val glowScale = remember { Animatable(.4f) }
    val logo = remember { Animatable(0f) }
    var textAt by remember { mutableFloatStateOf(-1f) }
    val onEnd by rememberUpdatedState(end)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch {
                glowAlpha.animateTo(.7f, keyframes { durationMillis = 2400; 1f at 1300 })
            }
            launch {
                glowScale.animateTo(1f, keyframes { durationMillis = 2400; 1.1f at 1300 })
            }
            launch {
                kotlinx.coroutines.delay(700)
                logo.animateTo(1f, tween(1800, easing = EaseOutExpo))
            }
            launch {
                val start = withFrameNanos { it }
                while (true) {
                    val now = withFrameNanos { it }
                    val s = (now - start) / 1e9f
                    if (s > 1.8f) textAt = s - 1.8f
                    if (s > LITE_SECONDS) break
                }
                onEnd()
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val glowSize = min(maxWidth.value, maxHeight.value).dp * .9f
        Box(
            Modifier
                .size(glowSize)
                .graphicsLayer { alpha = glowAlpha.value; scaleX = glowScale.value; scaleY = glowScale.value }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0f to Ergos.Violet.copy(alpha = .55f),
                        .45f to Ergos.Violet.copy(alpha = .12f),
                        .7f to Color.Transparent,
                    ),
                ),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
            val v = logo.value
            Image(
                painter = painterResource(R.drawable.logo_san_andres),
                contentDescription = "Iglesia Anglicana San Andrés",
                modifier = Modifier
                    .width(min(maxWidth.value * .64f, 320f).dp)
                    .graphicsLayer { alpha = v; scaleX = .92f + .08f * v; scaleY = .92f + .08f * v }
                    .blur((18f * (1f - v)).dp),
            )
            Box(Modifier.fillMaxWidth()) {
                if (textAt >= 0f) IntroTitle(textAt)
            }
        }
    }
}
