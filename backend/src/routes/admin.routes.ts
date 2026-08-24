import { Router, Request, Response } from 'express';
import { authenticate, requireAdmin, AuthenticatedRequest } from '../middleware/auth';
import { asyncHandler, AppError } from '../middleware/error';
import { ok, paginated, ErrorCodes } from '../utils/response';
import prisma from '../config/prisma';

const router = Router();

// All admin routes require authentication and admin role
router.use(authenticate, requireAdmin);

// =============================================================================
// DASHBOARD STATS
// =============================================================================

router.get('/stats', asyncHandler(async (req: Request, res: Response) => {
  const [
    totalUsers,
    totalMusic,
    totalSubscribers,
    totalRevenue,
    pendingReports,
    totalPlays,
    newUsersToday,
    newMusicToday,
  ] = await Promise.all([
    prisma.user.count({ where: { isDeleted: false } }),
    prisma.music.count({ where: { isRemoved: false } }),
    prisma.subscription.count({ where: { status: 'ACTIVE' } }),
    prisma.paymentRecord.aggregate({ where: { status: 'paid' }, _sum: { amount: true } }),
    prisma.report.count({ where: { status: 'PENDING' } }),
    prisma.music.aggregate({ _sum: { playCount: true } }),
    prisma.user.count({
      where: {
        createdAt: { gte: new Date(new Date().setHours(0, 0, 0, 0)) },
        isDeleted: false,
      },
    }),
    prisma.music.count({
      where: {
        createdAt: { gte: new Date(new Date().setHours(0, 0, 0, 0)) },
      },
    }),
  ]);

  res.json(ok({
    totalUsers,
    totalMusic,
    totalSubscribers,
    totalRevenue: totalRevenue._sum.amount || 0,
    totalPlays: totalPlays._sum.playCount || 0,
    pendingReports,
    newUsersToday,
    newMusicToday,
  }));
}));

// =============================================================================
// USER MANAGEMENT
// =============================================================================

router.get('/users', asyncHandler(async (req: Request, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const search = req.query.search as string;

  const where: any = { isDeleted: false };
  if (search) {
    where.OR = [
      { email: { contains: search } },
      { profile: { username: { contains: search } } },
      { profile: { displayName: { contains: search } } },
    ];
  }

  const [users, total] = await Promise.all([
    prisma.user.findMany({
      where,
      select: {
        id: true,
        email: true,
        role: true,
        emailVerified: true,
        premiumFreeUses: true,
        isDeleted: true,
        createdAt: true,
        profile: true,
        subscription: true,
        _count: {
          select: { music: { where: { isRemoved: false } } },
        },
      },
      skip: (page - 1) * limit,
      take: limit,
      orderBy: { createdAt: 'desc' },
    }),
    prisma.user.count({ where }),
  ]);

  res.json(paginated(users, page, limit, total));
}));

// Get user details
router.get('/users/:id', asyncHandler(async (req: Request, res: Response) => {
  const user = await prisma.user.findUnique({
    where: { id: req.params.id },
    select: {
      id: true,
      email: true,
      role: true,
      emailVerified: true,
      premiumFreeUses: true,
      isDeleted: true,
      createdAt: true,
      profile: true,
      subscription: true,
      stripeCustomer: true,
      _count: {
        select: {
          music: { where: { isRemoved: false } },
          followers: true,
          following: true,
          likes: true,
          favorites: true,
        },
      },
    },
  });

  if (!user) throw new AppError(ErrorCodes.NOT_FOUND, 'User not found.', 404);
  res.json(ok(user));
}));

// Toggle admin role
router.patch('/users/:id/role', asyncHandler(async (req: Request, res: Response) => {
  const { role } = req.body;
  if (!['USER', 'ADMIN'].includes(role)) {
    throw new AppError(ErrorCodes.VALIDATION_ERROR, 'Invalid role.', 400);
  }
  const user = await prisma.user.update({
    where: { id: req.params.id },
    data: { role },
    select: { id: true, role: true },
  });
  res.json(ok(user));
}));

