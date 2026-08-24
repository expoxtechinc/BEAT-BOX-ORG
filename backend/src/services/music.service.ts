import prisma from '../config/prisma';
import { StorageService } from './storage.service';
import { AppError } from '../middleware/error';
import { ErrorCodes } from '../utils/response';
import { config } from '../config';
import path from 'path';
import fs from 'fs';

/**
 * =============================================================================
 * MUSIC SERVICE
 * =============================================================================
 *
 * CRITICAL: Music upload is ALWAYS FREE.
 * This service must NEVER call PremiumService.checkAndConsume().
 * Music upload does NOT consume premium free uses.
 * =============================================================================
 */
export class MusicService {
  /**
   * Upload a new music track.
   * This is FREE and does NOT consume premium uses.
   */
  static async upload(
    userId: string,
    data: {
      title: string;
      artistName: string;
      albumName?: string;
      genre?: string;
      description?: string;
      categoryId?: string;
      audioFile: Express.Multer.File;
      artworkFile?: Express.Multer.File;
    }
  ) {
    // Validate title
    if (!data.title || data.title.trim().length < 1) {
      throw new AppError(ErrorCodes.VALIDATION_ERROR, 'Title is required.', 400);
    }
    if (data.title.length > 200) {
      throw new AppError(ErrorCodes.VALIDATION_ERROR, 'Title must be less than 200 characters.', 400);
    }

    // Validate artist name
    if (!data.artistName || data.artistName.trim().length < 1) {
      throw new AppError(ErrorCodes.VALIDATION_ERROR, 'Artist name is required.', 400);
    }

    const audioFile = data.audioFile;
    const audioExt = path.extname(audioFile.originalname).toLowerCase().replace('.', '');
    const audioKey = `audio/${audioFile.filename}`;
    const audioUrl = StorageService.getFileUrl(audioKey);

    let artworkUrl: string | undefined;
    let artworkKey: string | undefined;

    if (data.artworkFile) {
      artworkKey = `images/${data.artworkFile.filename}`;
      artworkUrl = StorageService.getFileUrl(artworkKey);

      // Store artwork record
      await prisma.uploadArtwork.create({
        data: {
          userId,
          url: artworkUrl,
          key: artworkKey,
        },
      });
    }

    // Get file size
    const fileSize = audioFile.size;
    const format = audioExt;

    // Try to get duration (would need a library like music-metadata in production)
    // For now, we'll set it to null and it can be updated later
    const duration = null;

    const music = await prisma.music.create({
      data: {
        userId,
        title: data.title.trim(),
        artistName: data.artistName.trim(),
        albumName: data.albumName?.trim() || null,
        genre: data.genre?.trim() || null,
        description: data.description?.trim() || '',
        audioUrl,
        audioKey,
        artworkUrl,
        artworkKey,
        duration,
        fileSize,
        format,
        categoryId: data.categoryId || null,
        isPublished: true,
      },
      include: {
        category: true,
        user: {
          select: {
            id: true,
            profile: {
              select: {
                username: true,
                displayName: true,
                avatarUrl: true,
              },
            },
          },
        },
      },
    });

    // Create notification for followers (in production, this would be async via a queue)
    // Not blocking the upload response

    return music;
  }

