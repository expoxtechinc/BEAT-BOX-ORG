import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { config } from '../config';
import { error, ErrorCodes } from '../utils/response';
import prisma from '../config/prisma';

export interface AuthenticatedRequest extends Request {
  userId?: string;
  userRole?: string;
  user?: any;
}

interface JwtPayload {
  sub: string;
  role: string;
}

/**
 * Verify JWT token and attach user info to request.
 */
export const authenticate = async (
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
) => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json(error(ErrorCodes.UNAUTHORIZED, 'Authentication required. Please log in.'));
    }

    const token = authHeader.substring(7);

    const decoded = jwt.verify(token, config.jwt.secret) as JwtPayload;

    const user = await prisma.user.findUnique({
      where: { id: decoded.sub },
      select: {
        id: true,
        email: true,
        role: true,
        emailVerified: true,
        isDeleted: true,
        premiumFreeUses: true,
      },
    });

    if (!user || user.isDeleted) {
      return res.status(401).json(error(ErrorCodes.UNAUTHORIZED, 'Account not found or has been deleted.'));
    }

    req.userId = user.id;
    req.userRole = user.role;
    req.user = user;

    next();
  } catch (err) {
    if (err instanceof jwt.TokenExpiredError) {
      return res.status(401).json(error(ErrorCodes.UNAUTHORIZED, 'Your session has expired. Please log in again.'));
    }
    return res.status(401).json(error(ErrorCodes.UNAUTHORIZED, 'Invalid authentication token.'));
  }
};

/**
 * Optional authentication - does not fail if no token, but attaches user if present.
 */
export const optionalAuth = async (
  req: AuthenticatedRequest,
  _res: Response,
  next: NextFunction
) => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return next();
    }

    const token = authHeader.substring(7);
    const decoded = jwt.verify(token, config.jwt.secret) as JwtPayload;

    const user = await prisma.user.findUnique({
      where: { id: decoded.sub },
      select: {
        id: true,
        email: true,
        role: true,
        emailVerified: true,
        isDeleted: true,
        premiumFreeUses: true,
      },
    });

    if (user && !user.isDeleted) {
      req.userId = user.id;
      req.userRole = user.role;
      req.user = user;
    }

    next();
  } catch {
    next();
  }
};

/**
 * Require admin role.
 */
export const requireAdmin = (
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
) => {
  if (req.userRole !== 'ADMIN') {
    return res.status(403).json(error(ErrorCodes.FORBIDDEN, 'Administrator access required.'));
  }
  next();
};
