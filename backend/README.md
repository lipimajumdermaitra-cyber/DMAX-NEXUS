# DMAX NEXUS AI Backend

This service keeps the model-provider key on the server and provides `POST /api/chat` for the Android app.

## Local setup

Requires Node.js 20 or later.

```bash
cd backend
npm install
cp .env.example .env
```

Edit `.env` and set `OPENAI_API_KEY` and `NEXUS_ACCESS_TOKEN`. The provider key must never be placed in Android code or committed to GitHub.

```bash
npm start
```

Health check: `http://localhost:8080/healthz`

Chat endpoint: `POST http://localhost:8080/api/chat`

Headers:

```text
Content-Type: application/json
Authorization: Bearer YOUR_NEXUS_ACCESS_TOKEN
```

Body:

```json
{
  "message": "Help me plan a Kotlin feature",
  "history": [
    { "role": "user", "content": "Hello" },
    { "role": "assistant", "content": "Hi!" },
    { "role": "user", "content": "Help me plan a Kotlin feature" }
  ]
}
```

The response has a `reply` string. The endpoint uses a fixed server-side model configuration, accepts only `user` and `assistant` history roles, applies rate limits, and rejects missing/wrong access tokens. It intentionally does not accept arbitrary system prompts or arbitrary upstream URLs from the client.

The Android client only accepts HTTPS endpoints. For the phone app, deploy the backend behind HTTPS before connecting it. You can still test the local endpoint directly with a REST client while developing.

## Hosted deployment

`render.yaml` describes a Render-compatible Node web service. Review your host's current free/paid limits before deploying. Add secrets through the host dashboard, not a source file. Configure `OPENAI_API_KEY`, `NEXUS_ACCESS_TOKEN`, `AI_BASE_URL`, and `AI_MODEL`. After deployment, copy the HTTPS `/api/chat` endpoint and access token into the app's Profile settings.

For a public or shared app, replace the shared-token approach with authenticated user identity, per-account quotas, abuse monitoring and backend-side authorization. Never consider a token embedded in an APK to be a strong secret.
