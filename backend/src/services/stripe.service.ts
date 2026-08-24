import Stripe from 'stripe';
import prisma from '../config/prisma';
import { config } from '../config';
import { AppError } from '../middleware/error';
import { ErrorCodes } from '../utils/response';

/**
 * =============================================================================
 * STRIPE SERVICE
 * =============================================================================
 *
 * Handles Stripe Checkout, Customer Portal, Webhooks, and Subscriptions.
 *
 * SECURITY: The Stripe Secret Key is ONLY used here on the backend.
 * It is NEVER exposed to the Android client.
 *
 * If Stripe is not configured (STRIPE_ENABLED=false), this service will
 * throw FEATURE_DISABLED errors. The rest of the application continues to work.
 * =============================================================================
 */

let stripeInstance: Stripe | null = null;

function getStripe(): Stripe {
  if (!config.stripe.enabled) {
    throw new AppError(
      ErrorCodes.STRIPE_NOT_CONFIGURED,
      'Stripe is not configured. Subscription features are currently unavailable. Music uploading and other features remain fully functional.',
      503
    );
  }
  if (!stripeInstance) {
    stripeInstance = new Stripe(config.stripe.secretKey, {
      apiVersion: '2024-06-20' as any,
    });
  }
  return stripeInstance;
}

export class StripeService {
  /**
   * Check if Stripe is enabled and configured.
   */
  static isEnabled(): boolean {
    return config.stripe.enabled && !!config.stripe.secretKey;
  }

  /**
   * Get public Stripe configuration (safe to send to client).
   */
  static getPublicConfig() {
    return {
      enabled: this.isEnabled(),
      publishableKey: config.stripe.publishableKey || null,
      plans: {
        monthly: {
          priceId: config.stripe.monthlyPriceId || null,
          name: 'Monthly',
          interval: 'month',
        },
        yearly: {
          priceId: config.stripe.yearlyPriceId || null,
          name: 'Yearly',
          interval: 'year',
        },
      },
    };
  }

