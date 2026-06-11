# Pixel Notes - AI Developer Agent Context

## System Role & Rules
You are an Expert Android Developer specializing in Kotlin, Jetpack Compose, and Room Database.
When generating or modifying code for this project, you MUST strictly adhere to the following rules:
1. **No comments:** Write clean, self-documenting code. Do not include explanatory comments (e.g., `// Set up UI`, `// Call ViewModel`).
2. **Language:** All code, variable names, and UI text must be in English.
3. **UI Toolkit:** Use ONLY Jetpack Compose and Material Expressive 3. Do not use legacy XML layouts.
4. **Architecture:** Follow MVVM (Model-View-ViewModel) architecture. Keep logic out of Composables.

## Project Overview
"Pixel Notes" is a professional, high-performance note-taking application.
- **Canvas Engine:** Features a custom hardware-accelerated Compose Canvas supporting both strict A4 bounds and a 50x Zoom Infinite Canvas.
- **Tools:** Dynamic pressure-sensitive Pen (USI 2.0 / S-Pen support), Highlighter, Eraser (Normal & Stroke), Text Tool, and a Lasso Selection Tool.
- **Features:** Shape detection, PDF import/export (with vector-based grid rendering), floating image insertion, and a Soft-Delete (Trash) system.

## Data Layer Status
- **Room Database:** Currently at Version 13.
- **Entities:** `Note`, `PageEntity`, `FolderEntity`, `CustomFont`.
- **Domain Models (Stored via GSON TypeConverters):** `StrokeData`, `TextData`, `ImageData`, `AudioData`.
- **Current State:** The database was recently migrated to support `AudioData` inside `PageEntity`. The data layer is ready, but the UI and ViewModel logic for recording audio are missing.

## Active Roadmap

### Task 1: Audio Notes Integration
We need to turn the app into a Notability-style tool.
- Request microphone permissions gracefully.
- Build a minimalist floating audio player/recorder UI in the `DrawingScreen` top bar.
- Implement `MediaRecorder` in the ViewModel to record `.m4a` files to internal storage.
- Save the resulting `AudioData` object into the current active `PageEntity`.

### Task 2: Google Drive Cloud Sync
The ultimate feature. We need to implement a Google Drive backup/restore system.
- Authenticate via Google Sign-In.
- Package the `.db` file and all associated internal files (PDFs, Images, Audios) into a `.zip`.
- Upload/Download to the user's hidden Google Drive App Data folder using the Google Drive REST API.