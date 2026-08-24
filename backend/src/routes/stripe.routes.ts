import { Router, Request, Response } from 'express';
import { StripeService } from '../services/stripe.service';
import { authenticate, AuthenticatedRequest } from '../middleware/auth';
import { asyncHandler, AppError } from '../middleware/error';
import { ok, ErrorCodes } from '../utils/response';

const router = Router();

// Get public Stripe configuration (safe for client)
router.get('/config', asyncHandler(async (req: Request, res: Response) => {
  const config = StripeService.getPublicConfig();
  res.json(ok(config));
}));

// Create checkout session
router.post('/checkout', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const { plan, couponCode, successUrl, cancelUrl } = req.body;

  if (!plan || !['MONTHLY', 'YEARLY'].includes(plan)) {
    throw new AppError(ErrorCodes.VALIDATION_ERROR, 'Invalid plan. Choose MONTHLY or YEARLY.', 400);
  }

  const result = await StripeService.createCheckoutSession(
    req.userId!,
    plan,
    couponCode,
    successUrl,
    cancelUrl
  );
  res.json(ok(result));
}));

// Create customer portal session
router.post('/portal', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const { returnUrl } = req.body;
  const result = await StripeService.createPortalSession(req.userId!, returnUrl);
  res.json(ok(result));
}));

// Stripe webhook handler - uses raw body
// This route must be registered BEFORE express.json() middleware
router.post('/webhook', (req: Request, res: Response) => {
  // The raw body is set by express.raw middleware configured per-route
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
    .then(() => {
      res.json({ received: true });
    })
    .catch((err) => {
      console.error('[Stripe Webhook] Processing failed:', err);
      res.status(500).json({ received: false, error: 'Webhook processing failed.' });
    });
});

export default router;