  /**
   * Create a Stripe Checkout session for subscription.
   *
   * Flow: Android → Backend (this) → Stripe Checkout URL → User pays →
   *       Stripe webhook → Backend updates subscription → Android polls/receives update
   */
  static async createCheckoutSession(
    userId: string,
    plan: 'MONTHLY' | 'YEARLY',
    couponCode?: string,
    successUrl?: string,
    cancelUrl?: string
  ) {
    const stripe = getStripe();

    // Get or create Stripe customer
    let stripeCustomer = await prisma.stripeCustomer.findUnique({
      where: { userId },
    });

    // Get user email
    const user = await prisma.user.findUnique({
      where: { id: userId },
      select: { email: true },
    });

    if (!user) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'User not found.', 404);
    }

    if (!stripeCustomer) {
      const customer = await stripe.customers.create({
        email: user.email,
        metadata: {
          beatboxUserId: userId,
        },
      });

      stripeCustomer = await prisma.stripeCustomer.create({
        data: {
          userId,
          stripeCustomerId: customer.id,
          email: user.email,
        },
      });
    }

    // Determine price ID
    const priceId = plan === 'YEARLY' ? config.stripe.yearlyPriceId : config.stripe.monthlyPriceId;
    if (!priceId) {
      throw new AppError(
        ErrorCodes.STRIPE_NOT_CONFIGURED,
        `Stripe price ID for ${plan} plan is not configured. Please contact support.`,
        500
      );
    }

    // Build checkout session parameters
    const sessionParams: Stripe.Checkout.SessionCreateParams = {
      customer: stripeCustomer.stripeCustomerId,
      mode: 'subscription',
      line_items: [
        {
          price: priceId,
          quantity: 1,
        },
      ],
      success_url: successUrl || config.app.webhookSuccessUrl,
      cancel_url: cancelUrl || config.app.webhookCancelUrl,
      metadata: {
        beatboxUserId: userId,
        plan,
      },
      subscription_data: {
        metadata: {
          beatboxUserId: userId,
          plan,
        },
      },
    };

    // Apply coupon/promotion code if provided
    if (couponCode) {
      try {
        // Try to find as a promotion code first
        const promotionCodes = await stripe.promotionCodes.list({
          code: couponCode,
          active: true,
          limit: 1,
        });

        if (promotionCodes.data.length > 0) {
          sessionParams.discounts = [{ promotion_code: promotionCodes.data[0].id }];
        } else {
          // Try as a coupon code
          const coupon = await stripe.coupons.retrieve(couponCode);
          if (coupon.valid) {
            sessionParams.discounts = [{ coupon: coupon.id }];
          } else {
            throw new AppError(ErrorCodes.BAD_REQUEST, 'This coupon is no longer valid.', 400);
          }
        }
      } catch (err) {
        if (err instanceof AppError) throw err;
        throw new AppError(ErrorCodes.BAD_REQUEST, 'Invalid or expired coupon code.', 400);
      }
    }

    const session = await stripe.checkout.sessions.create(sessionParams);

    return {
      sessionId: session.id,
      url: session.url,
    };
  }

  /**
   * Create a Stripe Customer Portal session for subscription management.
   */
  static async createPortalSession(userId: string, returnUrl?: string) {
    const stripe = getStripe();

    const stripeCustomer = await prisma.stripeCustomer.findUnique({
      where: { userId },
    });

    if (!stripeCustomer) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'No billing account found. Please subscribe first.', 404);
    }

    const portalSession = await stripe.billingPortal.sessions.create({
      customer: stripeCustomer.stripeCustomerId,
      return_url: returnUrl || config.app.url,
    });

    return {
      url: portalSession.url,
    };
  }

  /**
   * Verify webhook signature and parse the event.
   */
  static constructWebhookEvent(payload: Buffer | string, signature: string): Stripe.Event {
    const stripe = getStripe();

    if (!config.stripe.webhookSecret) {
      throw new AppError(ErrorCodes.FEATURE_DISABLED, 'Webhook secret is not configured.', 500);
    }

    return stripe.webhooks.constructEvent(
      payload,
      signature,
      config.stripe.webhookSecret
    );
  }

  /**
   * Process a webhook event.
   * This is idempotent - if the event has already been processed, it returns early.
   */
  static async processWebhookEvent(event: Stripe.Event): Promise<void> {
    // Check idempotency - has this event already been processed?
    const existingEvent = await prisma.webhookEvent.findUnique({
      where: { stripeEventId: event.id },
    });

    if (existingEvent?.processed) {
      // Already processed - return early (idempotent)
      return;
    }

    // Store the event
    const webhookEvent = await prisma.webhookEvent.upsert({
      where: { stripeEventId: event.id },
      create: {
        stripeEventId: event.id,
        type: event.type,
        payload: JSON.stringify(event),
      },
      update: {
        type: event.type,
        payload: JSON.stringify(event),
      },
    });

    try {
      switch (event.type) {
        case 'checkout.session.completed':
          await this.handleCheckoutCompleted(event.data.object as Stripe.Checkout.Session);
          break;

        case 'customer.subscription.created':
          await this.handleSubscriptionCreated(event.data.object as Stripe.Subscription);
          break;

        case 'customer.subscription.updated':
          await this.handleSubscriptionUpdated(event.data.object as Stripe.Subscription);
          break;

        case 'customer.subscription.deleted':
          await this.handleSubscriptionDeleted(event.data.object as Stripe.Subscription);
          break;

        case 'invoice.paid':
          await this.handleInvoicePaid(event.data.object as Stripe.Invoice);
          break;

        case 'invoice.payment_failed':
          await this.handleInvoicePaymentFailed(event.data.object as Stripe.Invoice);
          break;

        default:
          // Unhandled event type - log but don't fail
          console.log(`[Webhook] Unhandled event type: ${event.type}`);
      }

      // Mark as processed
      await prisma.webhookEvent.update({
        where: { id: webhookEvent.id },
        data: {
          processed: true,
          processedAt: new Date(),
        },
      });
    } catch (error) {
      console.error(`[Webhook] Error processing event ${event.id}:`, error);

      await prisma.webhookEvent.update({
        where: { id: webhookEvent.id },
        data: {
          error: error instanceof Error ? error.message : 'Unknown error',
        },
      });

      throw error;
    }
  }

  /**
   * Handle checkout.session.completed
   */
  private static async handleCheckoutCompleted(session: Stripe.Checkout.Session) {
    const userId = session.metadata?.beatboxUserId;
    if (!userId) return;

    // If this is a subscription checkout, the subscription events will handle the rest
    // But we can update the subscription status immediately if we have a subscription ID
    if (session.subscription) {
      const stripe = getStripe();
      const subscription = await stripe.subscriptions.retrieve(session.subscription as string);
      await this.syncSubscription(userId, subscription);
    }
  }

  /**
   * Handle customer.subscription.created
   */
  private static async handleSubscriptionCreated(subscription: Stripe.Subscription) {
    const userId = subscription.metadata?.beatboxUserId;
    if (!userId) {
      // Try to find user via customer
      const customerRecord = await prisma.stripeCustomer.findUnique({
        where: { stripeCustomerId: subscription.customer as string },
      });
      if (!customerRecord) return;
      await this.syncSubscription(customerRecord.userId, subscription);
    } else {
      await this.syncSubscription(userId, subscription);
    }
  }

  /**
   * Handle customer.subscription.updated
   */
  private static async handleSubscriptionUpdated(subscription: Stripe.Subscription) {
    const userId = subscription.metadata?.beatboxUserId;
    if (!userId) {
      const customerRecord = await prisma.stripeCustomer.findUnique({
        where: { stripeCustomerId: subscription.customer as string },
      });
      if (!customerRecord) return;
      await this.syncSubscription(customerRecord.userId, subscription);
    } else {
      await this.syncSubscription(userId, subscription);
    }
  }

  /**
   * Handle customer.subscription.deleted
   */
  private static async handleSubscriptionDeleted(subscription: Stripe.Subscription) {
    const userId = subscription.metadata?.beatboxUserId;
    let targetUserId = userId;

    if (!targetUserId) {
      const customerRecord = await prisma.stripeCustomer.findUnique({
        where: { stripeCustomerId: subscription.customer as string },
      });
      targetUserId = customerRecord?.userId;
    }

    if (!targetUserId) return;

    await prisma.subscription.update({
      where: { userId: targetUserId },
      data: {
        status: 'EXPIRED',
        plan: 'FREE',
        stripeSubscriptionId: null,
        currentPeriodEnd: null,
        canceledAt: new Date(),
        cancelAtPeriodEnd: false,
      },
    });
  }

  /**
   * Handle invoice.paid
   */
  private static async handleInvoicePaid(invoice: Stripe.Invoice) {
    const customerRecord = await prisma.stripeCustomer.findUnique({
      where: { stripeCustomerId: invoice.customer as string },
    });

    if (!customerRecord) return;

    // Record the payment
    await prisma.paymentRecord.create({
      data: {
        userId: customerRecord.userId,
        stripeInvoiceId: invoice.id,
        stripeChargeId: invoice.charge as string,
        stripePaymentIntentId: invoice.payment_intent as string,
        amount: invoice.amount_paid,
        currency: invoice.currency,
        status: 'paid',
        description: invoice.description || `BeatBox Premium - ${invoice.lines?.data?.[0]?.plan?.interval || 'subscription'}`,
        invoiceUrl: invoice.hosted_invoice_url,
        invoicePdfUrl: invoice.invoice_pdf,
        periodStart: invoice.period_start ? new Date(invoice.period_start * 1000) : null,
        periodEnd: invoice.period_end ? new Date(invoice.period_end * 1000) : null,
      },
    });

    // Create notification
    await prisma.notification.create({
      data: {
        userId: customerRecord.userId,
        type: 'SUBSCRIPTION_RENEWED',
        title: 'Payment Successful',
        body: 'Your BeatBox Premium subscription has been renewed.',
      },
    });
  }

  /**
   * Handle invoice.payment_failed
   */
  private static async handleInvoicePaymentFailed(invoice: Stripe.Invoice) {
    const customerRecord = await prisma.stripeCustomer.findUnique({
      where: { stripeCustomerId: invoice.customer as string },
    });

    if (!customerRecord) return;

    // Record the failed payment
    await prisma.paymentRecord.create({
      data: {
        userId: customerRecord.userId,
        stripeInvoiceId: invoice.id,
        amount: invoice.amount_due,
        currency: invoice.currency,
        status: 'failed',
        description: invoice.description || 'BeatBox Premium subscription',
        failureReason: invoice.last_finalization_error?.message || 'Payment failed',
        invoiceUrl: invoice.hosted_invoice_url,
      },
    });

    // Update subscription status to PAST_DUE
    await prisma.subscription.update({
      where: { userId: customerRecord.userId },
      data: { status: 'PAST_DUE' },
    });

    // Create notification
    await prisma.notification.create({
      data: {
        userId: customerRecord.userId,
        type: 'PAYMENT_FAILED',
        title: 'Payment Failed',
        body: 'Your BeatBox Premium payment failed. Please update your payment method to avoid losing premium access.',
      },
    });
  }

  /**
   * Sync subscription data from Stripe to our database.
   */
  private static async syncSubscription(userId: string, subscription: Stripe.Subscription) {
    const status = this.mapStripeStatus(subscription.status);
    const plan = subscription.metadata?.plan === 'YEARLY' ? 'YEARLY' : 'MONTHLY';

    const priceId = subscription.items.data[0]?.price?.id;
    const currentPeriodStart = subscription.current_period_start
      ? new Date(subscription.current_period_start * 1000)
      : null;
    const currentPeriodEnd = subscription.current_period_end
      ? new Date(subscription.current_period_end * 1000)
      : null;

    await prisma.subscription.upsert({
      where: { userId },
      create: {
        userId,
        status,
        plan,
        stripeSubscriptionId: subscription.id,
        stripePriceId: priceId,
        currentPeriodStart,
        currentPeriodEnd,
        cancelAtPeriodEnd: subscription.cancel_at_period_end,
        trialEnd: subscription.trial_end ? new Date(subscription.trial_end * 1000) : null,
      },
      update: {
        status,
        plan,
        stripeSubscriptionId: subscription.id,
        stripePriceId: priceId,
        currentPeriodStart,
        currentPeriodEnd,
        cancelAtPeriodEnd: subscription.cancel_at_period_end,
        trialEnd: subscription.trial_end ? new Date(subscription.trial_end * 1000) : null,
        canceledAt: subscription.canceled_at ? new Date(subscription.canceled_at * 1000) : null,
      },
    });

    // If subscription is active, create a notification (for new subscriptions)
    if (status === 'ACTIVE') {
      const existing = await prisma.subscription.findUnique({ where: { userId } });
      if (existing && existing.status !== 'ACTIVE') {
        await prisma.notification.create({
          data: {
            userId,
            type: 'SUBSCRIPTION_RENEWED',
            title: 'Welcome to BeatBox Premium!',
            body: 'Your premium subscription is now active. Enjoy unlimited premium features!',
          },
        }).catch(() => {}); // Non-critical
      }
    }
  }

  /**
   * Map Stripe subscription status to our internal status.
   */
  private static mapStripeStatus(stripeStatus: string): 'ACTIVE' | 'PAST_DUE' | 'CANCELED' | 'EXPIRED' {
    switch (stripeStatus) {
      case 'active':
      case 'trialing':
        return 'ACTIVE';
      case 'past_due':
        return 'PAST_DUE';
      case 'canceled':
        return 'CANCELED';
      case 'unpaid':
      case 'incomplete':
      case 'incomplete_expired':
        return 'EXPIRED';
      default:
        return 'EXPIRED';
    }
  }
}
