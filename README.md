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
./gradlew :core:database:connectedDebugAndroidTest   # migrations de la base (sur le telephone)
./gradlew :core:data:connectedDebugAndroidTest       # apprentissage des SMS de bout en bout (sur le telephone)
./gradlew assembleDebug
# APK : app/build/outputs/apk/debug/app-debug.apk
```

## Installer sur le téléphone (Wi-Fi)
Une fois le téléphone associé (Options de développement > Débogage sans fil) :
```bash
tools/run_on_phone.sh            # retrouve tout seul le port du telephone
tools/run_on_phone.sh IP:PORT    # ou en le donnant
```

## Lecture des SMS (ordre)
1. Règles exactes (`MvolaSmsParser`), formats connus.
2. Modèles appris des confirmations de l'utilisateur (`SmsTemplates`).
3. Lecteur universel (`GenericSmsReader`).

Les niveaux 2 et 3 ne sont acceptés que si le solde du SMS colle (`SmartSmsInterpreter`), sinon le SMS va dans « À vérifier ».
Quand on change la lecture : augmenter `MvolaSmsParser.VERSION`, l'app relit tout l'historique une fois.

## Ajouter une icône
```bash
python3 tools/material_symbol.py nom_icone
```
