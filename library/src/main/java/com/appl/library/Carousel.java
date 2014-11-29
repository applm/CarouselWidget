package com.appl.library;

import java.lang.ref.WeakReference;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.Adapter;
import android.widget.Scroller;

/**
 * @author Martin Appl
 */
public class Carousel extends ViewGroup {
    protected final int NO_VALUE = Integer.MIN_VALUE + 1777;

    /** Children added with this layout mode will be added after the last child */
    protected static final int LAYOUT_MODE_AFTER = 0;

    /** Children added with this layout mode will be added before the first child */
    protected static final int LAYOUT_MODE_TO_BEFORE = 1;

    /** User is not touching the list */
    protected static final int TOUCH_STATE_RESTING = 0;

    /** User is scrolling the list */
    protected static final int TOUCH_STATE_SCROLLING = 1;

    /** Fling gesture in progress */
    protected static final int TOUCH_STATE_FLING = 2;

    private final Scroller mScroller = new Scroller(getContext());
    private VelocityTracker mVelocityTracker;
    protected int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private float mLastMotionX;

    private int mTouchState = TOUCH_STATE_RESTING;

    /**
     * Relative spacing value of Views in container. If <1 Views will overlap, if >1 Views will have spaces between them
     */
    private float mSpacing = 0.5f;

    /**
     * Index of view in center of screen, which is most in foreground
     */
    private int mReverseOrderIndex = -1;
    /**
     * Movement speed will be divided by this coefficient;
     */
    private int mSlowDownCoefficient = 1;

    private int mChildWidth = 320;
    private int mChildHeight = 320;

    private int mSelection;
    private Adapter mAdapter;

    private int mFirstVisibleChild;
    private int mLastVisibleChild;

    private final ViewCache<View> mCache = new ViewCache<>();

    protected int mRightEdge = NO_VALUE;
    protected int mLeftEdge = NO_VALUE;

    public Carousel(Context context) {
        this(context, null);
    }

    public Carousel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Carousel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setChildrenDrawingOrderEnabled(true);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    public Adapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(Adapter adapter) {
        mAdapter = adapter;
    }

    public View getSelectedView() {
        return null; //todo implement
    }

    public int getSelection(){
        return mSelection;
    }

    public void setSelection(int position) {
        if(mAdapter == null) throw new IllegalStateException("You are trying to set selection on widget without adapter");
        if(position < 0 || position > mAdapter.getCount()-1)
            throw new IllegalArgumentException("Position index must be in range of adapter values (0 - getCount()-1)");

        mSelection = position;
    }

    @Override
    public void computeScroll() {
//        if(x < (leftInPixels - centerItemLeft)){
//            deltaX -= x - (leftInPixels - centerItemLeft);
//        } else if(x > rightInPixels - centerItemRight){
//            deltaX -= x - (rightInPixels - centerItemRight);
//        }
        final int centerItemLeft = getWidth() / 2 - mChildWidth / 2;
        final int centerItemRight = getWidth() / 2 + mChildWidth / 2;

        if(mRightEdge != NO_VALUE && mScroller.getFinalX() > mRightEdge - centerItemRight){
            mScroller.setFinalX(mRightEdge - centerItemRight);
        }
        if(mLeftEdge != NO_VALUE && mScroller.getFinalX() < mLeftEdge - centerItemLeft){
            mScroller.setFinalX(mLeftEdge - centerItemLeft);
        }

//        if(mRightEdge != NO_VALUE && getScrollX() > mRightEdge - getWidth()) {
//            if(mRightEdge - getWidth() > 0) scrollTo(mRightEdge - getWidth(), 0);
//            else scrollTo(0, 0);
//            return;
//        }

        if (mScroller.computeScrollOffset()) {
            if(mScroller.getFinalX() == mScroller.getCurrX()){
                mScroller.abortAnimation();
                mTouchState = TOUCH_STATE_RESTING;
                clearChildrenCache();
            }
            else{
                final int x = mScroller.getCurrX();
                scrollTo(x, 0);

                postInvalidate();
            }
        }
        else if(mTouchState == TOUCH_STATE_FLING){
            mTouchState = TOUCH_STATE_RESTING;
            clearChildrenCache();
        }

        refill();
        updateReverseOrderIndex();
    }



    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(mAdapter == null || mAdapter.getCount() == 0){
            return;
        }
        View v = null;
        if(getChildCount() == 0){
            v = mAdapter.getView(mSelection,null,this);
            addAndMeasureChild(v,LAYOUT_MODE_AFTER);

            final int horizontalCenter = getWidth() / 2;
            final int verticalCenter = getHeight() / 2;
            final int left = horizontalCenter - v.getMeasuredWidth() / 2;
            final int right = left + v.getMeasuredWidth();
            final int top = verticalCenter - v.getMeasuredHeight() / 2;
            final int bottom = top + v.getMeasuredHeight();
            v.layout(left,top,right,bottom);

            mFirstVisibleChild = mSelection;
            mLastVisibleChild = mSelection;

            if(mLastVisibleChild == mAdapter.getCount() - 1){
                mRightEdge = right;
            }
            if(mFirstVisibleChild == 0){
                mLeftEdge = left;
            }
        }

