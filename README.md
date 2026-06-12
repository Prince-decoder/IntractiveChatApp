
# ForIntern — Interactive Chat App
Merging all the concepts

# Jetpack Compose Intern Practice App

This repository contains a modern Android application built using Jetpack Compose, Kotlin Coroutines, Room Database, Firebase Firestore, WorkManager, and DataStore. It is structured around an offline-first data layer, a reactive UI state machine, and a multi-step onboarding flow.

**Package:** `com.my.forintern`

---

## Project Structure

```
app/src/main/java/com/my/forintern/
├── FireDatabase/           # Firebase Firestore layer (repository, ViewModel, worker)
├── HomeScreen/             # Home screen UI & ViewModel
├── Message/                # Shared ChatMessage data model
├── OnBoarding/             # Onboarding flow (screen, steps, ViewModel, DataStore repo)
├── UserRoomDataBase/       # Room persistence layer (entity, DAO, repository, converters)
├── Graph.kt                # Manual DI — app-wide dependency graph (singleton)
├── MainActivity.kt         # Entry point; handles nav graph and permission checks
├── Screens.kt              # Navigation route definitions
└── UserDataApp.kt          # Application class — calls Graph.provide()
```

---

## Part 1: Onboarding Flow

**Location:** `app/src/main/java/com/my/forintern/OnBoarding/`

| File | Responsibility |
|---|---|
| `OnboardingScreen.kt` | Main entry point; hosts the 3-step `HorizontalPager` |
| `Steps.kt` | Individual composables for each step (`Step1`, `Step2`, `Step3`) |
| `OnboardingViewModel.kt` | Validation logic, UI state (`MutableStateFlow<UserProfile>`), profile save |
| `UserProfileRepository.kt` | Reads/writes onboarding data via Android **DataStore Preferences** |
| `UserProfile.kt` | Plain data class: `name`, `age`, `phone`, `traits: Set<String>` |

**How it works:**

The onboarding uses a `HorizontalPager` to walk users through three distinct steps:

1. **Value Props** — Animated text reveal showcasing the app's value propositions.
2. **Collect Data** — Gathers Name, Age, Phone, and verifies a mock OTP (`"1234"`).
3. **Personality Selector** — User picks **exactly 3** personality traits from a `FlowRow` grid with custom scaling animations. `toggleTrait()` enforces the cap of 3 selections.

The ViewModel's `MutableStateFlow<UserProfile>` holds all input data, which is preserved across back-navigation. Upon completion:
- `saveProfile()` writes to **DataStore** (persisted via `UserProfileRepository`).
- **Room** is seeded with the user's phone number as the `idphone` primary key.
- `FirebaseSyncWorker.enqueue()` is called to trigger a network upload when connected.

**Validation (in `OnboardingViewModel`):**
- `isStep2Valid()` — name/age not blank, phone is exactly 10 digits, OTP equals `"1234"`.
- `isStep3Valid()` — exactly 3 traits selected.

**Benefits:**
- **Zero Data Loss:** All inputs live in ViewModel `StateFlow`; back-navigation never wipes the form.
- **Immediate Feedback:** Validation blocks progression with specific, contextual errors.
- **Smooth UX:** Compose animation APIs and `HorizontalPager` smooth-scroll make the flow feel polished.

---

## Part 2: Home Screen UI

**Location:** `app/src/main/java/com/my/forintern/HomeScreen/`

| File | Responsibility |
|---|---|
| `HomeScreen.kt` | `AuraCircle` Canvas animation, parallax scroll, custom keyboard |
| `HomeViewModel.kt` | `AudioRecord` mic logic, `HomeUIState`, `MessagePipeline`, CRUD on Room |

**UI State (`HomeUIState`):**

