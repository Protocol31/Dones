# Plan preliminar: App "Descubriendo Mi Ministerio" (Curva de Houts)

*Versión 0.2, 17 de octubre de 2026. Solo funcionalidad, flujos y wireframes. Sin diseño gráfico.*
*Fuentes convertidas a Markdown en `houts-app/fuentes/` (01 a 06). Contenido del cuestionario en `houts-app/contenido/houts-v1.json`.*

### Cambios de la v0.2 (pedidos por David)
1. **Sin correo.** Los resultados llegan **por WhatsApp** y quedan **guardados en el teléfono como PDF**.
2. **Certificado de participación** en PDF al terminar el curso.
3. **Sin pantalla "¿Cómo vas a usar la app?"**: la app abre directo en **"Escribe el código que ves en la pantalla"**, con botón para **leer el QR con la cámara**. No hay modo "por mi cuenta".
4. Al conectarse a la clase, la app pide los datos uno por uno: **nombre, edad, estado civil y WhatsApp**, explicando para qué se usa cada uno. El estado civil decide si aparecen las preguntas de celibato/sexualidad.

---

## 0. Resumen en un minuto

- **Qué es:** una app Android que acompaña la clase "Descubriendo Mi Ministerio". El participante escanea el QR de la pantalla del maestro y queda conectado a la clase. El maestro avanza desde su PC y la app de cada participante muestra el paso equivalente: contenido breve, preguntas para completar y bloques del cuestionario Houts. Al final entrega el **resultado: 1.º, 2.º y 3.er don**, la curva completa de 25 dones y el Perfil CORTE, en PDF guardado en el teléfono y enviado por WhatsApp, junto con un **certificado de participación**.
- **Lo más importante (el resultado)** se puede construir y probar **sin internet ni servidor**, usando un **código de prueba** que simula la clase. Por eso el demo empieza ahí.
- **Mejoras clave que propongo** (sección 1.2): que el resultado sea el **Perfil CORTE completo** y no solo el Houts; una **versión 2 del cuestionario** con técnicas anti-sesgo y un pequeño **"testigo"** (alguien que te conoce) que confirma; y que la **presentación del maestro sea una página web** en vez del PowerPoint, para que la sincronización sea simple.
- **Cuatro realidades que hay que saber antes** (sección 2): Google Play no es inmediato para cuentas nuevas; el cuestionario original tiene derechos de autor; las creencias religiosas son datos sensibles; WhatsApp automático requiere la API oficial de Meta.

---

## 1. Evaluación de la idea

### 1.1 Lo que está muy bien

- El usuario objetivo (40-65 años) está claro, y la prioridad (el resultado) también.
- Vincular la app con el ritmo de la clase es una buena idea: evita que la gente se adelante, se pierda o responda todo de golpe al final, cansada.
- Iterar con el equipo sobre flujos y wireframes antes del diseño es el orden correcto.
- No exigir cuenta/contraseña es correcto para este público.

### 1.2 Lo que mejoraría (la "mejor opción")

| # | Idea original | Propuesta | Por qué |
|---|---|---|---|
| 1 | Resultado = curva de Houts | Resultado = **Perfil CORTE** (Corazón, Obsequios, Rasgos, Talentos, Experiencias) con el Houts como sección "Obsequios" + **sugerencia de ministerios** de la iglesia | El curso entero lleva a eso (manual y hoja "Mi Perfil Personal"). El Houts solo es 1 de 5 factores. Termina en una acción concreta: "¿dónde sirvo?" |
| 2 | Mejorar preguntas para que no puedan mentir | **Cuestionario v2** con escenarios, elección forzada, frecuencia de conducta, preguntas de control + **confirmación de un testigo** | Ningún test puede impedir mentir; sí puede **reducir** el sesgo y **detectarlo**. El propio PDF dice que los dones deben ser confirmados por otros |
| 3 | Test "psicoanalítico" | Test **psicométrico** | Psicoanálisis es otra cosa (interpretación clínica). Lo que buscamos es medición confiable: consistencia, validez y control de sesgos |
| 4 | El maestro avanza "en la pantalla en vivo" (PowerPoint) | El maestro usa una **consola web** que proyecta las diapositivas y controla la clase | Engancharse a PowerPoint es frágil. Si las diapositivas viven en la web, "siguiente" mueve la pantalla y las apps a la vez |
| 5 | 125 preguntas seguidas | **5 bloques de 25** (cada bloque trae 1 ítem de cada don), repartidos en la clase | Menos fatiga. Si alguien abandona, los bloques terminados ya dan un resultado parcial equilibrado |
| 6 | Enviar por WhatsApp (v0.2: sin correo) | Demo: **PDF guardado + botón "Enviarme por WhatsApp"** (abre WhatsApp con el PDF listo). Producción: **envío automático con la API oficial de WhatsApp Business** | El envío automático exige cuenta verificada de Meta, un número dedicado, plantillas aprobadas y un pequeño costo por mensaje. Al quitar el correo, esto pasa a ser prioridad |
| 7 | "Subir al toque" a la tienda | Plan de publicación con **prueba cerrada** (sección 2.1) | Requisito de Google para cuentas personales nuevas |
| 8 | Sin login | Sin login **visible**, pero con identificación **anónima invisible** (Firebase Anonymous Auth) | Hace falta para proteger los datos en el servidor sin pedir contraseña |

---

## 2. Realidades a resolver antes de publicar

### 2.1 Google Play (no es "App Store")
"App Store" es de Apple. Con Android Studio publicamos en **Google Play**.
- Cuenta de desarrollador: pago único de USD 25.
- **Cuentas personales nuevas:** Google exige una **prueba cerrada con al menos 12 testers durante 14 días seguidos** antes de poder publicar en producción. El equipo de la iglesia sirve justamente de testers.
- Alternativa: registrar la cuenta como **organización** (la iglesia), que no tiene ese requisito pero pide un número D-U-N-S.
- Requisitos: política de privacidad publicada en una URL, formulario "Seguridad de los datos", nivel de API objetivo vigente, clasificación de contenido.
- *Estas reglas cambian; las revisamos de nuevo al momento de publicar.*

