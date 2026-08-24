import prisma from '../src/config/prisma';
import { config } from '../src/config';
import { hashPassword } from '../src/utils/password';

async function seed() {
  console.log('🌱 Seeding database...');

  // Create categories
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
  console.log(`✅ Created ${categories.length} categories`);

  // Create premium features
  const premiumFeatures = [
    { key: 'analytics', name: 'Advanced Analytics', description: 'Detailed insights into your music performance, listener demographics, and engagement metrics.', sortOrder: 1 },
    { key: 'boost', name: 'Promotion & Boosting', description: 'Boost your music to reach more listeners with featured placement in discovery feeds.', sortOrder: 2 },
    { key: 'featured', name: 'Featured Placement', description: 'Get your music featured on the BeatBox home page and trending sections.', sortOrder: 3 },
    { key: 'advanced_tools', name: 'Advanced Audio Tools', description: 'Access to advanced audio processing, waveform editing, and mastering tools.', sortOrder: 4 },
    { key: 'creator_tools', name: 'Advanced Creator Tools', description: 'Unlock advanced creator tools including scheduled releases and multi-track uploads.', sortOrder: 5 },
  ];

  for (const feat of premiumFeatures) {
    await prisma.premiumFeature.upsert({
      where: { key: feat.key },
      update: {},
      create: feat,
    });
  }
  console.log(`✅ Created ${premiumFeatures.length} premium features`);

  // Create admin user
  const adminEmail = config.app.adminEmail;
  const adminExists = await prisma.user.findUnique({ where: { email: adminEmail } });
  if (!adminExists) {
    const passwordHash = await hashPassword('Admin@BeatBox2024');
    const admin = await prisma.user.create({
      data: {
        email: adminEmail,
        passwordHash,
        role: 'ADMIN',
        emailVerified: true,
        premiumFreeUses: config.premium.freeUses,
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
    console.log(`✅ Created admin user: ${adminEmail} (password: Admin@BeatBox2024)`);
  } else {
    console.log('ℹ️  Admin user already exists');
  }

  // Create app config entries
  const appConfigs = [
    { key: 'app_name', value: 'BeatBox', description: 'Application name', isPublic: true },
    { key: 'support_email', value: 'support@beatbox.com', description: 'Support email address', isPublic: true },
    { key: 'terms_url', value: 'https://beatbox.com/terms', description: 'Terms of Service URL', isPublic: true },
    { key: 'privacy_url', value: 'https://beatbox.com/privacy', description: 'Privacy Policy URL', isPublic: true },
    { key: 'upload_terms_url', value: 'https://beatbox.com/upload-terms', description: 'Music Upload Terms URL', isPublic: true },
    { key: 'copyright_url', value: 'https://beatbox.com/copyright', description: 'Copyright Policy URL', isPublic: true },
    { key: 'community_guidelines_url', value: 'https://beatbox.com/guidelines', description: 'Community Guidelines URL', isPublic: true },
    { key: 'max_free_uploads', value: '1000', description: 'Maximum free uploads per user (technical limit)', isPublic: true },
  ];

  for (const cfg of appConfigs) {
    await prisma.appConfig.upsert({
      where: { key: cfg.key },
      update: {},
      create: cfg,
    });
  }
  console.log(`✅ Created ${appConfigs.length} app config entries`);

  console.log('');
  console.log('🌱 Seeding complete!');
  console.log('');
  console.log('Admin credentials:');
  console.log(`  Email: ${adminEmail}`);
  console.log('  Password: Admin@BeatBox2024');
  console.log('');
}

seed()
  .catch((err) => {
    console.error('❌ Seeding failed:', err);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
