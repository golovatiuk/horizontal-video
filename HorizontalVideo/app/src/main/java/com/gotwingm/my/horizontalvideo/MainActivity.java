package com.gotwingm.my.horizontalvideo;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.RectF;
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
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;


public class MainActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener {

    private SurfaceView surface;
    private SurfaceHolder surfaceHolder;
    private File video;
    private MediaRecorder videoRecorder;


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
        video = new File(dir, "video.3gp");

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

        Log.d("### -- display", displayMetrics.widthPixels + "X" + displayMetrics.heightPixels);

        Camera.Size cameraPreviewSizeForVideo =
                camera.getParameters().getPreferredPreviewSizeForVideo();

        Log.d("## -- camera preview", "" + cameraPreviewSizeForVideo.height + "X" + cameraPreviewSizeForVideo.width);

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        Matrix matrix = new Matrix();

        rectDisplay.set(0, 0,
                displayMetrics.widthPixels,
                displayMetrics.heightPixels);

        rectPreview.set(0, 0,
                cameraPreviewSizeForVideo.width,
                cameraPreviewSizeForVideo.height);

        matrix.setRectToRect(rectPreview, rectDisplay, Matrix.ScaleToFit.START);

        matrix.mapRect(rectPreview);

        surface.getLayoutParams().height = (int) rectPreview.bottom;
        surface.getLayoutParams().width = (int) rectPreview.right;

        Log.d("## -- surface ", "" + (int) rectPreview.bottom + (int) rectPreview.right);
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
                startRecorder();
                break;
            case R.id.btnStop:
                stopRecorder();
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

    private void stopRecorder() {

        if (videoRecorder != null) {

            videoRecorder.stop();
            videoRecorder.reset();
            videoRecorder.release();
            videoRecorder = null;
            camera.lock();

        }

    }
}
