
package com.beyarkay.opencvdeus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private static final String TAG = "TAGGY_MC_TAGFACE";
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    TextureView textureView;
    ImageView ivBitmap;
    LinearLayout llBottom;

    int currentImageType = Imgproc.COLOR_RGB2GRAY;

    ImageCapture imageCapture;
    ImageAnalysis imageAnalysis;
    Preview preview;
    RadioButton rbSelected;

    Button btnCapture, btnOk, btnCancel;
    int minHue = 0;
    int maxHue = 255;
    int minSat = 0;
    int maxSat = 255;
    int minVal = 0;
    int maxVal = 255;

    TextView tvHueMax;
    TextView tvHueMin;
    TextView tvSatMax;
    TextView tvSatMin;
    TextView tvValMax;
    TextView tvValMin;

    LinearLayout llHue;
    LinearLayout llSat;

    static {
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnCapture = findViewById(R.id.btnCapture);
        btnOk = findViewById(R.id.btnAccept);
        btnCancel = findViewById(R.id.btnReject);

        btnOk.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        llBottom = findViewById(R.id.llBottom);
        textureView = findViewById(R.id.textureView);
        ivBitmap = findViewById(R.id.ivBitmap);

        tvHueMin = findViewById(R.id.tvHueMin);
        tvHueMax = findViewById(R.id.tvHueMax);
        tvSatMin = findViewById(R.id.tvSatMin);
        tvSatMax = findViewById(R.id.tvSatMax);
        tvValMin = findViewById(R.id.tvValMin);
        tvValMax = findViewById(R.id.tvValMax);

        llHue = findViewById(R.id.llHue);
        llSat = findViewById(R.id.llSat);

        SeekBar.OnSeekBarChangeListener sbListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSeekBars(progress, seekBar.getId());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };


        SeekBar sbHueMin = findViewById(R.id.sbHueMin);
        sbHueMin.setOnSeekBarChangeListener(sbListener);

        SeekBar sbHueMax = findViewById(R.id.sbHueMax);
        sbHueMax.setOnSeekBarChangeListener(sbListener);

        SeekBar sbSatMin = findViewById(R.id.sbSatMin);
        sbSatMin.setOnSeekBarChangeListener(sbListener);

        SeekBar sbSatMax = findViewById(R.id.sbSatMax);
        sbSatMax.setOnSeekBarChangeListener(sbListener);

        SeekBar sbValMin = findViewById(R.id.sbValMin);
        sbValMin.setOnSeekBarChangeListener(sbListener);

        SeekBar sbValMax = findViewById(R.id.sbValMax);
        sbValMax.setOnSeekBarChangeListener(sbListener);

        sbHueMin.setProgress(150);
        sbHueMax.setProgress(2);
        sbSatMin.setProgress(190);
        sbSatMax.setProgress(80);


        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void updateSeekBars(int progress, int id) {

        switch (id) {
            case R.id.sbHueMin:
                minHue = progress;
                tvHueMin.setText(String.format("%d", minHue));

                break;
            case R.id.sbHueMax:
                maxHue = progress;
                tvHueMax.setText(String.format("%d", maxHue));
                break;

            case R.id.sbSatMin:
                minSat = progress;
                tvSatMin.setText(String.format("%d", minSat));
                break;
            case R.id.sbSatMax:
                maxSat = progress;
                tvSatMax.setText(String.format("%d", maxSat));
                break;

            case R.id.sbValMin:
                minVal = progress;
                tvValMin.setText(String.format("%d", minVal));
                break;
            case R.id.sbValMax:
                maxVal = progress;
                tvValMax.setText(String.format("%d", maxVal));
                break;
        }
        //Min Value
//        llHue.setBackgroundColor(Color.HSVToColor([(float)]));

        //Max value
//        llSat.setBackgroundColor();


    }

    private void startCamera() {

        CameraX.unbindAll();
        preview = setPreview();
        imageCapture = setImageCapture();
        imageAnalysis = setImageAnalysis();

        //bind to lifecycle:
        CameraX.bindToLifecycle(this, preview, imageCapture, imageAnalysis);
    }

    private Preview setPreview() {

        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen


        PreviewConfig pConfig = new PreviewConfig.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetResolution(screen)
                .setLensFacing(CameraX.LensFacing.BACK)
                .build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                });

        return preview;
    }

    private ImageCapture setImageCapture() {
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCapture = new ImageCapture(imageCaptureConfig);


        btnCapture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {


                imgCapture.takePicture(new ImageCapture.OnImageCapturedListener() {
                    @Override
                    public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                        Bitmap bitmap = textureView.getBitmap();
                        showAcceptedRejectedButton(true);
                        ivBitmap.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onError(ImageCapture.UseCaseError useCaseError, String message, @Nullable Throwable cause) {
                        super.onError(useCaseError, message, cause);
                    }
                });


                /*File file = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "" + System.currentTimeMillis() + "_JDCameraX.jpg");
                imgCapture.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        Bitmap bitmap = textureView.getBitmap();
                        showAcceptedRejectedButton(true);
                        ivBitmap.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {

                    }
                });*/
            }
        });

        return imgCapture;
    }

    @SuppressLint("DefaultLocale")
    private ImageAnalysis setImageAnalysis() {


        // Setup image analysis pipeline that computes average pixel luminance
        HandlerThread analyzerThread = new HandlerThread("OpenCVAnalysis");
        analyzerThread.start();


        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_NEXT_IMAGE)
                .setCallbackHandler(new Handler(analyzerThread.getLooper()))
                .setImageQueueDepth(1).build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);


        imageAnalysis.setAnalyzer(
                new ImageAnalysis.Analyzer() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void analyze(ImageProxy image, int rotationDegrees) {
                        //Analyzing live camera feed begins.


                        final Bitmap bitmap = textureView.getBitmap();

                        if (bitmap == null)
                            return;

                        Mat original = new Mat();
                        Utils.bitmapToMat(bitmap, original);
                        QRDetector detector = new QRDetector(original);

                        /*
                        Good values for constants seem to be:
                        thresh = 150
                        depth = 2
                        canny1 = 190
                        canny2 = 80
                         */


                        double canny_thresh2 = minSat;
                        double canny_thresh1 = maxSat;
                        double thresh_max = 255;
                        int depth = maxHue;
                        double thresh = minHue;


                        Mat grey = new Mat();
                        Imgproc.cvtColor(original, grey, Imgproc.COLOR_RGB2GRAY);

                        Mat blur = new Mat();
                        Imgproc.blur(grey, blur, new org.opencv.core.Size(5, 5));

                        Mat threshold = new Mat();
                        Imgproc.threshold(blur, threshold, thresh, thresh_max, Imgproc.THRESH_BINARY);

                        Mat canny = new Mat();
                        Imgproc.Canny(threshold, canny, canny_thresh1 / 255, canny_thresh2 / 255);

                        Mat hierarchy = new Mat();
                        List<MatOfPoint> contours = new ArrayList<>();
                        Imgproc.findContours(canny, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);



                        List<MatOfPoint> deepContours = new ArrayList<>();

                        for (int i = 0; i < contours.size(); i++) {
                            int current_child = i;
                            int current_depth = 0;

                            while(hierarchy.get(0, current_child)[3] != -1){
                                current_child = (int) hierarchy.get(0, current_child)[3];
                                current_depth ++;
                            }

                            if (current_depth >= depth) {
                                deepContours.add(contours.get(i));
                            }
                        }

                        Mat drawnContours = Mat.zeros(original.size(), CvType.CV_8UC3);
                        for (int i = 0; i < deepContours.size(); i++) {
                            Imgproc.drawContours(drawnContours, deepContours, i, new Scalar(255, 200, 0));
                        }


                        Mat[] matrixes = new Mat[]{
                                blur,
                                threshold,
                                canny,
                                drawnContours
                        };

                        final Bitmap result = combineMatrixesToBitmap(matrixes);

                        // Next, update the bitmap to be the processed image
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ivBitmap.setImageBitmap(result);
                            }
                        });

                    }
                });


        return imageAnalysis;

    }

    private static Bitmap combineMatrixesToBitmap(Mat[] matrices) {
        Bitmap[] bitmaps = new Bitmap[4];

        for (int i = 0; i < matrices.length; i++) {
            bitmaps[i] = Bitmap.createBitmap(matrices[i].width(), matrices[i].height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(matrices[i], bitmaps[i]);
        }

        Bitmap result;
        try {
            if (bitmaps[0] == null) {
                return null;
            }
            int bgWidth = bitmaps[0].getWidth();
            int bgHeight = bitmaps[0].getHeight();

            for (int i = 0; i < bitmaps.length; i++) {

            }

            result = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(result);
            cv.drawBitmap(bitmaps[0], 0, 0, null);
            cv.drawBitmap(bitmaps[1], (bgWidth) / 2, 0, null);
            cv.drawBitmap(bitmaps[2], 0, (bgHeight / 2), null);
            cv.drawBitmap(bitmaps[3], (bgWidth) / 2,  (bgHeight / 2), null);
            cv.save();
            cv.restore();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private void showAcceptedRejectedButton(boolean acceptedRejected) {
        if (acceptedRejected) {
            CameraX.unbind(preview, imageAnalysis);
            llBottom.setVisibility(View.VISIBLE);
            btnCapture.setVisibility(View.INVISIBLE);
            textureView.setVisibility(View.GONE);
        } else {
            btnCapture.setVisibility(View.VISIBLE);
            llBottom.setVisibility(View.GONE);
            textureView.setVisibility(View.VISIBLE);
            textureView.post(new Runnable() {
                @Override
                public void run() {
                    startCamera();
                }
            });
        }
    }

    private void updateTransform() {


        Log.d(TAG, "HELLO OWRLD!");
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int) textureView.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float) rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.botspot:
                currentImageType = Imgproc.COLOR_RGB2HLS;
                startCamera();
                return true;

            case R.id.black_white:
                currentImageType = Imgproc.COLOR_RGB2GRAY;
                startCamera();
                return true;

            case R.id.hsv:
                currentImageType = Imgproc.COLOR_RGB2HSV;
                startCamera();
                return true;

            case R.id.lab:
                currentImageType = Imgproc.COLOR_RGB2Lab;
                startCamera();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnReject:
                showAcceptedRejectedButton(false);
                break;

            case R.id.btnAccept:
                File file = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "" + System.currentTimeMillis() + "_JDCameraX.jpg");
                imageCapture.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        showAcceptedRejectedButton(false);

                        Toast.makeText(getApplicationContext(), "Image saved successfully in Pictures Folder", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {

                    }
                });
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

}

