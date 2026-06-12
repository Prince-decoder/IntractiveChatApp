

# IntractiveChatApp
Merging all the concept

# Jetpack Compose Intern Practice App

This repository contains a modern Android application built using Jetpack Compose, Kotlin Coroutines, Room Database, and WorkManager. It is structured around an offline-first data layer, a reactive UI state machine, and a multi-step onboarding flow.

Below is a detailed breakdown of the core concepts implemented in this application, where to find them, and their benefits.

---

## Part 1: Onboarding Flow

**Location:** `app/src/main/java/com/my/intern/Onboarding/`
- `OnboardingScreen.kt`: The main entry point containing the 3-step `HorizontalPager`.
- `Steps.kt`: Contains the individual composables for each step (`Step1ValueProps`, `Step2Form`, `Step3Personality`).
- `OnboardingViewModel.kt`: Manages validation logic, UI state, and saving the profile.
- `UserProfileRepository.kt`: Handles reading/writing the onboarding data to DataStore.

**How it is used:**
The onboarding uses a `HorizontalPager` allowing users to swipe through three distinct steps:
1. **Value Props:** Displays an animated text reveal for the app's value propositions.
2. **Collect Data:** Collects Name, Age, Phone, and verifies a mock OTP ("1234").
3. **Personality Selector:** Allows the user to select exactly 3 traits from a dynamic `FlowRow` grid using custom scaling animations.

The Next/Back buttons at the bottom handle navigation and validation. User data is kept in a `StateFlow` to prevent data loss on back navigation. Upon completion, the data is saved in Android `DataStore` and Room, triggering an initial remote sync.

**Benefits:**
- **Zero Data Loss:** UI state is hosted in the ViewModel, so navigating back and forth does not erase user inputs.
- **Immediate Feedback:** Specific validation blocks the user from proceeding with invalid data (e.g., incomplete phone number, incorrect OTP).
- **Smooth UX:** Utilizing Jetpack Compose animation APIs (`animateFloatAsState`, `HorizontalPager` smooth scrolling) makes the flow feel polished and premium.

---

## Part 2: Home Screen UI

**Location:** `app/src/main/java/com/my/intern/Home/`
- `HomeScreen.kt`: Contains the `AuraCircle` (pure Canvas animation), the main layout, parallax scroll logic, and the custom sliding keyboard.
- `HomeViewModel.kt`: Manages the AudioRecord logic for the Mic and visibility toggles.

**How it is used:**
The Home screen features a rich, animated UI divided into three layers managed via swipe gestures and offset parallax effects:
1. **Aura Circle:** A glowing, breathing orb drawn using pure Compose `Canvas`. When the Mic button is tapped, it listens to the microphone using `AudioRecord` and modulates the circle's amplitude based on real-time audio levels.
2. **Chat History:** Swiping up pushes the Aura Circle up with a parallax effect and slides in a Room-backed chat history list. 
3. **Custom Keyboard Input:** Tapping the keyboard icon smoothly slides up a custom text input field from the bottom (without relying on default Android keyboard pushes).

**Benefits:**
- **High Performance:** `AuraCircle` avoids Lottie/GIFs and uses pure Canvas `drawPath` and `Brush.radialGradient`, minimizing memory usage and APK size while running at 60fps.
- **Immersive Interaction:** Swipes, gestures, and audio-reactive elements make the app feel alive and "listening", significantly improving user engagement.
- **Modular & Reusable:** The Canvas element (`AuraCircle`) takes `state` and `amplitude` as parameters, meaning it can easily be dropped into other screens without being tied to the Home logic.

---

## Part 3: Coroutine State Machine (Message Pipeline)

**Location:** `app/src/main/java/com/my/intern/Home/HomeViewModel.kt`
- Look for `sealed class MessagePipeline` and the `sendMessage()` function.
- **Tests:** `app/src/test/java/com/my/intern/Home/HomeViewModelTest.kt`

**How it is used:**
Every message sent undergoes a strict state pipeline:
`Idle → Typing → Validating → Processing → Responding → Idle`

This is implemented using a sealed class exposed via a `StateFlow`. In `sendMessage()`, the ViewModel launches a Coroutine that steps through these states with built-in delays. 
- If a user types another message while the previous one is `Processing`, the `messageJob?.cancel()` is called, immediately halting the old flow and restarting the pipeline for the new message.
- A `withTimeout(8_000)` block wraps the `Processing` state. If it takes longer than 8 seconds, an `TimeoutCancellationException` is caught, and the state updates to `MessagePipeline.Error`, rendering a clickable "Retry" UI on the screen.

**Benefits:**
- **Deterministic UI:** Because the UI passively observes the `MessagePipeline` state, it is impossible for the UI to show conflicting states (e.g., showing both "Typing" and "Processing" at the same time).
- **Graceful Error Handling:** Timeout mechanisms prevent infinite loading spinners.
- **Cancellation:** Coroutine cancellation automatically prevents stale responses from overwriting newer interactions, preventing race conditions natively.
- **Highly Testable:** The pipeline relies on standard Coroutine dispatchers, allowing comprehensive coverage using `runTest` and `advanceTimeBy` for timeouts and cancellation.

---

## Part 4: Offline-First Data Layer & Synchronization

**Location:** 
- **Room Setup:** `app/src/main/java/com/my/intern/UserRoomDataBase/` (`Entities.kt`, `UserDao.kt`, `UserRepository.kt`, `Converters.kt`)
- **Sync Manager:** `app/src/main/java/com/my/intern/Sync/` (`SyncManager.kt`, `SyncWorker.kt`)

**How it is used:**
- **Room Database:** Defines entities like `UserProfile` and `ChatMessage`. `ChatMessage` uses a `@TypeConverter` to serialize a custom `MessageMeta` object into a JSON string. All DAO queries return Kotlin `Flow`, ensuring the UI reactively updates whenever the local database changes.
- **SyncManager (WorkManager):** An intent to sync is sent to `SyncManager`. It queues a `OneTimeWorkRequestBuilder` constrained to `NetworkType.CONNECTED`. 
- **Delta Syncs:** The database maintains a `lastModifiedAt` timestamp on every entity. The `SyncWorker` pulls a shared `lastSyncedAt` timestamp from `SharedPreferences`, queries Room for rows modified *after* that time, and pushes only those differences to Firebase. It then pulls from Firebase and uses a "Local Wins" strategy to resolve conflicts if the local timestamp is newer.

**Benefits:**
- **Works Offline:** Users can navigate the app, send messages, and update their profile without an active internet connection. Everything saves to Room locally first.
- **Battery & Data Efficient:** `NetworkType.CONNECTED` ensures the app never wakes the radio if the device is offline. The Delta Sync mechanism prevents re-uploading the entire database every time.
- **Reliability:** By not relying on third-party sync wrappers, we have exact control over conflict resolution ("Local Wins") and failure retries.
- **Reactive Architecture:** Because Room queries return `Flow`, the UI updates instantly when local data is modified or when a background sync successfully merges remote data into Room, removing the need for manual UI refresh triggers.


https://github.com/user-attachments/assets/8e41e7b2-160c-48d5-88fa-57613e46202e

