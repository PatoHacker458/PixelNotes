# Pixel Notes - AI Developer Agent Context

## System Role & Rules
You are an Expert Android Developer specializing in Kotlin, Jetpack Compose, and Room Database.
When generating or modifying code for this project, you MUST strictly adhere to the following rules:
1. **No comments:** Write clean, self-documenting code. Do not include explanatory comments.
2. **Language:** All code, variable names, and UI text must be in English.
3. **UI Toolkit:** Use ONLY Jetpack Compose and Material Expressive 3.
4. **Architecture:** Follow MVVM (Model-View-ViewModel). Keep logic in ViewModels, UI in Composables.

## Project Overview
"Pixel Notes" is a high-performance note-taking application.
- **Canvas Engine:** Hardware-accelerated Compose Canvas. Supports A4 bounds and 50x Zoom Infinite Canvas.
- **Core Tools:**
    - **Pen & Highlighter:** Pressure-sensitive (USI 2.0 / S-Pen) with Haptic Feedback.
    - **Eraser:** Normal (pixel-based) and Stroke (object-based) modes.
    - **Selection (Lasso):** Supports Free-form and Rectangle selection. Move, Scale, and Color change.
    - **Text Tool:** Dynamic text insertion with custom font support.
    - **Shape Snap:** Auto-perfecting lines, rectangles, and ovals during drawing.
- **Advanced Features:**
    - **Audio Notes:** Record `.m4a` files and anchor them to pages.
    - **PDF Integration:** High-fidelity PDF import (renders as background) and vector-based export.
    - **Cloud Sync:** Automated Google Drive App Data backup/restore with zip-based packaging.
    - **Floating Images:** Insert and transform images from gallery or camera.

## Data Layer Status
- **Room Database:** Version 15.
- **Entities:** `Note`, `PageEntity`, `FolderEntity`, `CustomFont`.
- **TypeConverters:** Handles `StrokeData`, `TextData`, `ImageData`, `AudioData` via GSON.
- **Storage:** Internal storage for PDFs, Audio (`audio_notes/`), and Fonts (`custom_fonts/`).

## Project Structure
- `ui/screens/`: Main screens (`NotesScreen`, `DrawingScreen`, `SettingsScreen`).
- `ui/components/`: Reusable Compose components (`DrawingCanvas`, `ExpressiveIconButton`, `ExpressiveFAB`, `ExpressiveButton`, `SideMenu`, `FolderCard`, `MorphingLoader`, `AudioPlayerSlider`).
- `ui/viewmodels/`: `NotesViewModel` manages state, tools, and sync.
- `domain/`: Business logic (`ShapeDetector`, `PdfExporter`, `CloudSyncManager`), and data models.
- `data/`: Room database, DAOs, and entities.

## Active Roadmap

### Task 1: UI & UX Polish
- Implement Material 3 adaptive layouts for tablets and foldables.
- Improve the "Pages" panel with drag-and-drop reordering animations.
- Refine the floating audio player with a waveform visualization.

### Task 2: Advanced Editing
- Add multi-layer support to the Drawing Canvas (user-facing layers).
- Enhance the Lasso tool with "Group" and "Layer" operations.
- Implement handwriting search or OCR indexing.

### Task 3: Performance Optimization
- Optimize `DrawingCanvas` rendering for extremely large notes with thousands of strokes.
- Implement more efficient bitmap caching for PDF backgrounds.
- Optimize Room database queries for large note collections.
