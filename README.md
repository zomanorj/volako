# Mon Argent

App Android pour suivre son argent MVola et espèces. Les SMS MVola sont lus automatiquement.

## Stack
- Kotlin, Jetpack Compose, Material 3
- AGP 9 (Kotlin intégré), Gradle 9, catalogue de versions (`gradle/libs.versions.toml`)
- Police Inter, icônes Material Symbols (`tools/material_symbol.py`)

## Modules
| Module | Rôle |
|---|---|
| `:app` | Point d'entrée, navigation |
| `:core:designsystem` | Thème, couleurs, typo, composants partagés |

## Compiler
```bash
export JAVA_HOME=~/devtools/jdk17
./gradlew assembleDebug
# APK : app/build/outputs/apk/debug/app-debug.apk
```

## Ajouter une icône
```bash
python3 tools/material_symbol.py nom_icone
```
