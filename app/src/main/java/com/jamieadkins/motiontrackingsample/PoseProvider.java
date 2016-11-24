package com.jamieadkins.motiontrackingsample;

import android.content.Context;

/**
 * Class that provides pose information (translations and rotation).
 */
public abstract class PoseProvider {
    protected Context mContext;
    protected PoseProviderListener mPoseProviderListener;

    protected PoseData mLatestPoseData;
    protected Intrinsics mIntrinsics;

    public static final Object POSE_LOCK = new Object();

    public interface PoseProviderListener {
        void onSetupComplete();

        void onNewPoseData(PoseData newPoseData);
    }

    public PoseProvider(Context context, PoseProviderListener listener) {
        mContext = context;
        mPoseProviderListener = listener;

        // Android APIs don't provide a way to obtain camera intrinsics so we spoof them instead.
        mIntrinsics = new Intrinsics();
    }

    public abstract void onStartPoseProviding();

    public abstract void onStopPoseProviding();

    public abstract void setup();

    protected void onNewPoseData(PoseData newPoseData){
        if (mPoseProviderListener != null) {
            mPoseProviderListener.onNewPoseData(newPoseData);
        }
    }

    public PoseData getLatestPoseData() {
        synchronized (POSE_LOCK) {
            return mLatestPoseData;
        }
    }

    public Intrinsics getIntrinsics() {
        return mIntrinsics;
    }
}
