package com.example.camerainservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraService extends Service implements Runnable {
    private static final long TAKE_PICTURE_DELAY = 1000 * 10; // millisec
    private Camera camera;
    private MainSurface mview;
    private WindowManager wm;
    private WindowManager.LayoutParams params;

    public CameraService() {
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.i("SERVICE", "onStart()");
        startPreview();
    }

    private float touchStartX;
    private float touchStartY;
    private float x;
    private float y;

    void startPreview() {
        camera = Camera.open(0);
        setCameraDisplayOrientation(0, camera);
        mview = new MainSurface(this);
        wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth() / 4;
        int height = wm.getDefaultDisplay().getHeight() / 4;
        params = new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        params.format=1;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE; // 不能抢占聚焦点
        params.flags = params.flags | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        params.flags = params.flags | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS; // 排版不受限制

        params.alpha = 1.0f;

        params.gravity= Gravity.LEFT|Gravity.TOP;   //调整悬浮窗口至左上角
        //以屏幕左上角为原点，设置x、y初始值
        params.x=0;
        params.y=0;
        mview.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    camera.setPreviewDisplay(holder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                camera.startPreview();
                mview.postDelayed(CameraService.this, TAKE_PICTURE_DELAY);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        wm.addView(mview, params);
        mview.setZOrderOnTop(true);

    }

    public class MainSurface extends SurfaceView {
        public MainSurface(Context context) {
            super(context);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            x = event.getRawX();
            y = event.getRawY() - 25;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartX = event.getX();
                    touchStartY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    params.x = (int) (x - touchStartX);
                    params.y = (int) (y - touchStartY);
                    wm.updateViewLayout(mview, params);
                    break;
                case MotionEvent.ACTION_UP:
                    params.x = (int) (x - touchStartX);
                    params.y = (int) (y - touchStartY);
                    wm.updateViewLayout(mview, params);
                    touchStartX = touchStartY = 0;
                    break;
            }

            return true;
        }
    }

    private void stopPreview() {
        camera.stopPreview();
        camera.release();
        wm.removeView(mview);
    }

    private void setCameraDisplayOrientation(int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        WindowManager windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degree) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degree + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    public void run() {
        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.i("SERVICE", "JPEG received " + data.length);
                File file = new File(Environment.getExternalStorageDirectory(), "CameraService");
                try {
                    file.mkdir();
                } catch (Exception ex) {
                    Log.e("SERVICE", "mkdir failed", ex);
                }
                file = new File(file, "qqq.jpg");
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(data);
                    fileOutputStream.close();
                    Log.i("SERVICE", "write complete");
                } catch (Exception ex) {
                    Log.e("SERVICE", "write failed");
                }

                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                file = new File(Environment.getExternalStorageDirectory(), "CameraService");
                file = new File(file, "qqqq.jpg");
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.close();
                    Log.i("SERVICE", "write from bitmap complete");
                } catch (Exception ex) {
                    Log.e("SERVICE", "write from bitmap failed", ex);
                }


                stopPreview();
            }
        });
    }

    @Override
    public void onCreate() {
        Log.i("SERVICE", "onCreate()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