  /**
   * Update music metadata.
   */
  static async update(
    musicId: string,
    userId: string,
    data: {
      title?: string;
      artistName?: string;
      albumName?: string;
      genre?: string;
      description?: string;
      categoryId?: string;
      artworkFile?: Express.Multer.File;
      isPublished?: boolean;
    }
  ) {
    const music = await prisma.music.findUnique({
      where: { id: musicId },
    });

    if (!music) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'Music not found.', 404);
    }

    if (music.userId !== userId) {
      throw new AppError(ErrorCodes.FORBIDDEN, 'You can only edit your own music.', 403);
    }

    if (music.isRemoved) {
      throw new AppError(ErrorCodes.BAD_REQUEST, 'This music has been removed and cannot be edited.', 400);
    }

    const updateData: any = {};
    if (data.title !== undefined) updateData.title = data.title.trim();
    if (data.artistName !== undefined) updateData.artistName = data.artistName.trim();
    if (data.albumName !== undefined) updateData.albumName = data.albumName?.trim() || null;
    if (data.genre !== undefined) updateData.genre = data.genre?.trim() || null;
    if (data.description !== undefined) updateData.description = data.description?.trim() || '';
    if (data.categoryId !== undefined) updateData.categoryId = data.categoryId || null;
    if (data.isPublished !== undefined) updateData.isPublished = data.isPublished;

    if (data.artworkFile) {
      const artworkKey = `images/${data.artworkFile.filename}`;
      updateData.artworkUrl = StorageService.getFileUrl(artworkKey);
      updateData.artworkKey = artworkKey;

      // Store artwork record
      await prisma.uploadArtwork.create({
        data: {
          userId,
          url: updateData.artworkUrl,
          key: artworkKey,
        },
      });

      // Delete old artwork if exists
      if (music.artworkKey) {
        await StorageService.deleteFile(music.artworkKey).catch(() => {});
      }
    }

    return prisma.music.update({
      where: { id: musicId },
      data: updateData,
      include: {
        category: true,
        user: {
          select: {
            id: true,
            profile: {
              select: {
                username: true,
                displayName: true,
                avatarUrl: true,
              },
            },
          },
        },
      },
    });
  }

  /**
   * Delete music.
   */
  static async delete(musicId: string, userId: string) {
    const music = await prisma.music.findUnique({
      where: { id: musicId },
    });

    if (!music) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'Music not found.', 404);
    }

    if (music.userId !== userId) {
      throw new AppError(ErrorCodes.FORBIDDEN, 'You can only delete your own music.', 403);
    }

    // Delete files from storage
    await StorageService.deleteFile(music.audioKey).catch(() => {});
    if (music.artworkKey) {
      await StorageService.deleteFile(music.artworkKey).catch(() => {});
    }

    // Delete from database (cascade will handle related records)
    await prisma.music.delete({
      where: { id: musicId },
    });

    return { success: true, message: 'Music deleted successfully.' };
  }

  /**
   * Get music by ID.
   */
  static async getById(musicId: string, userId?: string) {
    const music = await prisma.music.findUnique({
      where: { id: musicId },
      include: {
        category: true,
        user: {
          select: {
            id: true,
            profile: {
              select: {
                username: true,
                displayName: true,
                avatarUrl: true,
              },
            },
          },
        },
        _count: {
          select: {
            likes: true,
            favorites: true,
            plays: true,
          },
        },
      },
    });

    if (!music || music.isRemoved) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'Music not found.', 404);
    }

    // Check if the current user has liked/favorited this music
    let isLiked = false;
    let isFavorited = false;

    if (userId) {
      const [like, favorite] = await Promise.all([
        prisma.like.findUnique({ where: { userId_musicId: { userId, musicId } } }),
        prisma.favorite.findUnique({ where: { userId_musicId: { userId, musicId } } }),
      ]);
      isLiked = !!like;
      isFavorited = !!favorite;
    }

    return {
      ...music,
      isLiked,
      isFavorited,
      likeCount: music._count.likes,
      favoriteCount: music._count.favorites,
      playCount: music.playCount,
    };
  }

  /**
   * Get music uploaded by a user.
   */
  static async getByUserId(userId: string, page = 1, limit = 20) {
    const [music, total] = await Promise.all([
      prisma.music.findMany({
        where: {
          userId,
          isRemoved: false,
        },
        orderBy: { createdAt: 'desc' },
        skip: (page - 1) * limit,
        take: limit,
        include: {
          category: true,
          user: {
            select: {
              profile: {
                select: {
                  username: true,
                  displayName: true,
                  avatarUrl: true,
                },
              },
            },
          },
        },
      }),
      prisma.music.count({ where: { userId, isRemoved: false } }),
    ]);

    return { music, total, page, limit };
  }

  /**
   * Record a play (for recently played and play count).
   */
  static async recordPlay(musicId: string, userId: string) {
    const music = await prisma.music.findUnique({
      where: { id: musicId },
      select: { id: true, isRemoved: true, isPublished: true },
    });

    if (!music || music.isRemoved) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'Music not found.', 404);
    }

    // Increment play count atomically
    await prisma.music.update({
      where: { id: musicId },
      data: { playCount: { increment: 1 } },
    });

    // Record in recently played
    await prisma.recentlyPlayed.create({
      data: { userId, musicId },
    });

    // Keep only the last 100 recently played entries per user
    // (cleanup could be done async in production)
    const recentCount = await prisma.recentlyPlayed.count({ where: { userId } });
    if (recentCount > 100) {
      const oldEntries = await prisma.recentlyPlayed.findMany({
        where: { userId },
        orderBy: { playedAt: 'desc' },
        skip: 100,
        select: { id: true },
      });
      if (oldEntries.length > 0) {
        await prisma.recentlyPlayed.deleteMany({
          where: { id: { in: oldEntries.map((e) => e.id) } },
        });
      }
    }

    return { success: true };
  }

  /**
   * Search music.
   */
  static async search(query: string, page = 1, limit = 20, filters?: { genre?: string; categoryId?: string; sort?: string }) {
    const where: any = {
      isPublished: true,
      isRemoved: false,
    };

    if (query) {
      where.OR = [
        { title: { contains: query } },
        { artistName: { contains: query } },
        { albumName: { contains: query } },
        { genre: { contains: query } },
        { description: { contains: query } },
      ];
    }

    if (filters?.genre) where.genre = filters.genre;
    if (filters?.categoryId) where.categoryId = filters.categoryId;

    let orderBy: any = { createdAt: 'desc' };
    if (filters?.sort === 'popular') orderBy = { playCount: 'desc' };
    else if (filters?.sort === 'likes') orderBy = { likeCount: 'desc' };
    else if (filters?.sort === 'oldest') orderBy = { createdAt: 'asc' };

    const [music, total] = await Promise.all([
      prisma.music.findMany({
        where,
        orderBy,
        skip: (page - 1) * limit,
        take: limit,
        include: {
          category: true,
          user: {
            select: {
              id: true,
              profile: {
                select: {
                  username: true,
                  displayName: true,
                  avatarUrl: true,
                },
              },
            },
          },
        },
      }),
      prisma.music.count({ where }),
    ]);

    return { music, total, page, limit };
  }

  /**
   * Get trending music.
   */
  static async getTrending(limit = 20) {
    return prisma.music.findMany({
      where: {
        isPublished: true,
        isRemoved: false,
      },
      orderBy: { playCount: 'desc' },
      take: limit,
      include: {
        category: true,
        user: {
          select: {
            id: true,
            profile: {
              select: {
                username: true,
                displayName: true,
                avatarUrl: true,
              },
            },
          },
        },
      },
    });
  }

  /**
   * Get new releases.
   */
  static async getNewReleases(limit = 20) {
    return prisma.music.findMany({
      where: {
        isPublished: true,
        isRemoved: false,
      },
      orderBy: { createdAt: 'desc' },
      take: limit,
      include: {
        category: true,
        user: {
          select: {
            id: true,
            profile: {
              select: {
                username: true,
                displayName: true,
                avatarUrl: true,
              },
            },
          },
        },
      },
    });
  }

  /**
   * Get featured music.
   */
  static async getFeatured(limit = 20) {
    return prisma.music.findMany({
      where: {
        isPublished: true,
        isRemoved: false,
        isFeatured: true,
      },
      orderBy: { playCount: 'desc' },
      take: limit,
      include: {
        category: true,
        user: {
          select: {
            id: true,
            profile: {
              select: {
                username: true,
                displayName: true,
                avatarUrl: true,
              },
            },
          },
        },
      },
    });
  }

  /**
   * Get recently played for a user.
   */
  static async getRecentlyPlayed(userId: string, limit = 20) {
    const plays = await prisma.recentlyPlayed.findMany({
      where: { userId },
      orderBy: { playedAt: 'desc' },
      take: limit,
      include: {
        music: {
          include: {
            category: true,
            user: {
              select: {
                id: true,
                profile: {
                  select: {
                    username: true,
                    displayName: true,
                    avatarUrl: true,
                  },
                },
              },
            },
          },
        },
      },
    });

    // Deduplicate by music ID (keep most recent)
    const seen = new Set<string>();
    const result = [];
    for (const play of plays) {
      if (!seen.has(play.music.id) && !play.music.isRemoved) {
        seen.add(play.music.id);
        result.push(play.music);
      }
    }

    return result;
  }

  /**
   * Get recommended music for a user.
   * Simple recommendation: music from artists the user follows,
   * or music in genres the user has listened to.
   */
  static async getRecommended(userId: string, limit = 20) {
    // Get genres from recently played
    const recentPlays = await prisma.recentlyPlayed.findMany({
      where: { userId },
      take: 50,
      include: { music: { select: { genre: true, userId: true } } },
      orderBy: { playedAt: 'desc' },
    });

    const genres = new Set<string>();
    const artistIds = new Set<string>();
    recentPlays.forEach((p) => {
      if (p.music.genre) genres.add(p.music.genre);
      artistIds.add(p.music.userId);
    });

    // Get following IDs
    const following = await prisma.follow.findMany({
      where: { followerId: userId },
      select: { followingId: true },
    });
    following.forEach((f) => artistIds.add(f.followingId));

    const where: any = {
      isPublished: true,
      isRemoved: false,
      userId: { not: userId },
    };

    if (genres.size > 0 || artistIds.size > 0) {
      where.OR = [];
      if (genres.size > 0) where.OR.push({ genre: { in: Array.from(genres) } });
      if (artistIds.size > 0) where.OR.push({ userId: { in: Array.from(artistIds) } });
    }

    return prisma.music.findMany({
      where,
      orderBy: { playCount: 'desc' },
      take: limit,
      include: {
        category: true,
        user: {
          select: {
            id: true,
            profile: {
              select: {
                username: true,
                displayName: true,
                avatarUrl: true,
              },
            },
          },
        },
      },
    });
  }
}
