package com.arhath.camera.activities;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.arhath.camera.R;
import com.arhath.camera.services.FileUploadService;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;


public class CameraViewActivity extends Activity
{

    private static final int IMAGE_FORMAT = ImageFormat.NV21;
    private static int CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_FRONT;


    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private Camera.Size mSize;
    private int displayDegree;

    private byte[] mData;

    LinearLayout toggleLL,devLL;
    ImageButton toggleCameraIV,devIV;
    private static Dialog mDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_camera_view);
//        GetPermission();


        mSurfaceView = findViewById(R.id.surface);
        toggleLL = findViewById(R.id.toggleLL);
        toggleCameraIV = findViewById(R.id.toggleCameraIV);
        devLL = findViewById(R.id.devLL);
        devIV = findViewById(R.id.devIV);

        FirebaseApp.initializeApp(this);


        LinearLayout button = findViewById(R.id.take_picture);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                /// afetr selecting image

                if (mData != null)
                {
                    Bitmap bitmap = convertBitmap(mData, mCamera);
                    bitmap = getResizedBitmap(bitmap, 1000);

//                    callUploadFileApi(bitmap);

                    FileUploadService.startUploadBitmap(CameraViewActivity.this, bitmap);
                }
                else {

                }


            }
        });


        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {

                            SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
                            surfaceHolder.addCallback(new SurfaceHolder.Callback()
                            {
                                @Override
                                public void surfaceCreated(SurfaceHolder holder)
                                {

                                    openCamera(holder, CAMERA_ID);
                                }

                                @Override
                                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
                                {

                                }

                                @Override
                                public void surfaceDestroyed(SurfaceHolder holder)
                                {
                                    releaseCamera();
                                }
                            });


                            toggleCameraIV.setOnClickListener(new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View view)
                                {
                                    if (CAMERA_ID == Camera.CameraInfo.CAMERA_FACING_FRONT)
                                    {
                                        CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_BACK;
                                    }
                                    else
                                    {
                                        CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_FRONT;
                                    }
                                    openCamera(surfaceHolder, CAMERA_ID);
                                }
                            });


                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();



        devIV.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                startActivity(new Intent(CameraViewActivity.this, DeveloperInfoActivity.class));
            }
        });




    }

    private void openCamera(SurfaceHolder holder, int cameraId)
    {
        releaseCamera();
        mCamera = Camera.open(cameraId);
        Camera.Parameters parameters = mCamera.getParameters();
        displayDegree = setCameraDisplayOrientation(cameraId, mCamera);


        mSize = getOptimalSize(parameters.getSupportedPreviewSizes(), mSurfaceView.getWidth(), mSurfaceView.getHeight());
        parameters.setPreviewSize(mSize.width, mSize.height);
        parameters.setPreviewFormat(IMAGE_FORMAT);
//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        mCamera.setParameters(parameters);

        try
        {
            mCamera.setPreviewDisplay(holder);
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }


        mCamera.setPreviewCallback(new Camera.PreviewCallback()
        {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera)
            {
                mData = data;
                camera.addCallbackBuffer(data);
            }
        });


        mCamera.startPreview();


    }


    private synchronized void releaseCamera()
    {
        if (mCamera != null)
        {
            try
            {
                mCamera.setPreviewCallback(null);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            try
            {
                mCamera.stopPreview();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            try
            {
                mCamera.release();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            mCamera = null;
        }
    }

    private int setCameraDisplayOrientation(int cameraId, Camera camera)
    {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int displayDegree;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            displayDegree = (info.orientation + degrees) % 360;
            displayDegree = (360 - displayDegree) % 360;  // compensate the mirror
        }
        else
        {
            displayDegree = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(displayDegree);
        return displayDegree;
    }

    private static Camera.Size getOptimalSize(@NonNull List<Camera.Size> sizes, int w, int h)
    {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes)
        {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
            {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff)
            {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null)
        {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes)
            {
                if (Math.abs(size.height - targetHeight) < minDiff)
                {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }


    @SuppressLint("NewApi")
    public void GetPermission()
    {

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.CAMERA};
        if (!hasPermission(this, PERMISSIONS))
        {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            finish();
        }
    }

    public static boolean hasPermission(Context context, String... permissions)
    {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null)
        {
            for (String permission : permissions)
            {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public void onBackPressed()
    {

        finish();

    }


    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {

        super.onPause();
    }

    private Bitmap convertBitmap(byte[] data, Camera camera)
    {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        YuvImage yuvimage = new YuvImage(
                data,
                camera.getParameters().getPreviewFormat(),
                previewSize.width,
                previewSize.height,
                null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);
        byte[] rawImage = baos.toByteArray();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
        Matrix m = new Matrix();
        // Here my phone needs to rotate the image direction to be correct, if it’s not correct on your phone, adjust it yourself，
        //You can’t write this in a formal project. You need to calculate the direction. It is too troublesome to calculate the direction of YuvImage. I didn’t do it here。

        if (CAMERA_ID == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            m.setRotate(-displayDegree);
        }
        else
        {
            m.setRotate(displayDegree);
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }


    public Bitmap getResizedBitmap(Bitmap image, int maxSize)
    {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1)
        {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        }
        else
        {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void callUploadFileApi(Bitmap bitmap) {


        FirebaseStorage storage=FirebaseStorage.getInstance();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] data = stream.toByteArray();
        StorageReference imageStorage = storage.getReference();
        StorageReference imageRef = imageStorage.child("images/" + "imageName");

        Task<Uri> urlTask = imageRef.putBytes(data).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }

            // Continue with the task to get the download URL
            return imageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                String uri = downloadUri.toString();
//                sendMessageWithFile(uri);
                Log.d("Success", uri);
            } else {
                // Handle failures
                // ...
            }
//            progressBar.setVisibility(View.GONE);
        });

    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        FileUploadService.cancelUpload(this);
    }





}