### 2.2 Derechos de autor del cuestionario
El Cuestionario Houts Modificado por Wagner es material del Charles E. Fuller Institute (hoy lo comercializan terceros). Usarlo impreso dentro de un taller es una cosa; **publicarlo dentro de una app pública** es redistribución.
- Opción A: pedir licencia/permiso al titular actual.
- Opción B: usar el **cuestionario v2 propio** (sección 8) con ítems redactados por el equipo. Esto también resuelve el problema de sesgo.
- Para el **demo interno** podemos usar el original sin problema práctico; antes de publicar hay que decidir.

### 2.3 Datos personales sensibles
Nombre, edad, estado civil y WhatsApp son datos personales; un perfil de **dones espirituales revela creencias religiosas**, que la mayoría de las leyes (por ejemplo, la ley chilena de protección de datos, si están en Chile) tratan como **datos sensibles**.
- Cada dato se pide **con su motivo a la vista** (tabla 4.4) y hay una pantalla de **consentimiento explícito** (casilla no premarcada).
- Política de privacidad: qué guardamos, para qué, quién lo ve (¿el pastor?), cuánto tiempo, cómo pedir que se borre.
- Guardar lo mínimo: la edad se guarda como **tramo** (40-49, 50-59, 60-69…), no como fecha de nacimiento.
- Si alguien no quiere dar su WhatsApp: igual recibe el PDF en el teléfono, solo que no se le envía.

### 2.4 Contenido sensible del original
- La fila **Celibato** (ítems 22, 47, 72, 97, 122) presupone estar soltero, y el ítem 97 pregunta por deseos sexuales. **Decisión v0.2:** la app pregunta el estado civil al conectarse. Si la persona está **casada**, esos 5 ítems no aparecen y su curva se calcula sobre 24 dones (120 preguntas). Si está **soltera, viuda o divorciada/separada**, aparecen los 5.
- Filas carismáticas (lenguas, interpretación, sanidad, milagros, liberación): definir con el pastor si la iglesia las incluye.

---

## 3. Del curso a la app: mapa de contenido

El curso (fuentes 02, 03 y 04) se convierte en **pasos**. Cada paso tiene: contenido corto + (opcional) una actividad.

| Módulo | Contenido (fuente) | Actividad en la app |
|---|---|---|
| M0 Inicio | Código/QR → conexión a la clase → nombre, edad, estado civil, WhatsApp → consentimiento | Formulario (1 dato por pantalla) |
| M1 Bienvenida | Requisitos, propósito (Ef 4:11-12), principio, resultados, enfoque, metas | Completar espacios (ej.: "Los pastores son los que ___" → *equipan*) |
| M2 ¿Qué dice la Biblia? | Diakonos, a quién ministramos, necesidades, por qué ministramos | Completar espacios + 1 reflexión abierta |
| M3 Mi preparación personal (Ro 12:1-8) | 5 pasos: dedicar, eliminar, evaluar, cooperar, activar | Completar espacios + **Bloque Houts 1 (25 ítems)** |
| M4 ¿Cómo me ha formado Dios? | Formado con propósito, único, complejo; CORTE de Pablo | **Bloque Houts 2** |
| M5 C: Corazón | "Latidos" del corazón (15 motivaciones), mis logros | Elegir hasta 3 motivaciones + escribir 1 ejemplo · **Bloque Houts 3** |
| M6 O: Obsequios divinos | 10 verdades de los dones, precauciones | Completar espacios · **Bloque Houts 4 y 5** |
| M7 R: Rasgos | 4 pares (Extrovertido/Introvertido, etc.) | 4 deslizadores de 5 puntos |
| M8 T: Talentos | Lista de 25 habilidades | Marcar las mías |
| M9 E: Experiencias | Espirituales, dolorosas, educativas, ministeriales | 4 respuestas abiertas cortas (opcionales) |
| M10 Resultado | Top 3 + curva + Perfil CORTE + ministerios sugeridos | Ver; PDF de resultados guardado; envío por WhatsApp |
| M11 Cierre | Certificado de participación | PDF del certificado guardado y enviado por WhatsApp |

> La distribución de los bloques Houts es configurable desde el contenido (archivo JSON), sin tocar código.

---

## 4. Mapa de flujos

### 4.1 Flujo del participante

```
[Abrir app]
     │
     ▼
¿Ya estoy en una clase? ──Sí──► [Volver al paso actual de la clase]
     │No
     ▼
[ESCRIBE EL CÓDIGO QUE VES EN LA PANTALLA]  (6 dígitos)
     │            └──[ Usar la cámara para leer el QR ]──► [Cámara] ──┐
     │◄───────────────────────────────────────────────────────────────┘
     ▼
¿Código válido y clase abierta? ──No──► [Mensaje claro: "Revisa el código" / "La clase aún no empieza"]
     │Sí
     ▼
[Conectado a la clase ✓  "Antes de comenzar, unos datos"]
     ▼
[Para qué pedimos tus datos]  (1 pantalla, ver 4.4)
     ▼
[Nombre completo] ─► [Edad (tramo)] ─► [Estado civil] ─► [WhatsApp]   (1 dato por pantalla)
     ▼
[Consentimiento]  ──"No acepto"──► [Explicar que sin aceptar no podemos guardar ni enviar resultados; volver]
     │Acepto
     ▼
[Esperando al maestro…]   (o, si la clase ya empezó, directo al paso actual)
     ▼
[Paso N que indica el maestro] ◄────────(el maestro avanza)────────┐
     ├─ Contenido ─► Actividad ─► "Listo ✓" ───────────────────────┘
     └─ Bloque Houts (25 preguntas, 1 por pantalla; 24 si está casado/a)
     ▼
[El maestro abre "Resultados"]
     ▼
[¿Respondí todo?] ──No──► [Completar lo pendiente]
     │Sí
     ▼
[RESULTADO: tus 3 dones principales]
     ▼
[Ver curva completa] · [Mi Perfil CORTE] · [Dónde podría servir]
     ▼
[El maestro cierra el curso] ─► [CERTIFICADO de participación]
     ▼
PDF de resultados + PDF del certificado ─► guardados en el teléfono ─► enviados por WhatsApp
```

