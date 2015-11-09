package com.common.ui;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

public class FlingGesture extends ViewGesture {
    private final PointF mDownPos = new PointF();
	private VelocityTracker mVelocityTracker = null;
	private float mMinVelocity = Float.NaN;
	private float mMaxVelocity = Float.NaN;
	private float mMinAngle = 0.0f;
	private float mMaxAngle = 180.0f;
	
	// ### 内嵌类 ###
	public static interface GestureListener extends ViewGesture.GestureListener {
		void onFling(ViewGesture g, View v, PointF flingPoint, PointF velocity);
	}

	// ### 方法 ###
	public void setMinVelocity(float minVelocity) {
		mMinVelocity = minVelocity;
	}
	public void setMaxVelocity(float maxVelocity) {
		mMaxVelocity = maxVelocity;
	}
	public void setMinAngle(float minAngle) {
		mMinAngle = minAngle;
	}
	public void setMaxAngle(float maxAngle) {
		mMaxAngle = maxAngle;
	}
	
	// ### ViewGesture抽象函数重写 ###
	@Override
	protected void doRestart(View v, boolean reset) {
		if (reset) {
			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
		} else {
			if (mVelocityTracker != null) {
				mVelocityTracker.clear();
			}
		}
	}
	@Override
	protected void doDetect(View v, final MotionEvent m, boolean delayed, ViewGesture.GestureListener listener) {
		// 判断是否需要探测手势
		if (listener instanceof GestureListener == false) {
			keepDetecting(false);
			return;
		}
		final GestureListener flingListener = (GestureListener) listener;
        final MotionEvent m2screen = UiUtils.obtainMotionEvent(m, v, null);

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(m2screen);

        if (m.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mDownPos.set(m2screen.getX(), m2screen.getY());
        }

        while (m.getActionMasked() == MotionEvent.ACTION_UP) {
            final PointF upPos = new PointF(m2screen.getX(), m2screen.getY());
            if (calcDistance(mDownPos, upPos) < UiUtils.getScaledTouchSlop(v.getContext()))
                break;

			final float minVelocity = Float.isNaN(mMinVelocity) ? UiUtils.getScaledMinFlingVelocity(v.getContext()) : mMinVelocity;
			final float maxVelocity = Float.isNaN(mMaxVelocity) ? UiUtils.getScaledMaxFlingVelocity(v.getContext()) : mMaxVelocity;

            mVelocityTracker.computeCurrentVelocity(1000, maxVelocity);
            final PointF velocity = new PointF(mVelocityTracker.getXVelocity(), mVelocityTracker.getYVelocity());
			if (Math.abs(velocity.x) < minVelocity)
				velocity.x = 0.0f;
			if (Math.abs(velocity.y) < minVelocity)
				velocity.y = 0.0f;

            UiUtils.transformOffsetFromScreen(velocity, v);
            if (UiUtils.isLineBetween(new PointF(0.0f, 0.0f), velocity, mMinAngle, mMaxAngle) == false)
				break;
			
			if (Float.compare(Math.abs(velocity.x), 0.0f) != 0 || Float.compare(Math.abs(velocity.y), 0.0f) != 0) {
				flingListener.onFling(this, v, new PointF(m.getX(0), m.getY(0)), velocity);
			}

            break;
		}

        m2screen.recycle();
    }
}
