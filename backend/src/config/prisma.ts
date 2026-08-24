import { PrismaClient } from '@prisma/client';
import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';

// On Vercel serverless, the filesystem is read-only except for /tmp.
// If using SQLite, we need to create the database at /tmp/dev.db on first cold start.
let prisma: PrismaClient;

function initializeDatabase() {
  const dbUrl = process.env.DATABASE_URL || 'file:/tmp/dev.db';

  // Only auto-initialize SQLite on Vercel
  if (process.env.VERCEL && dbUrl.startsWith('file:')) {
    const dbPath = dbUrl.replace('file:', '');
    const dbDir = path.dirname(dbPath);

    // Ensure the directory exists
    if (!fs.existsSync(dbDir)) {
      fs.mkdirSync(dbDir, { recursive: true });
    }

    // If the database file doesn't exist, create it by pushing the schema
    if (!fs.existsSync(dbPath)) {
      try {
        console.log('[BeatBox] Initializing SQLite database at', dbPath);
        execSync('npx prisma db push --schema=backend/prisma/schema.prisma --accept-data-loss --skip-generate', {
          stdio: 'pipe',
          env: { ...process.env, DATABASE_URL: dbUrl },
          timeout: 30000,
        });
        console.log('[BeatBox] Database schema pushed successfully');

        // Seed the database
        try {
          execSync('npx tsx backend/prisma/seed.ts', {
            stdio: 'pipe',
            env: { ...process.env, DATABASE_URL: dbUrl },
            timeout: 30000,
          });
          console.log('[BeatBox] Database seeded successfully');
        } catch (seedErr) {
          console.error('[BeatBox] Seeding failed (non-critical):', seedErr);
        }
      } catch (err) {
        console.error('[BeatBox] Database initialization failed:', err);
      }
    }
  }
}

// Initialize database before creating Prisma client
initializeDatabase();

prisma = new PrismaClient({
  log: process.env.NODE_ENV === 'development' ? ['error', 'warn'] : ['error'],
});

export default prisma;
