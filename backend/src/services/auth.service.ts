import prisma from '../config/prisma';
import { config } from '../config';
import { hashPassword, comparePassword, validatePasswordStrength, validateEmail } from '../utils/password';
import { generateAccessToken, generateRefreshToken, verifyRefreshToken, generateSecureToken } from '../utils/jwt';
import { AppError } from '../middleware/error';
import { ErrorCodes } from '../utils/response';
import { v4 as uuidv4 } from 'uuid';

export class AuthService {
  /**
   * Register a new user.
   * Creates user, profile, and subscription record.
   * Music upload is always free - does NOT affect premium counter.
   */
  static async register(email: string, password: string, username: string, displayName?: string) {
    // Validate email
    if (!validateEmail(email)) {
      throw new AppError(ErrorCodes.VALIDATION_ERROR, 'Please enter a valid email address.', 400);
    }

    // Validate password
    const passwordCheck = validatePasswordStrength(password);
    if (!passwordCheck.valid) {
      throw new AppError(ErrorCodes.VALIDATION_ERROR, 'Password does not meet requirements.', 400, passwordCheck.errors);
    }

    // Check if email already exists
    const existingEmail = await prisma.user.findUnique({ where: { email: email.toLowerCase() } });
    if (existingEmail) {
      throw new AppError(ErrorCodes.CONFLICT, 'An account with this email already exists.', 409);
    }

    // Check if username already exists
    const existingUsername = await prisma.profile.findUnique({ where: { username: username.toLowerCase() } });
    if (existingUsername) {
      throw new AppError(ErrorCodes.CONFLICT, 'This username is already taken.', 409);
    }

    // Validate username
    if (username.length < 3 || username.length > 30) {
      throw new AppError(ErrorCodes.VALIDATION_ERROR, 'Username must be between 3 and 30 characters.', 400);
    }
    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
      throw new AppError(ErrorCodes.VALIDATION_ERROR, 'Username can only contain letters, numbers, and underscores.', 400);
    }

    const passwordHash = await hashPassword(password);

    // Determine if this should be an admin account
    const role = email.toLowerCase() === config.app.adminEmail.toLowerCase() ? 'ADMIN' : 'USER';

    // Create user with profile and subscription in a transaction
    const result = await prisma.$transaction(async (tx) => {
      const user = await tx.user.create({
        data: {
          email: email.toLowerCase(),
          passwordHash,
          role: role as any,
          premiumFreeUses: config.premium.freeUses, // Start with 5 free premium uses
          emailVerifyToken: generateSecureToken(),
        },
      });

      await tx.profile.create({
        data: {
          userId: user.id,
          username: username.toLowerCase(),
          displayName: displayName || username,
          bio: '',
        },
      });

      await tx.subscription.create({
        data: {
          userId: user.id,
          status: 'FREE',
          plan: 'FREE',
        },
      });

      return user;
    });

    // Generate tokens
    const tokenPayload = { sub: result.id, role: result.role };
    const accessToken = generateAccessToken(tokenPayload);
    const refreshToken = generateRefreshToken(tokenPayload);

