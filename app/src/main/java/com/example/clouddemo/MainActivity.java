package com.example.clouddemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.clouddemo.api.ApiManager;
import com.example.clouddemo.model.ResponseData;
import com.example.clouddemo.utils.MediaUtils;
import com.example.clouddemo.utils.cloudinary.CloudinaryManager;
import com.example.clouddemo.utils.cloudinary.CloudinaryTransformationHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String FILEPROVIDER_AUTHORITY = "com.example.clouddemo.fileprovider";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private Uri currentMediaUri;
    private File savedMediaFile;
    private File videoThumbnailFile;
    private CloudinaryManager cloudinaryManager;
    private String currentMediaType = "image"; // Default to image

    private ImageView imagePreview;
    private VideoView videoPreview;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private Button btnUpload;
    private View previewPlaceholder;

    String url; // TODO: rm

    // Activity result launcher for picking image/video from gallery
    private final ActivityResultLauncher<String[]> pickMediaLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    // Persist permission for this URI
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    // Determine media type
                    currentMediaType = MediaUtils.getMediaType(MainActivity.this, uri);

                    Log.i(TAG, "Selected media type: " + currentMediaType);
                    Log.i(TAG, "Selected media URI: " + uri.toString());
                    handleMediaResult(uri);
                }
            });

    // Activity result launcher for taking photo with camera
    private final ActivityResultLauncher<Uri> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && currentMediaUri != null) {
                    currentMediaType = "image";
                    handleMediaResult(currentMediaUri);
                }
            });

    // Activity result launcher for recording video with camera
    private final ActivityResultLauncher<Uri> takeVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.CaptureVideo(),
            success -> {
                if (success && currentMediaUri != null) {
                    currentMediaType = "video";
                    handleMediaResult(currentMediaUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize CloudinaryManager
        cloudinaryManager = CloudinaryManager.getInstance(this);

        // Configure Cloudinary (you should replace these with your actual credentials)
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dan8zea37");

        cloudinaryManager.initialize(config);

        // Initialize views
        imagePreview = findViewById(R.id.imagePreview);
        videoPreview = findViewById(R.id.videoPreview);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        btnUpload = findViewById(R.id.btnUpload);
        previewPlaceholder = findViewById(R.id.previewPlaceholder);

        Button btnGallery = findViewById(R.id.btnGallery);
        Button btnCamera = findViewById(R.id.btnCamera);

        // Set up click listeners
        btnGallery.setOnClickListener(v -> openGallery());
        btnCamera.setOnClickListener(v -> showCameraOptions());
        btnUpload.setOnClickListener(v -> uploadToCloudinary());

        // Initially hide upload button until media is selected
        btnUpload.setVisibility(View.GONE);

    }

    /**
     * Test del resource
     */
    private void TestDelResource() {
        cloudinaryManager.deleteResource("file_kl3hmg", "image", new CloudinaryManager.CloudinaryCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "del onSuccess");
                Log.d(TAG, result);
            }

            @Override
            public void onError(String errorMsg) {
                Log.d(TAG, "del onError");
                Log.d(TAG, errorMsg);
            }

            @Override
            public void onProgress(int progress) {
                Log.d(TAG, "del onProgress");
                Log.d(TAG, String.valueOf(progress));
            }
        });
    }

    /**
     * Test get image with public id
     */
    private void TestGetImage() {
        // TODO: rm
        CloudinaryTransformationHelper cloudinaryTransformationHelper = new CloudinaryTransformationHelper();
        cloudinaryTransformationHelper.autoFormat();
        String publicID = "file_kl3hmg";
        String resourceType = "image";
        this.url = cloudinaryManager.getResourceUrl(
                publicID,
                resourceType,
                cloudinaryTransformationHelper.getTransformations()
        );
        Log.i(TAG, "URL: " + url);

        MediaUtils.getMediaFromHost(this, url, "")
                .thenAccept(a -> {
                    if (a == null) {
                        Log.i(TAG, "File path: null");
                    } else {
                        Log.i(TAG, "File path: " + a.getAbsolutePath());
                        runOnUiThread(() -> {
                            imagePreview.setVisibility(View.VISIBLE);
                            imagePreview.setImageURI(Uri.fromFile(a));
                        });
                    }
                });
    }

    /**
     * Open gallery for media selection
     */
    private void openGallery() {
        pickMediaLauncher.launch(new String[]{"image/*", "video/*"});
    }

    /**
     * Show camera options dialog (photo or video)
     */
    private void showCameraOptions() {
        // Check camera permission first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }

        // Show dialog to choose between photo and video
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Capture Media");
        builder.setItems(new String[]{"Take Photo", "Record Video"}, (dialog, which) -> {
            if (which == 0) {
                takePhoto();
            } else {
                takeVideo();
            }
        });
        builder.show();
    }

    /**
     * Open camera to take a photo
     */
    private void takePhoto() {
        try {
            File photoFile = createMediaFile(".jpg");

            currentMediaUri = FileProvider.getUriForFile(this,
                    FILEPROVIDER_AUTHORITY,
                    photoFile);
            Log.i(TAG, "Photo URI: " + currentMediaUri.toString());
            takePhotoLauncher.launch(currentMediaUri);

        } catch (IOException ex) {
            Log.e(TAG, "Error creating image file", ex);
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open camera to record a video
     */
    private void takeVideo() {
        try {
            File videoFile = createMediaFile(".mp4");

            currentMediaUri = FileProvider.getUriForFile(this,
                    FILEPROVIDER_AUTHORITY,
                    videoFile);

            Log.i(TAG, "Video URI: " + currentMediaUri.toString());
            takeVideoLauncher.launch(currentMediaUri);

        } catch (IOException ex) {
            Log.e(TAG, "Error creating video file", ex);
            Toast.makeText(this, "Error creating video file", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Create a temporary file for media capture
     */
    private File createMediaFile(String extension) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "MEDIA_" + timeStamp + "_";
        File storageDir = getFilesDir();
        return File.createTempFile(fileName, extension, storageDir);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCameraOptions();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Handle the selected/captured media
     *
     * @param mediaUri URI of the selected/captured media
     */
    private void handleMediaResult(Uri mediaUri) {
        tvStatus.setText("Processing media...");
        progressBar.setVisibility(View.VISIBLE);
        previewPlaceholder.setVisibility(View.GONE);

        // Save media to internal storage
        new Thread(() -> {
            savedMediaFile = MediaUtils.saveMediaToInternalStorage(
                    this,
                    mediaUri,
                    currentMediaType
            );

            if (savedMediaFile != null) {
                runOnUiThread(() -> {
                    // Show preview of the media
                    displayMediaPreview(savedMediaFile);
                    Log.i(TAG, "Saved media file path: " + savedMediaFile.getAbsolutePath());

                    progressBar.setVisibility(View.GONE);

                    // For videos, create a thumbnail
                    if ("video".equals(currentMediaType)) {
                        videoThumbnailFile = MediaUtils.createVideoThumbnail(this, savedMediaFile);
                    }

                    // Show upload button
                    btnUpload.setVisibility(View.VISIBLE);

                    tvStatus.setText("Ready to upload. Click 'Upload to Cloudinary' button.");
                });
            } else {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    previewPlaceholder.setVisibility(View.VISIBLE);
                    tvStatus.setText("Failed to save media");
                    Toast.makeText(this, "Failed to save media", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Display media preview
     *
     * @param mediaFile File to display
     */
    private void displayMediaPreview(File mediaFile) {
        if ("image".equals(currentMediaType)) {
            imagePreview.setVisibility(View.VISIBLE);
            videoPreview.setVisibility(View.GONE);
            imagePreview.setImageURI(Uri.fromFile(mediaFile));
        } else if ("video".equals(currentMediaType)) {
            imagePreview.setVisibility(View.GONE);
            videoPreview.setVisibility(View.VISIBLE);
            videoPreview.setVideoURI(Uri.fromFile(mediaFile));
            videoPreview.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                videoPreview.start();
            });
        }
    }

    /**
     * Upload currently selected media to Cloudinary
     */
    private void uploadToCloudinary() {
        if (savedMediaFile == null || !savedMediaFile.exists()) {
            Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "File path check before upload:");
        Log.d(TAG, "- Absolute path: " + Uri.parse(savedMediaFile.getAbsolutePath()));
        Log.d(TAG, "- File exists: " + savedMediaFile.exists());
        Log.d(TAG, "- File length: " + savedMediaFile.length());
        Log.d(TAG, "- File can read: " + savedMediaFile.canRead());

        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Uploading to Cloudinary...");
        btnUpload.setEnabled(false);

        if ("image".equals(currentMediaType)) {
            uploadImage(savedMediaFile);
        } else if ("video".equals(currentMediaType)) {
            uploadVideo(savedMediaFile);
        }
    }

    /**
     * Upload image to Cloudinary
     */
    private void uploadImage(File imageFile) {
        String folder = "/users/test/images/";
        cloudinaryManager.uploadImage(
                Uri.fromFile(imageFile).toString(),
                folder,
                new CloudinaryManager.CloudinaryCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnUpload.setEnabled(true);

                            // Get public ID and URL
                            String publicId = (String) result.get("public_id");
                            String url = (String) result.get("url");

                            Log.d(TAG, "Public Id: " + publicId);
                            Log.d(TAG, "URL: " + url);

                            tvStatus.setText("Upload successful!\nURL: " + url);
                            Log.d(TAG, "Public Id: " + publicId);
                            Log.d(TAG, "URL: " + url);
                            Toast.makeText(MainActivity.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String errorMsg) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnUpload.setEnabled(true);
                            tvStatus.setText("Upload failed: " + errorMsg);
                            Toast.makeText(MainActivity.this, "Upload failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Upload failed: " + errorMsg);
                        });
                    }

                    @Override
                    public void onProgress(int progress) {
                        runOnUiThread(() -> {
                            progressBar.setProgress(progress);
                            tvStatus.setText("Uploading: " + progress + "%");
                        });
                    }
                });
    }

    /**
     * Upload video to Cloudinary
     */
    private void uploadVideo(File videoFile) {
        String folder = "/users/test/videos/";
        cloudinaryManager.uploadVideo(
                Uri.fromFile(videoFile).toString(),
                folder,
                new CloudinaryManager.CloudinaryCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                        // Video upload successful, now upload thumbnail if available
                        String publicId = (String) result.get("public_id");
                        String url = (String) result.get("url");
                        Log.d(TAG, "Public Id: " + publicId);
                        Log.d(TAG, "URL: " + url);

                        if (videoThumbnailFile != null && videoThumbnailFile.exists()) {
                            // Upload thumbnail separately
                            uploadVideoThumbnail(videoThumbnailFile, publicId, url);
                        } else {
                            // No thumbnail, finish here
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                btnUpload.setEnabled(true);
                                tvStatus.setText("Upload successful!\nVideo URL: " + url);
                                Toast.makeText(MainActivity.this, "Video uploaded successfully", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onError(String errorMsg) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnUpload.setEnabled(true);
                            tvStatus.setText("Upload failed: " + errorMsg);
                            Toast.makeText(MainActivity.this, "Upload failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Upload failed: " + errorMsg);
                        });
                    }

                    @Override
                    public void onProgress(int progress) {
                        runOnUiThread(() -> {
                            progressBar.setProgress(progress);
                            tvStatus.setText("Uploading video: " + progress + "%");
                        });
                    }
                });
    }

    /**
     * Upload video thumbnail to Cloudinary
     */
    private void uploadVideoThumbnail(File thumbnailFile, String videoPublicId, String videoUrl) {
        String folder = "/users/test/thumbnails/";
        cloudinaryManager.uploadImage(
                Uri.fromFile(thumbnailFile).toString(),
                "/users/test/thumbnails/",
                new CloudinaryManager.CloudinaryCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnUpload.setEnabled(true);

                            // Get thumbnail public ID and URL
                            String thumbnailPublicId = (String) result.get("public_id");
                            String thumbnailUrl = (String) result.get("url");

                            Log.d(TAG, "Thumbnail Public Id: " + thumbnailPublicId);
                            Log.d(TAG, "Thumbnail URL: " + thumbnailUrl);

                            tvStatus.setText("Upload successful!\nVideo URL: " + videoUrl +
                                    "\nThumbnail URL: " + thumbnailUrl);
                            Toast.makeText(MainActivity.this, "Video and thumbnail uploaded", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String errorMsg) {
                        // Thumbnail upload failed, but video upload succeeded
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnUpload.setEnabled(true);
                            tvStatus.setText("Video uploaded, but thumbnail failed: " + errorMsg +
                                    "\nVideo URL: " + videoUrl);
                            Toast.makeText(MainActivity.this, "Video uploaded, thumbnail failed", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Upload failed: " + errorMsg);
                        });
                    }

                    @Override
                    public void onProgress(int progress) {
                        runOnUiThread(() -> {
                            progressBar.setProgress(progress);
                            tvStatus.setText("Uploading thumbnail: " + progress + "%");
                        });
                    }
                });
    }
}