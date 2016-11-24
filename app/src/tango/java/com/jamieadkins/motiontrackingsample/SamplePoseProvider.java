package com.jamieadkins.motiontrackingsample;

import android.content.Context;
import android.util.Log;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import java.util.ArrayList;

/**
 * Provides pose data using Android Sensors.
 */
public class SamplePoseProvider extends PoseProvider {
    private final String TAG = getClass().getSimpleName();
    private Tango mTango;
    private TangoConfig mConfig;
    private Runnable mOnTangoReady = new Runnable() {
        @Override
        public void run() {
            mConfig = mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
            mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
            // Low latency integration is necessary to achieve a precise alignment of
            // virtual objects with the RBG image and produce a good AR effect.
            mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);

            TangoCameraIntrinsics intrinsics =
                    mTango.getCameraIntrinsics(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);

            mIntrinsics = new Intrinsics(intrinsics.width, intrinsics.height,
                    intrinsics.fx, intrinsics.fy);

            mPoseProviderListener.onSetupComplete();
        }
    };

    public SamplePoseProvider(Context context, PoseProviderListener listener) {
        super(context, listener);
    }

    @Override
    public void setup() {
        mTango = new Tango(mContext, mOnTangoReady);
    }

    @Override
    public void onStartPoseProviding() {
        try {
            mTango.connect(mConfig);
            Log.d(TAG, "Tango Connected");
        } catch (TangoErrorException e) {
            Log.e(TAG, "Couldn't connect to Tango", e);
        }

        // Tango Listeners.
        try {
            setTangoListeners();
        } catch (TangoErrorException | SecurityException e) {
            Log.e(TAG, "Couldn't set Tango listeners", e);
        }
    }

    @Override
    public void onStopPoseProviding() {
        try {
            mTango.disconnect();
            Log.d(TAG, "Tango Disconnected");
        } catch (TangoErrorException e) {
            throw new AssertionError("Cannot disconnect from Tango.", e);
        }
    }

    /**
     * Set up the TangoConfig and the listeners for the Tango service, then begin using the Motion
     * Tracking API. This is called in response to the user clicking the 'Start' Button.
     */
    private void setTangoListeners() {
        // Select coordinate frame pair.
        final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        // Listen for new Tango data.
        mTango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {
            @Override
            public void onPoseAvailable(final TangoPoseData pose) {
                synchronized (POSE_LOCK) {
                    mLatestPoseData = new PoseData(pose.getTranslationAsFloats(),
                            pose.getRotationAsFloats(), (long) pose.timestamp);

                    // Log whenever Motion Tracking enters an invalid state.
                    if (pose.statusCode == TangoPoseData.POSE_INVALID) {
                        Log.w(TAG, "Pose Data Invalid");
                    }

                    onNewPoseData(mLatestPoseData);
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData data) {
                // Do nothing.
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData data) {
                // Do nothing.
            }

            @Override
            public void onTangoEvent(final TangoEvent event) {
                // Do nothing.
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // Do nothing.
            }
        });
    }
}
