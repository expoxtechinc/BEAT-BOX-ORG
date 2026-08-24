// =============================================================================
// Vercel Serverless Function Entry Point for BeatBox Backend
// =============================================================================
// This file adapts the Express app for Vercel's serverless environment.
// Vercel automatically detects and deploys this file from the /api directory.
// =============================================================================

// CRITICAL: Set DATABASE_URL before any other imports so Prisma can read it
import { ensureDatabaseUrl, initDatabase } from '../backend/src/config/db-init';
ensureDatabaseUrl();

import type { VercelRequest, VercelResponse } from '@vercel/node';
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import path from 'path';
import { config } from '../backend/src/config';
import { apiRateLimiter } from '../backend/src/middleware/rateLimiter';
import { errorHandler, notFoundHandler } from '../backend/src/middleware/error';
import { ok } from '../backend/src/utils/response';

// Routes
import authRoutes from '../backend/src/routes/auth.routes';
import musicRoutes from '../backend/src/routes/music.routes';
import premiumRoutes from '../backend/src/routes/premium.routes';
import stripeRoutes from '../backend/src/routes/stripe.routes';
import socialRoutes from '../backend/src/routes/social.routes';
import searchRoutes from '../backend/src/routes/search.routes';
import reportRoutes from '../backend/src/routes/report.routes';
import adminRoutes from '../backend/src/routes/admin.routes';
import { StripeService } from '../backend/src/services/stripe.service';

const app = express();

// Security
app.use(helmet({
  crossOriginResourcePolicy: { policy: 'cross-origin' },
  contentSecurityPolicy: false,
}));

app.use(cors({
  origin: config.cors.origins,
  credentials: true,
}));

app.use(morgan('combined'));

// Body parsing
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Rate limiting (relaxed for serverless)
app.use(apiRateLimiter);

// Static files for uploads (if using local storage)
const uploadPath = path.resolve(config.storage.uploadDir);
try {
  if (!require('fs').existsSync(uploadPath)) {
    require('fs').mkdirSync(uploadPath, { recursive: true });
  }
} catch (e) {
  // Directory creation may fail, that's OK
}
app.use('/uploads', express.static(uploadPath, {
  setHeaders: (res, filePath) => {
    if (filePath.endsWith('.mp3')) res.setHeader('Content-Type', 'audio/mpeg');
    else if (filePath.endsWith('.wav')) res.setHeader('Content-Type', 'audio/wav');
    else if (filePath.endsWith('.m4a')) res.setHeader('Content-Type', 'audio/mp4');
    else if (filePath.endsWith('.aac')) res.setHeader('Content-Type', 'audio/aac');
    res.setHeader('Accept-Ranges', 'bytes');
  },
}));

// Stripe webhook (raw body) — must be before express.json
app.post('/api/v1/stripe/webhook', express.raw({ type: 'application/json' }), (req, res) => {
  const signature = req.headers['stripe-signature'] as string;
  if (!signature) {
    return res.status(400).json({ success: false, error: { code: 'BAD_REQUEST', message: 'Missing stripe-signature header.' } });
  }

  let event;
  try {
    event = StripeService.constructWebhookEvent(req.body, signature);
  } catch (err) {
    console.error('[Stripe Webhook] Signature verification failed:', err);
    return res.status(400).json({ success: false, error: { code: 'BAD_REQUEST', message: 'Invalid webhook signature.' } });
  }

  StripeService.processWebhookEvent(event)
    .then(() => res.json({ received: true }))
    .catch((err) => {
      console.error('[Stripe Webhook] Processing failed:', err);
      res.status(500).json({ received: false, error: 'Webhook processing failed.' });
    });
});

// API Routes
const prefix = config.apiPrefix;
app.use(`${prefix}/auth`, authRoutes);
app.use(`${prefix}/music`, (req, res, next) => {
  if (req.path === '/webhook') return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Not found.' } });
  next();
}, musicRoutes);
app.use(`${prefix}/premium`, premiumRoutes);
app.use(`${prefix}/stripe`, (req, res, next) => {
  if (req.path === '/webhook') return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Use POST method for webhook.' } });
  next();
}, stripeRoutes);
app.use(`${prefix}/social`, socialRoutes);
app.use(`${prefix}/search`, searchRoutes);
app.use(`${prefix}/reports`, reportRoutes);
app.use(`${prefix}/admin`, adminRoutes);

// Health check
app.get('/health', (req, res) => {
  res.json(ok({
    status: 'healthy',
    environment: config.env,
    timestamp: new Date().toISOString(),
    stripeEnabled: config.stripe.enabled,
    vercel: config.isVercel,
  }));
});

// Error handling
app.use(notFoundHandler);
app.use(errorHandler);

// Initialize database on cold start, then export the app
// Vercel will call the default export for each request
let dbInitPromise: Promise<void> | null = null;

async function ensureDbReady() {
  if (!dbInitPromise) {
    dbInitPromise = initDatabase();
  }
  await dbInitPromise;
}

// Export a handler that ensures DB is ready before processing requests
export default async function handler(req: VercelRequest, res: VercelResponse) {
  await ensureDbReady();
  return app(req, res);
}
