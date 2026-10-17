# Dones · "Descubriendo Mi Ministerio"

App Android que acompaña el curso **Descubriendo Mi Ministerio** y calcula la **curva de dones** con el Cuestionario Houts modificado por Wagner (125 afirmaciones, 25 dones).

El plan completo (flujos, wireframes, arquitectura, puntaje) está en [`docs/plan-app-curva-houts.md`](docs/plan-app-curva-houts.md).

## Estado: demo núcleo (fase 1 del plan)

Funciona **sin internet ni servidor**. No hay que esperar al maestro: cada persona avanza sola, página por página, siguiendo la clase.

Flujo del demo:

1. **Escribe el código** que ves en la pantalla (o léelo con la cámara). Usa `000000`.
2. Conectado → para qué pedimos tus datos → **nombre, edad, estado civil, WhatsApp** (uno por pantalla) → revisar → **consentimiento**.
3. **La enseñanza**, una idea por página (secciones I a V del manual: bienvenida, la Biblia, preparación personal, cómo me formó Dios y el CORTE): texto, versículos, **completar espacios** (toca *Revisar* y ve la respuesta), preguntas abiertas y opciones para marcar (latidos del corazón, rasgos, talentos, áreas de ministerio). Botones *Siguiente ›* y *‹ Anterior*.
4. Al llegar a su lugar en la enseñanza aparece cada **bloque de 25 afirmaciones**, una por pantalla (Mucho / Algo / Poco / Nada). Si la persona está casada, no ve los 5 ítems de celibato (120 en total).
5. Después del último bloque: **Resultados** (top 3 de dones, curva completa, detalle de cada don, ministerios sugeridos) y el **certificado de participación**.
6. **Mis documentos**: PDF "Mis dones" y PDF "Certificado", guardados en el teléfono, con el botón **Enviarme por WhatsApp**.

Todo se guarda en el teléfono en cada toque: si se cierra la app, vuelve a la misma página. En la pantalla de resultados hay un enlace **Reiniciar demo** que borra todo.

La enseñanza está en `app/src/main/assets/leccion-v1.json`: cada página es un objeto; `"___"` marca un espacio y su respuesta va en `"blanks"`; `{"houtsBlock": 3}` es el lugar donde aparece el bloque 3. Se puede editar o reordenar sin tocar código.

## Cómo abrirlo en Android Studio

1. Android Studio → **File › Open** → elegir la carpeta del repositorio.
2. Esperar a que termine la sincronización de Gradle (la primera vez descarga dependencias). Si Android Studio ofrece actualizar versiones de AGP o librerías, se puede aceptar.
3. Conectar un teléfono Android (8.0 o superior) con depuración USB, o crear un emulador.
4. Botón **Run ▶** con la configuración `app`.

Para probar el lector de QR, genera un QR con el texto `000000` (cualquier generador sirve) y muéstralo en la pantalla de tu PC.

## Estructura

| Carpeta | Qué hay |
|---|---|
| `core/` | Lógica en Kotlin puro, sin Android: lectura del cuestionario, **puntaje**, ranking con empates, ministerios sugeridos, clase simulada, qué pantalla toca según el paso del maestro, indicadores de calidad, código del certificado. Tiene pruebas automáticas. |
| `app/` | App Android (Kotlin + Jetpack Compose): pantallas, cámara/QR (CameraX + ML Kit), PDFs, envío por WhatsApp. |
| `app/src/main/assets/leccion-v1.json` | La enseñanza del curso, una idea por página, con los bloques del cuestionario intercalados. |
| `app/src/main/assets/houts-v1.json` | El cuestionario: 125 ítems, 5 bloques, clave de puntaje, 25 dones, ministerios. Se puede editar sin tocar código. |
| `docs/` | Plan de la app y cuestionario original limpio. |

## Puntaje

- Mucho = 3, Algo = 2, Poco = 1, Nada = 0.
- Don del ítem *n*: fila `(n − 1) mod 25` → A (Profecía) … Y (Servicio).
- Puntaje del don = suma de sus ítems (0 a 15). Se ordena por porcentaje sobre lo respondido.
- Dominantes = puestos 1-3; subordinados = 4-6. Los empates comparten puesto.
- Casados/as: el don Celibato queda fuera del ranking.

## Pruebas

```bash
./gradlew :core:test
```

## Lo que falta (siguientes fases del plan)

- Clase en vivo real: consola web del maestro + Firebase (reemplaza al maestro simulado).
- Confirmar con el maestro las respuestas de "10 verdades de los dones" (el manual del alumno las trae en blanco).
- Llevar lo que la persona marca y escribe en el CORTE a la hoja "Mi Perfil Personal" del PDF.
- Envío automático por WhatsApp con la API oficial de WhatsApp Business.
- Guardar copia de los PDFs en la carpeta Descargas del teléfono.
- Diseño gráfico definitivo (por ahora es solo funcional, con letra grande).
- Licencia del cuestionario original antes de publicar en Google Play.
