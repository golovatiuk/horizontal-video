package com.gotwingm.my.horizontalvideo;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.netcompss.ffmpeg4android.CommandValidationException;
import com.netcompss.loader.LoadJNI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener {

    private SurfaceView surface;
    private SurfaceHolder surfaceHolder;
    private File video;
    private MediaRecorder videoRecorder;
    private boolean isHorizontal;

    static Camera camera;

    public static final int CAMERA_ID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        videoRecorder = null;

        surface = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surface.getHolder();
        surfaceHolder.addCallback(this);

        File dir = Environment.getExternalStorageDirectory();
        video = new File(dir, "in.mp4");

    }

    @Override
    protected void onResume() {
        super.onResume();

        camera = Camera.open(CAMERA_ID);
        setSurfaceSize();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(camera != null) {

            camera.release();

        }

        camera = null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        camera.stopPreview();
        setCameraOrientation();

        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    void setSurfaceSize() {

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        Camera.Size cameraPreviewSizeForVideo =
                getMaxPreviewSize(camera);

        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(cameraPreviewSizeForVideo.width,
                cameraPreviewSizeForVideo.height);
        camera.setParameters(parameters);

        isHorizontal = displayMetrics.widthPixels > displayMetrics.heightPixels;

        surface.getLayoutParams().height = cameraPreviewSizeForVideo.height;
        surface.getLayoutParams().width = cameraPreviewSizeForVideo.width;


        if (isHorizontal) {

            surface.getLayoutParams().height = cameraPreviewSizeForVideo.height;
            surface.getLayoutParams().width = cameraPreviewSizeForVideo.width;

        } else {

            surface.getLayoutParams().height = cameraPreviewSizeForVideo.width;
            surface.getLayoutParams().width = cameraPreviewSizeForVideo.height;


            RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(displayMetrics.widthPixels,
                            (camera.getParameters().getPreviewSize().width - 480) / 2);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

            findViewById(R.id.topFrame).setLayoutParams(params);

            findViewById(R.id.bottomFrame).setLayoutParams(
                    new RelativeLayout.LayoutParams(displayMetrics.widthPixels,
                            (camera.getParameters().getPreviewSize().width - 480) / 2));

        }
    }

    private void setCameraOrientation() {

        int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
        int rotationDegrees = 0;
        int cameraRotationDegree = 0;

        switch (displayRotation) {
            case Surface.ROTATION_0:
                rotationDegrees = 0;
                break;
            case Surface.ROTATION_90:
                rotationDegrees = 90;
                break;
            case Surface.ROTATION_180:
                rotationDegrees = 180;
                break;
            case Surface.ROTATION_270:
                rotationDegrees = 270;
                break;
        }

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(CAMERA_ID, cameraInfo);

        cameraRotationDegree = 360 - rotationDegrees + cameraInfo.orientation;
        cameraRotationDegree = cameraRotationDegree % 360;

        camera.setDisplayOrientation(cameraRotationDegree);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btnStart:
                lockOrientation();
                startRecorder();
                break;
            case R.id.btnStop:
                stopRecorder();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                break;
        }
    }

    private void startRecorder() {

        camera.unlock();

        videoRecorder = new MediaRecorder();

        videoRecorder.setCamera(camera);
        videoRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        videoRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        videoRecorder.setProfile(CamcorderProfile
                .get(CamcorderProfile.QUALITY_HIGH));
        videoRecorder.setOrientationHint(videoHint());
        videoRecorder.setOutputFile(video.getAbsolutePath());
        videoRecorder.setPreviewDisplay(surface.getHolder().getSurface());

        try {
            videoRecorder.prepare();
            videoRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void lockOrientation() {

        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Surface.ROTATION_90:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Surface.ROTATION_180:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_270:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
        }
    }

    private void stopRecorder() {

        if (videoRecorder != null) {

            videoRecorder.stop();
            videoRecorder.reset();
            videoRecorder.release();
            videoRecorder = null;
            camera.lock();

            if (!isHorizontal) {

                VideoProcessing vp = new VideoProcessing();
                vp.run();
                Toast.makeText(getApplicationContext(),
                        "video saved to " + Environment.getExternalStorageDirectory(),
                        Toast.LENGTH_LONG).show();

            }
        }
    }

    private int videoHint() {

        switch(getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
                return 90;
            case Surface.ROTATION_90:
                return 0;
            case Surface.ROTATION_180:
                return 270;
            case Surface.ROTATION_270:
                return 180;
            default:
                return 0;
        }
    }

    private class VideoProcessing implements Runnable {

        private void rotateVideo() {

            LoadJNI loadJNI = new LoadJNI();

            String[] command = {"ffmpeg", "-i",
                    video.getAbsolutePath(),
                    "-vf", "rotate=PI/2",
                    "sdcard/out.avi"};

            try {
                loadJNI.run(command,
                        getApplicationContext().getFilesDir().getAbsolutePath(),
                        getApplicationContext());
            } catch (CommandValidationException e) {
                e.printStackTrace();
            }

        }

        private void cropVideo() {

            LoadJNI loadJNI = new LoadJNI();

            String[] command = {"ffmpeg", "-i",
                    "sdcard/out.avi",
                    "-vf",
                    "crop=480:320",
                    "sdcard/" + System.currentTimeMillis() + ".avi"};

            try {
                loadJNI.run(command,
                        getApplicationContext().getFilesDir().getAbsolutePath(),
                        getApplicationContext());
            } catch (CommandValidationException e) {
                e.printStackTrace();
            }

        }

        protected void delRaw() {

            if (video.exists()) {

                video.delete();

            }

            File out = new File(Environment.getExternalStorageDirectory(), "out.avi");

            if (out.exists()) {

                out.delete();

            }
        }

        @Override
        public void run() {

            rotateVideo();
            cropVideo();
            delRaw();
        }
    }

    private Camera.Size getMaxPreviewSize(Camera camera) {

        Camera.Size maxSize = camera.getParameters().getPreviewSize();

        Log.d("## -- ", "before preview size " + maxSize.width + "X" + maxSize.height);

        ArrayList<Camera.Size> sizes = (ArrayList<Camera.Size>)
                camera.getParameters().getSupportedPreviewSizes();

        for (Camera.Size size : sizes) {

            Log.d("## -- ", "preview size " + size.width + "X" + size.height);

            if(size.height > maxSize.height || size.width > maxSize.width) {

                maxSize = size;

            }
        }

        Log.d("## -- ", "max preview size " + maxSize.width + "X" + maxSize.height);

        return maxSize;

    }

}
