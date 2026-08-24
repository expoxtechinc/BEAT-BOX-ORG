import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import path from 'path';
import { config } from './config';
import { apiRateLimiter } from './middleware/rateLimiter';
import { errorHandler, notFoundHandler } from './middleware/error';
import { ok } from './utils/response';

// Routes
import authRoutes from './routes/auth.routes';
import musicRoutes from './routes/music.routes';
import premiumRoutes from './routes/premium.routes';
import stripeRoutes from './routes/stripe.routes';
import socialRoutes from './routes/social.routes';
import searchRoutes from './routes/search.routes';
import reportRoutes from './routes/report.routes';
import adminRoutes from './routes/admin.routes';

const app = express();

// =============================================================================
// SECURITY MIDDLEWARE
// =============================================================================

app.use(helmet({
  crossOriginResourcePolicy: { policy: 'cross-origin' },
  contentSecurityPolicy: config.isProduction ? undefined : false,
}));

app.use(cors({
  origin: config.cors.origins,
  credentials: true,
}));

// Request logging
app.use(morgan(config.isProduction ? 'combined' : 'dev'));

// Body parsing
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Rate limiting
app.use(apiRateLimiter);

// =============================================================================
// STATIC FILES (for serving uploads in development)
// =============================================================================

const uploadPath = path.resolve(config.storage.uploadDir);
app.use('/uploads', express.static(uploadPath, {
  setHeaders: (res, filePath) => {
    // Set appropriate content type for audio files
    if (filePath.endsWith('.mp3')) res.setHeader('Content-Type', 'audio/mpeg');
    else if (filePath.endsWith('.wav')) res.setHeader('Content-Type', 'audio/wav');
    else if (filePath.endsWith('.m4a')) res.setHeader('Content-Type', 'audio/mp4');
    else if (filePath.endsWith('.aac')) res.setHeader('Content-Type', 'audio/aac');
    else if (filePath.endsWith('.flac')) res.setHeader('Content-Type', 'audio/flac');
    else if (filePath.endsWith('.ogg')) res.setHeader('Content-Type', 'audio/ogg');
    // Support range requests for audio streaming
    res.setHeader('Accept-Ranges', 'bytes');
  },
}));

// =============================================================================
// STRIPE WEBHOOK (must be before express.json, needs raw body)
// The stripe router handles this, but we need to register it with raw body parsing
// =============================================================================

app.post(
  `${config.apiPrefix}/stripe/webhook`,
  express.raw({ type: 'application/json' }),
  (req, res, next) => {
    // Pass to the stripe routes handler
    const stripeHandler = stripeRoutes;
    // The stripe router has a POST /webhook handler, but since we're mounting
    // it differently, we call the webhook logic directly
    import('./services/stripe.service').then(({ StripeService }) => {
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
  }
);

// =============================================================================
// API ROUTES
// =============================================================================

const prefix = config.apiPrefix;

app.use(`${prefix}/auth`, authRoutes);
app.use(`${prefix}/music`, musicRoutes);
app.use(`${prefix}/premium`, premiumRoutes);
app.use(`${prefix}/stripe`, (req, res, next) => {
  // Skip the webhook route since it's handled above
  if (req.path === '/webhook') {
    return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Use POST method for webhook.' } });
  }
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
  }));
});

// =============================================================================
// ERROR HANDLING
// =============================================================================

app.use(notFoundHandler);
app.use(errorHandler);

// =============================================================================
// START SERVER
// =============================================================================

const PORT = config.port;

app.listen(PORT, () => {
  console.log('');
  console.log('╔══════════════════════════════════════════╗');
  console.log('║          BeatBox Backend Server           ║');
  console.log('╠══════════════════════════════════════════╣');
  console.log(`║  Port:         ${PORT.toString().padEnd(25)}║`);
  console.log(`║  Environment:  ${config.env.padEnd(25)}║`);
  console.log(`║  API Prefix:   ${prefix.padEnd(25)}║`);
  console.log(`║  Stripe:       ${(config.stripe.enabled ? 'ENABLED' : 'DISABLED').padEnd(25)}║`);
  console.log(`║  Storage:      ${(config.storage.isS3Configured ? 'S3' : 'Local').padEnd(25)}║`);
  console.log('╚══════════════════════════════════════════╝');
  console.log('');
  console.log('  Music Upload: FREE (does not consume premium uses)');
  console.log('  Premium Free Uses: 5 per user');
  console.log('');
});

export default app;
