/**
 * Database initialization for Vercel serverless environment.
 * 
 * On Vercel, the /tmp directory is the only writable location and it is
 * ephemeral (reset on each cold start). This module ensures the SQLite
 * database exists, has the correct schema, and is seeded with initial data.
 */

import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';

let isInitialized = false;

/**
 * Ensure DATABASE_URL is set before Prisma tries to read it.
 * On Vercel, default to /tmp/dev.db if not explicitly set.
 */
export function ensureDatabaseUrl(): void {
  if (!process.env.DATABASE_URL) {
    process.env.DATABASE_URL = 'file:/tmp/dev.db';
  }
}

/**
 * Initialize the database on cold start.
 * - Creates the database file if it doesn't exist
 * - Pushes the Prisma schema
 * - Seeds initial data if the database is empty
 * 
 * This is safe to call on every cold start — it checks if initialization
 * has already been done in this function instance.
 */
export async function initDatabase(): Promise<void> {
  if (isInitialized) return;
  isInitialized = true;

  ensureDatabaseUrl();

  const dbPath = process.env.DATABASE_URL.replace('file:', '');
  const dbExists = fs.existsSync(dbPath);

  if (!dbExists) {
    // Ensure /tmp directory exists
    const dbDir = path.dirname(dbPath);
    if (!fs.existsSync(dbDir)) {
      fs.mkdirSync(dbDir, { recursive: true });
    }

    // Push schema to create the database and tables
    try {
      execSync('npx prisma db push --skip-generate --accept-data-loss', {
        stdio: 'pipe',
        env: process.env,
        cwd: process.cwd(),
      });
    } catch (e) {
      console.error('[DB Init] Schema push failed:', e);
    }

    // Seed initial data
    try {
      // Inline seed to avoid importing tsx
      const { PrismaClient } = require('@prisma/client');
      const bcrypt = require('bcryptjs');
      const prisma = new PrismaClient();

      const adminEmail = process.env.ADMIN_EMAIL || 'admin@beatbox.com';
      const existingAdmin = await prisma.user.findUnique({ where: { email: adminEmail } });

      if (!existingAdmin) {
        // Categories
        const categories = [
          { name: 'Pop', slug: 'pop', sortOrder: 1 },
          { name: 'Rock', slug: 'rock', sortOrder: 2 },
          { name: 'Hip Hop', slug: 'hip-hop', sortOrder: 3 },
          { name: 'Electronic', slug: 'electronic', sortOrder: 4 },
          { name: 'Jazz', slug: 'jazz', sortOrder: 5 },
          { name: 'Classical', slug: 'classical', sortOrder: 6 },
          { name: 'R&B', slug: 'rnb', sortOrder: 7 },
          { name: 'Country', slug: 'country', sortOrder: 8 },
          { name: 'Reggae', slug: 'reggae', sortOrder: 9 },
          { name: 'Indie', slug: 'indie', sortOrder: 10 },
          { name: 'Ambient', slug: 'ambient', sortOrder: 11 },
          { name: 'Lo-Fi', slug: 'lo-fi', sortOrder: 12 },
        ];

        for (const cat of categories) {
          await prisma.category.upsert({
            where: { slug: cat.slug },
            update: {},
            create: cat,
          });
        }

        // Premium features
        const features = [
          { key: 'analytics', name: 'Advanced Analytics', description: 'Detailed insights into your music performance, listener demographics, and engagement metrics.', sortOrder: 1 },
          { key: 'boost', name: 'Promotion & Boosting', description: 'Boost your music to reach more listeners with featured placement in discovery feeds.', sortOrder: 2 },
          { key: 'featured', name: 'Featured Placement', description: 'Get your music featured on the BeatBox home page and trending sections.', sortOrder: 3 },
          { key: 'advanced_tools', name: 'Advanced Audio Tools', description: 'Access to advanced audio processing, waveform editing, and mastering tools.', sortOrder: 4 },
          { key: 'creator_tools', name: 'Advanced Creator Tools', description: 'Unlock advanced creator tools including scheduled releases and multi-track uploads.', sortOrder: 5 },
        ];

        for (const feat of features) {
          await prisma.premiumFeature.upsert({
            where: { key: feat.key },
            update: {},
            create: feat,
          });
        }

        // Admin user
        const passwordHash = bcrypt.hashSync('Admin@BeatBox2024', 12);
        await prisma.user.create({
          data: {
            email: adminEmail,
            passwordHash,
            role: 'ADMIN',
            emailVerified: true,
            premiumFreeUses: 5,
            profile: {
              create: {
                username: 'admin',
                displayName: 'BeatBox Admin',
                bio: 'BeatBox platform administrator',
              },
            },
            subscription: {
              create: {
                status: 'FREE',
                plan: 'FREE',
              },
            },
          },
        });

        // App config
        const configs = [
          { key: 'app_name', value: 'BeatBox', description: 'Application name', isPublic: true },
          { key: 'support_email', value: 'support@beatbox.com', description: 'Support email address', isPublic: true },
          { key: 'terms_url', value: 'https://beatbox.com/terms', description: 'Terms of Service URL', isPublic: true },
          { key: 'privacy_url', value: 'https://beatbox.com/privacy', description: 'Privacy Policy URL', isPublic: true },
        ];

        for (const cfg of configs) {
          await prisma.appConfig.upsert({
            where: { key: cfg.key },
            update: {},
            create: cfg,
          });
        }

        console.log('[DB Init] Database seeded successfully');
      }

      await prisma.$disconnect();
    } catch (e) {
      console.error('[DB Init] Seeding failed:', e);
    }
  }
}