Reglas del modo clase:
- El participante **no puede adelantarse** al maestro, pero **sí puede volver** a pasos anteriores.
- Si pierde la conexión, la app queda en el último paso y sigue funcionando; al reconectar salta al paso actual.
- Si llega tarde, entra directo al paso actual y los pasos anteriores quedan marcados "pendiente".
- Todo se guarda en el teléfono en cada toque (cerrar la app no pierde nada).

### 4.2 Flujo del maestro (consola web en la PC)

```
[Abrir consola web] ─► [Crear clase: iglesia + nombre del maestro] ─► Se genera código 6 dígitos + QR
       ▼
[Pantalla proyectada: QR GRANDE + código, mientras la gente se conecta]
       ▼ (el maestro toca "Comenzar")
[Pantalla proyectada: diapositiva + código/QR pequeño en una esquina, para los que llegan tarde]
       ▼
[Panel del maestro (otra pestaña o laptop)]
   ├─ ◄ Anterior | Siguiente ►   (también con flechas del teclado o un control de presentaciones)
   ├─ Participantes conectados: 18
   ├─ Avance del paso actual: 12/18 listos
   └─ [Abrir bloque Houts] [Cerrar bloque] [Abrir resultados] [Cerrar curso y emitir certificados]
```

### 4.3 Flujo de PDFs y WhatsApp (sin correo)

```
[Resultado listo]                          [Curso terminado]
       ▼                                          ▼
 PDF "Mis dones" (en el teléfono)        PDF "Certificado" (en el teléfono)
       └──────────────┬───────────────────────────┘
                      ▼
      Guardados en Descargas/DescubriendoMiMinisterio
      y siempre visibles en la pantalla "Mis documentos" de la app
                      ▼
 ├─► Demo:        botón "Enviarme por WhatsApp" ─► abre WhatsApp con el PDF adjunto
 │                (la persona elige "Tú" o su propio chat y toca Enviar)
 └─► Producción:  la app sube los PDFs ─► función en el servidor ─► WhatsApp Business API
                  ─► mensaje plantilla "Hola María, aquí están tus resultados y tu certificado"
                     + los 2 documentos, al número que registró
```

Por qué así: en el demo no hace falta ninguna cuenta de Meta; en producción el envío es automático, como pediste, y el PDF igual queda en el teléfono por si el mensaje falla.

### 4.4 Para qué se usa cada dato (texto que verá el participante)

| Dato | Cómo se pide | Para qué (se muestra en pantalla) |
|---|---|---|
| Nombre completo | Texto libre | "Para escribirlo en tu certificado y en tus resultados." |
| Edad | Tramos: menos de 40 · 40-49 · 50-59 · 60-69 · 70 o más | "Para conocer mejor a los grupos que hacen el curso. No aparece en tu certificado." |
| Estado civil | Soltero/a · Casado/a · Viudo/a · Separado/a o divorciado/a | "Algunas preguntas solo aplican a personas sin pareja. Así no te mostramos preguntas que no son para ti." |
| WhatsApp | Código de país prellenado + número | "Para enviarte tus resultados y tu certificado." |

### 4.5 Certificado de participación
- Se genera cuando el maestro **cierra el curso** y la persona respondió el cuestionario completo (regla a confirmar: ¿basta con asistir?).
- Contenido: nombre completo, nombre del curso, iglesia, fecha, nombre del maestro, y un **código de verificación** corto.
- El certificado **no muestra los dones** (es de participación); los dones van en el PDF de resultados.
- Datos de la iglesia y del maestro: se configuran una vez en la consola web al crear la clase.

---

## 5. Wireframes (texto)

Convenciones: `[ Botón ]`, `( ) opción`, `▓` progreso. Toda pantalla tiene **una sola tarea**. Letra base grande, botones de ancho completo.

### W1 Inicio: escribir el código (primera pantalla de la app)
```
┌──────────────────────────────┐
│                              │
│  Escribe el código que ves   │
│  en la pantalla              │
│                              │
│   ┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐   │
│   │4 ││8 ││1 ││ _││  ││  │   │
│   └──┘└──┘└──┘└──┘└──┘└──┘   │
│                              │
│  ┌────────────────────────┐  │
│  │        Entrar          │  │
│  └────────────────────────┘  │
│                              │
│  ┌────────────────────────┐  │
│  │ 📷 Usar la cámara para │  │
│  │    leer el código QR   │  │
│  └────────────────────────┘  │
└──────────────────────────────┘
```
Teclado numérico grande. Con el 6.º dígito entra solo. La primera vez que se toca "Usar la cámara", Android pide permiso de cámara; antes la app explica en una línea para qué.

### W1b Cámara
```
┌──────────────────────────────┐
│ ‹ Volver                     │
│  Apunta a la pantalla        │
│  del maestro                 │
│   ┌──────────────────────┐   │
│   │                      │   │
│   │    [ vista cámara ]  │   │
│   │      ┌────────┐      │   │
│   │      │  QR    │      │   │
│   │      └────────┘      │   │
│   └──────────────────────┘   │
│  Se conecta solo al leerlo.  │
└──────────────────────────────┘
```

### W2 Conectado
```
┌──────────────────────────────┐
│                              │
│   Conectado a la clase ✓     │
│   Descubriendo Mi Ministerio │
│   Iglesia ______             │
│                              │
│   Antes de comenzar          │
│   necesitamos 4 datos.       │
│                              │
│  ┌────────────────────────┐  │
│  │       Continuar        │  │
│  └────────────────────────┘  │
└──────────────────────────────┘
```

### W2b Para qué son tus datos
```
┌──────────────────────────────┐
│  Para qué te pedimos datos   │
│                              │
│  • Tu nombre va en tu        │
│    certificado.              │
│  • Tu WhatsApp, para         │
│    enviarte resultados y     │
│    certificado.              │
│  • Tu estado civil, para no  │
│    mostrarte preguntas que   │
│    no son para ti.           │
│  • Tu edad, para conocer a   │
│    los grupos del curso.     │
│  ┌────────────────────────┐  │
│  │       Entendido        │  │
│  └────────────────────────┘  │
└──────────────────────────────┘
```

