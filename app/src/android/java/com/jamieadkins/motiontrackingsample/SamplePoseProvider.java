package com.jamieadkins.motiontrackingsample;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Provides pose data using Android Sensors.
 */
public class SamplePoseProvider extends PoseProvider {
    private final String TAG = getClass().getSimpleName();
    private SensorManager mSensorManager;
    private Sensor m6DoFSensor;

    private SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            synchronized (POSE_LOCK) {
                mLatestPoseData = new PoseData(event.values, event.timestamp);
            }

            onNewPoseData(mLatestPoseData);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public SamplePoseProvider(Context context, PoseProviderListener poseListener) {
        super(context, poseListener);
        mIntrinsics = new Intrinsics();
    }

    @Override
    public void onStartPoseProviding() {
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        m6DoFSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_POSE_6DOF);
        boolean setupSuccessful = mSensorManager.registerListener(mSensorListener, m6DoFSensor,
                SensorManager.SENSOR_DELAY_FASTEST);

        if (!setupSuccessful) {
            Log.e(TAG, "Failed to set 6dof sensor");
        }
    }

    @Override
    public void onStopPoseProviding() {
        mSensorManager.unregisterListener(mSensorListener);
    }

    @Override
    public void setup() {
        mPoseProviderListener.onSetupComplete();
    }
}
