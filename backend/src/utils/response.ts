/**
 * Standard API response utilities.
 * All API responses follow a consistent format.
 */

export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
    details?: any;
  };
  meta?: {
    page?: number;
    limit?: number;
    total?: number;
    totalPages?: number;
    hasMore?: boolean;
  };
}

export function ok<T>(data: T, meta?: ApiResponse['meta']): ApiResponse<T> {
  return { success: true, data, meta };
}

export function paginated<T>(data: T[], page: number, limit: number, total: number): ApiResponse<T[]> {
  const totalPages = Math.ceil(total / limit);
  return {
    success: true,
    data,
    meta: {
      page,
      limit,
      total,
      totalPages,
      hasMore: page < totalPages,
    },
  };
}

export function error(code: string, message: string, details?: any): ApiResponse {
  return { success: false, error: { code, message, details } };
}

/**
 * Common error codes
 */
export const ErrorCodes = {
  UNAUTHORIZED: 'UNAUTHORIZED',
  FORBIDDEN: 'FORBIDDEN',
  NOT_FOUND: 'NOT_FOUND',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  CONFLICT: 'CONFLICT',
  RATE_LIMITED: 'RATE_LIMITED',
  INTERNAL_ERROR: 'INTERNAL_ERROR',
  PAYMENT_REQUIRED: 'PAYMENT_REQUIRED',
  FEATURE_DISABLED: 'FEATURE_DISABLED',
  PREMIUM_REQUIRED: 'PREMIUM_REQUIRED',
  BAD_REQUEST: 'BAD_REQUEST',
  FILE_TOO_LARGE: 'FILE_TOO_LARGE',
  UNSUPPORTED_FORMAT: 'UNSUPPORTED_FORMAT',
  STRIPE_NOT_CONFIGURED: 'STRIPE_NOT_CONFIGURED',
} as const;
