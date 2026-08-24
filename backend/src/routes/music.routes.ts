import { Router, Request, Response } from 'express';
import { MusicService } from '../services/music.service';
import { PremiumService } from '../services/premium.service';
import { authenticate, optionalAuth, AuthenticatedRequest } from '../middleware/auth';
import { asyncHandler, AppError } from '../middleware/error';
import { ok, paginated, ErrorCodes } from '../utils/response';
import { uploadMusic, uploadImage, handleUploadError } from '../middleware/upload';
import { uploadRateLimiter } from '../middleware/rateLimiter';
import prisma from '../config/prisma';

const router = Router();

// =============================================================================
// MUSIC UPLOAD - FREE (does NOT consume premium uses)
// =============================================================================

router.post(
  '/upload',
  authenticate,
  uploadRateLimiter,
  uploadMusic,
  handleUploadError,
  asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
    const files = req.files as { audio?: Express.Multer.File[]; artwork?: Express.Multer.File[] };

    if (!files?.audio?.[0]) {
      throw new AppError(ErrorCodes.VALIDATION_ERROR, 'Audio file is required.', 400);
    }

    // CRITICAL: Music upload is FREE. Do NOT call PremiumService.checkAndConsume().
    // Music upload does NOT consume premium free uses.

    const music = await MusicService.upload(req.userId!, {
      title: req.body.title,
      artistName: req.body.artistName,
      albumName: req.body.albumName,
      genre: req.body.genre,
      description: req.body.description,
      categoryId: req.body.categoryId,
      audioFile: files.audio[0],
      artworkFile: files.artwork?.[0],
    });

    res.status(201).json(ok(music, { message: 'Music uploaded successfully. Upload is free!' }));
  })
);

// =============================================================================
// MUSIC MANAGEMENT
// =============================================================================

// Update music
router.put(
  '/:id',
  authenticate,
  uploadImage,
  handleUploadError,
  asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
    const artworkFile = req.file as Express.Multer.File | undefined;
    const music = await MusicService.update(req.params.id, req.userId!, {
      title: req.body.title,
      artistName: req.body.artistName,
      albumName: req.body.albumName,
      genre: req.body.genre,
      description: req.body.description,
      categoryId: req.body.categoryId,
      isPublished: req.body.isPublished !== undefined ? req.body.isPublished === 'true' : undefined,
      artworkFile,
    });
    res.json(ok(music));
  })
);

// Delete music
router.delete('/:id', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const result = await MusicService.delete(req.params.id, req.userId!);
  res.json(ok(result));
}));

// Get music by ID
router.get('/:id', optionalAuth, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const music = await MusicService.getById(req.params.id, req.userId);
  res.json(ok(music));
}));

// Get user's uploads
router.get('/user/:userId', asyncHandler(async (req: Request, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const result = await MusicService.getByUserId(req.params.userId, page, limit);
  res.json(paginated(result.music, page, limit, result.total));
}));

// Get my uploads
router.get('/my/uploads', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const result = await MusicService.getByUserId(req.userId!, page, limit);
  res.json(paginated(result.music, page, limit, result.total));
}));

// Record play
router.post('/:id/play', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const result = await MusicService.recordPlay(req.params.id, req.userId!);
  res.json(ok(result));
}));

// Publish/unpublish
router.patch('/:id/publish', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const music = await MusicService.update(req.params.id, req.userId!, {
    isPublished: req.body.isPublished,
  });
  res.json(ok(music));
}));

// =============================================================================
// DISCOVERY
// =============================================================================

// Search music
router.get('/', asyncHandler(async (req: Request, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const query = (req.query.q as string) || '';
  const filters = {
    genre: req.query.genre as string | undefined,
    categoryId: req.query.categoryId as string | undefined,
    sort: req.query.sort as string | undefined,
  };
  const result = await MusicService.search(query, page, limit, filters);
  res.json(paginated(result.music, page, limit, result.total));
}));

// Trending
router.get('/discover/trending', asyncHandler(async (req: Request, res: Response) => {
  const limit = parseInt(req.query.limit as string) || 20;
  const music = await MusicService.getTrending(limit);
  res.json(ok(music));
}));

// New releases
router.get('/discover/new', asyncHandler(async (req: Request, res: Response) => {
  const limit = parseInt(req.query.limit as string) || 20;
  const music = await MusicService.getNewReleases(limit);
  res.json(ok(music));
}));

// Featured
router.get('/discover/featured', asyncHandler(async (req: Request, res: Response) => {
  const limit = parseInt(req.query.limit as string) || 20;
  const music = await MusicService.getFeatured(limit);
  res.json(ok(music));
}));

// Recommended
router.get('/discover/recommended', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const limit = parseInt(req.query.limit as string) || 20;
  const music = await MusicService.getRecommended(req.userId!, limit);
  res.json(ok(music));
}));

// Recently played
router.get('/discover/recent', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const limit = parseInt(req.query.limit as string) || 20;
  const music = await MusicService.getRecentlyPlayed(req.userId!, limit);
  res.json(ok(music));
}));

export default router;
