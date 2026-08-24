import { config } from '../config';
import path from 'path';

/**
 * Storage service - handles file URL generation.
 * In development, files are served locally.
 * In production, files are stored in S3-compatible storage.
 */
export class StorageService {
  /**
   * Get the public URL for a stored file.
   */
  static getFileUrl(key: string): string {
    if (config.storage.isS3Configured && config.storage.s3.publicBaseUrl) {
      return `${config.storage.s3.publicBaseUrl}/${key}`;
    }
    // Local development - serve via /uploads endpoint
    return `/uploads/${key}`;
  }

  /**
   * Get the storage key from a filename and type.
   */
  static generateKey(type: 'audio' | 'images', filename: string): string {
    return `${type}/${filename}`;
  }

  /**
   * Get the local file path for a storage key.
   */
  static getLocalPath(key: string): string {
    return path.resolve(config.storage.uploadDir, key);
  }

  /**
   * Delete a file from storage.
   */
  static async deleteFile(key: string): Promise<void> {
    if (config.storage.isS3Configured) {
      // TODO: Implement S3 deletion when S3 is configured
      // const s3 = new S3Client({ ... });
      // await s3.send(new DeleteObjectCommand({ Bucket: config.storage.s3.bucket, Key: key }));
      return;
    }

    const fs = require('fs');
    const filePath = this.getLocalPath(key);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
  }

  /**
   * Check if a file exists in storage.
   */
  static fileExists(key: string): boolean {
    if (config.storage.isS3Configured) {
      // TODO: Implement S3 check
      return true;
    }
    const fs = require('fs');
    return fs.existsSync(this.getLocalPath(key));
  }
}
