# DMAX NEXUS — Complete Multi-Phase Starter

DMAX NEXUS is a native Android application built with Kotlin and Jetpack Compose. This repository brings the core roadmap together so you can build once, then run a single verification pass on your phone.

## Included in this project

### Android app
- Premium obsidian/emerald visual system, plus Purple Cyber, Crimson and Monochrome themes.
- Five-tab navigation: Home, AI Studio, Hub, Community and Profile.
- Local global search across saved notes, tasks, server profiles and workflows.
- Room persistence for notes, tasks, Minecraft/game server profiles, AI chat history and reusable workflows.
- Task completion/deletion, note creation/deletion, game profile validation and workflow launch into AI Studio.
- Speech-to-text input through Android's available speech-recognition app.
- Low-data AI mode that sends less history context to the backend.
- Optional Firebase email/password auth, live cloud channel chat, UID-based direct messages and report submission.
- Unit tests for form and endpoint validation.

### AI backend (`backend/`)
- Node.js HTTP API that can proxy an OpenAI-compatible chat-completions service.
- Provider API key stays on the server, never in Android source or APK.
- Bearer-token gate, input validation, request size limits, security headers, per-IP rate limiting and upstream timeout.
- `render.yaml` at the repository root for a hosted Node service blueprint; other Node hosts can also be used.

### Firebase (`firebase/`)
- Firestore security rules for profiles, authenticated channel messages, participant-only DMs and private report submissions.
- Firebase setup remains optional, so the Android project can build without a personal Firebase configuration file.

## Version and build details

- Application ID: `com.dmax.nexus`
- Version: `1.0.0`
- Minimum Android API: 23
- Compile SDK: 36
- Java: 17
- Gradle: 8.13
- Android Gradle Plugin: 8.13.2
- Kotlin: 2.2.21

## Run the cloud build

1. Download/extract this project ZIP.
2. Create a repository on GitHub, preferably Private while you're developing.
3. From Termux, install Git and GitHub CLI and complete browser/device login without putting a token in a command:
   ```bash
   pkg update
   pkg install git gh
   gh auth login --hostname github.com --git-protocol https --web
   ```
4. In the extracted project folder, run `git init`, `git branch -M main`, `git add .`, and `git commit -m "Build DMAX NEXUS"`. Add the GitHub repository as `origin`, then run `git push -u origin main`.
5. Open the repository's **Actions** tab and open **DMAX NEXUS - Build and Test**.
6. The workflow installs Java and Android SDK packages, checks the backend JavaScript syntax, compiles the debug app, runs unit tests, and uploads `DMAX-NEXUS-debug-APK`.
7. Download that artifact from a successful workflow run and extract the ZIP to get the APK.
8. Copy the APK to your Android phone and install it. Android may ask you to allow installation from the file manager/browser used to open it.

Workflow status is authoritative: a source ZIP existing here does not mean the APK has already been built. A failed job must be reviewed before installation.

## Optional: enable cloud community

1. Create a Firebase project.
2. Register an Android app with package `com.dmax.nexus`.
3. Enable **Authentication → Sign-in method → Email/Password**.
4. Create a **Cloud Firestore** database.
5. Download `google-services.json` into the Android project's `app/` directory.
6. Open Firebase Console → Firestore → Rules, paste `firebase/firestore.rules`, and publish.
7. Push the configuration file and source updates to GitHub; run the workflow again.

The Google Services Gradle plugin is applied only when `app/google-services.json` exists. Do not enable public/anonymous write rules. Cloud use may have quotas or charges according to the Firebase project; check its current billing and usage screen.

### How community works in this starter

- Email/password sign-up and sign-in.
- Three public channels: `general`, `gaming`, `showcase`.
- Direct messages by entering another signed-in user's Firebase UID.
- Report message action creates a report document that only the project owner can inspect from the Firebase console.
- Firestore rules block unauthenticated reading/writing, message editing/deletion, and private DM access by non-participants.

This is a starter community, not a complete trust-and-safety operation. It does not have a staffed moderation team, push notifications, image uploads, end-to-end encryption or voice/video calls. Do not use it to store sensitive private information.

## Optional: enable live AI chat

The app intentionally does not include a provider API key. Deploy the private proxy in `backend/` first.

1. Use a Node.js host that supports Node 20 or later.
2. Set server environment variables from `backend/.env.example`:
   - `OPENAI_API_KEY`: provider API key (server-side only).
   - `NEXUS_ACCESS_TOKEN`: long random token known only to you and your app setup.
   - `AI_BASE_URL`: provider's OpenAI-compatible API base URL.
   - `AI_MODEL`: a model available to your provider account.
3. Deploy the backend. Confirm `/healthz` returns `ok: true` and `configured: true`.
4. In DMAX NEXUS → Profile → AI backend, save `https://YOUR-SERVICE-DOMAIN/api/chat` and the same `NEXUS_ACCESS_TOKEN` value.
5. Open AI Studio and send a short test message. Provider usage limits and billing apply.

Never add `.env`, a provider API key or a production secret to GitHub. The app stores its personal backend access token in app-private DataStore preferences; avoid sharing your APK or device profile. For public deployments, prefer per-user authentication and enforce quotas at the backend or API gateway instead of relying on a single shared client token.

## Local build (when Android SDK is available)

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

Expected debug APK path:

`app/build/outputs/apk/debug/app-debug.apk`

## What still needs real-world verification

No cloud runner or physical Android device is connected to this conversation. The project source, rules, tests and CI workflow have been prepared, but the final Gradle build and on-device behavior must be verified by the GitHub Actions run and then by installing that APK. Live AI requires a deployed backend; cloud chat requires the Firebase setup steps above.
