import { Router, Request, Response } from 'express';
import { authenticate, optionalAuth, AuthenticatedRequest } from '../middleware/auth';
import { asyncHandler } from '../middleware/error';
import { ok, paginated } from '../utils/response';
import prisma from '../config/prisma';

const router = Router();

// Universal search - searches music, users, playlists
router.get('/', optionalAuth, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const query = (req.query.q as string) || '';
  const type = req.query.type as string | undefined; // 'music', 'users', 'playlists', 'all'
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;

  if (!query.trim()) {
    return res.json(ok({ music: [], users: [], playlists: [] }));
  }

  const results: any = {};

  if (!type || type === 'music' || type === 'all') {
    const music = await prisma.music.findMany({
      where: {
        isPublished: true,
        isRemoved: false,
        OR: [
          { title: { contains: query } },
          { artistName: { contains: query } },
          { albumName: { contains: query } },
          { genre: { contains: query } },
        ],
      },
      take: type === 'music' ? limit : 10,
      orderBy: { playCount: 'desc' },
      include: {
        category: true,
        user: {
          select: {
            id: true,
            profile: { select: { username: true, displayName: true, avatarUrl: true } },
          },
        },
      },
    });
    results.music = music;
  }

  if (!type || type === 'users' || type === 'all') {
    const users = await prisma.user.findMany({
      where: {
        isDeleted: false,
        OR: [
          { profile: { username: { contains: query } } },
          { profile: { displayName: { contains: query } } },
        ],
      },
      take: type === 'users' ? limit : 10,
      select: {
        id: true,
        profile: {
          select: { username: true, displayName: true, avatarUrl: true, bio: true },
        },
        _count: { select: { followers: true, music: { where: { isPublished: true, isRemoved: false } } } },
      },
    });
    results.users = users;
  }

  if (!type || type === 'playlists' || type === 'all') {
    const playlists = await prisma.playlist.findMany({
      where: {
        isPublic: true,
        OR: [
          { name: { contains: query } },
          { description: { contains: query } },
        ],
      },
      take: type === 'playlists' ? limit : 10,
      include: {
        _count: { select: { items: true } },
        user: {
          select: {
            id: true,
            profile: { select: { username: true, displayName: true, avatarUrl: true } },
          },
        },
      },
    });
    results.playlists = playlists;
  }

  res.json(ok(results));
}));

// Save search to history
router.post('/history', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const { query } = req.body;
  if (!query || !query.trim()) return res.json(ok({ success: false }));

  // Limit to 20 searches
  const count = await prisma.searchHistory.count({ where: { userId: req.userId! } });
  if (count >= 20) {
    // Remove oldest
    const oldest = await prisma.searchHistory.findMany({
      where: { userId: req.userId! },
      orderBy: { createdAt: 'asc' },
      take: count - 19,
      select: { id: true },
    });
    await prisma.searchHistory.deleteMany({
      where: { id: { in: oldest.map((s) => s.id) } },
    });
  }

  await prisma.searchHistory.create({
    data: { userId: req.userId!, query: query.trim() },
  });

  res.json(ok({ success: true }));
}));

// Get search history
router.get('/history', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const history = await prisma.searchHistory.findMany({
    where: { userId: req.userId! },
    orderBy: { createdAt: 'desc' },
    take: 20,
  });
  res.json(ok(history));
}));

// Clear search history
router.delete('/history', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  await prisma.searchHistory.deleteMany({ where: { userId: req.userId! } });
  res.json(ok({ success: true, message: 'Search history cleared.' }));
}));

export default router;
