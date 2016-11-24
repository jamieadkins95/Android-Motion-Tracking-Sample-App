package com.jamieadkins.motiontrackingsample;

/**
 * Contains camera intrinsic information. Can be set manually or use some dummy values.
 */
public class Intrinsics {
    private static final double DEFAULT_FOCAL_LENGTH = 3.5;
    private static final double DEFAULT_SENSOR_WIDTH = 5.376;
    private static final double DEFAULT_SENSOR_HEIGHT = 3.04;
    /**
     * Can any value other than 0 by default as they cancel each other out in the maths in
     * ComplexMovementRenderer.calculateProjectionMatrix(..)
     */
    private static final int DEFAULT_WIDTH = 1;
    /**
     * Can any value other than 0 by default as they cancel each other out in the maths in
     * ComplexMovementRenderer.calculateProjectionMatrix(..)
     */
    private static final int DEFAULT_HEIGHT = 1;

    private int mWidth;
    private int mHeight;
    private double mFocalLengthInPixelsX;
    private double mFocalLengthInPixelsY;

    public Intrinsics() {
        double focalLengthX = DEFAULT_FOCAL_LENGTH * DEFAULT_WIDTH / DEFAULT_SENSOR_WIDTH;
        double focalLengthY = DEFAULT_FOCAL_LENGTH * DEFAULT_SENSOR_HEIGHT / DEFAULT_SENSOR_HEIGHT;
        new Intrinsics(DEFAULT_WIDTH, DEFAULT_HEIGHT, focalLengthX, focalLengthY);
    }

    public Intrinsics(int width, int height, double focalLengthInPixelsX, double focalLengthInPixelsY) {
        mWidth = width;
        mHeight = height;
        mFocalLengthInPixelsX = focalLengthInPixelsX;
        mFocalLengthInPixelsY = focalLengthInPixelsY;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public double getFocalLengthInPixelsX() {
        return mFocalLengthInPixelsX;
    }

    public double getFocalLengthInPixelsY() {
        return mFocalLengthInPixelsY;
    }
}
