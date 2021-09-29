package com.example.timbersmartbarcodescanner.scan;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.timbersmartbarcodescanner.R;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class BarcodeScannerActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {


    private static final String TAG = "BarcodeScannerActivity";
    public static final String RESULT_DATA_BARCODES = "barcodes";
    public static final String RESULT_DATA_IMAGE_ID = "image";

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 0;
    private static final Matrix BITMAP_ROTATION_MATRIX = new Matrix() {{
        postRotate(-90);
    }};

    private final BarcodeScanner scanner = BarcodeScanning.getClient();

    private File bitmapDirectory;
    private int width, height;

    private TextureView textureView;
    private ImageView icBack;
    private CameraDevice camera;
    private Size size;

    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner);
        textureView = findViewById(R.id.tv);
        icBack = findViewById(R.id.back);
        icBack.setOnClickListener(v -> finish());
        textureView.setSurfaceTextureListener(this);
        textureView.setOnClickListener(v -> finish());
        bitmapDirectory = new ContextWrapper(getApplicationContext()).getDir("bitmap_", Context.MODE_PRIVATE);

    }


    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        this.width = width;
        this.height = height;
        setupCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        this.width = width;
        this.height = height;
        setupCamera();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        if (camera != null) {
            camera.close();
            camera = null;
        }
        return false;
    }

    private long preScanTime = 0;
    private long intervalTime;

    @Override
    protected void onResume() {
        super.onResume();
        intervalTime = getSharedPreferences("Scan interval", Context.MODE_PRIVATE).getLong("Scan interval time", 2000);
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        Log.i(TAG, "onSurfaceTextureUpdated: first scan ready");
        if ((System.currentTimeMillis() - preScanTime) > intervalTime) {
            Bitmap bitmap = textureView.getBitmap();
            scanner.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener(barcodes -> {
                        Log.i(TAG, "onSurfaceTextureUpdated: first scan ");
                        preScanTime = System.currentTimeMillis();
                        onBarcodeRead(barcodes, bitmap);
                    })
                    .addOnFailureListener(e -> isRunning = false);
        } else {
            Log.i(TAG, "onSurfaceTextureUpdated: repetition");
            Toast.makeText(this, "Do not scan code repeatedly within " + intervalTime / 1000 + " seconds", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
            setupCamera();
        }
    }

    private void setupCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }

        if (camera != null) {
            camera.close();
            camera = null;
        }

        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            String cameraId = cameraManager.getCameraIdList()[0];

            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            size = map.getOutputSizes(SurfaceTexture.class)[0];

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    BarcodeScannerActivity.this.camera = camera;
                    SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
                    surfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
                    Surface surface = new Surface(surfaceTexture);

                    try {
                        // Ignore deprecation inspection for android version match.
                        //noinspection deprecation
                        camera.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                try {
                                    CaptureRequest.Builder captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                    captureRequestBuilder.addTarget(surface);
                                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                    CaptureRequest captureRequest = captureRequestBuilder.build();

                                    session.setRepeatingRequest(captureRequest, null, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }

                                configureTransform(width, height);
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    BarcodeScannerActivity.this.camera = null;

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    BarcodeScannerActivity.this.camera = null;

                }
            }, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == textureView || null == size) {
            return;
        }
        int rotation = getDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, size.getHeight(), size.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / size.getHeight(),
                    (float) viewWidth / size.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    private void onBarcodeRead(List<Barcode> barcodes, Bitmap capture) {
        isRunning = false;
        if (barcodes.isEmpty()) return;
        int imageId = saveImage(capture);
        String[] barcodeArr = barcodes.stream().map(Barcode::getRawValue).toArray(String[]::new);
        Intent intent = new Intent();
        intent.putExtra(RESULT_DATA_BARCODES, barcodeArr);
        intent.putExtra(RESULT_DATA_IMAGE_ID, imageId);
        setResult(RESULT_OK, intent);
        finish();
    }


    public void playSound() {
        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.beep);
        mp.start();
    }


    private int saveImage(Bitmap image) {
        int imageId = generateUniqueId();

        File file = new File(bitmapDirectory, imageId + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bitmapToByteArray(image));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageId;
    }

    private static byte[] bitmapToByteArray(Bitmap bitmap) {
        Bitmap processed = processBitmap(bitmap);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        processed.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        processed.recycle();
        return stream.toByteArray();
    }

    private static Bitmap processBitmap(Bitmap bitmap) {
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), BITMAP_ROTATION_MATRIX, false);
        Bitmap scaled = Bitmap.createScaledBitmap(rotated, rotated.getHeight(), rotated.getWidth(), false);
        rotated.recycle();
        return scaled;
    }

    private static int generateUniqueId() {
        // int id = Data.getDataInstance().getImageIdCount();
        //  Data.getDataInstance().setImageIdCount(id + 1);
        int id = com.example.timbersmartbarcodescanner.Barcode.getImageIdCount();
        com.example.timbersmartbarcodescanner.Barcode.setImageIdCount(id + 1);
        return id;
    }
}