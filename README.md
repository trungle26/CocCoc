# CocCoc News & Podcast App

Android news aggregator and podcast player with AI-powered summarization.

## Features

- Vietnamese news feed (DanTri, VnExpress RSS)
- Podcast player with notification controls
- AI content summarization (Google Gemini)
- Audio download for offline listening
- Bilingual: Vietnamese/English
- Offline caching with pagination

## Architecture

**Clean Architecture + MVVM**

```
app/
├── data/           # RSS parser, Room DB, Repository
├── domain/         # Models, Use Cases, Interfaces  
├── ui/             # Compose screens, ViewModels
├── service/        # MediaSessionService for audio
└── utils/          # AI, Content extraction, Download
```

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Room, Paging 3, Media3, Coroutines/Flow

## Key Challenges & Solutions

### 1. WebView Content Extraction
**Problem:** Simple CSS selectors extracted ads/metadata instead of article text.

**Solution:** Implemented Readability algorithm with scoring system:
- Scores elements by paragraphs, text length, semantic tags
- Penalizes media controls, ads, navigation (-60 points)

### 2. AI Summarization API
**Problem:** Finding free API with Vietnamese support.

**Solution:** Google Gemini AI (gemini-pro)
- Free tier: 1,500 requests/day
- Language-aware prompts (Vietnamese → Vietnamese, English → English translation)
- Limited input to 4,000 chars to avoid errors

### 3. Audio Playback Controls
**Problem:** Basic MediaPlayer lacked OS integration and lock screen controls.

**Solution:** MediaSessionService with Media3
- Rich notifications with album art
- Lock screen + Bluetooth controls
- Background playback with foreground service

### 4. Notification Navigation
**Problem:** Clicking notification reloaded audio and interrupted playback.

**Solution:**
- Encoded article JSON in Intent extras
- ViewModel checks if podcast already loaded before reloading
- Binder pattern syncs UI with service state

### 5. Other Challenges
- **WebView memory leak:** Used `WeakReference<WebView>`
- **Paging reorder:** Only reorder on refresh, not real-time

## Setup

**Requirements:** Android Studio, Min SDK 24, Target SDK 34

1. Clone repo
2. Get [Gemini API key](https://aistudio.google.com/apikey) (free)
3. Add key to `local.properties`:
   ```
   GEMINI_API_KEY=
   ```
4. Run app

## 📖 Usage

- **News:** Scroll, pull-to-refresh, tap article
- **Summarize:** Tap Gemini icon in article screen
- **Podcast:** Tap article with podcast badge, you can also control audio from notification
- **Download:** Tap ⬇️ button when audio detected
- **Language:** Android Settings → Languages (auto-switches)

---

**Author:** Trung Le  
**Note:** Learning project demonstrating Clean Architecture, MVVM, Jetpack Compose, and AI integration.