### W2c Datos (una pantalla por dato)
```
┌──────────────────────────────┐   ┌──────────────────────────────┐
│  Dato 1 de 4                 │   │  Dato 2 de 4                 │
│  ¿Cuál es tu nombre          │   │  ¿Cuál es tu edad?           │
│  completo?                   │   │  ┌────────────────────────┐  │
│  Así aparecerá en tu         │   │  │ Menos de 40            │  │
│  certificado.                │   │  ├────────────────────────┤  │
│  ┌────────────────────────┐  │   │  │ 40 a 49                │  │
│  │ María González Pérez   │  │   │  ├────────────────────────┤  │
│  └────────────────────────┘  │   │  │ 50 a 59                │  │
│  ┌────────────────────────┐  │   │  ├────────────────────────┤  │
│  │       Continuar        │  │   │  │ 60 a 69                │  │
│  └────────────────────────┘  │   │  ├────────────────────────┤  │
│  ‹ Volver                    │   │  │ 70 o más               │  │
└──────────────────────────────┘   │  └────────────────────────┘  │
                                   └──────────────────────────────┘
┌──────────────────────────────┐   ┌──────────────────────────────┐
│  Dato 3 de 4                 │   │  Dato 4 de 4                 │
│  ¿Cuál es tu estado civil?   │   │  ¿Cuál es tu WhatsApp?       │
│  ┌────────────────────────┐  │   │  ┌─────┐┌─────────────────┐  │
│  │ Soltero/a              │  │   │  │ +56 ││ 9 1234 5678     │  │
│  ├────────────────────────┤  │   │  └─────┘└─────────────────┘  │
│  │ Casado/a               │  │   │  Ahí te llegarán tus         │
│  ├────────────────────────┤  │   │  resultados y tu certificado.│
│  │ Viudo/a                │  │   │  ┌────────────────────────┐  │
│  ├────────────────────────┤  │   │  │       Continuar        │  │
│  │ Separado/a o           │  │   │  └────────────────────────┘  │
│  │ divorciado/a           │  │   │  Prefiero no darlo ›         │
│  └────────────────────────┘  │   └──────────────────────────────┘
└──────────────────────────────┘
```
Al final, una pantalla de resumen ("¿Están bien tus datos?") para corregir el nombre antes del certificado.

### W3 Consentimiento
```
┌──────────────────────────────┐
│  Cuidamos tus datos          │
│                              │
│  Tus respuestas tratan sobre │
│  tu fe. Solo las usamos para │
│  darte tus resultados y tu   │
│  certificado.                │
│  [ Leer la política ]        │
│                              │
│  [☐] Acepto                  │
│                              │
│  ┌────────────────────────┐  │
│  │       Comenzar         │  │
│  └────────────────────────┘  │
└──────────────────────────────┘
```

### W5 Esperando al maestro
```
┌──────────────────────────────┐
│                              │
│   Ya estás en la clase ✓     │
│                              │
│   Espera a que el maestro    │
│   comience.                  │
│                              │
│   (indicador animado)        │
└──────────────────────────────┘
```

### W6 Paso de lección (contenido)
```
┌──────────────────────────────┐
│ ▓▓▓▓░░░░░░  Bienvenida  2/7  │
│                              │
│  El Propósito                │
│  Efesios 4:11-12             │
│                              │
│  Los pastores son los que    │
│  equipan.                    │
│  Los miembros son los que    │
│  ministran.                  │
│                              │
│  ┌────────────────────────┐  │
│  │      Entendido ✓       │  │
│  └────────────────────────┘  │
│  ‹ Anterior                  │
└──────────────────────────────┘
```
En modo clase no hay botón "Siguiente": avanza el maestro.

### W7 Completar el espacio (pregunta sobre lo estudiado)
```
┌──────────────────────────────┐
│ ▓▓▓▓░░░░░░  Bienvenida  3/7  │
│                              │
│  Mi ministerio está          │
│  determinado por…            │
│                              │
│  ┌────────────────────────┐  │
│  │ mi propia forma de ser │  │
│  └────────────────────────┘  │
│  ┌────────────────────────┐  │
│  │ lo que la iglesia      │  │
│  │ necesita               │  │
│  └────────────────────────┘  │
│  ┌────────────────────────┐  │
│  │ mi edad                │  │
│  └────────────────────────┘  │
└──────────────────────────────┘
 Al tocar: la correcta se marca ✓ y se muestra
 la frase completa. Sin puntaje, sin "error" rojo.
```

### W8 Pregunta Houts (escala, original)
```
┌──────────────────────────────┐
│ Bloque 2 de 5   ▓▓▓▓▓░░ 14/25│
│                              │
│  "He animado verbalmente a   │
│  los indecisos, afligidos    │
│  o desanimados."             │
│                              │
│  ¿Qué tan cierto es en       │
│  tu vida?                    │
│  ┌────────────────────────┐  │
│  │ Mucho                  │  │
│  ├────────────────────────┤  │
│  │ Algo                   │  │
│  ├────────────────────────┤  │
│  │ Poco                   │  │
│  ├────────────────────────┤  │
│  │ Nada                   │  │
│  └────────────────────────┘  │
│  ‹ Anterior                  │
└──────────────────────────────┘
 Al tocar: se marca, pausa breve, pasa sola a la siguiente.
```

### W9 Pregunta de elección forzada (v2)
```
┌──────────────────────────────┐
│ Bloque 2 de 5   ▓▓▓▓▓░░ 15/25│
│                              │
│  Tienes un sábado libre en   │
│  la iglesia. ¿Qué preferirías│
│  hacer?                      │
│                              │
│  ┌────────────────────────┐  │
│  │ Organizar la logística │  │
│  │ del evento             │  │
│  └────────────────────────┘  │
│  ┌────────────────────────┐  │
│  │ Visitar a un hermano   │  │
│  │ enfermo                │  │
│  └────────────────────────┘  │
└──────────────────────────────┘
```

