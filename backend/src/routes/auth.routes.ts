import { Router, Request, Response } from 'express';
import { AuthService } from '../services/auth.service';
import { authenticate, AuthenticatedRequest } from '../middleware/auth';
import { asyncHandler } from '../middleware/error';
import { ok } from '../utils/response';
import { authRateLimiter } from '../middleware/rateLimiter';

const router = Router();

// Register
router.post('/register', authRateLimiter, asyncHandler(async (req: Request, res: Response) => {
  const { email, password, username, displayName } = req.body;
  const result = await AuthService.register(email, password, username, displayName);
  res.status(201).json(ok(result));
}));

// Login
router.post('/login', authRateLimiter, asyncHandler(async (req: Request, res: Response) => {
  const { email, password } = req.body;
  const result = await AuthService.login(email, password);
  res.json(ok(result));
}));

// Refresh token
router.post('/refresh', asyncHandler(async (req: Request, res: Response) => {
  const { refreshToken } = req.body;
  const result = await AuthService.refresh(refreshToken);
  res.json(ok(result));
}));

// Logout
router.post('/logout', asyncHandler(async (req: Request, res: Response) => {
  const { refreshToken } = req.body;
  if (refreshToken) {
    await AuthService.logout(refreshToken);
  }
  res.json(ok({ success: true, message: 'Logged out successfully.' }));
}));

// Logout from all devices
router.post('/logout-all', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  await AuthService.logoutAll(req.userId!);
  res.json(ok({ success: true, message: 'Logged out from all devices.' }));
}));

// Get current user
router.get('/me', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const user = await AuthService.getCurrentUser(req.userId!);
  res.json(ok(user));
}));

// Verify email
router.post('/verify-email', asyncHandler(async (req: Request, res: Response) => {
  const { token } = req.body;
  const result = await AuthService.verifyEmail(token);
  res.json(ok(result));
}));

// Request password reset
router.post('/forgot-password', authRateLimiter, asyncHandler(async (req: Request, res: Response) => {
  const { email } = req.body;
  const result = await AuthService.requestPasswordReset(email);
  res.json(ok(result));
}));

// Reset password
router.post('/reset-password', authRateLimiter, asyncHandler(async (req: Request, res: Response) => {
  const { token, password } = req.body;
  const result = await AuthService.resetPassword(token, password);
  res.json(ok(result));
}));

// Change password (authenticated)
router.post('/change-password', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const { currentPassword, newPassword } = req.body;
  const result = await AuthService.changePassword(req.userId!, currentPassword, newPassword);
  res.json(ok(result));
}));

// Delete account
router.delete('/account', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const { password } = req.body;
  const result = await AuthService.deleteAccount(req.userId!, password);
  res.json(ok(result));
}));

// Resend email verification
router.post('/resend-verification', authenticate, asyncHandler(async (req: AuthenticatedRequest, res: Response) => {
  const { generateSecureToken } = await import('../utils/jwt');
  const prisma = (await import('../config/prisma')).default;
  const token = generateSecureToken();
  await prisma.user.update({
    where: { id: req.userId! },
    data: { emailVerifyToken: token },
  });
  // In production, send email with verification link
  res.json(ok({ success: true, message: 'Verification email sent.' }));
}));

export default router;
