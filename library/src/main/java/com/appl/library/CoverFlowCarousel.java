package com.appl.library;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author Martin Appl
 */
public class CoverFlowCarousel extends Carousel {

    /**
     * Widget size on which was tuning of parameters done. This value is used to scale parameters on when widgets has different size
     */
    private int mTuningWidgetSize = 1280;

    /**
     * Distance from center as fraction of half of widget size where covers start to rotate into center
     * 1 means rotation starts on edge of widget, 0 means only center rotated
     */
    private float mRotationThreshold = 0.3f;

    /**
     * Distance from center as fraction of half of widget size where covers start to zoom in
     * 1 means scaling starts on edge of widget, 0 means only center scaled
     */
    private float mScalingThreshold = 0.3f;

    /**
     * Distance from center as fraction of half of widget size,
     * where covers start enlarge their spacing to allow for smooth passing each other without jumping over each other
     * 1 means edge of widget, 0 means only center
     */
    private float mAdjustPositionThreshold = 0.1f;

    /**
     * By enlarging this value, you can enlarge spacing in center of widget done by position adjustment
     */
    private float mAdjustPositionMultiplier = 0.8f;

    /**
     * Absolute value of rotation angle of cover at edge of widget in degrees
     */
    private float mMaxRotationAngle = 70.0f;

    /**
     * Scale factor of item in center
     */
    private float mMaxScaleFactor = 1.2f;

    /**
     * Radius of circle path which covers follow. Range of screen is -1 to 1, minimal radius is therefore 1
     */
    private float mRadius = 2f;

    /**
     * Size multiplier used to simulate perspective
     */
    private float mPerspectiveMultiplier = 1f;

    /**
     * Size of reflection as a fraction of original image (0-1)
     */
    private float mReflectionHeight = 0.5f;

    public CoverFlowCarousel(Context context) {
        super(context);
    }

    public CoverFlowCarousel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CoverFlowCarousel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void setTransformation(View v){
        int c = getChildCenter(v);
        v.setRotationY(getRotationAngle(c));
        v.setTranslationX(getChildAdjustPosition(v));
        float scale = getScaleFactor(c) - getChildCircularPathZOffset(c);
        v.setScaleX(scale);
        v.setScaleY(scale);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        for(int i=0; i < getChildCount(); i++){
            setTransformation(getChildAt(i));
        }
    }

    @Override
    protected int getPartOfViewCoveredBySibling() {
        return 0;
    }

    private float getRotationAngle(int childCenter){
        return -mMaxRotationAngle * getClampedRelativePosition(getRelativePosition(childCenter), mRotationThreshold * getWidgetSizeMultiplier());
    }

    private float getScaleFactor(int childCenter){
        return 1 + (mMaxScaleFactor-1) * (1 - Math.abs(getClampedRelativePosition(getRelativePosition(childCenter), mScalingThreshold * getWidgetSizeMultiplier())));
    }

    /**
     * Clamps relative position by threshold, and produces values in range -1 to 1 directly usable for transformation computation
     * @param position value int range -1 to 1
     * @param threshold always positive value of threshold distance from center in range 0-1
     * @return
     */
    private float getClampedRelativePosition(float position, float threshold){
        if(position < 0){
            if(position < -threshold) return -1f;
            else return position/threshold;
        }
        else{
            if(position > threshold) return 1;
            else return position/threshold;
        }
    }

    /**
     * Calculates relative position on screen in range -1 to 1, widgets out of screen can have values ove 1 or -1
     * @param pixexPos Absolute position in pixels including scroll offset
     * @return relative position
     */
    private float getRelativePosition(int pixexPos){
        final int half = getWidth()/2;
        final int centerPos = getScrollX() + half;

        return (pixexPos - centerPos)/((float) half);
    }

    private float getWidgetSizeMultiplier(){
        return ((float)mTuningWidgetSize)/((float)getWidth());
    }

    private float getChildAdjustPosition(View child) {
        final int c = getChildCenter(child);
        final float crp = getClampedRelativePosition(getRelativePosition(c), mAdjustPositionThreshold * getWidgetSizeMultiplier());
        final float d = mChildWidth * mAdjustPositionMultiplier * mSpacing * crp * getSpacingMultiplierOnCirlce(c);

        return d;
    }

    private float getSpacingMultiplierOnCirlce(int childCenter){
        float x = getRelativePosition(childCenter)/mRadius;
        return (float) Math.sin(Math.acos(x));
    }

    /**
     * Compute offset following path on circle
     * @param childCenter
     * @return offset from position on unitary circle
     */
    private float getOffsetOnCircle(int childCenter){
        float x = getRelativePosition(childCenter)/mRadius;
        if(x < -1.0f) x = -1.0f;
        if(x > 1.0f) x = 1.0f;

        return  (float) (1 - Math.sin(Math.acos(x)));
    }

    private float getChildCircularPathZOffset(int center){

        final float v = getOffsetOnCircle(center);
        final float z = mPerspectiveMultiplier * v;

        return  z;
    }
}
