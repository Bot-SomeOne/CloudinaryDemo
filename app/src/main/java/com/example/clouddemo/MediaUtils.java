package com.example.clouddemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MediaUtils {
    private static final String TAG = "MediaUtils";
    private static final String IMAGE_FOLDER = "images";
    private static final String VIDEO_FOLDER = "videos";

    /**
     * Determine if a URI is an image or video
     *
     * @param context Application context
     * @param uri Media URI to check
     * @return "image", "video", or null if not determined
     */
    public static String getMediaType(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                return "image";
            } else if (mimeType.startsWith("video/")) {
                return "video";
            }
        }
        return null;
    }

    /**
     * Save media file from Uri to internal storage
     *
     * @param context Application context
     * @param mediaUri Source media Uri
     * @param mediaType "image" or "video"
     * @return File object of the saved media, or null if saving failed
     */
    public static File saveMediaToInternalStorage(Context context, Uri mediaUri, String mediaType) {
        try {
            // Generate filename based on timestamp
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName;
            String folderName;

            if ("image".equals(mediaType)) {
                fileName = "IMG_" + timeStamp + ".jpg";
                folderName = IMAGE_FOLDER;
            } else if ("video".equals(mediaType)) {
                fileName = "VID_" + timeStamp + getFileExtension(context, mediaUri);
                folderName = VIDEO_FOLDER;
            } else {
                Log.e(TAG, "Unsupported media type: " + mediaType);
                return null;
            }

            // Create directory in app's files directory
            File directory = new File(context.getFilesDir(), folderName);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File destinationFile = new File(directory, fileName);

            // For images, we might want to compress/resize
            if ("image".equals(mediaType)) {
                InputStream inputStream = context.getContentResolver().openInputStream(mediaUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) {
                    inputStream.close();
                }

                // Optional: Compress image if too large
                // bitmap = compressImage(bitmap, 85, 1280, 1280);

                FileOutputStream fos = new FileOutputStream(destinationFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                fos.flush();
                fos.close();
            }
            // For videos, copy the file directly
            else if ("video".equals(mediaType)) {
                InputStream inputStream = context.getContentResolver().openInputStream(mediaUri);
                FileOutputStream fileOutputStream = new FileOutputStream(destinationFile);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, length);
                }

                fileOutputStream.flush();
                fileOutputStream.close();
                inputStream.close();
            }

            Log.d(TAG, "Media saved successfully: " + destinationFile.getAbsolutePath());
            return destinationFile;
        } catch (IOException e) {
            Log.e(TAG, "Error saving media: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a thumbnail from a video file
     *
     * @param context Application context
     * @param videoFile Video file to create thumbnail from
     * @return File object of the saved thumbnail, or null if creation failed
     */
    public static File createVideoThumbnail(Context context, File videoFile) {
        try {
            // Generate filename for thumbnail
            String videoName = videoFile.getName();
            String thumbnailName = videoName.substring(0, videoName.lastIndexOf(".")) + "_thumb.jpg";

            // Create directory
            File directory = new File(context.getFilesDir(), "thumbnails");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File thumbnailFile = new File(directory, thumbnailName);

            // Create thumbnail
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoFile.getAbsolutePath());
            Bitmap bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            retriever.release();

            // Resize thumbnail if needed
            bitmap = Bitmap.createScaledBitmap(bitmap, 512, 512 * bitmap.getHeight() / bitmap.getWidth(), true);

            // Save thumbnail
            FileOutputStream fos = new FileOutputStream(thumbnailFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();

            Log.d(TAG, "Video thumbnail created: " + thumbnailFile.getAbsolutePath());
            return thumbnailFile;
        } catch (Exception e) {
            Log.e(TAG, "Error creating video thumbnail: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get file extension from URI including the dot (e.g., ".mp4")
     */
    private static String getFileExtension(Context context, Uri uri) {
        String extension;

        if (uri.getScheme().equals("content")) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(
                    context.getContentResolver().getType(uri));
        } else {
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
        }

        if (extension == null || extension.isEmpty()) {
            // Default extensions if we can't determine
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType != null) {
                if (mimeType.startsWith("video/")) {
                    extension = "mp4";
                } else if (mimeType.startsWith("image/")) {
                    extension = "jpg";
                }
            }
        }

        return extension != null ? "." + extension : "";
    }

    /**
     * Delete media file from internal storage
     *
     * @param file File to delete
     * @return True if deleted successfully
     */
    public static boolean deleteMedia(File file) {
        if (file != null && file.exists()) {
            boolean result = file.delete();
            Log.d(TAG, "Media deleted: " + result + " - " + file.getAbsolutePath());
            return result;
        }
        return false;
    }
}