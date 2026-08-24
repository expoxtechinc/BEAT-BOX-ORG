import prisma from '../config/prisma';
import { config } from '../config';
import { AppError } from '../middleware/error';
import { ErrorCodes } from '../utils/response';

/**
 * =============================================================================
 * PREMIUM SERVICE
 * =============================================================================
 *
 * CRITICAL BUSINESS RULES:
 *
 * 1. Music upload is ALWAYS FREE. It does NOT consume premium uses.
 * 2. All other premium features start with 5 free uses.
 * 3. Each premium feature use decrements premiumFreeUses by 1.
 * 4. When premiumFreeUses reaches 0, subscription is required.
 * 5. Active subscribers get unlimited premium access.
 * 6. The backend is authoritative - the counter cannot be bypassed by
 *    clearing app data, reinstalling, or changing devices.
 *
 * All decrement operations use atomic database operations (Prisma updateMany
 * with WHERE clause) to prevent race conditions.
 * =============================================================================
 */
export class PremiumService {
  /**
   * Get the user's premium status including remaining free uses
   * and whether they have an active subscription.
   */
  static async getPremiumStatus(userId: string) {
    const user = await prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true,
        premiumFreeUses: true,
        subscription: {
          select: {
            status: true,
            plan: true,
            currentPeriodEnd: true,
            cancelAtPeriodEnd: true,
          },
        },
      },
    });

    if (!user) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'User not found.', 404);
    }

    const isSubscribed = this.isSubscriptionActive(user.subscription?.status);

    return {
      premiumFreeUses: user.premiumFreeUses,
      isSubscribed,
      subscription: user.subscription,
      canUsePremium: isSubscribed || user.premiumFreeUses > 0,
      // Music upload is ALWAYS free - this flag is always true
      canUploadMusic: true,
    };
  }

  /**
   * Check if a subscription status means the user has active premium access.
   */
  static isSubscriptionActive(status?: string): boolean {
    return status === 'ACTIVE' || status === 'PAST_DUE'; // PAST_DUE still has access during grace period
  }

  /**
   * Check if a user can use a premium feature.
   * Returns true if they have free uses remaining OR are subscribed.
   */
  static async canUsePremiumFeature(userId: string): Promise<{ canUse: boolean; reason?: string; remainingUses: number; isSubscribed: boolean }> {
    const user = await prisma.user.findUnique({
      where: { id: userId },
      select: {
        premiumFreeUses: true,
        subscription: { select: { status: true } },
      },
    });

    if (!user) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'User not found.', 404);
    }

    const isSubscribed = this.isSubscriptionActive(user.subscription?.status);

    if (isSubscribed) {
      return { canUse: true, remainingUses: user.premiumFreeUses, isSubscribed: true };
    }

    if (user.premiumFreeUses > 0) {
      return { canUse: true, remainingUses: user.premiumFreeUses, isSubscribed: false };
    }

    return {
      canUse: false,
      reason: 'You have used all 5 free premium uses. Subscribe to BeatBox Premium to continue.',
      remainingUses: 0,
      isSubscribed: false,
    };
  }

  /**
   * ATOMIC premium use consumption.
   *
   * Uses a conditional update (updateMany with WHERE clause) to ensure
   * the decrement is atomic and cannot be bypassed by concurrent requests.
   *
   * The WHERE clause ensures we only decrement if:
   * - The user is NOT subscribed (subscribers don't consume free uses)
   * - The user has premiumFreeUses > 0
   *
   * If the update affects 0 rows, it means either:
   * - The user is subscribed (no consumption needed - OK)
   * - The user has 0 free uses (subscription required - BLOCK)
   */
  static async consumePremiumUse(userId: string, featureKey: string): Promise<{
    consumed: boolean;
    remainingUses: number;
    isSubscribed: boolean;
    requiresSubscription: boolean;
  }> {
    // First check if user is subscribed
    const user = await prisma.user.findUnique({
      where: { id: userId },
      select: {
        premiumFreeUses: true,
        subscription: { select: { status: true } },
      },
    });

    if (!user) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'User not found.', 404);
    }

    const isSubscribed = this.isSubscriptionActive(user.subscription?.status);

    // Subscribers don't consume free uses
    if (isSubscribed) {
      // Log the usage for analytics (but don't decrement)
      await prisma.premiumUsage.create({
        data: {
          userId,
          featureKey,
        },
      });

      return {
        consumed: false,
        remainingUses: user.premiumFreeUses,
        isSubscribed: true,
        requiresSubscription: false,
      };
    }

    // ATOMIC DECREMENT: Only decrement if premiumFreeUses > 0
    // This prevents race conditions - two concurrent requests cannot both
    // decrement from the same value because the WHERE clause is checked atomically.
    const result = await prisma.user.updateMany({
      where: {
        id: userId,
        premiumFreeUses: { gt: 0 },
      },
      data: {
        premiumFreeUses: { decrement: 1 },
      },
    });

    if (result.count === 0) {
      // User has 0 free uses and is not subscribed - subscription required
      return {
        consumed: false,
        remainingUses: 0,
        isSubscribed: false,
        requiresSubscription: true,
      };
    }

    // Get the updated value
    const updatedUser = await prisma.user.findUnique({
      where: { id: userId },
      select: { premiumFreeUses: true },
    });

    // Log the usage
    await prisma.premiumUsage.create({
      data: {
        userId,
        featureKey,
      },
    });

    return {
      consumed: true,
      remainingUses: updatedUser?.premiumFreeUses ?? 0,
      isSubscribed: false,
      requiresSubscription: false,
    };
  }

  /**
   * Check and consume a premium feature in one operation.
   * Throws PREMIUM_REQUIRED error if the user cannot use the feature.
   *
   * IMPORTANT: This is the method that should be called by premium feature endpoints.
   * Music upload endpoints should NOT call this method.
   */
  static async checkAndConsume(userId: string, featureKey: string) {
    const result = await this.consumePremiumUse(userId, featureKey);

    if (result.requiresSubscription) {
      throw new AppError(
        ErrorCodes.PREMIUM_REQUIRED,
        'You have used all 5 free premium uses. Subscribe to BeatBox Premium to continue.',
        402 // Payment Required
      );
    }

    return result;
  }

  /**
   * Reset a user's premium free uses (admin only).
   * Useful for testing or customer support.
   */
  static async resetFreeUses(userId: string): Promise<number> {
    await prisma.user.update({
      where: { id: userId },
      data: { premiumFreeUses: config.premium.freeUses },
    });
    return config.premium.freeUses;
  }

  /**
   * Get all premium features (for the client to display).
   */
  static async getPremiumFeatures() {
    return prisma.premiumFeature.findMany({
      where: { isActive: true },
      orderBy: { sortOrder: 'asc' },
    });
  }

  /**
   * Get a user's premium usage history.
   */
  static async getUsageHistory(userId: string, page = 1, limit = 20) {
    const [usages, total] = await Promise.all([
      prisma.premiumUsage.findMany({
        where: { userId },
        orderBy: { usedAt: 'desc' },
        skip: (page - 1) * limit,
        take: limit,
      }),
      prisma.premiumUsage.count({ where: { userId } }),
    ]);

    return { usages, total, page, limit };
  }
}
