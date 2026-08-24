import { Router, Request, Response } from 'express';
import { authenticate, AuthenticatedRequest } from '../middleware/auth';
import { asyncHandler, AppError } from '../middleware/error';
import { ok, ErrorCodes } from '../utils/response';
import prisma from '../config/prisma';

const router = Router();

// Report music
router.post('/music/:musicId', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const { reason, description } = req.body;

  const music = await prisma.music.findUnique({ where: { id: req.params.musicId } });
  if (!music) throw new AppError(ErrorCodes.NOT_FOUND, 'Music not found.', 404);

  // Check if already reported by this user
  const existing = await prisma.report.findFirst({
    where: { reporterId: req.userId!, reportedMusicId: req.params.musicId },
  });
  if (existing) {
    throw new AppError(ErrorCodes.CONFLICT, 'You have already reported this content.', 409);
  }

  const report = await prisma.report.create({
    data: {
      reporterId: req.userId!,
      reportedMusicId: req.params.musicId,
      reportedUserId: music.userId,
      reason,
      description,
    },
  });

  // Mark music as reported
  await prisma.music.update({
    where: { id: req.params.musicId },
    data: { isReported: true },
  });

  res.status(201).json(ok(report));
}));

// Report user
router.post('/user/:userId', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const { reason, description } = req.body;

  const user = await prisma.user.findUnique({ where: { id: req.params.userId, isDeleted: false } });
  if (!user) throw new AppError(ErrorCodes.NOT_FOUND, 'User not found.', 404);

  const report = await prisma.report.create({
    data: {
      reporterId: req.userId!,
      reportedUserId: req.params.userId,
      reason,
      description,
    },
  });

  res.status(201).json(ok(report));
}));

// Block user
router.post('/block/:userId', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  if (req.userId === req.params.userId) {
    throw new AppError(ErrorCodes.BAD_REQUEST, 'You cannot block yourself.', 400);
  }

  try {
    await prisma.blockedUser.create({
      data: {
        userId: req.userId!,
        blockedId: req.params.userId,
      },
    });

    // Also unfollow if following
    await prisma.follow.deleteMany({
      where: {
        OR: [
          { followerId: req.userId!, followingId: req.params.userId },
          { followerId: req.params.userId, followingId: req.userId! },
        ],
      },
    });
  } catch (err: any) {
    if (err.code === 'P2002') {
      // Already blocked
    } else {
      throw err;
    }
  }

  res.json(ok({ success: true, message: 'User blocked.' }));
}));

// Unblock user
router.delete('/block/:userId', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  await prisma.blockedUser.deleteMany({
    where: { userId: req.userId!, blockedId: req.params.userId },
  });
  res.json(ok({ success: true, message: 'User unblocked.' }));
}));

// Get blocked users
router.get('/blocked', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const blocked = await prisma.blockedUser.findMany({
    where: { userId: req.userId! },
    include: {
      blocked: {
        select: {
          id: true,
          profile: { select: { username: true, displayName: true, avatarUrl: true } },
        },
      },
    },
  });
  res.json(ok(blocked.map((b) => b.blocked)));
}));

export default router;
