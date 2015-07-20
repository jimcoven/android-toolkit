package com.mikimedia.android.nuori;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;

import com.mikimedia.android.R;

public class Nuori {

    private static final String TAG = Nuori.class.getSimpleName();

    public static Nuori from(NuoriParallaxView view) {
        return view.getNuori();
    }

    /**
     * The host for nuori
     */
    private final NuoriParallaxView mHost;

    /**
     * Sets the image view height relative to the screen
     */
    private final  float mHeightToScreen;
    private static final float DEFAULT_SCREEN_TO_HEIGHT = 0.5f;

    /**
     * Determines the theoretical zoom for the image view
     */
    private float mZoomRatio;
    private static final float DEFAULT_ZOOM_RATIO = 2.0f;

    /**
     * Activated is only set true after prepare is called.
     * This is to prevent scrolling when not ready.
     */
    private boolean mActivated = false;

    /**
     * determines the initial height of the image view.
     * this is needed to tell the bounce animation what height to revert to
     */
    private int mInitialHeight = -1;

    /**
     * maxZoomHeight is a calculated via mZoomRatio x intrinsic height of drawable
     */
    private int mMaxZoomHeight = -1;

    private ImageView mImageView = null;
    private View mHeaderView = null;

    private float mZoomMultiplier = -1;