```kotlin
data class HomeUIState(
    val auraState: AuraState,          // IDLE or LISTENING
    val amplitude: Float,              // Real-time mic amplitude (0f–1f)
    val hasMicPermission: Boolean,
    val isKeyboardVisible: Boolean,
    val inputText: String,
    val allChatMessages: List<ChatMessage>,
    val visibleMessages: List<ChatMessage>,   // Paginated — last N messages
    val visibleMessagesCount: Int,            // Default: 20, +20 per scroll-to-top
    val isChatHistoryVisible: Boolean,
    val editingMessage: ChatMessage?          // Non-null when editing an existing message
)
```

**Three interaction layers:**

1. **Aura Circle** — A glowing, breathing orb rendered with pure Compose `Canvas` using `drawPath` and `Brush.radialGradient`. When the mic button is tapped, `AudioRecord` (44100 Hz, PCM 16-bit, mono) captures audio on an `IO` dispatcher. RMS amplitude is normalized (`maxExpectedRms = 2500f`) and smoothed with a low-pass filter (`α = 0.3`) before being piped into `HomeUIState.amplitude`.
2. **Chat History** — Swiping up triggers `isChatHistoryVisible = true` with a parallax offset on the Aura Circle. The list is sourced from Room via `getUserFlowById(phoneId)` — a `Flow`-backed query that reactively updates the UI. Pagination is handled by `loadMoreMessages()` (+20 items per call).
3. **Custom Keyboard** — Tapping the keyboard icon calls `toggleKeyboard()`, smoothly animating a custom `TextField` up from the bottom. This avoids the default Android keyboard resize behavior.

**Message CRUD from `HomeViewModel`:**
- `sendMessage()` — runs the full `MessagePipeline` (see Part 3), then calls `addMessageToUser()` or `editMessage()` on the Room DAO.
- `deleteMessage()` — calls `userRepo.deleteMessage(phoneId, message)`.
- `setEditingMessage()` — populates `inputText` with the existing message content and sets `editingMessage`, so `sendMessage()` routes to `editMessage()`.

**Benefits:**
- **High Performance:** No Lottie/GIF; pure Canvas at 60fps via 16ms loop delay.
- **Immersive & Audio-Reactive:** The Aura Circle breathes with real mic amplitude, making the app feel "alive".
- **Paginated History:** `visibleMessages` is a `takeLast(N)` window, preventing excessive rendering of large chat histories.

---

## Part 3: Coroutine State Machine (Message Pipeline)

**Location:** `app/src/main/java/com/my/forintern/HomeScreen/HomeViewModel.kt`

**Sealed class:**

```kotlin
sealed class MessagePipeline {
    object Idle       : MessagePipeline()
    object Typing     : MessagePipeline()
    object Validating : MessagePipeline()
    object Processing : MessagePipeline()
    object Responding : MessagePipeline()
    data class Error(val reason: String, val lastMessage: String) : MessagePipeline()
}
```

**How it works:**

Every message sent steps through this strict pipeline:

`Idle → Typing → Validating → Processing → Responding → Idle`

- **Typing** is triggered by `updateInputText()` whenever the field is non-blank.
- **sendMessage()** launches a `messageJob` coroutine that transitions states with built-in delays simulating real network activity.
- `messageJob?.cancel()` is called at the top of `sendMessage()` — if the user fires a second message mid-pipeline, the old coroutine is immediately cancelled and a fresh one starts. There are no stale-response race conditions.
- A `withTimeout(800)` block wraps the `Processing` state. On `TimeoutCancellationException`, the state moves to `MessagePipeline.Error`, rendering a clickable **Retry** UI element. `retryMessage()` restores `inputText` and re-runs the pipeline.
- `CancellationException` is explicitly re-thrown to correctly cooperate with coroutine structured concurrency.

**Benefits:**
- **Deterministic UI:** The UI passively observes a single `StateFlow<MessagePipeline>` — conflicting states are structurally impossible.
- **Graceful Timeouts:** `withTimeout` prevents infinite spinners on slow or failed processing.
- **Race-condition-free:** Coroutine cancellation ensures only the latest message's response can ever update the UI.
- **Testable:** `processingDelayMs` is `internal` and settable, allowing `runTest` + `advanceTimeBy` to drive timeout and cancellation coverage without real delays.

