import rateLimit from 'express-rate-limit';
import { config } from '../config';
import { ErrorCodes } from '../utils/response';

// General API rate limiter
export const apiRateLimiter = rateLimit({
  windowMs: config.rateLimit.windowMs,
  max: config.rateLimit.max,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: {
      code: ErrorCodes.RATE_LIMITED,
      message: 'Too many requests. Please try again later.',
    },
  },
});

// Stricter rate limiter for auth endpoints
export const authRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 20, // 20 requests per 15 minutes
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: {
      code: ErrorCodes.RATE_LIMITED,
      message: 'Too many authentication attempts. Please try again later.',
    },
  },
});

// Rate limiter for upload endpoints
export const uploadRateLimiter = rateLimit({
  windowMs: 60 * 60 * 1000, // 1 hour
  max: 50, // 50 uploads per hour
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: {
      code: ErrorCodes.RATE_LIMITED,
      message: 'Too many uploads. Please try again later.',
    },
  },
});
