# Cadejo — Guardián de la Noche

Un puzzle-roguelite por turnos para Android. Controlás al **Cadejo blanco**
(perro espiritual del folklore guatemalteco) escoltando viajeros nocturnos por
tableros hexagonales, evitando al Cadejo negro y otros espíritus que se mueven con
patrones predecibles. Sesiones de 2–5 minutos, runs de 10 niveles procedurales,
meta-progresión roguelite.

> Estado: **todas las fases (1–6) completas** — juego jugable de punta a punta,
> con meta-progresión, modo diario, monetización, CI/CD y listo para preparar la
> publicación en Play. Ver el roadmap más abajo y `PUBLISHING.md`.

## Stack

- **Kotlin** + **Jetpack Compose** (tablero y animaciones sobre `Canvas`).
- **Clean Architecture / MVVM** en módulos Gradle separados.
- **Hilt** (DI), **Coroutines/Flow** (concurrencia).
- **Gradle Kotlin DSL** + version catalog (`gradle/libs.versions.toml`) +
  convention plugins en `build-logic`.
- minSdk **26**, targetSdk **35**, `applicationId` `gt.guardian.cadejo`.

## Arquitectura de módulos

```
:app  ──────────────► ensambla, Hilt root, MainActivity
  ├─ :feature:game ─► tablero, HUD, run (ViewModel + Compose)
  ├─ :feature:meta ─► tienda/progresión (stub, Fase 3)
  ├─ :feature:daily ► modo diario/leaderboard (stub, Fase 4)
  ├─ :core:ui ──────► CadejoTheme, paleta nocturna, render hexagonal en Canvas
  ├─ :core:data ────► implementaciones (persistencia, RNG del sistema, red)
  └─ :core:domain ──► Kotlin PURO: reglas del juego, game loop, generación
                      procedural, patrones de enemigos. Sin Android.
```

**Regla rectora:** toda la lógica de juego vive en `:core:domain` como Kotlin puro
y **determinista**. El mismo `seed` + la misma secuencia de acciones producen
exactamente el mismo resultado. Esto habilita: niveles reproducibles, el modo
diario compartido, y la validación server-side del leaderboard (el backend
re-ejecuta el run y recomputa el score, sin confiar en el cliente).

## Cómo compilar

Requisitos: JDK 17+ y el Android SDK (platform 35, build-tools 35).

```bash
# Copiá la plantilla y apuntá al SDK
cp local.properties.template local.properties
# editá sdk.dir=... en local.properties

./gradlew :app:assembleDebug      # genera app/build/outputs/apk/debug/app-debug.apk
./gradlew test                    # lógica de dominio (JVM, sin emulador)
./gradlew testDebugUnitTest       # tests de ViewModel (Turbine) y demás
```

`local.properties` y cualquier secreto/keystore están en `.gitignore` y **nunca**
se commitean (ver `local.properties.template`).

## Determinismo y tests

La lógica pura se prueba en la JVM (rápido, sin device). Cobertura de Fase 1:

| Área | Qué se verifica |
|------|-----------------|
| `Hex` | coords axiales, distancia, vecindad, disco |
| `SeededRng` | misma seed → misma secuencia; snapshot reanudable |
| `ChasePattern` | BFS rodea muros, se acerca, se queda quieto si está bloqueado |
| `GameEngine.reduce` | movimiento legal/ilegal, victoria, derrota, determinismo |
| `LevelGenerator` | misma seed → estado idéntico; tablero siempre resoluble |
| `GameViewModel` | estado inicial, movimiento, esperar, reiniciar (Turbine) |

## Roadmap

1. ✅ **Fase 1** — tablero hex + movimiento + un enemigo con patrón fijo.
2. ✅ **Fase 2** — escolta + habilidades + patrones + run de 10 niveles con puntaje.
3. ✅ **Fase 3** — meta-progresión (Room) + save firmado con HMAC/Keystore + tienda.
4. ✅ **Fase 4** — modo diario + backend Supabase (SQL + Edge Function que revalida).
5. ✅ **Fase 5** — AdMob (rewarded) + Play Billing v7 + UMP + navegación + ajustes.
6. ✅ **Fase 6** — lint (ktlint+detekt), CI/CD, firma release, privacidad y checklist Play.

## Documentos

- `PRIVACY.md` — borrador de política de privacidad (según lo que la app recolecta).
- `PUBLISHING.md` — checklist de publicación en Google Play + cómo medir rendimiento.
- `backend/README.md` — despliegue del backend del leaderboard y validación server-side.
- `.github/workflows/ci.yml` — CI: lint, tests y build de release firmado.

## Calidad

```bash
./gradlew ktlintCheck detekt   # estilo + análisis estático
./gradlew test testDebugUnitTest
./gradlew bundleRelease        # AAB firmado (con secrets configurados)
```
