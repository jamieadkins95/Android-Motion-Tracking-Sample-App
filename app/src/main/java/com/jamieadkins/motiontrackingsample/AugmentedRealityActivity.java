/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jamieadkins.motiontrackingsample;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.surface.RajawaliSurfaceView;

/**
 * This is a simple example that shows how to use the Android Sensor APIs to create an augmented
 * reality (AR)application. It displays the Planet Earth floating in space one meter in front of the
 * device, and the Moon rotating around it.
 * <p/>
 * This example uses Rajawali for the OpenGL rendering. This includes the color camera image in the
 * background and a 3D sphere with a texture of the Earth floating in space three meter forward.
 * This part is implemented in the {@code AugmentedRealityRenderer} class, like a regular Rajawali
 * application.
 * <p/>
 * This example also uses the Camera2 API to obtain the color camera image for the AR effect.
 */
public class AugmentedRealityActivity extends Activity
        implements PoseProvider.PoseProviderListener {
    private static final String TAG = AugmentedRealityActivity.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;
    private static final int COLOR_CAMERA_ID = 0;
    private static final int PERMISSIONS_REQUEST_CODE = 1112;

    private RajawaliSurfaceView mSurfaceView;
    private AugmentedRealityRenderer mRenderer;

    private PoseProvider mPoseProvider;

    private boolean mCameraPermissionGranted = false;

    private int mConnectedTextureId = INVALID_TEXTURE_ID;

    private int mColorCameraToDisplayAndroidRotation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = (RajawaliSurfaceView) findViewById(R.id.surfaceview);
        mRenderer = new AugmentedRealityRenderer(this);

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {}

                @Override
                public void onDisplayChanged(int displayId) {
                    synchronized (this) {
                        setAndroidOrientation();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {}
            }, null);
        }

        mCameraPermissionGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        if (!mCameraPermissionGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CODE);
        }

        setupRenderer();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPoseProvider = new SamplePoseProvider(this, this);
        mPoseProvider.setup();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                mCameraPermissionGranted = grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();

        setAndroidOrientation();

        // Set render mode to RENDERMODE_CONTINUOUSLY to force getting onDraw callbacks until the
        // Tango service is properly set-up and we start getting onFrameAvailable callbacks.
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
        // Synchronize against disconnecting while the service is being used in the OpenGL thread or
        // in the UI thread.
        // NOTE: DO NOT lock against this same object in the Tango callback thread. Tango.disconnect
        // will block here until all Tango callback calls are finished. If you lock against this
        // object in a Tango callback thread it will cause a deadlock.
        synchronized (this) {
            if (mCameraPermissionGranted) {
                mRenderer.disconnectCamera();
            }
        }

        mPoseProvider.onStopPoseProviding();
    }

    /**
     * Connects the view and renderer to the color camara and callbacks.
     */
    private void setupRenderer() {
        // Register a Rajawali Scene Frame Callback to update the scene camera pose whenever a new
        // RGB frame is rendered.
        // (@see https://github.com/Rajawali/Rajawali/wiki/Scene-Frame-Callbacks)
        mRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                // NOTE: This is called from the OpenGL render thread, after all the renderer
                // onRender callbacks had a chance to run and before scene objects are rendered
                // into the scene.

                // Prevent concurrent access to {@code mIsFrameAvailableTangoThread} from the Tango
                // callback thread and service disconnection from an onPause event.
                try {
                    synchronized (AugmentedRealityActivity.this) {
                        // Set-up scene camera projection to match RGB camera intrinsics.
                        if (!mRenderer.isSceneCameraConfigured()) {
                            mRenderer.setProjectionMatrix(
                                    projectionMatrixFromCameraIntrinsics(
                                            mPoseProvider.getIntrinsics(),
                                            mColorCameraToDisplayAndroidRotation));
                        }

                        if (mCameraPermissionGranted) {
                            // Connect the camera texture to the OpenGL Texture if necessary
                            // NOTE: When the OpenGL context is recycled, Rajawali may re-generate the
                            // texture with a different ID.
                            if (mConnectedTextureId != mRenderer.getTextureId()) {
                                mRenderer.connectCamera();
                                mConnectedTextureId = mRenderer.getTextureId();
                                Log.d(TAG, "connected to texture id: " + mRenderer.getTextureId());
                            }

                            mRenderer.updateTexture();
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Exception on the OpenGL thread", t);
                }
            }

            @Override
            public void onPreDraw(long sceneTime, double deltaTime) {

            }

            @Override
            public void onPostFrame(long sceneTime, double deltaTime) {

            }

            @Override
            public boolean callPreFrame() {
                return true;
            }
        });

        mSurfaceView.setSurfaceRenderer(mRenderer);
    }

    private static int getColorCameraToDisplayAndroidRotation(int displayRotation,
                                                              int cameraRotation) {
        int cameraRotationNormalized = 0;
        switch (cameraRotation) {
            case 90:
                cameraRotationNormalized = 1;
                break;
            case 180:
                cameraRotationNormalized = 2;
                break;
            case 270:
                cameraRotationNormalized = 3;
                break;
            default:
                cameraRotationNormalized = 0;
                break;
        }
        int ret = displayRotation - cameraRotationNormalized;
        if (ret < 0) {
            ret += 4;
        }
        return ret;
    }

    /**
     * Use Tango camera intrinsics to calculate the projection Matrix for the Rajawali scene.
     * @param intrinsics camera instrinsics for computing the project matrix.
     * @param rotation the relative rotation between the camera intrinsics and display glContext.
     */
    private static float[] projectionMatrixFromCameraIntrinsics(Intrinsics intrinsics,
                                                                int rotation) {
        // Adjust camera intrinsics according to rotation
        float width = (float) intrinsics.getWidth();
        float height = (float) intrinsics.getHeight();
        float fx = (float) intrinsics.getFocalLengthInPixelsX();
        float fy = (float) intrinsics.getFocalLengthInPixelsY();

        switch (rotation) {
            case Surface.ROTATION_90:
                width = (float) intrinsics.getHeight();
                height = (float) intrinsics.getWidth();
                fx = (float) intrinsics.getFocalLengthInPixelsY();
                fy = (float) intrinsics.getFocalLengthInPixelsX();
                break;
            case Surface.ROTATION_180:
                break;
            case Surface.ROTATION_270:
                width = (float) intrinsics.getHeight();
                height = (float) intrinsics.getWidth();
                fx = (float) intrinsics.getFocalLengthInPixelsY();
                fy = (float) intrinsics.getFocalLengthInPixelsX();
                break;
            default:
                break;
        }

        // Uses frustumM to create a projection matrix taking into account calibrated camera
        // intrinsic parameter.
        // Reference: http://ksimek.github.io/2013/06/03/calibrated_cameras_in_opengl/
        float near = 0.1f;
        float far = 100;

        float xScale = near / fx;
        float yScale = near / fy;

        float m[] = new float[16];
        Matrix.frustumM(m, 0,
                xScale * -width / 2.0f,
                xScale * width / 2.0f,
                yScale * -height / 2.0f,
                yScale * height / 2.0f,
                near, far);
        return m;
    }

    /**
     * Set the color camera background texture rotation and save the camera to display rotation.
     */
    private void setAndroidOrientation() {
        Display display = getWindowManager().getDefaultDisplay();
        Camera.CameraInfo colorCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(COLOR_CAMERA_ID, colorCameraInfo);

        mColorCameraToDisplayAndroidRotation =
                getColorCameraToDisplayAndroidRotation(display.getRotation(),
                        colorCameraInfo.orientation);
        mRenderer.updateColorCameraTextureUv(mColorCameraToDisplayAndroidRotation);
    }

    @Override
    public void onSetupComplete() {
        mPoseProvider.onStartPoseProviding();
    }

    @Override
    public void onNewPoseData(PoseData newPoseData) {
        mRenderer.updateRenderCameraPose(newPoseData);
    }
}