### W10 Fin de bloque
```
┌──────────────────────────────┐
│                              │
│   ¡Bloque 2 terminado! ✓     │
│   Llevas 50 de 125.          │
│                              │
│   Espera al maestro para     │
│   continuar.                 │
└──────────────────────────────┘
```

### W11 Resultado: tus 3 dones (la pantalla más importante)
```
┌──────────────────────────────┐
│  María, estos son tus        │
│  dones principales           │
│                              │
│  ┌────────────────────────┐  │
│  │ 1  Misericordia   13/15│  │
│  │    Ver qué significa › │  │
│  └────────────────────────┘  │
│  ┌────────────────────────┐  │
│  │ 2  Hospitalidad   12/15│  │
│  └────────────────────────┘  │
│  ┌────────────────────────┐  │
│  │ 3  Ayuda          11/15│  │
│  └────────────────────────┘  │
│                              │
│  Es un punto de partida:     │
│  confírmalo con otros.       │
│                              │
│  [ Ver mi curva completa ]   │
│  [ Mi Perfil CORTE ]         │
│  [ Dónde podría servir ]     │
│  [ Mis documentos ]          │
└──────────────────────────────┘
```

### W12 Curva completa (lista de barras, ordenada)
```
┌──────────────────────────────┐
│  Tu curva de dones           │
│                              │
│  Dominantes                  │
│  Misericordia  ████████████▌ │
│  Hospitalidad  ███████████▌  │
│  Ayuda         ██████████▌   │
│  Subordinados                │
│  Servicio      █████████     │
│  Exhortación   ████████▌     │
│  Intercesión   ████████      │
│  Otros                       │
│  Dar           ██████        │
│  …  (25 en total)            │
│  [ Mostrar todos ]           │
└──────────────────────────────┘
```
Barras horizontales (no "curva" de línea): más fáciles de leer en un teléfono y para este público.

### W13 Detalle de un don
```
┌──────────────────────────────┐
│ ‹ Misericordia               │
│                              │
│  Sentir compasión genuina    │
│  por quien sufre y ayudarle  │
│  con alegría.                │
│                              │
│  Romanos 12:8 · Lucas 10:33  │
│                              │
│  Dónde podrías servir:       │
│  • Visitas a hospitales      │
│  • Visitas a los visitantes  │
└──────────────────────────────┘
```

### W14 Mis documentos (reemplaza "Compartir / enviar")
```
┌──────────────────────────────┐
│  Mis documentos              │
│                              │
│  ┌────────────────────────┐  │
│  │ 📄 Mis dones (PDF)     │  │
│  │    Abrir               │  │
│  ├────────────────────────┤  │
│  │ 📄 Certificado (PDF)   │  │
│  │    Abrir               │  │
│  └────────────────────────┘  │
│                              │
│  ✓ Enviados a tu WhatsApp    │
│    +56 9 1234 5678           │
│                              │
│  [ Enviarme por WhatsApp ]   │
│   (en el demo: abre WhatsApp │
│    con los PDFs adjuntos)    │
└──────────────────────────────┘
```

### W16 Certificado (contenido del PDF, sin diseño)
```
┌────────────────────────────────────────────┐
│          CERTIFICADO DE PARTICIPACIÓN      │
│                                            │
│   Se certifica que                         │
│          MARÍA GONZÁLEZ PÉREZ              │
│   participó en el curso                    │
│          "Descubriendo Mi Ministerio"      │
│                                            │
│   Iglesia: __________                      │
│   Fecha: 17 de octubre de 2026             │
│                                            │
│   ____________________                     │
│   Nombre del maestro                       │
│                                            │
│   Código de verificación: DMM-4819-7K2Q    │
└────────────────────────────────────────────┘
```

### W15 Consola del maestro (web, PC)
```
┌───────────────────────────────────────────────────────────┐
│ Clase: Descubriendo Mi Ministerio     Código: 481 902 [QR] │
├───────────────────────────────────────┬───────────────────┤
│                                       │ Conectados: 18    │
│   (diapositiva actual, grande,        │ Listos: 12/18     │
│    para proyectar)                    │                   │
│                                       │ Paso 9 de 42      │
│                                       │ M3 · Paso 2       │
│                                       │                   │
│                                       │ [Abrir bloque     │
│                                       │  Houts 1]         │
│                                       │ [Mostrar QR grande│
│                                       │  para conectarse] │
│                                       │ [Abrir resultados]│
│                                       │ [Cerrar curso y   │
│                                       │  emitir certific.]│
├───────────────────────────────────────┴───────────────────┤
│   ◄ Anterior                                Siguiente ►   │
└───────────────────────────────────────────────────────────┘
 Botón "Proyectar" abre solo la diapositiva a pantalla completa
 en una segunda ventana; el panel queda en la laptop del maestro.
 Al iniciar la clase, la pantalla proyectada muestra el QR grande y el
 código de 6 dígitos hasta que el maestro toca "Comenzar". Al crear la
 clase se escriben una vez el nombre de la iglesia y del maestro (van en el certificado).
```

---

## 6. Arquitectura

### 6.1 Componentes

```
 ┌──────────────────────┐        ┌──────────────────────────┐
 │ App Android          │        │ Consola del maestro (web)│
 │ Kotlin + Compose     │        │ HTML/JS o Kotlin/JS       │
 │ Contenido JSON local │        │ Diapositivas en HTML      │
 │ Room (respuestas)    │        └────────────┬─────────────┘
 │ Puntaje en el equipo │                     │ escribe paso actual
 └──────────┬───────────┘                     ▼
            │ escucha paso actual   ┌──────────────────────────┐
            └──────────────────────►│ Firebase                 │
              sube resultado        │ • Firestore (clases)     │
                                    │ • Auth anónima           │
                                    │ • Cloud Functions        │
                                    │   (envío por WhatsApp)   │
                                    │ • Hosting (consola web)  │
                                    └──────────────────────────┘
```

