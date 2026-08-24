import prisma from '../config/prisma';
import { AppError } from '../middleware/error';
import { ErrorCodes } from '../utils/response';
import { StorageService } from './storage.service';

export class SocialService {
  // ===========================================================================
  // PROFILES
  // ===========================================================================

  static async getProfile(userId: string) {
    const profile = await prisma.profile.findUnique({
      where: { userId },
      include: {
        user: {
          select: {
            id: true,
            email: true,
            role: true,
            emailVerified: true,
            premiumFreeUses: true,
            subscription: true,
            createdAt: true,
            _count: {
              select: {
                music: { where: { isRemoved: false, isPublished: true } },
                followers: true,
                following: true,
              },
            },
          },
        },
      },
    });

    if (!profile) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'Profile not found.', 404);
    }

    return profile;
  }

  static async getPublicProfile(username: string, currentUserId?: string) {
    const profile = await prisma.profile.findUnique({
      where: { username: username.toLowerCase() },
      include: {
        user: {
          select: {
            id: true,
            _count: {
              select: {
                music: { where: { isRemoved: false, isPublished: true } },
                followers: true,
                following: true,
              },
            },
          },
        },
      },
    });

    if (!profile || !profile.isPublic) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'Profile not found.', 404);
    }

    let isFollowing = false;
    if (currentUserId) {
      const follow = await prisma.follow.findUnique({
        where: {
          followerId_followingId: {
            followerId: currentUserId,
            followingId: profile.userId,
          },
        },
      });
      isFollowing = !!follow;
    }

    return {
      ...profile,
      isFollowing,
      stats: {
        uploads: profile.user._count.music,
        followers: profile.user._count.followers,
        following: profile.user._count.following,
      },
    };
  }

  static async updateProfile(
    userId: string,
    data: {
      displayName?: string;
      bio?: string;
      location?: string;
      website?: string;
      avatarFile?: Express.Multer.File;
      isPublic?: boolean;
    }
  ) {
    const updateData: any = {};
    if (data.displayName !== undefined) updateData.displayName = data.displayName;
    if (data.bio !== undefined) updateData.bio = data.bio;
    if (data.location !== undefined) updateData.location = data.location;
    if (data.website !== undefined) updateData.website = data.website;
    if (data.isPublic !== undefined) updateData.isPublic = data.isPublic;

    if (data.avatarFile) {
      const avatarKey = `images/${data.avatarFile.filename}`;
      updateData.avatarUrl = StorageService.getFileUrl(avatarKey);
    }

    return prisma.profile.update({
      where: { userId },
      data: updateData,
    });
  }

  // ===========================================================================
  // FOLLOWS
  // ===========================================================================

  static async follow(followerId: string, followingId: string) {
    if (followerId === followingId) {
      throw new AppError(ErrorCodes.BAD_REQUEST, 'You cannot follow yourself.', 400);
    }

    const targetUser = await prisma.user.findUnique({
      where: { id: followingId, isDeleted: false },
    });

    if (!targetUser) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'User not found.', 404);
    }

    try {
      await prisma.follow.create({
        data: { followerId, followingId },
      });

      // Create notification
      await prisma.notification.create({
        data: {
          userId: followingId,
          type: 'NEW_FOLLOWER',
          title: 'New Follower',
          body: 'You have a new follower!',
          data: JSON.stringify({ followerId }),
        },
      }).catch(() => {});

      return { success: true, following: true };
    } catch (err: any) {
      // Already following
      if (err.code === 'P2002') {
        return { success: true, following: true };
      }
      throw err;
    }
  }

  static async unfollow(followerId: string, followingId: string) {
    await prisma.follow.deleteMany({
      where: { followerId, followingId },
    });
    return { success: true, following: false };
  }

  static async getFollowers(userId: string, page = 1, limit = 20) {
    const [followers, total] = await Promise.all([
      prisma.follow.findMany({
        where: { followingId: userId },
        include: {
          follower: {
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
        skip: (page - 1) * limit,
        take: limit,
        orderBy: { createdAt: 'desc' },
      }),
      prisma.follow.count({ where: { followingId: userId } }),
    ]);

    return {
      followers: followers.map((f) => f.follower),
      total,
      page,
      limit,
    };
  }

  static async getFollowing(userId: string, page = 1, limit = 20) {
    const [following, total] = await Promise.all([
      prisma.follow.findMany({
        where: { followerId: userId },
        include: {
          following: {
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
        skip: (page - 1) * limit,
        take: limit,
        orderBy: { createdAt: 'desc' },
      }),
      prisma.follow.count({ where: { followerId: userId } }),
    ]);

    return {
      following: following.map((f) => f.following),
      total,
      page,
      limit,
    };
  }

  // ===========================================================================
  // LIKES
  // ===========================================================================

  static async toggleLike(userId: string, musicId: string) {
    const music = await prisma.music.findUnique({
      where: { id: musicId, isRemoved: false },
    });

    if (!music) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'Music not found.', 404);
    }

    const existing = await prisma.like.findUnique({
      where: { userId_musicId: { userId, musicId } },
    });

    if (existing) {
      await prisma.$transaction([
        prisma.like.delete({ where: { userId_musicId: { userId, musicId } } }),
        prisma.music.update({
          where: { id: musicId },
          data: { likeCount: { decrement: 1 } },
        }),
      ]);
      return { liked: false };
    }

    await prisma.$transaction([
      prisma.like.create({ data: { userId, musicId } }),
      prisma.music.update({
        where: { id: musicId },
        data: { likeCount: { increment: 1 } },
      }),
    ]);

    // Create notification for the music owner
    if (music.userId !== userId) {
      await prisma.notification.create({
        data: {
          userId: music.userId,
          type: 'NEW_LIKE',
          title: 'New Like',
          body: 'Someone liked your music!',
          data: JSON.stringify({ musicId, likerId: userId }),
        },
      }).catch(() => {});
    }

    return { liked: true };
  }

  static async getLikedMusic(userId: string, page = 1, limit = 20) {
    const [likes, total] = await Promise.all([
      prisma.like.findMany({
        where: { userId },
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
        skip: (page - 1) * limit,
        take: limit,
        orderBy: { createdAt: 'desc' },
      }),
      prisma.like.count({ where: { userId } }),
    ]);

    return {
      music: likes.filter((l) => !l.music.isRemoved).map((l) => l.music),
      total,
      page,
      limit,
    };
  }

  // ===========================================================================
  // FAVORITES
  // ===========================================================================

  static async toggleFavorite(userId: string, musicId: string) {
    const music = await prisma.music.findUnique({
      where: { id: musicId, isRemoved: false },
    });

    if (!music) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'Music not found.', 404);
    }

    const existing = await prisma.favorite.findUnique({
      where: { userId_musicId: { userId, musicId } },
    });

    if (existing) {
      await prisma.$transaction([
        prisma.favorite.delete({ where: { userId_musicId: { userId, musicId } } }),
        prisma.music.update({
          where: { id: musicId },
          data: { favoriteCount: { decrement: 1 } },
        }),
      ]);
      return { favorited: false };
    }

    await prisma.$transaction([
      prisma.favorite.create({ data: { userId, musicId } }),
      prisma.music.update({
        where: { id: musicId },
        data: { favoriteCount: { increment: 1 } },
      }),
    ]);

    return { favorited: true };
  }

  static async getFavoriteMusic(userId: string, page = 1, limit = 20) {
    const [favorites, total] = await Promise.all([
      prisma.favorite.findMany({
        where: { userId },
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
        skip: (page - 1) * limit,
        take: limit,
        orderBy: { createdAt: 'desc' },
      }),
      prisma.favorite.count({ where: { userId } }),
    ]);

    return {
      music: favorites.filter((f) => !f.music.isRemoved).map((f) => f.music),
      total,
      page,
      limit,
    };
  }

  // ===========================================================================
  // PLAYLISTS
  // ===========================================================================

  static async createPlaylist(userId: string, name: string, description?: string, isPublic = true) {
    return prisma.playlist.create({
      data: {
        userId,
        name,
        description: description || '',
        isPublic,
      },
    });
  }

  static async getPlaylists(userId: string, page = 1, limit = 20) {
    const [playlists, total] = await Promise.all([
      prisma.playlist.findMany({
        where: { userId },
        include: {
          _count: { select: { items: true } },
        },
        skip: (page - 1) * limit,
        take: limit,
        orderBy: { updatedAt: 'desc' },
      }),
      prisma.playlist.count({ where: { userId } }),
    ]);

    return { playlists, total, page, limit };
  }

  static async getPlaylistById(playlistId: string, userId?: string) {
    const playlist = await prisma.playlist.findUnique({
      where: { id: playlistId },
      include: {
        items: {
          orderBy: { sortOrder: 'asc' },
          include: {
            music: {
              include: {
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
        },
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

    if (!playlist) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'Playlist not found.', 404);
    }

    // Check visibility
    if (!playlist.isPublic && playlist.userId !== userId) {
      throw new AppError(ErrorCodes.FORBIDDEN, 'This playlist is private.', 403);
    }

    // Filter out removed music from items
    playlist.items = playlist.items.filter((item) => !item.music.isRemoved);

    return playlist;
  }

  static async updatePlaylist(playlistId: string, userId: string, data: { name?: string; description?: string; isPublic?: boolean; coverUrl?: string }) {
    const playlist = await prisma.playlist.findUnique({ where: { id: playlistId } });
    if (!playlist) throw new AppError(ErrorCodes.NOT_FOUND, 'Playlist not found.', 404);
    if (playlist.userId !== userId) throw new AppError(ErrorCodes.FORBIDDEN, 'You can only edit your own playlists.', 403);

    return prisma.playlist.update({
      where: { id: playlistId },
      data,
    });
  }

  static async deletePlaylist(playlistId: string, userId: string) {
    const playlist = await prisma.playlist.findUnique({ where: { id: playlistId } });
    if (!playlist) throw new AppError(ErrorCodes.NOT_FOUND, 'Playlist not found.', 404);
    if (playlist.userId !== userId) throw new AppError(ErrorCodes.FORBIDDEN, 'You can only delete your own playlists.', 403);

    await prisma.playlist.delete({ where: { id: playlistId } });
    return { success: true, message: 'Playlist deleted.' };
  }

  static async addMusicToPlaylist(playlistId: string, userId: string, musicId: string) {
    const playlist = await prisma.playlist.findUnique({ where: { id: playlistId } });
    if (!playlist) throw new AppError(ErrorCodes.NOT_FOUND, 'Playlist not found.', 404);
    if (playlist.userId !== userId) throw new AppError(ErrorCodes.FORBIDDEN, 'You can only modify your own playlists.', 403);

    const music = await prisma.music.findUnique({ where: { id: musicId, isRemoved: false, isPublished: true } });
    if (!music) throw new AppError(ErrorCodes.NOT_FOUND, 'Music not found.', 404);

    // Get current max sort order
    const maxItem = await prisma.playlistItem.findFirst({
      where: { playlistId },
      orderBy: { sortOrder: 'desc' },
    });

    try {
      await prisma.playlistItem.create({
        data: {
          playlistId,
          musicId,
          sortOrder: (maxItem?.sortOrder ?? -1) + 1,
        },
      });
    } catch (err: any) {
      if (err.code === 'P2002') {
        throw new AppError(ErrorCodes.CONFLICT, 'This song is already in the playlist.', 409);
      }
      throw err;
    }

    return { success: true };
  }

  static async removeMusicFromPlaylist(playlistId: string, userId: string, musicId: string) {
    const playlist = await prisma.playlist.findUnique({ where: { id: playlistId } });
    if (!playlist) throw new AppError(ErrorCodes.NOT_FOUND, 'Playlist not found.', 404);
    if (playlist.userId !== userId) throw new AppError(ErrorCodes.FORBIDDEN, 'You can only modify your own playlists.', 403);

    await prisma.playlistItem.deleteMany({
      where: { playlistId, musicId },
    });

    return { success: true };
  }

  static async reorderPlaylist(playlistId: string, userId: string, musicIds: string[]) {
    const playlist = await prisma.playlist.findUnique({ where: { id: playlistId } });
    if (!playlist) throw new AppError(ErrorCodes.NOT_FOUND, 'Playlist not found.', 404);
    if (playlist.userId !== userId) throw new AppError(ErrorCodes.FORBIDDEN, 'You can only reorder your own playlists.', 403);

    await prisma.$transaction(
      musicIds.map((musicId, index) =>
        prisma.playlistItem.updateMany({
          where: { playlistId, musicId },
          data: { sortOrder: index },
        })
      )
    );

    return { success: true };
  }

  // ===========================================================================
  // NOTIFICATIONS
  // ===========================================================================

  static async getNotifications(userId: string, page = 1, limit = 20) {
    const [notifications, total, unreadCount] = await Promise.all([
      prisma.notification.findMany({
        where: { userId },
        orderBy: { createdAt: 'desc' },
        skip: (page - 1) * limit,
        take: limit,
      }),
      prisma.notification.count({ where: { userId } }),
      prisma.notification.count({ where: { userId, isRead: false } }),
    ]);

    return { notifications, total, unreadCount, page, limit };
  }

  static async markNotificationRead(notificationId: string, userId: string) {
    const notification = await prisma.notification.findUnique({ where: { id: notificationId } });
    if (!notification) throw new AppError(ErrorCodes.NOT_FOUND, 'Notification not found.', 404);
    if (notification.userId !== userId) throw new AppError(ErrorCodes.FORBIDDEN, 'Not authorized.', 403);

    return prisma.notification.update({
      where: { id: notificationId },
      data: { isRead: true },
    });
  }

  static async markAllNotificationsRead(userId: string) {
    await prisma.notification.updateMany({
      where: { userId, isRead: false },
      data: { isRead: true },
    });
    return { success: true };
  }

  // ===========================================================================
  // CATEGORIES
  // ===========================================================================

  static async getCategories() {
    return prisma.category.findMany({
      where: { isActive: true },
      orderBy: { sortOrder: 'asc' },
    });
  }

  static async getPopularArtists(limit = 20) {
    const users = await prisma.user.findMany({
      where: {
        isDeleted: false,
        role: 'USER',
        music: { some: { isPublished: true, isRemoved: false } },
      },
      select: {
        id: true,
        profile: {
          select: {
            username: true,
            displayName: true,
            avatarUrl: true,
            bio: true,
          },
        },
        _count: {
          select: {
            music: { where: { isPublished: true, isRemoved: false } },
            followers: true,
          },
        },
      },
      orderBy: {
        followers: { _count: 'desc' },
      },
      take: limit,
    });

    return users;
  }
}