    /**
     * Nuori is instantiated from within the mParent
     * @param mHost
     */
    Nuori(NuoriParallaxView host, Context context, AttributeSet attrs,
          int defStyleAttr, int defStyleRes) {
        this.mHost = host;

        // Read and apply provided attributes
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.NuoriParallaxView, defStyleAttr, defStyleRes);
        mZoomRatio = a.getFloat(R.styleable.NuoriParallaxView_zoomRatio, DEFAULT_ZOOM_RATIO);
        mHeightToScreen = a.getFloat(R.styleable.NuoriParallaxView_heightToScreenRatio,
                DEFAULT_SCREEN_TO_HEIGHT);
        a.recycle();
    }

    public Nuori setImageView(ImageView imageView) {
        this.mImageView = imageView;
        return this;
    }

    View getHeaderView() {
        return mHeaderView;
    }

    public Nuori setHeaderView(View headerView) {
        this.mHeaderView = headerView;
        return this;
    }

    public Nuori setZoomRatio(float zoomRatio) {
        this.mZoomRatio = zoomRatio;
        return this;
    }

    public Nuori into() {

        if (mImageView == null) {
            throw new NullPointerException("No ImageView has been set");
        }

        if (mHeaderView == null) {
            if (mHost instanceof NuoriParallaxListView) {
                throw new NullPointerException("No header view has been set");
            }
        }

        if (mZoomRatio < 1.0) {
            throw new IllegalStateException("ZoomRatio must be larger than 1.0");
        }

        prepare();

        return mHost.setNuori(this);
    }

    /**
     * Preparation before we set the objects into the mParent
     */

    private void prepare() {

        bounceBack = new BounceBackAnimation(mHost, mImageView);

        final DisplayMetrics metrics = mHost.getContext().getResources().getDisplayMetrics();
        mInitialHeight = (int) (mHeightToScreen * metrics.heightPixels);
        mImageView.getLayoutParams().height = mInitialHeight;
        mImageView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        mImageView.requestLayout();

        mImageView.post(new Runnable() {
            @Override
            public void run() {
                notifyViewBoundsChanged();
            }
        });
    }

    /**
     * TODO: runnable is required because the drawable may not yet be set.
     * but this is NOT SAFE. It depends on how long the image is set.
     * Need to have a look at this again.
     *
     * Also when the imageview is reused, the bounds have to recalculated.
     */
    public void notifyViewBoundsChanged() {

        if (mImageView.getDrawable() != null) {
            int dw = mImageView.getDrawable().getIntrinsicWidth();
            int dh = mImageView.getDrawable().getIntrinsicHeight();
//            Log.d(TAG, "dh = " + dh);
//            Log.d(TAG, "dw = " + dw);
//            Log.d(TAG, "width = " + mImageView.getWidth());

            final double ratio = ((double) mImageView.getWidth()) / dw;
            mMaxZoomHeight = (int) (dh * ratio * mZoomRatio);
            mZoomMultiplier = (float) mMaxZoomHeight / mInitialHeight;
//            Log.d(TAG, "ratio = " + ratio);
//            Log.d(TAG, "mMaxZoomHeight = " + mMaxZoomHeight);
//            Log.d(TAG, "mInitialHeight = " + mInitialHeight);
//            Log.d(TAG, "mZoomMultiplier A= " + mZoomMultiplier);

            mActivated = true;
        } else {
            mActivated = false;
        }

    }

    void onScrollChanged(int l, int t, int oldl, int oldt) {
        if (!mActivated) return;

//        View firstView = (View) mImageView.getParent();
//        // firstView.getTop < getPaddingTop means mImageView will be covered by top padding,
//        // so we can layout it to make it shorter
//        if (firstView.getTop() < mHost.getPaddingTop() && mImageView.getHeight() > mInitialHeight) {
//            mImageView.getLayoutParams().height = Math.max(mImageView.getHeight() - (mHost.getPaddingTop() - firstView.getTop()), mInitialHeight);
//            // to set the firstView.mTop to 0,
//            // maybe use View.setTop() is more easy, but it just support from Android 3.0 (API 11)
//            firstView.layout(firstView.getLeft(), 0, firstView.getRight(), firstView.getHeight());
//            mImageView.requestLayout();
//        }
    }

    /**
     * All the values here are primitive. No worries about affecting the values in the calling function
     */
    void overScrollBy(int deltaX, int deltaY, int scrollX,
                         int scrollY, int scrollRangeX, int scrollRangeY,
                         int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        if (!mActivated) return;

//        Log.d(TAG, "...... mImageView.getHeight() = " + mImageView.getHeight());
//        Log.d(TAG, "...... mMaxZoomHeight = " + mMaxZoomHeight);
//        Log.d(TAG, "...... deltaX = " + deltaX);
//        Log.d(TAG, "...... deltaY = " + deltaY);
//        Log.d(TAG, "...... scrollX = " + scrollX);
//        Log.d(TAG, "...... scrollY = " + scrollY);
//        Log.d(TAG, "...... maxOverScrollX = " + maxOverScrollX);
//        Log.d(TAG, "...... maxOverScrollY = " + maxOverScrollY);

        // isTouchEvent - not due to fling or other motions. User is actually touching
        if (mImageView.getHeight() <= mMaxZoomHeight && isTouchEvent && scrollY <= 0) {
            if (deltaY < 0) { // downard swipe
                int futureY = (int) (mImageView.getHeight() - deltaY * mZoomMultiplier);
                if (futureY >= mInitialHeight) {
                    mImageView.getLayoutParams().height = futureY < mMaxZoomHeight ? futureY : mMaxZoomHeight;
                    mImageView.requestLayout();
                }
            } else { // upward swipe
                if (mImageView.getHeight() > mInitialHeight) {
                    int futureY = mImageView.getHeight() - deltaY;
                    mImageView.getLayoutParams().height = futureY > mInitialHeight ? futureY : mInitialHeight;
                    mImageView.requestLayout();
                }
            }
        }
    }

    void onTouchEvent(MotionEvent ev) {
        if (!mActivated) return;

        switch(ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                bounceBack.cancel();
                break;

            case MotionEvent.ACTION_UP:
                if (mInitialHeight < mImageView.getHeight()) {
                    bounceBack.start(mInitialHeight, mZoomMultiplier);
                }

                break;
        }
    }

    private BounceBackAnimation bounceBack = null;

    /**
     * Cant really use matrix transformation here, because the listview items layout
     * will depend on the actual physical layout of the imageview
     */
    private static class BounceBackAnimation extends Animation implements Animation.AnimationListener {
        private float initialHeight;
        private float extraHeight;

        private float translateY;
        private float deltaShifted;
        private float endPointY;

        private final View view;
        private final NuoriParallaxScrollView host;

        private BounceBackAnimation(NuoriParallaxView host, View view) {
            this.host = (NuoriParallaxScrollView)host;
            this.view = view;
            setDuration(200);
            setAnimationListener(this);
//            setFillAfter(true);
        }

        private void start(int initialHeight, float zoomMultiplier) {
            float zoomedHeight = view.getHeight();
            this.initialHeight = initialHeight;
            this.extraHeight = initialHeight - zoomedHeight;


            float zoomed = (zoomedHeight / initialHeight);

            float density = 3.0f;
            float scrollY = host.getScrollY();

            translateY = (zoomed - 1) / zoomed * (scrollY - view.getTop() * density);
            deltaShifted = 0;

            translateY = -translateY;
            endPointY = scrollY + translateY;

            System.out.println("..... initialHeight = " + initialHeight);
            System.out.println("..... zoomedHeight = " + zoomedHeight);
            System.out.println("..... view.getTop() = " + view.getTop());
            System.out.println("..... zoomed = " + zoomed);
            System.out.println("..... translateY = " + translateY);
            System.out.println("..... scrollY = " + scrollY);
            System.out.println("..... endPointY = " + endPointY);

            view.startAnimation(this);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
//            System.out.println("applyTransformation " + interpolatedTime);
            int newHeight = (int) (initialHeight - extraHeight * (1 - interpolatedTime));
            view.getLayoutParams().height = newHeight;
            view.requestLayout();

            int delta = (int) (translateY * (interpolatedTime) - deltaShifted);
            deltaShifted += delta;
            host.scrollBy(0, delta);
        }

        public void cancel() {
            if (hasStarted()) {
                super.cancel();

                /** Its not super necessary to clear animation here
                 * because it is cleared on animation end.
                 * But just putting a hard stop here in case
                 * a threading lag due to GC or stuff causes
                 * another applyTransformation to occur
                 */
                view.clearAnimation();

                /**
                 * Check if the API level supports canceling existing animations via the
                 * ViewPropertyAnimator, and cancel as a brute force measure :)
                 */

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    view.animate().cancel();
                }
            }
        }

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            /**
             * Do not remove this. There is an android bug that keeps the
             * animation running (i.e applyTransformation is being called)
             * even when it is supposedly ended. This is a very bad android bug.
             */
            view.clearAnimation();

//            host.scrollBy(0, (int) -translateY);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

}
