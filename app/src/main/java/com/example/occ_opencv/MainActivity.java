package com.example.occ_opencv;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
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
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private Button btnCapture;
    private TextureView textureView;
    //Check state orientation of output image
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }
    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    //Save to FILE
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 123;
    private int ISO_VALUE = 1000;
    private long SHUTTER_SPEED = 1000000000L / 1500;
    private Range<Integer> isoRange;
    private Range<Long> speedRange;

    //VARIABLES DE SEEKBAR
    private SeekBar shutterSpeedSeekBar, isoSeekBar;
    private TextView shutterSpeedLabel, isoLabel;
    private int currentShutterSpeed, currentIso;
    //VARIABLES DE SEEKBAR
    private ImageProcessor imageProcessor;
    private Handler uiHandler;

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            //cameraDevice=null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = (TextureView)findViewById(R.id.textureView);

        //From Java 1.4 , you can use keyword 'assert' to check expression true or false
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        btnCapture = (Button)findViewById(R.id.btnCapture);

        //BARRA INICIALIZACION
        shutterSpeedSeekBar = findViewById(R.id.shutterSpeedSeekBar);
        isoSeekBar = findViewById(R.id.isoSeekBar);
        shutterSpeedLabel = findViewById(R.id.shutterSpeedLabel);
        isoLabel = findViewById(R.id.isoLabel);
        //BARRA INICIALIZACION


        //SET VALUES
        shutterSpeedSeekBar.setMin(100000);
        shutterSpeedSeekBar.setMax(1333333);
        isoSeekBar.setMin(100);
        isoSeekBar.setMax(1500);

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });
        shutterSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Calcula el valor de shutter speed y actualiza el texto correspondiente


                //SHUTTER_SPEED = (long) (1000000000L / Math.pow(2, progress / 10.0));
                SHUTTER_SPEED=progress;
                shutterSpeedLabel.setText("Shutter Speed: " + SHUTTER_SPEED);
                updatePreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No es necesario implementar en este caso
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No es necesario implementar en este caso
            }
        });


        isoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Actualiza el valor de ISO y el texto correspondiente
                ISO_VALUE = progress;
                isoLabel.setText("ISO: " + ISO_VALUE);
                updatePreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No es necesario implementar en este caso
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No es necesario implementar en este caso
            }
        });

        //MENSAJE DECODIFICADO
        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                // Muestra un Toast con el mensaje en medio de la pantalla
                String mensaje = (String) msg.obj;
                Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
            }
        };

        // Inicializa ImageProcessor
        imageProcessor = new ImageProcessor();





    }

    //private static final int UMBRAL_ROJO = 60;
    //private static final int UMBRAL_AZUL = 180;
    private void takePicture(){
        if(cameraDevice == null)
            return;
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if(characteristics != null)
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);

            //Capture image with custom size
            int width = 640;
            int height = 480;
            if(jpegSizes != null && jpegSizes.length > 0)
            {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            imageReader = ImageReader.newInstance(width,height,ImageFormat.JPEG,1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(imageReader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));

            //calibrationValues(characteristics);


            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            //captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // configure CAMERA CALIBRATION
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, ISO_VALUE);
            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, SHUTTER_SPEED);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);

            //CONFIGURE RESOLUTION


            //Check orientation base on device
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));
            File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            file = new File(picturesDirectory, UUID.randomUUID().toString() + ".jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    try{

                        image = imageReader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);

                        // Continúa con el resto de tu lógica, por ejemplo, guardar la imagen procesada


                        if (OpenCVLoader.initDebug()) {
                            Log.d("LOADTAG", "OpenCV initialized on Image");

                            // Crear una instancia de ImageProcessor si aún no existe
                            if (imageProcessor == null) {
                                imageProcessor = new ImageProcessor();
                            }

                            // Llamar al método processImage de ImageProcessor y pasar la imagen
                            imageProcessor.processImage(bytes, uiHandler);


                            //save(bytes);

                        } else {
                            Log.e("LOADTAG", "OpenCV initialization failed");
                        }


                        // Guardar las imágenes procesadas
                        //save(ImageAnalysis.binarizar(canalRojo, UMBRAL_ROJO), "rojo");
                        //save(ImageAnalysis.binarizar(canalAzul, UMBRAL_AZUL), "azul");

                        Log.d("TAG","Esto es proceso de imagen");



                        //save(bytes);
                    }
                    /*catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    finally {
                        {
                            if(image != null)
                                image.close();
                        }
                    }*/
                    catch (Exception e) {
                        // Capturar y manejar excepciones generales
                        Log.e("ERROR", "Error al procesar la imagen: " + e.getMessage());
                    } finally {
                        // Asegurarse de liberar la imagen en el bloque finally
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream outputStream = null;
                    try{
                        outputStream = new FileOutputStream(file);
                        outputStream.write(bytes);
                    }finally {
                        if(outputStream != null)
                            outputStream.close();
                    }
                }

                private void saveImage(Bitmap bitmap, String nombre) {
                    File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    File file = new File(picturesDirectory, UUID.randomUUID().toString() + "_" + nombre + ".jpg");
                    OutputStream outputStream = null;
                    try {
                        outputStream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (outputStream != null) {
                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            };


            MediaScannerConnection.scanFile(
                    this,
                    new String[]{file.getAbsolutePath()},
                    null,
                    (path, uri) -> {
                        Log.i("TAG", "Scanned file " + path);
                        // Puedes realizar acciones adicionales después del escaneo, si es necesario
                    }
            );

            imageReader.setOnImageAvailableListener(readerListener,mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    //Toast.makeText(MainActivity.this, "Saved "+file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        cameraCaptureSession.capture(captureBuilder.build(),captureListener,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            },mBackgroundHandler);


        }catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }
    private void calibrationValues(CameraCharacteristics characteristics){
        isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        speedRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        Log.d("ATAG CAL:", "Rango ISO=" + isoRange);
        Log.d("ATAG CAL:", "Rango SPEED=" + speedRange);
    }

    private void createCameraPreview(){
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert  texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(),imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Changed", Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void updatePreview() {
        if (cameraDevice == null)
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();

        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
        captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, ISO_VALUE);
        captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, SHUTTER_SPEED);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);

        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(){
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //Check realtime permission if run higher API 23
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },REQUEST_CAMERA_PERMISSION);
                return;

            }
            // Configuración de la captura con nuevos valores de ISO y velocidad de obturación


            manager.openCamera(cameraId,stateCallback,null);
            calibrationValues(characteristics);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }









    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            openCamera();
            //configureTransform(width, height); // Llama a configureTransform aquí

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE_PERMISSION) {
            if(requestCode == REQUEST_CAMERA_PERMISSION)
            {
                if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(this, "You can't use camera without permission", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if(textureView.isAvailable()) {
            openCamera();

        }else{
            textureView.setSurfaceTextureListener(textureListener);}
    }
    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try{
            mBackgroundThread.join();
            mBackgroundThread= null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ... (código existente)

    private Matrix matrix = new Matrix(); // Declaración de la matriz como variable de instancia

    // Método configureTransform
    private void configureTransform(int viewWidth, int viewHeight) {
        if (imageDimension == null || textureView == null) {
            return;
        }

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        float textureViewWidth = viewWidth;
        float textureViewHeight = viewHeight;

        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            textureViewWidth = viewHeight;
            textureViewHeight = viewWidth;
        }

        RectF viewRect = new RectF(0, 0, textureViewWidth, textureViewHeight);
        RectF bufferRect = new RectF(0, 0, imageDimension.getHeight(), imageDimension.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

        float scale = Math.max(
                (float) textureViewHeight / imageDimension.getHeight(),
                (float) textureViewWidth / imageDimension.getWidth()
        );

        matrix.postScale(scale, scale, centerX, centerY);

        switch (rotation) {
            case Surface.ROTATION_0:
                matrix.postRotate(0, centerX, centerY);
                break;
            case Surface.ROTATION_90:
                matrix.postRotate(90, centerX, centerY);
                break;
            case Surface.ROTATION_180:
                matrix.postRotate(180, centerX, centerY);
                break;
            case Surface.ROTATION_270:
                matrix.postRotate(270, centerX, centerY);
                break;
        }

        textureView.setTransform(matrix);
    }







}