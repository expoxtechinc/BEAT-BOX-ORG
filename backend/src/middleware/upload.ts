import multer from 'multer';
import path from 'path';
import fs from 'fs';
import { config } from '../config';
import { v4 as uuidv4 } from 'uuid';
import { ErrorCodes } from '../utils/response';
import { AppError } from './error';

// Ensure upload directory exists
const uploadDir = path.resolve(config.storage.uploadDir);
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

const audioSubdir = path.join(uploadDir, 'audio');
const imageSubdir = path.join(uploadDir, 'images');
if (!fs.existsSync(audioSubdir)) fs.mkdirSync(audioSubdir, { recursive: true });
if (!fs.existsSync(imageSubdir)) fs.mkdirSync(imageSubdir, { recursive: true });

// Storage configuration
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    if (file.fieldname === 'audio' || file.fieldname === 'audioFile') {
      cb(null, audioSubdir);
    } else if (file.fieldname === 'artwork' || file.fieldname === 'image' || file.fieldname === 'avatar') {
      cb(null, imageSubdir);
    } else {
      cb(null, uploadDir);
    }
  },
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    const filename = `${uuidv4()}${ext}`;
    cb(null, filename);
  },
});

// File filter for audio files
const audioFileFilter = (req: any, file: Express.Multer.File, cb: multer.FileFilterCallback) => {
  const ext = path.extname(file.originalname).toLowerCase().replace('.', '');
  const allowed = config.uploadLimits.allowedAudio;

  if (allowed.includes(ext)) {
    cb(null, true);
  } else {
    cb(new AppError(
      ErrorCodes.UNSUPPORTED_FORMAT,
      `Unsupported audio format: .${ext}. Supported formats: ${allowed.join(', ')}.`,
      400
    ));
  }
};

// File filter for image files
const imageFileFilter = (req: any, file: Express.Multer.File, cb: multer.FileFilterCallback) => {
  const ext = path.extname(file.originalname).toLowerCase().replace('.', '');
  const allowed = config.uploadLimits.allowedImage;

  if (allowed.includes(ext)) {
    cb(null, true);
  } else {
    cb(new AppError(
      ErrorCodes.UNSUPPORTED_FORMAT,
      `Unsupported image format: .${ext}. Supported formats: ${allowed.join(', ')}.`,
      400
    ));
  }
};

// Multer instances
export const uploadAudio = multer({
  storage,
  fileFilter: audioFileFilter,
  limits: {
    fileSize: config.uploadLimits.maxAudioSize,
  },
}).single('audio');

export const uploadImage = multer({
  storage,
  fileFilter: imageFileFilter,
  limits: {
    fileSize: config.uploadLimits.maxImageSize,
  },
}).single('artwork');

export const uploadAvatar = multer({
  storage,
  fileFilter: imageFileFilter,
  limits: {
    fileSize: config.uploadLimits.maxImageSize,
  },
}).single('avatar');

// Combined upload for music (audio + artwork)
export const uploadMusic = multer({
  storage,
  fileFilter: (req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase().replace('.', '');
    if (file.fieldname === 'audio' && config.uploadLimits.allowedAudio.includes(ext)) {
      cb(null, true);
    } else if (file.fieldname === 'artwork' && config.uploadLimits.allowedImage.includes(ext)) {
      cb(null, true);
    } else {
      cb(new AppError(
        ErrorCodes.UNSUPPORTED_FORMAT,
        `Unsupported file format for field ${file.fieldname}.`,
        400
      ));
    }
  },
  limits: {
    fileSize: config.uploadLimits.maxAudioSize, // Use the larger limit
  },
}).fields([
  { name: 'audio', maxCount: 1 },
  { name: 'artwork', maxCount: 1 },
]);

// Multer error handler
export const handleUploadError = (err: any, _req: any, res: any, next: any) => {
  if (err instanceof multer.MulterError) {
    if (err.code === 'LIMIT_FILE_SIZE') {
      return res.status(400).json({
        success: false,
        error: {
          code: ErrorCodes.FILE_TOO_LARGE,
          message: `File too large. Maximum size is ${config.uploadLimits.maxAudioSize / (1024 * 1024)}MB for audio and ${config.uploadLimits.maxImageSize / (1024 * 1024)}MB for images.`,
        },
      });
    }
    return res.status(400).json({
      success: false,
      error: {
        code: ErrorCodes.BAD_REQUEST,
        message: `Upload error: ${err.message}`,
      },
    });
  }
  next(err);
};
