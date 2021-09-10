package com.arhath.camera.services;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.arhath.camera.R;
import com.arhath.camera.activities.CameraViewActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Objects;

public class FileUploadService extends Service
{

    public static final String START_UPLOAD = "START_UPLOAD";
    public static final String CANCEL_UPLOAD = "CANCEL_UPLOAD";
    private static final String FILE_LIST = "FILE_LIST";
    private static final String BITMAP = "BITMAP";
    private static final String TAG = FileUploadService.class.getSimpleName();
    private static String CHANNEL_ID = "FILE_UPLOAD";
    private static final int NOTIFICATION_ID = 111;





    private boolean isFileUploadRunning;

    static Bitmap imgBitmap;

    private CharSequence CHANNEL_NAME;
    private String CHANNEL_DESCRIPTION;
    private Dialog mDialog;
     static  Context context;

    public static void startUpload(Context context, ArrayList<String> fileList)
    {
        Intent intent = new Intent(context, FileUploadService.class);
        intent.setAction(START_UPLOAD);
        intent.putStringArrayListExtra(FILE_LIST, fileList);
        context.startService(intent);
    }

    public static void startUploadBitmap(Context mContext, Bitmap bitmap)
    {
        context= mContext;
        Intent intent = new Intent(mContext, FileUploadService.class);
        intent.setAction(START_UPLOAD);
        imgBitmap = bitmap;
        mContext.startService(intent);
    }

    public static void cancelUpload(Context context)
    {
        Intent intent = new Intent(context, FileUploadService.class);
        intent.setAction(CANCEL_UPLOAD);
        context.startService(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        String action = intent.getAction();
        if (!TextUtils.isEmpty(action))
        {
            switch (action)
            {
                case START_UPLOAD:


                    if (imgBitmap != null)
                    {
                        loadingDailog();
                        FirebaseApp.initializeApp(this);
                        startUploadingFile();
                    }


                    break;
                case CANCEL_UPLOAD:
                    cancelFileUpload();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void cancelFileUpload()
    {
        imgBitmap = null;
        isFileUploadRunning = false;
        hideNotification();
    }


    private void startUploadingFile()
    {


        String filePath = String.valueOf(System.currentTimeMillis());
        Log.e(TAG, "Upload File: " + filePath);

        callUploadFileApi(filePath);


        startForeground(100, showBigTextStyleNotification());


    }


    private void callUploadFileApi(String filePath)
    {


        FirebaseStorage storage = FirebaseStorage.getInstance();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        imgBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] data = stream.toByteArray();
        StorageReference imageStorage = storage.getReference();
        StorageReference imageRef = imageStorage.child("images/" + filePath);

        Task<Uri> urlTask = imageRef.putBytes(data).continueWithTask(task ->
        {
            if (!task.isSuccessful())
            {
                throw task.getException();
            }

            // Continue with the task to get the download URL
            return imageRef.getDownloadUrl();
        }).addOnCompleteListener(task ->
        {
            if (task.isSuccessful())
            {
                Uri downloadUri = task.getResult();
                String uri = downloadUri.toString();
//                sendMessageWithFile(uri);


                isFileUploadRunning = false;
                Log.e(TAG, "File Upload Complete.");
                hideNotification();

                Log.d("Success", uri);

                ShowSuccessNotification(uri);
                submitSuccesfullDailog();

            }
            else
            {

                submitFailDailog();
                // Handle failures
                // ...
            }
//            progressBar.setVisibility(View.GONE);
        });


    }

    private void hideNotification()
    {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
        stopForeground(true);
    }


    private Notification showBigTextStyleNotification()
    {

        CHANNEL_ID = this.getString(R.string.app_name);
        CHANNEL_NAME = this.getString(R.string.app_name);
        CHANNEL_DESCRIPTION = this.getString(R.string.app_name);

        String title = "File Upload", message = "Uploading to file to firebase storage";


        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(title);
        bigTextStyle.setSummaryText(message);
        bigTextStyle.bigText(message);

        NotificationCompat.Builder notificationBuilder = new
                NotificationCompat.Builder(this, CHANNEL_ID)
                .setStyle(bigTextStyle)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentTitle(title)
                .setContentText(message)
                .setTicker(message)
                .setSubText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.mipmap.ic_launcher));

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes);
            notificationManager.createNotificationChannel(channel);
        }

//        notificationManager.notify(101, notificationBuilder.build());


        return notificationBuilder.build();


    }


    private void ShowSuccessNotification(String url)
    {

        String title = "File Upload Successfully",
                message = "Click to view uploaded image";


        Intent resultIntent = new Intent(Intent.ACTION_VIEW);
        resultIntent.setData(Uri.parse(url));

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(title);
        bigTextStyle.setSummaryText(message);
        bigTextStyle.bigText(message);

        NotificationCompat.Builder notificationBuilder = new
                NotificationCompat.Builder(this, CHANNEL_ID)
                .setStyle(bigTextStyle)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentTitle(title)
                .setContentText(message)
                .setTicker(message)
                .setSubText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.mipmap.ic_launcher));

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(CameraViewActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(101, notificationBuilder.build());
    }


    public void loadingDailog()
    {
        if (mDialog != null)
        {
            mDialog.cancel();
        }

        final View dialogViewShareIntent = View.inflate(context,
                R.layout.layout_loading, null);
        mDialog = new Dialog(context);
        mDialog.setContentView(dialogViewShareIntent);
        mDialog.setCancelable(false);

        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(0));

//        mDialogShareIntent.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;

        mDialog.show();
    }


    public void submitSuccesfullDailog()
    {
        if (mDialog != null)
        {
            mDialog.cancel();
        }

        final View dialogViewShareIntent = View.inflate(context,
                R.layout.layout_enroll_successfull, null);
        mDialog = new Dialog(context);
        mDialog.setContentView(dialogViewShareIntent);
        mDialog.setCancelable(false);

        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(0));
//        mDialogShareIntent.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        mDialog.findViewById(R.id.cancelBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mDialog.dismiss();
            }
        });


        mDialog.show();
    }

    public void submitFailDailog()
    {
        if (mDialog != null)
        {
            mDialog.cancel();
        }

        final View dialogViewShareIntent = View.inflate(context,
                R.layout.layout_enroll_failed, null);
        mDialog = new Dialog(context);
        mDialog.setContentView(dialogViewShareIntent);
        mDialog.setCancelable(false);

        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(0));
//        mDialogShareIntent.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        mDialog.findViewById(R.id.cancelBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mDialog.dismiss();
                if (imgBitmap != null)
                {
                    loadingDailog();
                    FirebaseApp.initializeApp(context);
                    startUploadingFile();
                }


            }
        });


        mDialog.show();
    }



}