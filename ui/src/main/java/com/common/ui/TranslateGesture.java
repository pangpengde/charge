package com.common.ui;


import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class TranslateGesture extends ViewGesture {
	private final ViewMotion mLastMotion = new ViewMotion();
	private float mMinStep = 1.0f;
	private float mMinAngle = 0.0f;
	private float mMaxAngle = 180.0f;
	private int mTranslateSlop = 0;
	private int mDetectOrder = 0;
	private int mDetectedCount = -1;

	// ### 内嵌类 ###
	public static interface GestureListener extends ViewGesture.GestureListener {
		void onTranslate(ViewGesture g, View v, PointF origin, PointF translation);
	}

	// ### 构造函数 ###
	public TranslateGesture() {
		this(1.0f);
	}
	public TranslateGesture(float minStep) {
		mMinStep = minStep;
	}
	
	// ### 方法 ###
	public float getMaxAngle() {
		return mMaxAngle;
	}
	public float getMinAngle() {
		return mMinAngle;
	}
	public void setMinStep(float minStep) {
		mMinStep = minStep;
	}
	public void setMinAngle(float minAngle) {
		mMinAngle = minAngle;
	}
	public void setMaxAngle(float maxAngle) {
		mMaxAngle = maxAngle;
	}
	public void setTranslateSlop(int slop) {
		mTranslateSlop = slop;
	}
	public void setDetectOrder(int order) {
		mDetectOrder = Math.max(0, order);
	}
    public int getTouchPointCount() {
        return mLastMotion.getPointerCount();
    }

	// ### ViewGesture抽象方法实现 ###
	@Override
	protected void doRestart(View v, boolean reset) {
		setLastMotion(null);
		mDetectedCount = -1;
	}
	@Override
	protected void doDetect(View view, MotionEvent event, boolean delayed, ViewGesture.GestureListener listener) {
		// TODO(by lizhan@duokan.com): doDetect参数需要调整为ViewMotion
		final ViewMotion m = new ViewMotion(view, event);
		
		// 判断是否需要探测手势
		if (listener instanceof GestureListener == false) {
			keepDetecting(false);
			return;
		}
		GestureListener translateListener = (GestureListener) listener;

		if (m.getActionMasked() != MotionEvent.ACTION_MOVE) {
			setLastMotion(null);
			return;
		}

		if (mLastMotion.isEmpty()) {
			setLastMotion(m);
			return;
		}

		if (m.getPointerCount() > 2 || m.getPointerCount() != mLastMotion.getPointerCount()) {
			setLastMotion(m);
			return;
		}
		
		if (m.getPointerCount() == 1) {
			PointF origin = mLastMotion.copyScreenCoords(0, new PointF());
			PointF translation = m.copyScreenCoords(0, new PointF());
			translation.offset(-origin.x, -origin.y);

			if (mDetectedCount < 0) {
				if (Double.compare(Math.pow(translation.x, 2) + Math.pow(translation.y, 2), Math.pow(mTranslateSlop, 2)) >= 0) {
					m.transformPointFromScreen(origin);
					m.transformOffsetFromScreen(translation);
					if (isLineBetween(new PointF(0.0f, 0.0f), translation, mMinAngle, mMaxAngle)) {
						mDetectedCount++;
					}					
					setLastMotion(m);
				}
				return;
			}
			
			if (++mDetectedCount > mDetectOrder) {
				if (Double.compare(Math.pow(translation.x, 2) + Math.pow(translation.y, 2), Math.pow(mMinStep, 2)) >= 0) {
					m.transformPointFromScreen(origin);
					m.transformOffsetFromScreen(translation);
					translateListener.onTranslate(this, m.getView(), origin, translation);
					setLastMotion(m);
				}
			}
			
		} else {
			final int pi10 = m.findPointerIndex(mLastMotion.getPointerId(0));
			final int pi11 = m.findPointerIndex(mLastMotion.getPointerId(1));
			if (pi10 < 0 || pi11 < 0) {
				keepDetecting(false);
				return;
			}
			
			PointF p00 = mLastMotion.copyScreenCoords(0, new PointF());
			PointF p01 = mLastMotion.copyScreenCoords(1, new PointF()); 
			PointF p10 = m.copyScreenCoords(pi10, new PointF());
			PointF p11 = m.copyScreenCoords(pi11, new PointF());
			
			double a0 = calcAngle(p00, p01); 
			double a1 = calcAngle(p10, p11);
			double d0 = calcDistance(p00, p01);
			double d1 = calcDistance(p10, p11);
			
			if (Math.abs(a1 - a0) > 10 || Math.abs(d1 - d0) > getScaledTouchSlop(m.getView())) {
				setLastMotion(m);
				return;
			}
			
			PointF c0 = new PointF((p00.x + p01.x) / 2.0f, (p00.y + p01.y) / 2.0f);
			PointF c1 = new PointF((p10.x + p11.x) / 2.0f, (p10.y + p11.y) / 2.0f);
			
			PointF origin = c0;
			PointF translation = new PointF(c1.x - c0.x, c1.y - c0.y);

			if (mDetectedCount < 0) {
				if (Double.compare(Math.pow(translation.x, 2) + Math.pow(translation.y, 2), Math.pow(mTranslateSlop, 2)) >= 0) {
					m.transformPointFromScreen(origin);
					m.transformOffsetFromScreen(translation);
					if (isLineBetween(new PointF(0.0f, 0.0f), translation, mMinAngle, mMaxAngle)) {
						mDetectedCount++;
					}					
					setLastMotion(m);
				}
				return;
			}

			if (++mDetectedCount > mDetectOrder) {
				if (Double.compare(Math.pow(translation.x, 2) + Math.pow(translation.y, 2), Math.pow(mMinStep, 2)) >= 0) {
					m.transformPointFromScreen(origin);
					m.transformOffsetFromScreen(translation);
					translateListener.onTranslate(this, m.getView(), origin, translation);
					setLastMotion(m);
				}
			}
		}
	}
	private void setLastMotion(ViewMotion m) {
		mLastMotion.set(m);
	}
}
