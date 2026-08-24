# BeatBox

**Your Music. Your Platform.**

A complete music/audio platform with an Android app (Kotlin/Jetpack Compose) and a secure Node.js/Express backend with Prisma, Stripe subscriptions, and a server-authoritative premium system.

---

## Critical Business Rule

| Feature | Cost |
|---|---|
| **Music Upload** | **FREE FOREVER** — does not consume premium uses, never triggers paywall |
| **Premium Features** | **5 free uses** per user, then subscription required |
| **Active Subscriber** | **Unlimited** premium features |
| **Music Upload (even after 0 uses)** | **Still FREE** |

---

## Project Structure

```
beatbox/
├── api/                    # Vercel serverless entry point
│   └── index.ts
├── backend/                # Node.js/Express backend API
│   ├── prisma/             # Database schema & seed
│   ├── src/
│   │   ├── config/         # App configuration, Prisma client
│   │   ├── middleware/     # Auth, error handling, upload, rate limiting
│   │   ├── routes/         # API route handlers
│   │   ├── services/       # Business logic (auth, music, premium, stripe)
│   │   └── utils/          # JWT, password hashing, response helpers
│   ├── package.json
│   └── tsconfig.json
├── android/                # Android app (Kotlin/Jetpack Compose)
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── java/com/beatbox/
│   │       │   ├── data/       # API client, DTOs, repositories
│   │       │   ├── di/         # Hilt dependency injection
│   │       │   ├── ui/        # Compose screens, theme, navigation
│   │       │   └── util/      # Session manager, constants
│   │       └── res/           # Resources (themes, fonts, drawables)
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── vercel.json             # Vercel deployment config
└── .gitignore
```

---

## Tech Stack

### Backend
- **Runtime**: Node.js with TypeScript
- **Framework**: Express.js
- **Database**: SQLite (dev) / PostgreSQL (production) via Prisma ORM
- **Auth**: JWT with refresh token rotation, bcrypt password hashing
- **Payments**: Stripe Checkout, Customer Portal, Webhooks
- **Storage**: Local filesystem (dev) / S3-compatible (production)
- **Security**: Helmet, CORS, rate limiting, encrypted token storage

### Android
- **Language**: Kotlin
- **UI**: Jetpack Compose, Material 3
- **Font**: Times New Roman (via FontFamily.Serif, centralized in Type.kt)
- **Architecture**: MVVM / Clean Architecture
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp with token refresh interceptor
- **Audio**: Media3 / ExoPlayer
- **Image Loading**: Coil
- **Navigation**: Navigation Compose
- **Storage**: DataStore (preferences), EncryptedSharedPreferences (tokens)

---

## Backend API Endpoints

### Authentication (`/api/v1/auth`)
| Method | Path | Description |
|---|---|---|
| POST | `/register` | Create account |
| POST | `/login` | Login |
| POST | `/refresh` | Refresh access token |
| POST | `/logout` | Logout |
| POST | `/logout-all` | Logout all devices |
| GET | `/me` | Get current user |
| POST | `/verify-email` | Verify email token |
| POST | `/forgot-password` | Request password reset |
| POST | `/reset-password` | Reset password with token |
| POST | `/change-password` | Change password (authenticated) |
| DELETE | `/account` | Delete account |

### Music (`/api/v1/music`)
| Method | Path | Description |
|---|---|---|
| POST | `/upload` | **Upload music (FREE — does not consume premium uses)** |
| PUT | `/:id` | Update music metadata |
| DELETE | `/:id` | Delete music |
| GET | `/:id` | Get music by ID |
| GET | `/my/uploads` | Get my uploads |
| GET | `/user/:userId` | Get user's uploads |
| POST | `/:id/play` | Record play |
| GET | `/` | Search music |
| GET | `/discover/trending` | Trending music |
| GET | `/discover/new` | New releases |
| GET | `/discover/featured` | Featured music |
| GET | `/discover/recommended` | Recommended music |
| GET | `/discover/recent` | Recently played |

