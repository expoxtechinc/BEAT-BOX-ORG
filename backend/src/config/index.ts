import dotenv from 'dotenv';

dotenv.config();

// On Vercel, use /tmp for SQLite (only writable directory)
const isVercel = !!process.env.VERCEL;
const defaultDbUrl = isVercel ? 'file:/tmp/dev.db' : 'file:./dev.db';

function required(key: string, fallback?: string): string {
  const value = process.env[key] ?? fallback;
  if (value === undefined) {
    throw new Error(`Missing required environment variable: ${key}`);
  }
  return value;
}

function optional(key: string, fallback?: string): string | undefined {
  return process.env[key] ?? fallback;
}

function bool(key: string, fallback: boolean): boolean {
  const value = process.env[key];
  if (value === undefined) return fallback;
  return value === 'true' || value === '1';
}

function int(key: string, fallback: number): number {
  const value = parseInt(process.env[key] ?? '', 10);
  return isNaN(value) ? fallback : value;
}

export const config = {
  env: optional('NODE_ENV', isVercel ? 'production' : 'development') as string,
  port: int('PORT', 3000),
  apiPrefix: optional('API_PREFIX', '/api/v1') as string,

  database: {
    url: required('DATABASE_URL', defaultDbUrl),
  },

  jwt: {
    secret: required('JWT_SECRET', 'dev_secret_change_me'),
    expiresIn: optional('JWT_EXPIRES_IN', '7d') as string,
    refreshSecret: required('JWT_REFRESH_SECRET', 'dev_refresh_secret_change_me'),
    refreshExpiresIn: optional('JWT_REFRESH_EXPIRES_IN', '30d') as string,
  },

  cors: {
    origins: (optional('CORS_ORIGINS', '*') as string)
      .split(',')
      .map((s) => s.trim()),
  },

  storage: {
    uploadDir: optional('UPLOAD_DIR', isVercel ? '/tmp/uploads' : './uploads') as string,
    s3: {
      endpoint: optional('S3_ENDPOINT'),
      bucket: optional('S3_BUCKET'),
      region: optional('S3_REGION', 'us-east-1'),
      accessKeyId: optional('S3_ACCESS_KEY_ID'),
      secretAccessKey: optional('S3_SECRET_ACCESS_KEY'),
      publicBaseUrl: optional('S3_PUBLIC_BASE_URL'),
    },
    isS3Configured: !!(optional('S3_ENDPOINT') && optional('S3_BUCKET')),
  },

  uploadLimits: {
    maxAudioSize: int('MAX_AUDIO_FILE_SIZE_MB', 100) * 1024 * 1024,
    maxImageSize: int('MAX_IMAGE_FILE_SIZE_MB', 10) * 1024 * 1024,
    allowedAudio: (optional('ALLOWED_AUDIO_FORMATS', 'mp3,wav,m4a,aac,flac,ogg') as string)
      .split(',')
      .map((s) => s.trim()),
    allowedImage: (optional('ALLOWED_IMAGE_FORMATS', 'jpg,jpeg,png,webp') as string)
      .split(',')
      .map((s) => s.trim()),
  },

  premium: {
    freeUses: int('PREMIUM_FREE_USES', 5),
  },

  stripe: {
    enabled: bool('STRIPE_ENABLED', false),
    secretKey: optional('STRIPE_SECRET_KEY', ''),
    webhookSecret: optional('STRIPE_WEBHOOK_SECRET', ''),
    monthlyPriceId: optional('STRIPE_MONTHLY_PRICE_ID', ''),
    yearlyPriceId: optional('STRIPE_YEARLY_PRICE_ID', ''),
    publishableKey: optional('STRIPE_PUBLISHABLE_KEY', ''),
  },

  app: {
    url: optional('APP_URL', 'http://localhost:8080') as string,
    webhookSuccessUrl: optional('WEBHOOK_SUCCESS_URL', 'http://localhost:8080/subscription/success') as string,
    webhookCancelUrl: optional('WEBHOOK_CANCEL_URL', 'http://localhost:8080/subscription/cancel') as string,
    adminEmail: optional('ADMIN_EMAIL', 'admin@beatbox.com') as string,
  },

  rateLimit: {
    windowMs: int('RATE_LIMIT_WINDOW_MS', 900000),
    max: int('RATE_LIMIT_MAX', 300),
  },

  isProduction: optional('NODE_ENV', isVercel ? 'production' : 'development') === 'production',
  isDevelopment: optional('NODE_ENV', isVercel ? 'production' : 'development') === 'development',
  isVercel,
} as const;

export type AppConfig = typeof config;