// Ban/block user (soft delete)
router.patch('/users/:id/ban', asyncHandler(async (req: Request, res: Response) => {
  await prisma.user.update({
    where: { id: req.params.id },
    data: {
      isDeleted: true,
      deletedAt: new Date(),
    },
  });
  await prisma.session.deleteMany({ where: { userId: req.params.id } });
  res.json(ok({ success: true, message: 'User banned.' }));
}));

// Unban user
router.patch('/users/:id/unban', asyncHandler(async (req: Request, res: Response) => {
  await prisma.user.update({
    where: { id: req.params.id },
    data: {
      isDeleted: false,
      deletedAt: null,
    },
  });
  res.json(ok({ success: true, message: 'User unbanned.' }));
}));

// =============================================================================
// MUSIC MANAGEMENT
// =============================================================================

router.get('/music', asyncHandler(async (req: Request, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;

  const [music, total] = await Promise.all([
    prisma.music.findMany({
      where: { isRemoved: false },
      include: {
        user: {
          select: {
            id: true,
            profile: { select: { username: true, displayName: true } },
          },
        },
        category: true,
      },
      skip: (page - 1) * limit,
      take: limit,
      orderBy: { createdAt: 'desc' },
    }),
    prisma.music.count({ where: { isRemoved: false } }),
  ]);

  res.json(paginated(music, page, limit, total));
}));

// Feature/unfeature music
router.patch('/music/:id/feature', asyncHandler(async (req: Request, res: Response) => {
  const music = await prisma.music.update({
    where: { id: req.params.id },
    data: { isFeatured: req.body.isFeatured },
  });
  res.json(ok(music));
}));

// Remove music (admin moderation)
router.delete('/music/:id', asyncHandler(async (req: Request, res: Response) => {
  await prisma.music.update({
    where: { id: req.params.id },
    data: { isRemoved: true, isPublished: false },
  });
  res.json(ok({ success: true, message: 'Music removed.' }));
}));

// Restore music
router.patch('/music/:id/restore', asyncHandler(async (req: Request, res: Response) => {
  await prisma.music.update({
    where: { id: req.params.id },
    data: { isRemoved: false, isPublished: true },
  });
  res.json(ok({ success: true, message: 'Music restored.' }));
}));

// =============================================================================
// CATEGORY MANAGEMENT
// =============================================================================

router.get('/categories', asyncHandler(async (req: Request, res: Response) => {
  const categories = await prisma.category.findMany({
    orderBy: { sortOrder: 'asc' },
    include: { _count: { select: { music: { where: { isRemoved: false } } } } },
  });
  res.json(ok(categories));
}));

router.post('/categories', asyncHandler(async (req: Request, res: Response) => {
  const { name, description, slug, iconUrl, sortOrder } = req.body;
  const category = await prisma.category.create({
    data: {
      name,
      slug: slug || name.toLowerCase().replace(/\s+/g, '-'),
      description,
      iconUrl,
      sortOrder: sortOrder || 0,
    },
  });
  res.status(201).json(ok(category));
}));

router.put('/categories/:id', asyncHandler(async (req: Request, res: Response) => {
  const { name, description, slug, iconUrl, sortOrder, isActive } = req.body;
  const category = await prisma.category.update({
    where: { id: req.params.id },
    data: { name, description, slug, iconUrl, sortOrder, isActive },
  });
  res.json(ok(category));
}));

router.delete('/categories/:id', asyncHandler(async (req: Request, res: Response) => {
  await prisma.category.delete({ where: { id: req.params.id } });
  res.json(ok({ success: true, message: 'Category deleted.' }));
}));

// =============================================================================
// PREMIUM FEATURES MANAGEMENT
// =============================================================================

router.get('/premium-features', asyncHandler(async (req: Request, res: Response) => {
  const features = await prisma.premiumFeature.findMany({
    orderBy: { sortOrder: 'asc' },
  });
  res.json(ok(features));
}));

