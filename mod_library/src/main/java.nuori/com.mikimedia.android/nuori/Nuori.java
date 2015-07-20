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
    private int mInitialHeightPx = -1;

    /**
     * maxZoomHeight is a calculated via mZoomRatio x intrinsic height of drawable
     */
    private int mMaxZoomHeight = -1;

    private ImageView mImageView = null;
    private View mHeaderView = null;

    private float mZoomMultiplier = -1;
    private float mScreenDensity = -1;

    /**
     * Nuori is instantiated from within the mParent
     * @param host
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
        return into(true);
    }

    public Nuori into(boolean auto) {

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

        prepare(auto);

        return mHost.setNuori(this);
    }

    /**
     * Preparation before we set the objects into the mParent
     */

    private void prepare(boolean auto) {

        bounceBack = new BounceBackAnimation(mHost, mImageView);

        final DisplayMetrics metrics = mHost.getContext().getResources().getDisplayMetrics();
        mScreenDensity = metrics.density;
        mInitialHeightPx = (int) (mHeightToScreen * metrics.heightPixels);
        mImageView.getLayoutParams().height = mInitialHeightPx;
        mImageView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        mImageView.requestLayout();

//        Log.d(TAG, "screen height" + metrics.heightPixels);

        if (auto) notifyViewBoundsChanged();
    }

    /**
     * TODO: runnable is required because the drawable may not yet be set.
     * but this is NOT SAFE. It depends on how long the image is set.
     * Need to have a look at this again.
     *
     * Also when the imageview is reused, the bounds have to recalculated.
     */
    public void notifyViewBoundsChanged() {

        mImageView.post(new Runnable() {
            @Override
            public void run() {
                if (mImageView.getDrawable() != null) {
                    int dw = mImageView.getDrawable().getIntrinsicWidth();
                    int dh = mImageView.getDrawable().getIntrinsicHeight();
                    final double ratio = ((double) mImageView.getWidth()) / dw;
                    mMaxZoomHeight = (int) (dh * ratio * mZoomRatio);
                    mZoomMultiplier = (float) mMaxZoomHeight / mInitialHeightPx;

                    Log.d(TAG, "dh = " + dh);
                    Log.d(TAG, "dw = " + dw);
                    Log.d(TAG, "width = " + mImageView.getWidth());
                    Log.d(TAG, "ratio = " + ratio);
                    Log.d(TAG, "mMaxZoomHeight = " + mMaxZoomHeight);
                    Log.d(TAG, "mInitialHeight = " + mInitialHeightPx);
                    Log.d(TAG, "mZoomMultiplier A= " + mZoomMultiplier);

                    mActivated = true;
                } else {
                    mActivated = false;
                }
            }
        });
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
                if (futureY >= mInitialHeightPx) {
                    mImageView.getLayoutParams().height = futureY < mMaxZoomHeight ? futureY : mMaxZoomHeight;
                    mImageView.requestLayout();
                }
            } else { // upward swipe
                if (mImageView.getHeight() > mInitialHeightPx) {
                    int futureY = mImageView.getHeight() - deltaY;
                    mImageView.getLayoutParams().height = futureY > mInitialHeightPx ? futureY : mInitialHeightPx;
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
                if (mInitialHeightPx < mImageView.getHeight()) {
                    bounceBack.start(mInitialHeightPx, mScreenDensity);
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

        private float currHeightPx;
        private float extraHeight;

        private float translateY;
        private float deltaShiftedY;

        private final ImageView view;
        private final NuoriParallaxView host;

        private BounceBackAnimation(NuoriParallaxView host, ImageView view) {
            this.host = host;
            this.view = view;
            setDuration(200);
            setAnimationListener(this);
//            setFillAfter(true);
        }

        private void start(int initHeightPx, float density) {
            this.currHeightPx = view.getHeight();
            this.extraHeight = currHeightPx - initHeightPx;

            /**
             * In order to bring the user back to PERSPECTIVE.
             * This is so that the user's point of focus (top of the image view remains in tact).
             *
             * Some images will be scaled when intrinsic width < view width.
             * so the adjIntrinsicHeightPx > intrinsicHeightPx.
             *
             * say view width is 1000. but the pic is 400 w x 500 h. It will then stretch to fill
             * at 1000 w x 1250 h.
             * intrinsicHeightPx = 500
             * adjIntrinsicHeightPx = 1250
             *
             * For images that have adjIntrinsicHeightPx < initHeightPx, they will actaully
             * be stretched right from the start to fill the view height. So the
             * adjIntrinsicHeightPx' = initHeightPx.
             *
             * Taking both into account, Math.max(initHeightPx, adjIntrinsicHeightPx)
             *
             * Zooming only happens when currHeightPx (or rather pulled height) > adjIntrinsicHeightPx'
             *
             */
            float adjIntrinsicHeightPx = view.getWidth() * view.getDrawable().getIntrinsicHeight() /
                    view.getDrawable().getIntrinsicWidth();
            float zoomed = currHeightPx / Math.max(initHeightPx, adjIntrinsicHeightPx);

            translateY = 0;
            deltaShiftedY = 0;

            // out of the total scrollY, part of it comes from scrolling till view.getTop()
            // no zoom happens here, so reverse translation is not needed.
            translateY = host.computePerspectiveOffset(initHeightPx, zoomed, view.getTop() * density);

            view.startAnimation(this);

//            Log.d(TAG, "......................................................");
//            Log.d(TAG, "widthZoom = " + view.getwidth() / view.getdrawable().getintrinsicwidth());
//            log.d(tag, "initheightpx = " + initheightpx);
//            log.d(tag, "adjintrinsicheightpx = " + adjintrinsicheightpx);
//            log.d(tag, "currheightpx = " + currheightpx);
//            log.d(tag, "zoomed = " + zoomed);
//            log.d(tag, "translateY = " + translateY);
//            Log.d(TAG, "host.getScrollY() = " + host.getScrollY());

        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
//            System.out.println("applyTransformation " + interpolatedTime);

            if (translateY != 0) {
                int delta = (int) (translateY * (interpolatedTime) - deltaShiftedY);
                deltaShiftedY += delta;
                host.scrollBy(0, delta);
            }

            view.getLayoutParams().height = (int) (currHeightPx - extraHeight * interpolatedTime);
            view.requestLayout();
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

            view.getLayoutParams().height =(int) (currHeightPx - extraHeight);
            view.requestLayout();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

}
