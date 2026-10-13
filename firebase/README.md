# Firebase setup

1. Create a Firebase project and add Android app package `com.dmax.nexus`.
2. Download `google-services.json` into `app/google-services.json`.
3. In Firebase Authentication, enable Email/Password.
4. Create a Cloud Firestore database.
5. Publish `firestore.rules` in Firebase Console → Firestore → Rules.
6. Rebuild the Android app.

The community uses the following Firestore paths:
- `users/{uid}` — limited public username profile. User documents do not store email addresses.
- `communities/public/channels/{general|gaming|showcase}/messages/{messageId}` — authenticated channel messages.
- `directMessages/{sortedUidPair}/messages/{messageId}` — messages readable only by the two participant UIDs.
- `reports/{reportId}` — report submissions; reads are denied to normal clients so the project owner can review them in the Firebase Console.

Before enabling real users, test the rules in the Firebase Rules Playground or Emulator Suite using an unauthenticated user, a regular signed-in user, and two different UIDs. Never replace these rules with open read/write rules. Cloud service limits and possible costs depend on your Firebase plan and usage.
