package com.hanny.cameraview;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public final static int SIZE_1 = 640;
    public final static int SIZE_2 = 480;
    private static final int REQUEST_PRERECORD = 0x101;
    private static final int REQUEST_RECORD_VIDEO = 0x102;
    private static final int REQUEST_PREVIEW_PHOTO = 0x103;
    private SurfaceView cameraShowView;
    private RingView recordView;
    boolean flagRecord = false;//是否正在录像
    int cameraType = 0;
    //相机
    Camera camera;
    int frontRotate;
    int frontOri;
    private SurfaceHolder surfaceHolder;
    private MediaRecorder mediaRecorder;
    int rotationRecord = 90;
    private File videoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initClick();
        initData();
    }

    private void initView() {
        //录制界面
        cameraShowView = (SurfaceView) findViewById(R.id.camera_show_view);
        //相机切换
        //长按录制按钮
        recordView = (RingView) findViewById(R.id.recordView);

        //设置分辨率
        doStartSize();


    }

    private void initClick() {
        recordView.setOnRecordListener(new RingView.OnRecordListener() {
            @Override
            public void startRecord() {
                if (!flagRecord) {
                    if (prepareRecord()) {
                    } else {
                        endRecord();
                    }
                }
            }

            @Override
            public void stopRecord() {
                endRecord();
            }

            @Override
            public void takePhoto() {
                captrue();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        //检查权限
        checkPerm();
        if (camera == null) {
            camera = getCamera(cameraType);
            initCamera();
        } else {
            releaseCamera();
            camera = Camera.open(cameraType);
            initCamera();
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceHolder = holder;
        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surfaceHolder = holder;
        initCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        endRecord();
        releaseCamera();
    }



    /**
     * 获取Camera实例
     *
     * @return
     */
    private Camera getCamera(int id) {
        Camera camera = null;
        try {
            camera = Camera.open(id);
        } catch (Exception e) {

        }
        return camera;
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (flagRecord) {
                endRecord();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (flagRecord) {
            //如果是录制中的就完成录制
            onPause();
            return;
        }
        super.onBackPressed();
    }


    /**
     * 结束录制
     */
    private void endRecord() {
        //反正多次进入，比如surface的destroy和界面onPause
        if (!flagRecord) {
            return;
        }
        flagRecord = false;
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(this, VideoPlayActivity.class);
        intent.putExtra(VideoPlayActivity.DATA, videoFile.getAbsolutePath());
        startActivityForResult(intent, REQUEST_RECORD_VIDEO);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * 录制准备工作
     *
     * @return
     */
    private boolean prepareRecord() {
        //初始化相机
        boolean isInitCamera = initRecord();
        if (!isInitCamera) {
            return false;
        }
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }
        try {
            mediaRecorder.setCamera(camera);
            // 这两项需要放在setOutputFormat之前
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            // Set output file format，输出格式
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            //必须在setEncoder之前
            mediaRecorder.setVideoFrameRate(15);  //帧数  一分钟帧，15帧就够了
            mediaRecorder.setVideoSize(SIZE_1, SIZE_2);//这个大小就够了

            // 这两项需要放在setOutputFormat之后
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            mediaRecorder.setVideoEncodingBitRate(3 * SIZE_1 * SIZE_2);//第一个数字越大，清晰度就越高，考虑文件大小的缘故，就调整为1
            int frontRotation;
            if (rotationRecord == 180) {
                //反向的前置
                frontRotation = 180;
            } else {
                //正向的前置
                frontRotation = (rotationRecord == 0) ? 270 - frontOri : frontOri; //录制下来的视屏选择角度，此处为前置
            }
            mediaRecorder.setOrientationHint((cameraType == 1) ? frontRotation : rotationRecord);
            //把摄像头的画面给它
            mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
            //创建好视频文件用来保存
            videoFile = Util.videoDir();
            if (videoFile != null) {
                //设置创建好的输入路径
                mediaRecorder.setOutputFile(videoFile.getPath());
                mediaRecorder.prepare();
                mediaRecorder.start();
                //不能旋转
                flagRecord = true;
            }
        } catch (Exception e) {
            //一般没有录制权限或者录制参数出现问题都走这里
            e.printStackTrace();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            FileUtils.deleteFile(videoFile.getPath());
            return false;
        }

        return true;
    }

    /**
     * 录制初始化
     *
     * @return
     */
    private boolean initRecord() {
        if (camera != null) {
            //如果已经初始化过，就先释放
            releaseCamera();
        }

        try {
            camera = Camera.open(cameraType);
            if (camera == null) {
                return false;
            }
            camera.lock();

            //Point screen = new Point(getScreenWidth(this), getScreenHeight(this));
            //现在不用获取最高的显示效果
            //Point show = getBestCameraShow(camera.getParameters(), screen);

            Camera.Parameters parameters = camera.getParameters();
            if (cameraType == 0) {
                //基本是都支持这个比例
                parameters.setPreviewSize(SIZE_1, SIZE_2);
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
                camera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
            }
            camera.setParameters(parameters);
            if (cameraType == 1) {
                frontCameraRotate();
                camera.setDisplayOrientation(frontRotate);
            } else {
                camera.setDisplayOrientation(90);
            }
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
            camera.unlock();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            releaseCamera();
        }
        return false;
    }

    private boolean initCamera() {
        try {
            setupCamera(camera);
            camera.setPreviewDisplay(surfaceHolder);
            Util.setCameraDisplayOrientation(this, cameraType, camera);
            camera.startPreview();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setupCamera(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();

        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        //这里第三个参数为最小尺寸 getPropPreviewSize方法会对从最小尺寸开始升序排列 取出所有支持尺寸的最小尺寸
        Camera.Size previewSize = Util.getPropSizeForHeight(parameters.getSupportedPreviewSizes(), 800);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
//        parameters.setPreviewSize(SIZE_1, SIZE_2);

        Camera.Size pictrueSize = Util.getPropSizeForHeight(parameters.getSupportedPictureSizes(), 800);
        parameters.setPictureSize(pictrueSize.width,pictrueSize.height );
//        parameters.setPictureSize(SIZE_1, SIZE_2);

        camera.setParameters(parameters);

    }

    /**
     * 旋转前置摄像头为正的
     */
    private void frontCameraRotate() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(1, info);
        int degrees = getDisplayRotation(this);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        frontOri = info.orientation;
        frontRotate = result;
    }


    /**
     * 释放摄像头资源
     */
    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.lock();
                camera.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initData() {
        surfaceHolder = cameraShowView.getHolder();
        surfaceHolder.addCallback(this);
        // setType必须设置，要不出错.
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * 因为录制改分辨率的比例可能和屏幕比例一直，所以需要调整比例显示
     */
    private void doStartSize() {
        int screenWidth = Util.getScreenWidth(this);
        int screenHeight = Util.getScreenHeight(this);
        Util.setViewSize(cameraShowView, screenWidth * SIZE_1 / SIZE_2, screenHeight);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (null == camera) {
                return;
            }

            //参数说明
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    //将data 转换为位图 或者你也可以直接保存为文件使用 FileOutputStream
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    Bitmap bitmapSave = Util.setTakePicktrueOrientation(cameraType, bitmap);
                    //存入本地获取地址
                    File file = Util.saveImageToGallery(MainActivity.this, bitmapSave);
                    if (null != file) {
                        Intent intent = new Intent(MainActivity.this, ImagePreview.class);
                        intent.putExtra(ImagePreview.DATA, file.getAbsolutePath());
                        startActivityForResult(intent, REQUEST_PREVIEW_PHOTO);
                    }
                }
            });
        }
    };

    /**
     * 拍照
     */
    private void captrue() {
        handler.sendEmptyMessage(0);
    }

    /**
     * 权限允许去录像(相机，写SD卡，录音)
     */
    private void checkPerm() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_PRERECORD);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PRERECORD) {
            if (grantResults.length == 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(MainActivity.this, getString(R.string.premisson_refuse), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * 获取旋转角度
     */
    private int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PREVIEW_PHOTO) {
            recordView.setTakePhoto(false);
        } else if (requestCode == REQUEST_RECORD_VIDEO) {
        }
    }
}
