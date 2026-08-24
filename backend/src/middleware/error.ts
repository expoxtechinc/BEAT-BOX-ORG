import { Request, Response, NextFunction } from 'express';
import { Prisma } from '@prisma/client';
import { error, ErrorCodes } from '../utils/response';
import { config } from '../config';

export class AppError extends Error {
  code: string;
  statusCode: number;
  details?: any;

  constructor(code: string, message: string, statusCode: number, details?: any) {
    super(message);
    this.code = code;
    this.statusCode = statusCode;
    this.details = details;
  }
}

export const errorHandler = (
  err: Error | AppError,
  _req: Request,
  res: Response,
  _next: NextFunction
) => {
  console.error('[Error]', err.message, err.stack);

  if (err instanceof AppError) {
    return res.status(err.statusCode).json(error(err.code, err.message, err.details));
  }

  // Prisma known errors
  if (err instanceof Prisma.PrismaClientKnownRequestError) {
    switch (err.code) {
      case 'P2002':
        // Unique constraint violation
        const target = (err.meta?.target as string[])?.join(', ') ?? 'field';
        return res.status(409).json(error(ErrorCodes.CONFLICT, `A record with this ${target} already exists.`));
      case 'P2025':
        return res.status(404).json(error(ErrorCodes.NOT_FOUND, 'Record not found.'));
      case 'P2003':
        return res.status(400).json(error(ErrorCodes.BAD_REQUEST, 'Referenced record does not exist.'));
      default:
        return res.status(400).json(error(ErrorCodes.BAD_REQUEST, 'Database operation failed.'));
    }
  }

  if (err.name === 'ZodError') {
    return res.status(400).json(error(ErrorCodes.VALIDATION_ERROR, 'Validation failed', err));
  }

  // Don't expose internal errors in production
  const message = config.isProduction
    ? 'An unexpected error occurred. Please try again.'
    : err.message;

  return res.status(500).json(error(ErrorCodes.INTERNAL_ERROR, message));
};

export const notFoundHandler = (_req: Request, res: Response) => {
  return res.status(404).json(error(ErrorCodes.NOT_FOUND, 'The requested resource was not found.'));
};

export const asyncHandler =
  (fn: (req: Request, res: Response, next: NextFunction) => Promise<any>) =>
  (req: Request, res: Response, next: NextFunction) => {
    Promise.resolve(fn(req, res, next)).catch(next);
  };
