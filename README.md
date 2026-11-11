# 🎵 Memorizer — Local Audio & Memorization Loop Player

[![Build & Release Signed Debug APK](https://github.com/KG6K/Memorizer/actions/workflows/android-debug-release.yml/badge.svg)](https://github.com/KG6K/Memorizer/actions/workflows/android-debug-release.yml)
[![Latest Release](https://img.shields.io/github/v/release/KG6K/Memorizer?include_prereleases&label=Latest%20Release)](https://github.com/KG6K/Memorizer/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-API%2023%2B-green.svg)](https://developer.android.com)

**Memorizer** is a modern, high-performance offline Android audio player built specifically for **rote memorization, rehearsal, language learning, and precision listening**. Whether you are memorizing religious scriptures (Qur'an, Bible verses, hymns), poetry, dramatic monologues, vocabulary audiobooks, or musical sections, Memorizer gives you granular control over repetitions, intervals, and playback queues.

Built with **Jetpack Compose** and **Material Design 3**, Memorizer offers an intuitive, responsive interface tailored for effortless focus.

---

## ✨ Key Features

### 🔁 Advanced Memorization Looper
- **Repetition Multipliers**: Repeat any track or section 1x, 2x, 3x, 5x, 10x, or infinitely.
- **Inter-Loop Delay (Pause Countdown)**: Insert a customizable silent pause (e.g., 3s, 5s, 10s) between each repetition to recite or recall before hearing it again.
- **Echo Mode (Match Duration)**: Automatically matches the silent delay to the exact duration of the track or selected section, giving you a 1:1 window to repeat the audio back out loud.
- **A–B Section Looping**: Pinpoint exact timestamps within a track to practice difficult phrases without replaying the entire file.
- **Speed Pitch & Tempo Control**: Slow down complex phrases to 0.5x–0.8x or speed up review to 1.25x–2.0x.

### 📑 Multi-Select & Range Selection
- **Intuitive Long-Press Range Gestures**: Long-press a song to start selection mode, then long-press any second song in **Folders**, **Playlists**, or **Albums** to automatically select every song in between.
- **Floating Selection Bar**: View live selection count, tap **Select All**, clear selection, or tap **Play** to immediately spin up a dedicated playback queue for the selected tracks.
- **Clean Badging**: Selected items highlight with distinct Material 3 check badges and elevation.

### 📁 Library & File Management
- **Folder Scanner & Hierarchical Explorer**: Organize your local audio files by directory structure.
- **Playlists & Albums**: Create and edit custom playlists, or browse by album and artist metadata.
- **Recent Playback Tracking**: Pick up right where you left off with full playback history.
- **Large Artwork / High-Contrast Thumbnails**: Modern dark-themed music artwork with dynamic fallbacks when embedded album art is unavailable.

### 💖 Creator Support & Open Source
- Support the independent development of Memorizer directly within the app via **UPI**:
  - **UPI ID**: `ansariusman@ptyes`
  - Integrated copy button and one-tap launch into installed UPI payment apps (GPay, PhonePe, Paytm, etc.).

---

## 📱 Screenshots & UI Highlights

- **Studio Player**: Focused playback controls, repetition pills, countdown timer badge, A-B scrubber, and volume/seek controls.
- **Library Tabs**: Tabs for *Songs*, *Playlists*, *Folders*, and *Artists/Albums*.
- **Multi-Selection Mode**: Contextual top bar with counter, Select All, and instantaneous queue play.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.1
- **UI Toolkit**: Jetpack Compose with Material Design 3 (M3)
- **Audio Engine**: Android `MediaPlayer` / Media3 architecture
- **Architecture**: MVVM with Kotlin Coroutines (`StateFlow`, `SharedFlow`)
- **Compatibility**:
  - `minSdk`: **23** (Android 6.0 Marshmallow)
  - `targetSdk`: **36** (Android 16 Baklava)
  - `compileSdk`: **36.1**

---

## 📥 Download & Install

Download the latest pre-built APK directly from the [Releases](https://github.com/KG6K/Memorizer/releases) page:

1. Go to [Releases](https://github.com/KG6K/Memorizer/releases).
2. Download `Memorizer-v<version>-debug-signed.apk` (or `Memorizer-v<version>-debug.apk`).
3. Tap the downloaded file on your Android device and confirm installation (allow installation from unknown sources if prompted).

---

## 🔨 Building from Source

To compile and build Memorizer locally:

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 21 (Temurin / OpenJDK)
- Android SDK Build-Tools 36.x

### Build Commands
Clone the repository and run Gradle:
```bash
git clone https://github.com/KG6K/Memorizer.git
cd Memorizer

# Build the debug APK
gradle :app:assembleDebug

# The built APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## ☕ Support the Project

If you find Memorizer helpful for your memorization or studies, consider supporting its development:

- **UPI ID**: `ansariusman@ptyes`
- **Star this repository** on GitHub ⭐ to help others discover it!

---

## 📄 License

```
Copyright (c) 2026 KG6K

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
