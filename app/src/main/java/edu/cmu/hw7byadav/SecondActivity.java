package edu.cmu.hw7byadav;

import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SecondActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    Camera camera; // camera class variable
    SurfaceView camView; // drawing camera preview using this variable
    SurfaceHolder surfaceHolder; // variable to hold surface for surfaceView which means display
    boolean camCondition = false;  // conditional variable for camera preview checking and set to false
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        getWindow().setFormat(PixelFormat.UNKNOWN);
        camView = (SurfaceView)findViewById(R.id.scan_camera);
        surfaceHolder = camView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }

    @SuppressWarnings("deprecation")
    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera c) {

            FileOutputStream outStream = null;
            try {

                // Directory and name of the photo. We put system time
                // as a postfix, so all photos will have a unique file name.
                outStream = new FileOutputStream("/sdcard/AndroidCodec_" +
                        System.currentTimeMillis()+".jpg");
                outStream.write(data);
                outStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }

        }
    };

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        camera = Camera.open();   // opening camera
        camera.setDisplayOrientation(90);   // setting camera preview orientation
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        // stop the camera
        if(camCondition){
            camera.stopPreview(); // stop preview using stopPreview() method
            camCondition = false; // setting camera condition to false means stop
        }
        // condition to check whether your device have camera or not
        if (camera != null){
            try {
                Camera.Parameters parameters = camera.getParameters();
                parameters.setColorEffect(Camera.Parameters.EFFECT_SEPIA); //applying effect on camera
                camera.setParameters(parameters); // setting camera parameters
                camera.setPreviewDisplay(surfaceHolder); // setting preview of camera
                camera.startPreview();  // starting camera preview

                camCondition = true; // setting camera to true which means having camera
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        camera.stopPreview();  // stopping camera preview
        camera.release();       // releasing camera
        camera = null;          // setting camera to null when left
        camCondition = false;   // setting camera condition to false also when exit from application
    }
}