        refill();

        if(v != null){
            mReverseOrderIndex = indexOfChild(v);
            v.setSelected(true);
        } else {
            updateReverseOrderIndex();
        }
    }

    private void updateReverseOrderIndex(){
        int oldReverseIndex = mReverseOrderIndex;
        final int screenCenter = getWidth()/2 + getScrollX();
        final int c = getChildCount();

        int minDiff = Integer.MAX_VALUE;
        int minDiffIndex = -1;

        int viewCenter, diff;
        for(int i=0; i < c; i++){
            viewCenter = getChildCenter(i);
            diff = Math.abs(screenCenter - viewCenter);
            if(diff < minDiff){
                minDiff = diff;
                minDiffIndex = i;
            }
        }

        if(minDiff != Integer.MAX_VALUE) {
            mReverseOrderIndex = minDiffIndex;
        }
//        //check for case in which last update was done with more elements and one element was removed in meantime
//        if(oldReverseIndex > getChildCount() - 1){
//            oldReverseIndex = getChildCount() - 1;
//        }

        if(oldReverseIndex != mReverseOrderIndex){
            View oldSelected = getChildAt(oldReverseIndex);
            View newSelected = getChildAt(mReverseOrderIndex);

            oldSelected.setSelected(false);
            newSelected.setSelected(true);
        }


//        final int screenCenter = getWidth()/2 + getScrollX();
//
//        if(mReverseOrderIndex >= getChildCount()){
//            mReverseOrderIndex = getChildCount() - 1;
//        }
//        if(mReverseOrderIndex < 0){
//            mReverseOrderIndex = 0;
//        }
//        int oldIndex = mReverseOrderIndex;
//        final int oldCenter = getChildCenter(oldIndex);
//        final int oldDif = Math.abs(oldCenter - screenCenter);
//
//        int newIndexLeft = oldIndex - 1;
//        int newIndexRigth = oldIndex + 1;
//
//        int newDifLeft = Integer.MAX_VALUE;
//        int newDifRight = Integer.MAX_VALUE;
//        if(newIndexLeft >= 0){
//            final int newCenterLeft = getChildCenter(newIndexLeft);
//            newDifLeft = newCenterLeft - screenCenter;
//            if(Math.abs(newDifLeft) < Math.abs(oldDif)){
//                mReverseOrderIndex = newIndexLeft;
//            }
//        }
//
//        if(newIndexRigth < getChildCount()){
//            final int newCenterRight = getChildCenter(newIndexLeft);
//            newDifRight = newCenterRight - screenCenter;
//            if(Math.abs(newDifRight) < Math.abs(oldDif)){
//                mReverseOrderIndex = newIndexRigth;
//            }
//        }
    }

    /**
     *  Layout children from right to left
     */
    protected int layoutChildToBefore(View v, int right){
        final int verticalCenter = getHeight() / 2;

        int l,t,r,b;
        l = right - v.getMeasuredWidth();
        t = verticalCenter - v.getMeasuredHeight() / 2;;
        r = right;
        b = t + v.getMeasuredHeight();

        v.layout(l, t, r, b);
        return r - (int)(v.getMeasuredWidth() * mSpacing);
    }

    /**
     * @param left X coordinate where should we start layout
     */
    protected int layoutChild(View v, int left){
        final int verticalCenter = getHeight() / 2;

        int l,t,r,b;
        l = left;
        t = verticalCenter - v.getMeasuredHeight() / 2;;
        r = l + v.getMeasuredWidth();
        b = t + v.getMeasuredHeight();

        v.layout(l, t, r, b);
        return l + (int)(v.getMeasuredWidth() * mSpacing);
    }

    /**
     * Adds a view as a child view and takes care of measuring it
     *
     * @param child The view to add
     * @param layoutMode Either LAYOUT_MODE_LEFT or LAYOUT_MODE_RIGHT
     * @return child which was actually added to container, subclasses can override to introduce frame views
     */
    protected View addAndMeasureChild(final View child, final int layoutMode) {
        if(child.getLayoutParams() == null) child.setLayoutParams(new LayoutParams(mChildWidth,
            mChildHeight));

        final int index = layoutMode == LAYOUT_MODE_TO_BEFORE ? 0 : -1;
        addViewInLayout(child, index, child.getLayoutParams(), true);

        final int pwms = MeasureSpec.makeMeasureSpec(mChildWidth, MeasureSpec.EXACTLY);
        final int phms = MeasureSpec.makeMeasureSpec(mChildHeight, MeasureSpec.EXACTLY);
        measureChild(child, pwms, phms);
        child.setDrawingCacheEnabled(isChildrenDrawnWithCacheEnabled());

        return child;
    }

    /**
     * Remove all data, reset to initial state and attempt to refill
     */
    private void reset() {
        if(getChildCount() == 0 || mReverseOrderIndex == -1){
            return;
        }
        View selectedView = getChildAt(mReverseOrderIndex);
        int selectedLeft = selectedView.getLeft();
        int selectedTop = selectedView.getTop();

        removeAllViewsInLayout();
        mRightEdge = NO_VALUE;
        mLeftEdge = NO_VALUE;

        View v = mAdapter.getView(mSelection,null,this);
        addAndMeasureChild(v,LAYOUT_MODE_AFTER);

        final int right = selectedLeft + v.getMeasuredWidth();
        final int bottom = selectedTop + v.getMeasuredHeight();
        v.layout(selectedLeft,selectedTop,right,bottom);


        mFirstVisibleChild = mSelection;
        mLastVisibleChild = mSelection;

        if(mLastVisibleChild == mAdapter.getCount() - 1){
            mRightEdge = right;
        }
        if(mFirstVisibleChild == 0){
            mLeftEdge = selectedLeft;
        }

        refill();

        mReverseOrderIndex = indexOfChild(v);
        v.setSelected(true);
    }

    protected void refill(){
        if(mAdapter == null) return;

        final int leftScreenEdge = getScrollX();
        int rightScreenEdge = leftScreenEdge + getWidth();

        removeNonVisibleViewsLeftToRight(leftScreenEdge);
        removeNonVisibleViewsRightToLeft(rightScreenEdge);

        if(getChildCount() == 0){
            Log.wtf("debug","zero children");
        }

        refillLeftToRight(leftScreenEdge, rightScreenEdge);
        refillRightToLeft(leftScreenEdge);
    }

    /**
     * Checks and refills empty area on the left
     * @return firstItemPosition
     */
    protected void refillRightToLeft(final int leftScreenEdge){
        if(getChildCount() == 0) return;

        View child = getChildAt(0);
        int childRight = child.getRight();
        int lastLeft = childRight - (int) (mChildWidth * mSpacing);

        while(lastLeft > leftScreenEdge && mFirstVisibleChild > 0){
            mFirstVisibleChild--;

            child = mAdapter.getView(mFirstVisibleChild, mCache.getCachedView(), this);
            child.setSelected(false);
            mReverseOrderIndex++;

            addAndMeasureChild(child, LAYOUT_MODE_TO_BEFORE);
            lastLeft = layoutChildToBefore(child, lastLeft);

            if(mFirstVisibleChild <= 0) {
                mLeftEdge = child.getLeft();
            }
        }
        return;
    }

    /**
     * Checks and refills empty area on the right
     */
    protected void refillLeftToRight(final int leftScreenEdge, final int rightScreenEdge){

        View child;
        int lastRight;

        child = getChildAt(getChildCount() - 1);
        int childLeft = child.getLeft();
        lastRight = childLeft + (int) (mChildWidth * mSpacing);

        while(lastRight < rightScreenEdge && mLastVisibleChild < mAdapter.getCount()-1){
            mLastVisibleChild++;

            child = mAdapter.getView(mLastVisibleChild, mCache.getCachedView(), this);
            child.setSelected(false);

            addAndMeasureChild(child, LAYOUT_MODE_AFTER);
            lastRight = layoutChild(child, lastRight);

            if(mLastVisibleChild >= mAdapter.getCount()-1) {
                mRightEdge = child.getRight();
            }
        }
    }


    /**
     * Remove non visible views from left edge of screen
     */
    protected void removeNonVisibleViewsLeftToRight(final int leftScreenEdge){
        if(getChildCount() == 0) return;

        // check if we should remove any views in the left
        View firstChild = getChildAt(0);

        while (firstChild != null && firstChild.getRight()  < leftScreenEdge && getChildCount() > 1) {

            // remove view
            removeViewsInLayout(0, 1);

            mCache.cacheView(firstChild);

            mFirstVisibleChild++;
            mReverseOrderIndex--;

            if(mReverseOrderIndex == 0){
                break;
            }

            // Continue to check the next child only if we have more than
            // one child left
            if (getChildCount() > 1) {
                firstChild = getChildAt(0);
            } else {
                firstChild = null;
            }
        }

    }

    /**
     * Remove non visible views from right edge of screen
     */
    protected void removeNonVisibleViewsRightToLeft(final int rightScreenEdge){
        if(getChildCount() == 0) return;

        // check if we should remove any views in the right
        View lastChild = getChildAt(getChildCount() - 1);
        while (lastChild != null && lastChild.getLeft() > rightScreenEdge && getChildCount() > 1) {
            // remove the right view
            removeViewsInLayout(getChildCount() - 1, 1);

            mCache.cacheView(lastChild);

            mLastVisibleChild--;
            if(getChildCount() - 1 == mReverseOrderIndex){
                break;
            }

            // Continue to check the next child only if we have more than
            // one child left
            if (getChildCount() > 1) {
                lastChild = getChildAt(getChildCount() - 1);
            } else {
                lastChild = null;
            }
        }

    }

    private int getChildCenter(View v){
        final int w = v.getRight() - v.getLeft();
        return v.getLeft() + w/2;
    }

    private int getChildCenter(int i){
        return getChildCenter(getChildAt(i));
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (i < mReverseOrderIndex) {
            return i;
        } else {
            return childCount - 1 - (i - mReverseOrderIndex);
        }

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onTouchEvent will be called and we do the actual
         * scrolling there.
         */


        /*
         * Shortcut the most recurring case: the user is in the dragging
         * state and he is moving his finger.  We want to intercept this
         * motion.
         */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mTouchState == TOUCH_STATE_SCROLLING)) {
            return true;
        }

        final float x = ev.getX();
        final float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
            	/*
                 * not dragging, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                 * Locally do absolute value. mLastMotionX is set to the x value
                 * of the down event.
                 */
                final int xDiff = (int) Math.abs(x - mLastMotionX);

                final int touchSlop = mTouchSlop;
                final boolean xMoved = xDiff > touchSlop;

                if (xMoved) {
                    // Scroll if the user moved far enough along the axis
                    mTouchState = TOUCH_STATE_SCROLLING;
                    enableChildrenCache();
                    cancelLongPress();
                }

                break;

            case MotionEvent.ACTION_DOWN:
                // Remember location of down touch
                mLastMotionX = x;

                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't.  mScroller.isFinished should be false when
                 * being flinged.
                 */
                mTouchState = mScroller.isFinished() ? TOUCH_STATE_RESTING : TOUCH_STATE_SCROLLING;
                break;

            case MotionEvent.ACTION_UP:
                mTouchState = TOUCH_STATE_RESTING;
                clearChildrenCache();
                break;
        }

        return mTouchState == TOUCH_STATE_SCROLLING;

    }

    protected void scrollByDelta(int deltaX){
        final int centerItemLeft = getWidth() / 2 - mChildWidth / 2;
        final int centerItemRight = getWidth() / 2 + mChildWidth / 2;

        final int rightInPixels;
        final int leftInPixels;
        if(mRightEdge == NO_VALUE){
            rightInPixels = Integer.MAX_VALUE;
        } else {
            rightInPixels = mRightEdge;
        }

        if(mLeftEdge == NO_VALUE){
            leftInPixels = Integer.MIN_VALUE + getWidth(); //we cant have min value because of integer overflow
        } else {
            leftInPixels = mLeftEdge;
        }

        final int x = getScrollX() + deltaX;

        if(x < (leftInPixels - centerItemLeft)){
            deltaX -= x - (leftInPixels - centerItemLeft);
        } else if(x > rightInPixels - centerItemRight){
            deltaX -= x - (rightInPixels - centerItemRight);
        }

        scrollBy(deltaX, 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            /*
             * If being flinged and user touches, stop the fling. isFinished
             * will be false if being flinged.
             */
                if (!mScroller.isFinished()) {
                    mScroller.forceFinished(true);
                }

                // Remember where the motion event started
                mLastMotionX = x;

                break;
            case MotionEvent.ACTION_MOVE:

                if (mTouchState == TOUCH_STATE_SCROLLING) {
                    // Scroll to follow the motion event
                    final int deltaX = (int) (mLastMotionX - x);
                    mLastMotionX = x;

                    scrollByDelta(deltaX);
                }
                else{
                    final int xDiff = (int) Math.abs(x - mLastMotionX);

                    final int touchSlop = mTouchSlop;
                    final boolean xMoved = xDiff > touchSlop;


                    if (xMoved) {
                        // Scroll if the user moved far enough along the axis
                        mTouchState = TOUCH_STATE_SCROLLING;
                        enableChildrenCache();
                        cancelLongPress();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                //if we had normal down click and we haven't moved enough to initiate drag, take action as a click on down coordinates
                if (mTouchState == TOUCH_STATE_SCROLLING) {

                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialXVelocity = (int) mVelocityTracker.getXVelocity();
                    int initialYVelocity = (int) mVelocityTracker.getYVelocity();

                    if (Math.abs(initialXVelocity) + Math.abs(initialYVelocity) > mMinimumVelocity) {
                        fling(-initialXVelocity, -initialYVelocity);
                    }
                    else{
                        // Release the drag
                        clearChildrenCache();
                        mTouchState = TOUCH_STATE_RESTING;
                    }

                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }

                    break;
                }

                // Release the drag
                clearChildrenCache();
                mTouchState = TOUCH_STATE_RESTING;

                break;
            case MotionEvent.ACTION_CANCEL:
                mTouchState = TOUCH_STATE_RESTING;
        }

        return true;
    }

    public void fling(int velocityX, int velocityY){
        mTouchState = TOUCH_STATE_FLING;
        final int x = getScrollX();
        final int y = getScrollY();

        final int centerItemLeft = getWidth() / 2 - mChildWidth / 2;
        final int centerItemRight = getWidth() / 2 + mChildWidth / 2;
        final int rightInPixels;
        final int leftInPixels;
        if(mRightEdge == NO_VALUE) rightInPixels = Integer.MAX_VALUE;
        else rightInPixels = mRightEdge;
        if(mLeftEdge == NO_VALUE) leftInPixels = Integer.MIN_VALUE + getWidth();
        else leftInPixels = mLeftEdge;

        mScroller.fling(x, y, velocityX, velocityY, leftInPixels - centerItemLeft,
            rightInPixels - centerItemRight + 1,0,0);

        invalidate();
    }

    private void enableChildrenCache() {
        setChildrenDrawnWithCacheEnabled(true);
        setChildrenDrawingCacheEnabled(true);
    }

    private void clearChildrenCache() {
        setChildrenDrawnWithCacheEnabled(false);
    }

    /**
     * Set widget spacing (float means fraction of widget size, 1 = widget size)
     * @param spacing the spacing to set
     */
    public void setSpacing(float spacing) {
        this.mSpacing = spacing;
    }

    public void setChildWidth(int width){
        mChildWidth = width;
    }

    public void setChildHeight(int height){
        mChildHeight = height;
    }

    public void setSlowDownCoefficient(int c){
        mSlowDownCoefficient = c;
    }

    public static class ViewCache <T extends View> {
        private final LinkedList<WeakReference<T>> mCachedItemViews = new LinkedList<WeakReference<T>>();

        /**
         * Check if list of weak references has any view still in memory to offer for recycling
         * @return cached view
         */
        public T getCachedView(){
            if (mCachedItemViews.size() != 0) {
                T v;
                do{
                    v = mCachedItemViews.removeFirst().get();
                }
                while(v == null && mCachedItemViews.size() != 0);
                return v;
            }
            return null;
        }

        public void cacheView(T v){
            WeakReference<T> ref = new WeakReference<T>(v);
            mCachedItemViews.addLast(ref);
        }
    }
}