router.post('/premium-features', asyncHandler(async (req: Request, res: Response) => {
  const { key, name, description, iconUrl, sortOrder } = req.body;
  const feature = await prisma.premiumFeature.create({
    data: { key, name, description, iconUrl, sortOrder: sortOrder || 0 },
  });
  res.status(201).json(ok(feature));
}));

router.put('/premium-features/:id', asyncHandler(async (req: Request, res: Response) => {
  const { name, description, iconUrl, sortOrder, isActive } = req.body;
  const feature = await prisma.premiumFeature.update({
    where: { id: req.params.id },
    data: { name, description, iconUrl, sortOrder, isActive },
  });
  res.json(ok(feature));
}));

router.delete('/premium-features/:id', asyncHandler(async (req: Request, res: Response) => {
  await prisma.premiumFeature.delete({ where: { id: req.params.id } });
  res.json(ok({ success: true, message: 'Feature deleted.' }));
}));

// =============================================================================
// REPORTS / MODERATION
// =============================================================================

router.get('/reports', asyncHandler(async (req: Request, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const status = req.query.status as string;

  const where: any = {};
  if (status) where.status = status;

  const [reports, total] = await Promise.all([
    prisma.report.findMany({
      where,
      include: {
        reporter: { select: { id: true, profile: { select: { username: true, displayName: true } } } },
        music: { select: { id: true, title: true, artistName: true, artworkUrl: true } },
      },
      skip: (page - 1) * limit,
      take: limit,
      orderBy: { createdAt: 'desc' },
    }),
    prisma.report.count({ where }),
  ]);

  res.json(paginated(reports, page, limit, total));
}));

router.patch('/reports/:id', asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const { status, resolution } = req.body;
  const report = await prisma.report.update({
    where: { id: req.params.id },
    data: {
      status,
      resolution,
      resolvedBy: req.userId,
      resolvedAt: new Date(),
    },
  });

  // If resolving with music removal
  if (status === 'RESOLVED' && report.reportedMusicId && req.body.removeMusic) {
    await prisma.music.update({
      where: { id: report.reportedMusicId },
      data: { isRemoved: true, isPublished: false },
    });
  }

  res.json(ok(report));
}));

// =============================================================================
// SUBSCRIPTIONS & PAYMENTS
// =============================================================================

router.get('/subscriptions', asyncHandler(async (req: Request, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const status = req.query.status as string;

  const where: any = {};
  if (status && status !== 'ALL') where.status = status;

  const [subscriptions, total] = await Promise.all([
    prisma.subscription.findMany({
      where,
      include: {
        user: {
          select: {
            id: true,
            email: true,
            profile: { select: { username: true, displayName: true } },
          },
        },
      },
      skip: (page - 1) * limit,
      take: limit,
      orderBy: { updatedAt: 'desc' },
    }),
    prisma.subscription.count({ where }),
  ]);

  res.json(paginated(subscriptions, page, limit, total));
}));

router.get('/payments', asyncHandler(async (req: Request, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;

  const [payments, total] = await Promise.all([
    prisma.paymentRecord.findMany({
      skip: (page - 1) * limit,
      take: limit,
      orderBy: { createdAt: 'desc' },
    }),
    prisma.paymentRecord.count(),
  ]);

  res.json(paginated(payments, page, limit, total));
}));

// =============================================================================
// APP CONFIGURATION
// =============================================================================

router.get('/config', asyncHandler(async (req: Request, res: Response) => {
  const configs = await prisma.appConfig.findMany();
  res.json(ok(configs));
}));

router.put('/config/:key', asyncHandler(async (req: Request, res: Response) => {
  const { value, description, isPublic } = req.body;
  const config = await prisma.appConfig.upsert({
    where: { key: req.params.key },
    create: { key: req.params.key, value, description, isPublic: isPublic ?? true },
    update: { value, description, isPublic },
  });
  res.json(ok(config));
}));

export default router;
