# TabletopKit · Board Game Toolbox

[English](README.en.md) · [中文](README.md)

A minimal board-game companion app built with Jetpack Compose. Before a game night begins, it helps you settle three pre-game chores in seconds: who goes first, what order everyone plays in, and how to split into teams.

> Inspired by the WeChat mini-program "左游助手". The core interaction: pass the phone around, each player taps the screen once to register, and after a few seconds of no input the app automatically finishes registration — no one needs to babysit it.

## Features

| Tool | Purpose |
| --- | --- |
| **First Player** | Randomly picks a starting player, with a ring-expansion + erase animation |
| **Player Order** | Randomly orders all players, dots labeled with seat numbers |
| **Team Splitter** | Splits players into the requested number of teams as evenly as possible |

All three tools share the same registration core: players tap the screen to register → an idle timeout (default 3 s, adjustable in Settings) ends registration automatically → the app runs the random logic and plays an animation.

## Tech Stack

- **Language / UI**: Kotlin + Jetpack Compose (Material 3)
- **Architecture**: single Activity + ViewModel + StateFlow, layered into `ui` / `vm` / `data` / `domain` / `core`
- **Persistence**: Jetpack DataStore Preferences (settings such as timeout, dot radius, theme mode)
- **Theming**: system / light / dark, optional dynamic color (Android 12+)

| Item | Value |
| --- | --- |
| Package | `com.ankopala.tabletopkit` |
| minSdk | 24 (Android 7.0) |
| targetSdk | 37 |
| versionName | 1.0 |
| Kotlin | 2.2.x (bundled with AGP) |
| AGP | 9.4.x |

## Build

```bash
# Point JAVA_HOME at the JDK bundled with Android Studio
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Debug build
./gradlew assembleDebug

# Release build (requires signing config first, see below)
./gradlew assembleRelease
```

### Release Signing

Signing files are never committed (see `.gitignore`). Keep a keystore locally, configure signing in the `release` block of `app/build.gradle.kts`, and you can produce an installable release APK. R8 minification only applies to `androidx.**` / `kotlin.**` / `kotlinx.**`; app code is not obfuscated.

## Project Structure

```
app/src/main/java/com/ankopala/tabletopkit/
├── core/          # cross-screen constants (e.g. player color palette)
├── data/          # data models + DataStore settings repository
├── domain/        # pure logic: shuffling, color assignment, team balancing
├── vm/            # main ViewModel
└── ui/
    ├── firstplayer/   # First Player
    ├── playerorder/   # Player Order
    ├── teamsplitter/  # Team Splitter
    ├── registration/  # shared registration core
    ├── settings/      # settings screen
    ├── home/          # home screen
    └── theme/         # theme & colors
```

## Design Notes

- **Animation model**: result screens use a "concentric ring band + erase/hole-punch mask" — on expansion it erases other players' dots in the winner's color, on retraction it punches out a background-colored outline, with a continuous breathing effect.
- **Seat number orientation**: number heads always point toward the screen center, rotating with angle.
- **Adjustable parameters**: dot radius (40–56 dp) and registration idle timeout, both configurable in Settings.

## License

Personal learning project.