    // Store refresh token session
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 30);
    await prisma.session.create({
      data: {
        userId: result.id,
        refreshToken,
        expiresAt,
      },
    });

    return {
      user: {
        id: result.id,
        email: result.email,
        role: result.role,
        emailVerified: result.emailVerified,
        premiumFreeUses: result.premiumFreeUses,
        emailVerifyToken: result.emailVerifyToken, // In production, send via email, not API response
      },
      accessToken,
      refreshToken,
    };
  }

  /**
   * Login user.
   */
  static async login(email: string, password: string) {
    const user = await prisma.user.findUnique({
      where: { email: email.toLowerCase() },
      include: {
        profile: true,
        subscription: true,
      },
    });

    if (!user) {
      throw new AppError(ErrorCodes.UNAUTHORIZED, 'Invalid email or password.', 401);
    }

    if (user.isDeleted) {
      throw new AppError(ErrorCodes.UNAUTHORIZED, 'This account has been deleted.', 401);
    }

    const validPassword = await comparePassword(password, user.passwordHash);
    if (!validPassword) {
      throw new AppError(ErrorCodes.UNAUTHORIZED, 'Invalid email or password.', 401);
    }

    const tokenPayload = { sub: user.id, role: user.role };
    const accessToken = generateAccessToken(tokenPayload);
    const refreshToken = generateRefreshToken(tokenPayload);

    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 30);
    await prisma.session.create({
      data: {
        userId: user.id,
        refreshToken,
        expiresAt,
      },
    });

    return {
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
        emailVerified: user.emailVerified,
        premiumFreeUses: user.premiumFreeUses,
        profile: user.profile,
        subscription: user.subscription,
      },
      accessToken,
      refreshToken,
    };
  }

  /**
   * Refresh access token using refresh token.
   */
  static async refresh(refreshToken: string) {
    try {
      const decoded = verifyRefreshToken(refreshToken);

      const session = await prisma.session.findFirst({
        where: {
          refreshToken,
          userId: decoded.sub,
          expiresAt: { gt: new Date() },
        },
      });

      if (!session) {
        throw new AppError(ErrorCodes.UNAUTHORIZED, 'Invalid or expired session.', 401);
      }

      const user = await prisma.user.findUnique({
        where: { id: decoded.sub },
        select: { id: true, role: true, isDeleted: true },
      });

      if (!user || user.isDeleted) {
        throw new AppError(ErrorCodes.UNAUTHORIZED, 'Account not found.', 401);
      }

      const tokenPayload = { sub: user.id, role: user.role };
      const newAccessToken = generateAccessToken(tokenPayload);
      const newRefreshToken = generateRefreshToken(tokenPayload);

      // Rotate refresh token
      await prisma.session.delete({ where: { id: session.id } });
      const expiresAt = new Date();
      expiresAt.setDate(expiresAt.getDate() + 30);
      await prisma.session.create({
        data: {
          userId: user.id,
          refreshToken: newRefreshToken,
          expiresAt,
        },
      });

      return { accessToken: newAccessToken, refreshToken: newRefreshToken };
    } catch (err) {
      if (err instanceof AppError) throw err;
      throw new AppError(ErrorCodes.UNAUTHORIZED, 'Invalid refresh token.', 401);
    }
  }

  /**
   * Logout - invalidate the session.
   */
  static async logout(refreshToken: string) {
    await prisma.session.deleteMany({
      where: { refreshToken },
    }).catch(() => {});
  }

  /**
   * Logout from all devices.
   */
  static async logoutAll(userId: string) {
    await prisma.session.deleteMany({
      where: { userId },
    });
  }

  /**
   * Verify email with token.
   */
  static async verifyEmail(token: string) {
    const user = await prisma.user.findFirst({
      where: { emailVerifyToken: token },
    });

    if (!user) {
      throw new AppError(ErrorCodes.BAD_REQUEST, 'Invalid or expired verification token.', 400);
    }

    await prisma.user.update({
      where: { id: user.id },
      data: {
        emailVerified: true,
        emailVerifyToken: null,
      },
    });

    return { success: true, message: 'Email verified successfully.' };
  }

  /**
   * Request password reset.
   */
  static async requestPasswordReset(email: string) {
    const user = await prisma.user.findUnique({
      where: { email: email.toLowerCase() },
    });

    // Always return success to prevent email enumeration
    if (!user) {
      return { success: true, message: 'If an account exists with this email, a reset link has been sent.' };
    }

    const resetToken = generateSecureToken();
    const expiresAt = new Date();
    expiresAt.setHours(expiresAt.getHours() + 1);

    await prisma.user.update({
      where: { id: user.id },
      data: {
        passwordResetToken: resetToken,
        passwordResetExpires: expiresAt,
      },
    });

    // In production, send email with reset link containing the token
    // For development, return the token (remove in production)
    return {
      success: true,
      message: 'If an account exists with this email, a reset link has been sent.',
      // devOnly: resetToken, // Remove in production
      resetToken: config.isDevelopment ? resetToken : undefined,
    };
  }

  /**
   * Reset password with token.
   */
  static async resetPassword(token: string, newPassword: string) {
    const passwordCheck = validatePasswordStrength(newPassword);
    if (!passwordCheck.valid) {
      throw new AppError(ErrorCodes.VALIDATION_ERROR, 'Password does not meet requirements.', 400, passwordCheck.errors);
    }

    const user = await prisma.user.findFirst({
      where: {
        passwordResetToken: token,
        passwordResetExpires: { gt: new Date() },
      },
    });

    if (!user) {
      throw new AppError(ErrorCodes.BAD_REQUEST, 'Invalid or expired reset token.', 400);
    }

    const passwordHash = await hashPassword(newPassword);

    await prisma.$transaction([
      prisma.user.update({
        where: { id: user.id },
        data: {
          passwordHash,
          passwordResetToken: null,
          passwordResetExpires: null,
        },
      }),
      prisma.session.deleteMany({ where: { userId: user.id } }),
    ]);

    return { success: true, message: 'Password reset successfully. Please log in with your new password.' };
  }

  /**
   * Change password (when authenticated).
   */
  static async changePassword(userId: string, currentPassword: string, newPassword: string) {
    const user = await prisma.user.findUnique({ where: { id: userId } });
    if (!user) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'User not found.', 404);
    }

    const validPassword = await comparePassword(currentPassword, user.passwordHash);
    if (!validPassword) {
      throw new AppError(ErrorCodes.UNAUTHORIZED, 'Current password is incorrect.', 401);
    }

    const passwordCheck = validatePasswordStrength(newPassword);
    if (!passwordCheck.valid) {
      throw new AppError(ErrorCodes.VALIDATION_ERROR, 'New password does not meet requirements.', 400, passwordCheck.errors);
    }

    const passwordHash = await hashPassword(newPassword);

    await prisma.$transaction([
      prisma.user.update({
        where: { id: userId },
        data: { passwordHash },
      }),
      prisma.session.deleteMany({ where: { userId } }),
    ]);

    return { success: true, message: 'Password changed successfully.' };
  }

  /**
   * Delete account (soft delete).
   */
  static async deleteAccount(userId: string, password: string) {
    const user = await prisma.user.findUnique({ where: { id: userId } });
    if (!user) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'User not found.', 404);
    }

    const validPassword = await comparePassword(password, user.passwordHash);
    if (!validPassword) {
      throw new AppError(ErrorCodes.UNAUTHORIZED, 'Password is incorrect.', 401);
    }

    await prisma.$transaction([
      prisma.user.update({
        where: { id: userId },
        data: {
          isDeleted: true,
          deletedAt: new Date(),
          email: `deleted_${uuidv4()}_${user.email}`, // Anonymize email
          passwordHash: 'deleted',
        },
      }),
      prisma.session.deleteMany({ where: { userId } }),
    ]);

    return { success: true, message: 'Account deleted successfully.' };
  }

  /**
   * Get current user with profile and subscription.
   */
  static async getCurrentUser(userId: string) {
    const user = await prisma.user.findUnique({
      where: { id: userId },
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
          select: {
            music: { where: { isRemoved: false } },
            followers: true,
            following: true,
          },
        },
      },
    });

    if (!user || user.isDeleted) {
      throw new AppError(ErrorCodes.NOT_FOUND, 'User not found.', 404);
    }

    return {
      id: user.id,
      email: user.email,
      role: user.role,
      emailVerified: user.emailVerified,
      premiumFreeUses: user.premiumFreeUses,
      profile: user.profile,
      subscription: user.subscription,
      stats: {
        uploads: user._count.music,
        followers: user._count.followers,
        following: user._count.following,
      },
    };
  }
}
