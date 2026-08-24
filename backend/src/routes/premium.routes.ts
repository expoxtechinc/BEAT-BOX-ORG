import { Router, Request, Response } from 'express';
import { PremiumService } from '../services/premium.service';
import { authenticate, requireAdmin, AuthenticatedRequest } from '../middleware/auth';
import { asyncHandler, AppError } from '../middleware/error';
import { ok, paginated, ErrorCodes } from '../utils/response';

const router = Router();

// Get premium status (free uses, subscription, canUsePremium)
router.get('/status', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const status = await PremiumService.getPremiumStatus(req.userId!);
  res.json(ok(status));
}));

// Get all premium features (public)
router.get('/features', asyncHandler(async (req: Request, res: Response) => {
  const features = await PremiumService.getPremiumFeatures();
  res.json(ok(features));
}));

// Get usage history
router.get('/usage-history', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const result = await PremiumService.getUsageHistory(req.userId!, page, limit);
  res.json(paginated(result.usages, page, limit, result.total));
}));

// Check and consume a premium feature use
// CRITICAL: This endpoint is for PREMIUM features ONLY.
// Music upload should NEVER call this endpoint.
router.post('/use', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const { featureKey } = req.body;
  if (!featureKey) {
    throw new AppError(ErrorCodes.VALIDATION_ERROR, 'featureKey is required.', 400);
  }

  // This will throw PREMIUM_REQUIRED (402) if user has 0 free uses and is not subscribed
  const result = await PremiumService.checkAndConsume(req.userId!, featureKey);

  res.json(ok({
    consumed: result.consumed,
    remainingUses: result.remainingUses,
    isSubscribed: result.isSubscribed,
    requiresSubscription: result.requiresSubscription,
  }));
}));

// Check if user can use a premium feature (without consuming)
router.get('/can-use', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const result = await PremiumService.canUsePremiumFeature(req.userId!);
  res.json(ok(result));
}));

// Admin: Reset free uses for a user
router.post('/reset', authenticate, requireAdmin, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const { userId } = req.body;
  if (!userId) {
    throw new AppError(ErrorCodes.VALIDATION_ERROR, 'userId is required.', 400);
  }
  const resetTo = await PremiumService.resetFreeUses(userId);
  res.json(ok({ userId, premiumFreeUses: resetTo, message: 'Free uses reset successfully.' }));
}));

export default router;