---

## Part 4: Offline-First Data Layer

### 4a. Room Database

**Location:** `app/src/main/java/com/my/forintern/UserRoomDataBase/`

| File | Responsibility |
|---|---|
| `UserDATASET.kt` | Single Room entity: `idphone` (PK, Long), `sender` (String), `message` (List<ChatMessage>) |
| `Converters.kt` | `@TypeConverter` using **Gson** to serialize `List<ChatMessage>` ↔ JSON `String` |
| `UserDao.kt` | Abstract DAO with `Flow`-backed queries and `@Transaction` helper methods |
| `UserRepository.kt` | Thin wrapper over `UserDao` |
| `UserViewModel.kt` | Wraps Room writes and auto-enqueues `FirebaseSyncWorker` after each mutation |
| `UserDataBase.kt` | `@Database` declaration; `fallbackToDestructiveMigration()` for schema changes |

**Entity: `UserDATASET`**

```kotlin
@Entity(tableName = "UserDataSet")
data class UserDATASET(
    @PrimaryKey var idphone: Long = 0L,    // User's phone number as primary key
    @ColumnInfo var sender: String = "",
    @ColumnInfo var message: List<ChatMessage>  // Stored as JSON via Converters
)
```

**`ChatMessage` (in `Message/`):**

```kotlin
data class ChatMessage(
    val text: String = "",
    val time: String = "",
    val issentByme: Boolean = false
)
```

**Key DAO operations (`UserDao`):**

| Method | Type | Description |
|---|---|---|
| `getAllUsers()` | `Flow<List<UserDATASET>>` | Reactive full-table read |
| `getUserFlowById(id)` | `Flow<UserDATASET?>` | Reactive per-user read (drives Home screen) |
| `getUserByIdSync(id)` | `suspend` | Used internally by `@Transaction` methods |
| `addMessageToUser(userId, msg)` | `@Transaction suspend` | Fetch → mutate list → update |
| `deleteMessageFromUser(userId, msg)` | `@Transaction suspend` | Fetch → remove → update |
| `editMessageForUser(userId, old, new)` | `@Transaction suspend` | Fetch → replace at index → update |
| `addOrUpdateUserKeepHistory(user)` | `@Transaction suspend` | Upsert preserving existing messages |

**Dependency Graph (`Graph.kt`):** A manual DI singleton initialized in `UserDataApp` (`Application` class). Provides `userdatabase` and a lazily initialized `userrepo` to the whole app.

### 4b. Firebase Sync Layer

**Location:** `app/src/main/java/com/my/forintern/FireDatabase/`

| File | Responsibility |
|---|---|
| `FirebaseSyncWorker.kt` | `CoroutineWorker` — reads Room, uploads all records to Firestore; retries on failure |
| `UserRepository.kt` (`UserFRepository`) | Direct Firestore operations: `saveUserToDatabase`, `getCurrentUser`, `getUserByPhone` |
| `UserViewModel.kt` (`UserFViewModel`) | Wraps Firebase repository; exposes `AuthState` flow and `userData` flow |
| `UserData.kt` | Firestore data model: `firstName`, `phone`, `message` (serialized JSON string) |
| `Result.kt` | `sealed class Results<T>`: `Success`, `error`, `Loading` |
| `Injection.kt` | Provides `FirebaseFirestore.getInstance()` |

**`FirebaseSyncWorker` flow:**
1. Fetches all `UserDATASET` rows from Room via `userrepo.getalluser().firstOrNull()`.
2. For each row, serializes `List<ChatMessage>` to JSON using `Converters`.
3. Maps to `UserData` and calls `firestore.collection("Customer").document(phone).set(userData).await()`.
4. On any exception → `Result.retry()` (WorkManager handles back-off).