- **App (Android Studio, Kotlin, Jetpack Compose, Material 3).** Arquitectura MVVM simple: `ui/` (pantallas), `domain/` (puntaje), `data/` (contenido JSON + Room + repositorio de clase).
- **El puntaje se calcula en el teléfono.** El resultado no depende de internet.
- **PDFs generados en el teléfono** (resultados y certificado) con la API de PDF de Android; se guardan en Descargas y en la pantalla "Mis documentos".
- **Lectura de QR:** CameraX + ML Kit (lector de códigos de Google, funciona sin internet).
- **WhatsApp:** demo con la función estándar de Android para compartir un archivo a WhatsApp; producción con WhatsApp Business Cloud API llamada desde una Cloud Function (nunca desde la app, para no exponer la clave).
- **Contenido como datos:** curso, ítems, clave de puntaje, definiciones y mapa de ministerios en archivos JSON dentro de la app. El equipo cambia preguntas sin tocar código. Más adelante pueden bajarse de Firestore para actualizar sin publicar otra versión.
- **Backend: Firebase** (plan gratuito alcanza para clases de iglesia). Supabase sería una alternativa equivalente.
- **Consola del maestro:** página web en Firebase Hosting; las diapositivas se rehacen como pasos HTML del mismo JSON del curso, así diapositiva y app nunca se desincronizan.

### 6.2 Modelo de contenido (JSON)

El archivo real ya existe: `contenido/houts-v1.json` (125 ítems, 5 bloques, 25 dones, ministerios, reglas de estado civil). Estructura del curso completo:

```json
{
  "course": {
    "id": "descubriendo-mi-ministerio",
    "version": 1,
    "modules": [
      {
        "id": "M1", "title": "Bienvenida",
        "steps": [
          { "id": "M1-S2", "type": "content",
            "title": "El Propósito", "ref": "Efesios 4:11-12",
            "body": ["Los pastores son los que equipan.", "Los miembros son los que ministran."] },
          { "id": "M1-S3", "type": "fill_blank",
            "prompt": "Mi ministerio está determinado por…",
            "options": ["mi propia forma de ser", "lo que la iglesia necesita", "mi edad"],
            "answer": 0 },
          { "id": "M3-H1", "type": "houts_block", "block": 1 }
        ]
      }
    ]
  },
  "houts": "ver contenido/houts-v1.json"
}
```

### 6.3 Datos en Firestore

```
classes/{code}
  courseId, courseVersion, currentStepId, status (waiting|live|ended),
  teacherUid, churchName, teacherName, createdAt
classes/{code}/participants/{anonUid}
  displayName, currentStepId, doneSteps[], lastSeen
results/{anonUid}
  name, ageRange, maritalStatus, whatsapp, consentAt, houtsScores{A..Y},
  top3[], profile{…}, quality{flags}, certificateCode, whatsappSentAt, createdAt
```
Reglas de seguridad: el participante solo lee su clase y escribe su propio documento; solo el maestro escribe `currentStepId`; nadie lee `results` ajenos (salvo el rol pastor, si se decide).

### 6.4 Sincronización en vivo
- La app se suscribe a `classes/{code}` (escucha en tiempo real de Firestore). Cuando cambia `currentStepId`, navega a ese paso.
- Firestore guarda en caché sin conexión: si el wifi de la iglesia falla, la app sigue y se pone al día al volver.
- Latencia esperada: menos de 1 segundo.

---

## 7. Modelo de puntaje

### 7.1 Original (Wagner-Houts)
- Respuesta: Mucho 3 · Algo 2 · Poco 1 · Nada 0.
- Don del ítem *n*: `fila = (n − 1) mod 25` → A..Y.
- Puntaje del don = suma de sus 5 ítems → **0 a 15**. Porcentaje = puntaje / 15.
- Orden descendente → **Dominantes = posiciones 1-3**, **Subordinados = 4-6**.

```kotlin
fun giftOf(n: Int): Char = 'A' + ((n - 1) % 25)

fun score(answers: Map<Int, Int>): List<GiftScore> =
    ('A'..'Y').map { g ->
        val items = (0 until 5).map { (g - 'A') + 1 + it * 25 }   // ej. A → 1,26,51,76,101
        val answered = items.mapNotNull { answers[it] }
        GiftScore(
            gift = g,
            raw = answered.sum(),
            max = answered.size * 3,
            pct = if (answered.isEmpty()) 0.0 else answered.sum() / (answered.size * 3.0)
        )
    }.sortedWith(compareByDescending<GiftScore> { it.pct }.thenBy { it.gift })
```
Usar porcentaje sobre ítems respondidos permite mostrar un **resultado parcial** con bloques incompletos.

**Estado civil (v0.2):** si la persona está casada, los ítems 22, 47, 72, 97 y 122 no se muestran y el don V (Celibato) queda **fuera del ranking** (no aparece con 0 %). Su curva tiene 24 dones.

### 7.2 Empates
Con 25 dones y puntajes de 0-15 los empates son frecuentes. Regla propuesta, en orden:
1. Mayor puntaje en los ítems de **evidencia** (lo que otros han visto / lo que ha ocurrido) frente a los de **deseo**.
2. Mayor puntaje en el bloque de **elección forzada** (v2).
3. Si sigue empatado, se muestran **juntos** ("2.º lugar compartido") en vez de inventar un orden.

### 7.3 Modelo v2 (cuando exista el cuestionario mejorado)
Por cada don se calculan tres sub-puntajes, todos llevados a 0-100:

| Componente | Qué mide | Peso sugerido |
|---|---|---|
| **E** Evidencia y conducta | Frecuencia real ("en los últimos 12 meses…"), testimonios de otros | 45 % |
| **F** Elección forzada / escenarios | Preferencia relativa entre dones igual de "deseables" | 35 % |
| **D** Deseo / inclinación | "Me gustaría…", "disfruto…" | 20 % |
| **T** Testigo (opcional) | Lo que una persona cercana confirma | Si existe, reemplaza 15 % repartido de E y D |

