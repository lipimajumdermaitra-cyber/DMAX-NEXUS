'use strict';

require('dotenv').config();
const crypto = require('node:crypto');
const express = require('express');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');

const app = express();
app.disable('x-powered-by');
app.set('trust proxy', 1);
app.use(helmet());
app.use(express.json({ limit: '32kb', strict: true }));

const PORT = Number.parseInt(process.env.PORT || '8080', 10);
const AI_BASE_URL = (process.env.AI_BASE_URL || 'https://api.openai.com/v1').replace(/\/+$/, '');
const AI_MODEL = process.env.AI_MODEL || 'gpt-4o-mini';
const PROVIDER_KEY = process.env.OPENAI_API_KEY || '';
const ACCESS_TOKEN = process.env.NEXUS_ACCESS_TOKEN || '';

const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 20,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Rate limit reached. Please try again later.' }
});
app.use('/api/', limiter);

function safeTokenEquals(received, expected) {
  if (!received || !expected) return false;
  const a = Buffer.from(received, 'utf8');
  const b = Buffer.from(expected, 'utf8');
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

function extractBearer(req) {
  const match = /^Bearer\s+(.+)$/i.exec(req.get('authorization') || '');
  return match ? match[1].trim() : '';
}

app.get('/healthz', (_req, res) => {
  res.status(200).json({ ok: true, service: 'dmax-nexus-ai', configured: Boolean(PROVIDER_KEY && ACCESS_TOKEN) });
});

app.post('/api/chat', async (req, res) => {
  if (!PROVIDER_KEY || !ACCESS_TOKEN) {
    return res.status(503).json({ error: 'The AI backend is not configured by its owner.' });
  }
  if (!safeTokenEquals(extractBearer(req), ACCESS_TOKEN)) {
    return res.status(401).json({ error: 'Invalid backend access token.' });
  }

  const message = typeof req.body?.message === 'string' ? req.body.message.trim() : '';
  const rawHistory = Array.isArray(req.body?.history) ? req.body.history : [];
  if (!message || message.length > 4000) {
    return res.status(400).json({ error: 'Message must contain 1 to 4000 characters.' });
  }
  if (rawHistory.length > 12) {
    return res.status(400).json({ error: 'At most 12 history messages are accepted.' });
  }

  const history = [];
  for (const entry of rawHistory) {
    if (!entry || !['user', 'assistant'].includes(entry.role) || typeof entry.content !== 'string') {
      return res.status(400).json({ error: 'History may contain only user/assistant text messages.' });
    }
    const content = entry.content.trim();
    if (!content || content.length > 4000) {
      return res.status(400).json({ error: 'A history message has an invalid length.' });
    }
    history.push({ role: entry.role, content });
  }
  if (history.length === 0 || history[history.length - 1].role !== 'user' || history[history.length - 1].content !== message) {
    history.push({ role: 'user', content: message });
  }

  try {
    const upstream = await fetch(`${AI_BASE_URL}/chat/completions`, {
      method: 'POST',
      headers: {
        authorization: `Bearer ${PROVIDER_KEY}`,
        'content-type': 'application/json',
        accept: 'application/json'
      },
      body: JSON.stringify({ model: AI_MODEL, messages: history, temperature: 0.7 }),
      signal: AbortSignal.timeout(45000)
    });
    const payload = await upstream.json().catch(() => ({}));
    if (!upstream.ok) {
      const providerMessage = typeof payload?.error?.message === 'string' ? payload.error.message : 'The AI provider rejected the request.';
      console.error('AI provider request failed with status', upstream.status);
      return res.status(502).json({ error: providerMessage.slice(0, 300) });
    }
    const reply = payload?.choices?.[0]?.message?.content;
    if (typeof reply !== 'string' || !reply.trim()) {
      return res.status(502).json({ error: 'The AI provider returned no text reply.' });
    }
    return res.status(200).json({ reply: reply.slice(0, 12000) });
  } catch (error) {
    const timedOut = error?.name === 'TimeoutError' || error?.name === 'AbortError';
    console.error('AI upstream request failed:', timedOut ? 'timeout' : 'network error');
    return res.status(502).json({ error: timedOut ? 'AI request timed out. Try again.' : 'Could not reach the AI provider.' });
  }
});

app.use((error, _req, res, _next) => {
  if (error?.type === 'entity.too.large') return res.status(413).json({ error: 'Request body is too large.' });
  if (error instanceof SyntaxError) return res.status(400).json({ error: 'Request body must be valid JSON.' });
  console.error('Unhandled request error');
  return res.status(500).json({ error: 'Unexpected backend error.' });
});

if (!Number.isInteger(PORT) || PORT < 1 || PORT > 65535) {
  throw new Error('PORT must be an integer between 1 and 65535.');
}
app.listen(PORT, '0.0.0.0', () => {
  console.log(`DMAX NEXUS AI backend listening on port ${PORT}`);
  if (!PROVIDER_KEY || !ACCESS_TOKEN) {
    console.warn('Backend secrets are missing. /api/chat will return 503 until configured.');
  }
});
