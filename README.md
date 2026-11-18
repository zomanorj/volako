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
| `:core:model` | Types de base (Kotlin pur) |
| `:core:sms` | Lecture des SMS MVola (Kotlin pur) |
| `:core:domain` | Règles métier : SMS → opération, soldes, résumé du mois (Kotlin pur) |
| `:core:database` | Base locale Room (schémas dans `core/database/schemas`) |
| `:core:data` | Dépôt : seul point d'accès aux données pour l'app |

## Compiler
```bash
export JAVA_HOME=~/devtools/jdk17
./gradlew test          # tests rapides
./gradlew assembleDebug
# APK : app/build/outputs/apk/debug/app-debug.apk
```

## Installer sur le téléphone (Wi-Fi)
Une fois le téléphone associé (Options de développement > Débogage sans fil) :
```bash
tools/run_on_phone.sh 192.168.x.x:PORT
```

## Ajouter une icône
```bash
python3 tools/material_symbol.py nom_icone
```
