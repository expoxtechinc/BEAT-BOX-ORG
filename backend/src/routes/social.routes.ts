import { Router, Request, Response } from 'express';
import { SocialService } from '../services/social.service';
import { authenticate, optionalAuth, AuthenticatedRequest } from '../middleware/auth';
import { asyncHandler, AppError } from '../middleware/error';
import { ok, paginated, ErrorCodes } from '../utils/response';
import { uploadAvatar, handleUploadError } from '../middleware/upload';

const router = Router();

// =============================================================================
// PROFILES
// =============================================================================

// Get my profile
router.get('/me', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const profile = await SocialService.getProfile(req.userId!);
  res.json(ok(profile));
}));

// Update my profile
router.put('/me', authenticate, uploadAvatar, handleUploadError, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const avatarFile = req.file as Express.Multer.File | undefined;
  const profile = await SocialService.updateProfile(req.userId!, {
    displayName: req.body.displayName,
    bio: req.body.bio,
    location: req.body.location,
    website: req.body.website,
    isPublic: req.body.isPublic !== undefined ? req.body.isPublic === 'true' : undefined,
    avatarFile,
  });
  res.json(ok(profile));
}));

// Get public profile by username
router.get('/u/:username', optionalAuth, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const profile = await SocialService.getPublicProfile(req.params.username, req.userId);
  res.json(ok(profile));
}));

// =============================================================================
// FOLLOWS
// =============================================================================

// Follow user
router.post('/follow/:userId', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const result = await SocialService.follow(req.userId!, req.params.userId);
  res.json(ok(result));
}));

// Unfollow user
router.delete('/follow/:userId', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const result = await SocialService.unfollow(req.userId!, req.params.userId);
  res.json(ok(result));
}));

// Get followers
router.get('/:userId/followers', asyncHandler(async (req: Request, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const result = await SocialService.getFollowers(req.params.userId, page, limit);
  res.json(paginated(result.followers, page, limit, result.total));
}));

// Get following
router.get('/:userId/following', asyncHandler(async (req: Request, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const result = await SocialService.getFollowing(req.params.userId, page, limit);
  res.json(paginated(result.following, page, limit, result.total));
}));

// =============================================================================
// LIKES
// =============================================================================

// Toggle like
router.post('/like/:musicId', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const result = await SocialService.toggleLike(req.userId!, req.params.musicId);
  res.json(ok(result));
}));

// Get liked music
router.get('/likes/music', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const result = await SocialService.getLikedMusic(req.userId!, page, limit);
  res.json(paginated(result.music, page, limit, result.total));
}));

// =============================================================================
// FAVORITES
// =============================================================================

// Toggle favorite
router.post('/favorite/:musicId', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const result = await SocialService.toggleFavorite(req.userId!, req.params.musicId);
  res.json(ok(result));
}));

// Get favorite music
router.get('/favorites/music', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const result = await SocialService.getFavoriteMusic(req.userId!, page, limit);
  res.json(paginated(result.music, page, limit, result.total));
}));

// =============================================================================
// PLAYLISTS
// =============================================================================

// Create playlist
router.post('/playlists', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const { name, description, isPublic } = req.body;
  const playlist = await SocialService.createPlaylist(req.userId!, name, description, isPublic);
  res.status(201).json(ok(playlist));
}));

// Get my playlists
router.get('/playlists/my', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const result = await SocialService.getPlaylists(req.userId!, page, limit);
  res.json(paginated(result.playlists, page, limit, result.total));
}));

// Get user's public playlists
router.get('/playlists/user/:userId', asyncHandler(async (req: Request, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const result = await SocialService.getPlaylists(req.params.userId, page, limit);
  res.json(paginated(result.playlists, page, limit, result.total));
}));

// Get playlist by ID
router.get('/playlists/:id', optionalAuth, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const playlist = await SocialService.getPlaylistById(req.params.id, req.userId);
  res.json(ok(playlist));
}));

// Update playlist
router.put('/playlists/:id', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const playlist = await SocialService.updatePlaylist(req.params.id, req.userId!, req.body);
  res.json(ok(playlist));
}));

// Delete playlist
router.delete('/playlists/:id', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const result = await SocialService.deletePlaylist(req.params.id, req.userId!);
  res.json(ok(result));
}));

// Add music to playlist
router.post('/playlists/:id/music/:musicId', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const result = await SocialService.addMusicToPlaylist(req.params.id, req.userId!, req.params.musicId);
  res.json(ok(result));
}));

// Remove music from playlist
router.delete('/playlists/:id/music/:musicId', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const result = await SocialService.removeMusicFromPlaylist(req.params.id, req.userId!, req.params.musicId);
  res.json(ok(result));
}));

// Reorder playlist
router.put('/playlists/:id/reorder', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const { musicIds } = req.body;
  const result = await SocialService.reorderPlaylist(req.params.id, req.userId!, musicIds);
  res.json(ok(result));
}));

// =============================================================================
// NOTIFICATIONS
// =============================================================================

// Get notifications
router.get('/notifications', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const page = parseInt(req.query.page as string) || 1;
  const limit = parseInt(req.query.limit as string) || 20;
  const result = await SocialService.getNotifications(req.userId!, page, limit);
  res.json(ok(result));
}));

// Mark notification as read
router.patch('/notifications/:id/read', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const result = await SocialService.markNotificationRead(req.params.id, req.userId!);
  res.json(ok(result));
}));

// Mark all as read
router.post('/notifications/read-all', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const result = await SocialService.markAllNotificationsRead(req.userId!);
  res.json(ok(result));
}));

// =============================================================================
// CATEGORIES & ARTISTS
// =============================================================================

// Get categories
router.get('/categories', asyncHandler(async (req: Request, res: Response) => {
  const categories = await SocialService.getCategories();
  res.json(ok(categories));
}));

// Get popular artists
router.get('/artists/popular', asyncHandler(async (req: Request, res: Response) => {
  const limit = parseInt(req.query.limit as string) || 20;
  const artists = await SocialService.getPopularArtists(limit);
  res.json(ok(artists));
}));

export default router;