`Puntaje = 0.45·E + 0.35·F + 0.20·D` (y con testigo: `0.38·E + 0.35·F + 0.12·D + 0.15·T`).
Los pesos son **una hipótesis inicial**: se ajustan con los datos del piloto (sección 8.4).

Además, el resultado muestra la **brecha deseo vs. evidencia**: "Te atrae mucho la Enseñanza, pero todavía hay poca experiencia; es un área para probar." Así el resultado no castiga el deseo, pero tampoco lo confunde con un don comprobado.

### 7.4 Indicadores de calidad de la respuesta (no se muestran como "mentiste")
- **Respuestas en línea recta:** más de 20 respuestas seguidas iguales.
- **Velocidad:** respuestas en menos de ~1,5 segundos por ítem en promedio (no se leyó).
- **Inconsistencia:** pares de ítems casi iguales con respuesta muy distinta.
- **Deseabilidad social:** puntaje alto en la escala de control (sección 8.2).
- Si se activa alguno, el resultado dice con suavidad: "Tus respuestas muestran un patrón poco común; te sugerimos conversar el resultado con tu líder." Y el pastor lo ve marcado (si se acuerda ese rol).

### 7.5 Dones → ministerios (para "Dónde podría servir")
Propuesta inicial, **a validar con el pastor**:

| Ministerio (fuente 06) | Dones afines |
|---|---|
| Ujieres / Equipo de bienvenida | Hospitalidad, Servicio, Ayuda |
| Música / sonido / proyector / diapositivas | Servicio, Ayuda, Exhortación |
| Lector laico | Enseñanza, Exhortación |
| Escuela dominical / Grupo de jóvenes / adolescentes | Enseñanza, Pastor, Exhortación |
| Sala cuna / Cuidar a los niños | Servicio, Ayuda, Misericordia |
| Grupo del altar / Flores / Poner las mesas / Limpieza | Servicio, Ayuda |
| Equipo de refrigerio / Cocineros | Hospitalidad, Servicio |
| Contar la ofrenda | Administración, Dar, Servicio |
| Líderes de mesa para Alfa | Evangelista, Hospitalidad, Pastor |
| Visitas a los visitantes | Evangelista, Hospitalidad, Exhortación |
| Visitas a hospitales y personas con discapacidad | Misericordia, Exhortación, Sanidad, Intercesión |
| Equipos de oración para liberación | Intercesión, Discernimiento, Liberación, Fe |
| Líderes de estudio bíblico | Enseñanza, Pastor, Conocimiento, Sabiduría |

Regla: sugerir los 3 ministerios con más coincidencias con los 6 primeros dones (dominantes pesan doble).

---

## 8. Cuestionario v2: cómo reducir el sesgo

### 8.1 Por qué el original es fácil de sesgar
- **Todas las afirmaciones son positivas y deseables** ("he llevado a otros a Cristo"). Nadie quiere marcar "Nada".
- **Mezcla deseo, gusto y evidencia** en la misma escala.
- **Orden cíclico:** cada 25 ítems vuelve el mismo don; una persona atenta puede notarlo.
- **No hay preguntas de control** ni forma de detectar respuestas al azar.
- Varios ítems **dependen del estado civil o de experiencias carismáticas** más que de un don.

### 8.2 Técnicas propuestas (con ejemplos)

**a) Frecuencia de conducta concreta** (en vez de "qué tan cierto es")
> Original 6: "He animado verbalmente a los indecisos…" (Mucho/Algo/Poco/Nada)
> v2: "En los **últimos 12 meses**, ¿cuántas veces buscaste a alguien desanimado **para animarlo**?"
> ( ) Ninguna ( ) 1-2 veces ( ) Varias veces ( ) Casi cada mes o más

**b) Elección forzada entre opciones igual de buenas** (no hay respuesta "más espiritual")
> "Hay una necesidad en la iglesia. ¿Qué te sale más natural?"
> ( ) Hacer un plan y repartir tareas *(Administración)*
> ( ) Ponerme a hacer lo que falta sin que me lo pidan *(Servicio)*
> Se arman ~30 pares que rotan entre los 25 dones; cada don aparece 2-3 veces.

**c) Escenarios (juicio situacional)**
> "Un hermano te cuenta que perdió el trabajo. ¿Qué haces primero?"
> ( ) Oro con él en ese momento *(Intercesión)*
> ( ) Le busco un texto bíblico que le hable a su situación *(Exhortación/Sabiduría)*
> ( ) Le ofrezco ayuda práctica: comida, contactos *(Misericordia/Ayuda)*
> ( ) Lo invito a comer a mi casa esta semana *(Hospitalidad)*

**d) Separar "deseo" de "evidencia"**
> Deseo: "Me gustaría enseñar la Biblia a un grupo." / Evidencia: "Alguien me ha dicho, sin que yo preguntara, que aprendió algo de la Biblia gracias a mí."

**e) Escala de control de deseabilidad social** (6-8 ítems tipo Marlowe-Crowne, mezclados en todo el test)
> "Nunca me he impacientado con nadie." · "Siempre escucho con atención, sin importar quién me hable."
> Casi nadie puede marcar "Mucho" con honestidad; muchos "Mucho" indican que la persona se está presentando idealizada.

**f) Pares de consistencia** (3-4 pares redactados distinto que miden lo mismo) y **1-2 ítems de atención** ("Para esta pregunta, toca 'Poco'").

**g) Orden aleatorio** dentro de cada bloque (manteniendo 1 ítem por don por bloque) y **sin mostrar el nombre del don** durante el test.

**h) Instrucción honesta al inicio:** "No hay respuestas buenas o malas. Un don que no tienes no te hace menos. Responde lo que *haces*, no lo que te gustaría hacer."

**i) Testigo (evaluación de 360° simplificada):** la persona invita a alguien que la conoce (cónyuge, amigo, líder) a responder **25 preguntas cortas** (una por don) por un enlace. Es la técnica que más reduce el auto-engaño y coincide con lo que pide el propio cuestionario y la hoja "Otros dicen que han visto estos dones en mí".