### Premium (`/api/v1/premium`)
| Method | Path | Description |
|---|---|---|
| GET | `/status` | Get premium status (free uses, subscription) |
| GET | `/features` | Get premium feature list |
| GET | `/usage-history` | Get premium usage history |
| POST | `/use` | Use a premium feature (consumes 1 free use or requires subscription) |
| GET | `/can-use` | Check if user can use premium (without consuming) |
| POST | `/reset` | Reset free uses (admin only) |

### Stripe (`/api/v1/stripe`)
| Method | Path | Description |
|---|---|---|
| GET | `/config` | Get public Stripe config |
| POST | `/checkout` | Create checkout session |
| POST | `/portal` | Create customer portal session |
| POST | `/webhook` | Stripe webhook handler |

### Social (`/api/v1/social`)
Profiles, follows, likes, favorites, playlists, notifications, categories, artists.

### Search (`/api/v1/search`)
Universal search (music, users, playlists), search history.

### Reports (`/api/v1/reports`)
Report music, report users, block users.

### Admin (`/api/v1/admin`)
Dashboard stats, user management, music moderation, category management, premium feature management, report resolution, subscription/payment viewing, app config.

---

## Local Development

### Backend

```bash
cd backend
cp .env.example .env
# Edit .env with your values
npm install
npx prisma generate
npx prisma db push
npx tsx prisma/seed.ts
npm run dev
```

The backend will be available at `http://localhost:3000`.

**Admin credentials (after seeding):**
- Email: `admin@beatbox.com`
- Password: `Admin@BeatBox2024`

### Android

1. Open Android Studio
2. Open the `android/` folder
3. Gradle sync will download dependencies
4. Update `API_BASE_URL` in `build.gradle.kts` to point to your backend
5. Run on emulator or device

---

## Environment Variables

Copy `backend/.env.example` to `backend/.env` and configure:

| Variable | Required | Description |
|---|---|---|
| `DATABASE_URL` | Yes | Database connection string |
| `JWT_SECRET` | Yes | Secret for signing JWTs |
| `JWT_REFRESH_SECRET` | Yes | Secret for refresh tokens |
| `CORS_ORIGINS` | Yes | Comma-separated allowed origins |
| `PREMIUM_FREE_USES` | No | Defaults to 5 |
| `STRIPE_ENABLED` | No | Set to `true` when Stripe is configured |
| `STRIPE_SECRET_KEY` | No | From Stripe Dashboard |
| `STRIPE_WEBHOOK_SECRET` | No | From Stripe Dashboard |
| `STRIPE_MONTHLY_PRICE_ID` | No | Stripe Price ID for monthly plan |
| `STRIPE_YEARLY_PRICE_ID` | No | Stripe Price ID for yearly plan |
| `STRIPE_PUBLISHABLE_KEY` | No | Safe to expose to client |
| `ADMIN_EMAIL` | No | First user with this email gets admin role |

---

## Stripe Configuration

Stripe is **optional**. The app works fully without Stripe — music upload, discovery, playback, and 5 free premium uses all work. Stripe only gates the subscription checkout and customer portal.

To enable Stripe:
1. Set `STRIPE_ENABLED=true` in your environment
2. Add your Stripe Secret Key, Webhook Secret, and Price IDs
3. Configure the webhook endpoint in Stripe Dashboard pointing to `/api/v1/stripe/webhook`

**Never put Stripe Secret Keys in the Android app or commit them to Git.**

---

## Premium System (Server-Authoritative)

The premium counter (`premiumFreeUses`) is stored in the database and managed exclusively by the backend.

- Every new user starts with `premiumFreeUses = 5`
- Using a premium feature triggers an **atomic decrement** (`UPDATE ... WHERE premiumFreeUses > 0`)
- Subscribers (status = `ACTIVE`) get unlimited access without consuming free uses
- Music upload **never** touches the counter — it is always free
- Clearing app data, reinstalling, or switching devices does **not** reset the counter

---

## Vercel Deployment

See the step-by-step guide in the deployment section below.

---

## License

This is a proprietary project. All rights reserved.
