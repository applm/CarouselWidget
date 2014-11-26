package com.appl.library;

import java.lang.ref.WeakReference;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

/**
 * @author Martin Appl
 */
public class Carousel extends ViewGroup {
    /** Children added with this layout mode will be added after the last child */
    protected static final int LAYOUT_MODE_AFTER = 0;

    /** Children added with this layout mode will be added before the first child */
    protected static final int LAYOUT_MODE_TO_BEFORE = 1;

    /**
     * Relative spacing value of Views in container. If <1 Views will overlap, if >1 Views will have spaces between them
     */
    private float mSpacing = 0.75f;

    /**
     * Index of view in center of screen, which is most in foreground
     */
    private int mReverseOrderIndex = -1;

    private int mChildWidth = 320;
    private int mChildHeight = 320;

    private int mSelection;
    private Adapter mAdapter;

    private int mFirstVisibleChild;
    private int mLastVisibleChild;

    private final ViewCache<View> mCache = new ViewCache<>();

    private final Rect mTouchRect = new Rect();
    private View mMotionTarget;
    private float mTargetLeft;
    private float mTargetTop;

    public Carousel(Context context) {
        this(context, null);
    }

    public Carousel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Carousel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setChildrenDrawingOrderEnabled(true);
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
        }

        refill();

        if(v != null){
            mReverseOrderIndex = indexOfChild(v);
        }
    }



    /**
     *  Layout children from right to left
     */
    protected int layoutChildToBefore(View v, int right){
        int left = right - v.getMeasuredWidth();
        left = layoutChild(v, left);
        return left;
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

    protected void refill(){
        if(mAdapter == null) return;

        final int leftScreenEdge = getScrollX();
        int rightScreenEdge = leftScreenEdge + getWidth();

        removeNonVisibleViewsLeftToRight(leftScreenEdge);
        removeNonVisibleViewsRightToLeft(rightScreenEdge);

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

            addAndMeasureChild(child, LAYOUT_MODE_TO_BEFORE);
            lastLeft = layoutChildToBefore(child, lastLeft);
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

            addAndMeasureChild(child, LAYOUT_MODE_AFTER);
            lastRight = layoutChild(child, lastRight);
        }
    }


    /**
     * Remove non visible views from left edge of screen
     */
    protected void removeNonVisibleViewsLeftToRight(final int leftScreenEdge){
        if(getChildCount() == 0) return;

        // check if we should remove any views in the left
        View firstChild = getChildAt(0);

        while (firstChild != null && firstChild.getRight()  < leftScreenEdge) {

            // remove view
            removeViewsInLayout(0, 1);

            mCache.cacheView(firstChild);

            mFirstVisibleChild++;

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
        while (lastChild != null && lastChild.getLeft() > rightScreenEdge) {
            // remove the right view
            removeViewsInLayout(getChildCount() - 1, 1);

            mCache.cacheView(lastChild);

            mLastVisibleChild--;

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
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        int oldIndex = mReverseOrderIndex;
        int newIndex;
        if(l < oldl){
            newIndex = oldIndex - 1;
        } else {
            newIndex = oldIndex + 1;
        }

        final int screenCenter = getWidth()/2 + getScrollX();


        final int oldCenter = getChildCenter(oldIndex);
        final int newCenter = getChildCenter(newIndex);
        final int oldDif = oldCenter - screenCenter;
        final int newDif = newCenter - screenCenter;

        if(Math.abs(newDif) > Math.abs(oldDif)){
            mReverseOrderIndex = newIndex;
        }

    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (i < mReverseOrderIndex) {
            return i;
        } else {
            return childCount - 1 - (i - mReverseOrderIndex);
        }

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