### 8.3 Advertencia importante
Cambiar ítems, escala o formato **altera la validez del instrumento original**: los puntajes v2 ya no son comparables con el Houts clásico ni con sus 50 años de uso. Además, el original tampoco tiene una validación científica sólida publicada. Por eso la v2 es un **instrumento nuevo** que conviene validar.

### 8.4 Cómo validar la v2 (piloto sencillo)
1. 30-50 personas de la iglesia responden **ambas versiones** (original y v2) en la app.
2. Medir:
   - **Coincidencia del top 3** entre versiones.
   - **Consistencia interna** por don (alfa de Cronbach ≥ 0,70 como meta).
   - **Test-retest:** repetir a las 3-4 semanas; el top 3 debería mantenerse.
   - **Acuerdo con el testigo** y con la opinión del pastor.
3. Ajustar ítems débiles y los pesos del modelo 7.3.
4. Lo ideal es que alguien con formación en psicometría revise los ítems; la app guarda los datos necesarios (anónimos) para hacerlo.

**Propuesta práctica:** el demo y el primer piloto usan el **original**; la v2 se redacta en paralelo y se prueba con el mismo grupo.

---

## 9. Legibilidad para personas de 40-65 años (reglas funcionales, no estéticas)

- **Una pregunta por pantalla.** Una idea por paso de lección (máx. ~40 palabras).
- Texto base de **20 sp o más**; títulos 26-28 sp. Respetar el tamaño de letra del sistema (no fijar tamaños).
- Botones de respuesta de **ancho completo y al menos 64 dp de alto**, separados entre sí.
- Contraste alto (WCAG AA mínimo, apuntar a AAA en texto).
- Nada depende solo del color: siempre texto o ícono además.
- Sin gestos ocultos (deslizar, mantener presionado). Todo con botones visibles; siempre un "‹ Volver".
- Progreso visible y con palabras: "Bloque 2 de 5 · 14 de 25".
- Avance automático tras responder, con una pausa breve y opción de corregir.
- Guardado automático en cada toque; nunca preguntar "¿guardar?".
- Compatible con TalkBack (lector de pantalla) y modo oscuro del sistema.
- Lenguaje cotidiano; versículos con referencia corta.
- Teclado numérico para el código y el WhatsApp; código de país prellenado.

---

## 10. Qué construir primero (orden recomendado, v0.2)

Con el cambio, la app **siempre empieza en el código de la clase**. Para no esperar al servidor, el demo usa un **código de prueba (000000)** que simula una clase dentro del teléfono: el equipo prueba todas las pantallas sin internet, avanzando con un botón de "maestro simulado".

| Fase | Entregable | Por qué en este orden | Depende de |
|---|---|---|---|
| **0 ✓** | Contenido del cuestionario en JSON (`contenido/houts-v1.json`): 125 ítems, bloques, clave, 25 definiciones, ministerios | Base de todo. **Ya está hecho**; el equipo puede revisarlo | Nada |
| **1. Demo núcleo** | App Android: pantalla de código + cámara QR → datos (nombre, edad, estado civil, WhatsApp) → consentimiento → 5 bloques Houts → **resultado top 3 + curva + detalle** → PDF de resultados y certificado guardados → "Enviarme por WhatsApp" | Es lo que más importa y no necesita servidor (código 000000) | Fase 0 |
| **2** | Prueba con 5-10 personas del equipo de 40-65 años | Validar legibilidad y flujo antes de seguir | Fase 1 |
| **3** | **Clase en vivo real**: consola web (QR grande, Siguiente, cerrar curso) + Firestore | Reemplaza el maestro simulado | Fase 1 |
| **4** | Contenido del curso dentro de la app (pasos, completar espacios, CORTE) | Lo que se ve entre bloque y bloque | Fase 3 |
| **5** | **WhatsApp automático** (Business Cloud API) | Al no haber correo, es el canal principal; el trámite con Meta toma tiempo, así que conviene **iniciarlo ya en paralelo** | Cuenta de Meta verificada |
| **6** | Cuestionario v2 + testigo + piloto | Corre en paralelo desde la fase 2 | Piloto |
| **7** | Cuenta de Play, política de privacidad, prueba cerrada (12 testers / 14 días), producción | Requisitos de Google | Fases 1-5 |

## 11. Decisiones para el equipo

1. ¿El curso se da en **una sola sesión** o en **varias**? (cambia cuántos bloques Houts van por sesión)
2. ¿Se incluyen las filas **carismáticas** (lenguas, sanidad, milagros, liberación), o se adaptan? (Celibato ya quedó resuelto con el estado civil)
3. ¿Quién puede ver los resultados además del participante (pastor/líderes)?
4. ¿Licencia del cuestionario original, o ir directo a la v2 propia para publicar?
5. ¿Cuenta de Google Play **personal** o de **organización** (iglesia)?
6. ¿Se confirma el mapa dones → ministerios de la sección 7.5?
7. Nombre definitivo de la app.
8. ¿El certificado exige terminar el cuestionario, o basta con asistir a la clase?
9. ¿Qué número de WhatsApp de la iglesia enviará los mensajes? (debe ser un número que no esté en uso en la app normal de WhatsApp)

---

## Anexo: archivos fuente

| Archivo | Contenido |
|---|---|
| `fuentes/01-cuestionario-houts-wagner.md` | 125 ítems limpios, clave de puntaje, 25 definiciones, Paso IV |
| `fuentes/02-manual-maestro-descubriendo-mi-ministerio.md` | Manual con respuestas del maestro |
| `fuentes/03-manual-alumno-descubriendo-mi-ministerio.md` | Hoja de trabajo del alumno + herramientas CORTE |
| `fuentes/04-diapositivas-descubriendo-mi-ministerio.md` | 20 diapositivas (versión con respuestas) |
| `fuentes/05-perfil-personal-ministerio.md` | Hoja final "Mi Perfil Personal" |
| `fuentes/06-menu-de-ministerios.md` | Lista de ministerios de la iglesia |
| `contenido/houts-v1.json` | Cuestionario listo para la app: ítems, bloques, clave, dones, ministerios |