**`UserFRepository` operations:**
- `saveUserToDatabase(UserData)` — writes to Firestore under `"Customer/{phone}"` with a 10-second `withTimeout`.
- `getCurrentUser()` — fetches the currently authenticated user's document from `Source.SERVER`.
- `getUserByPhone(phone)` — fetches any user document by phone number.

**Enqueueing sync:**
```kotlin
// Called after every Room mutation in UserViewModel
FirebaseSyncWorker.enqueue(Graph.appContext)
```
Uses `OneTimeWorkRequestBuilder` with `NetworkType.CONNECTED` constraint and `ExistingWorkPolicy.REPLACE` — so rapid mutations coalesce into a single upload attempt.

**Benefits:**
- **Works Offline:** All writes hit Room first. The UI never waits on the network.
- **Battery & Data Efficient:** `CONNECTED` constraint prevents unnecessary radio wake-ups. `REPLACE` policy de-duplicates queued sync jobs.
- **Typed Error Handling:** The `Results<T>` sealed class forces all callers to handle `Success`, `error`, and `Loading` explicitly — no unchecked exceptions leak to the UI.
- **Reactive Architecture:** `Flow`-backed DAO queries mean the UI auto-refreshes the moment Room is updated by a background sync, with no manual refresh triggers needed.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                         │
│   OnboardingScreen  ←→  HomeScreen                      │
│   (HorizontalPager)      (Canvas + Chat + Keyboard)     │
└────────────┬────────────────────┬───────────────────────┘
             │                    │
    ┌─────────▼────────┐ ┌────────▼────────────┐
    │ OnboardingViewModel│ │   HomeViewModel     │
    │  (AndroidViewModel)│ │  (AndroidViewModel) │
    │  StateFlow<UP>     │ │  StateFlow<HUIState>│
    │  isStep2Valid()    │ │  MessagePipeline    │
    │  isStep3Valid()    │ │  AudioRecord        │
    └─────────┬──────────┘ └────────┬────────────┘
              │                     │
    ┌─────────▼──────────────────────▼────────────┐
    │               Graph (Manual DI)              │
    │   userdatabase: UserDataBase                 │
    │   userrepo: UserRepository                   │
    └─────────┬────────────────────┬───────────────┘
              │                    │
    ┌─────────▼────────┐  ┌────────▼──────────────┐
    │  Room Database   │  │   DataStore Prefs     │
    │  UserDATASET     │  │   UserProfileRepository│
    │  UserDao (Flow)  │  │   (name/age/phone/    │
    │  Converters(Gson)│  │    traits)            │
    └─────────┬────────┘  └───────────────────────┘
              │
    ┌─────────▼────────────────────────────────────┐
    │      FirebaseSyncWorker (WorkManager)         │
    │      Constraint: NetworkType.CONNECTED        │
    │      Policy: ExistingWorkPolicy.REPLACE       │
    │      On success → Result.success()            │
    │      On failure → Result.retry()              │
    └─────────┬────────────────────────────────────┘
              │
    ┌─────────▼──────────────────────────────────  ┐
    │   Firebase Firestore                          │
    │   Collection: "Customer"                      │
    │   Document ID: phone (Long)                   │
    └───────────────────────────────────────────────┘
```

---

## Key Technology Decisions

| Decision | Rationale |
|---|---|
| Single `UserDATASET` entity (not normalized) | Simpler schema; messages stored as serialized JSON via `Converters` |
| `fallbackToDestructiveMigration()` | Allows rapid schema iteration during development |
| Manual DI via `Graph.kt` | No Hilt/Dagger overhead for an intern-sized project |
| `ExistingWorkPolicy.REPLACE` | Rapid edits collapse into a single sync — avoids queuing many redundant uploads |
| `AndroidViewModel` (not `ViewModel`) | `HomeViewModel` and `OnboardingViewModel` need `Application` context for `AudioRecord` and `DataStore` |
| `Source.SERVER` in `getCurrentUser()` | Forces a fresh read from Firestore, bypassing the client cache |


https://github.com/user-attachments/assets/8e41e7b2-160c-48d5-88fa-57613e46202e

