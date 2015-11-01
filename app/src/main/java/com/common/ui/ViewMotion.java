package com.common.ui;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

public class ViewMotion {
	private View mView = null;
	private MotionEvent mEvent = null;
	private boolean mRecycle = false;
	
	public ViewMotion() {
		
	}
	public ViewMotion(View view, MotionEvent event) {
		mView = view;
		mEvent = event;
	}

	public void set(ViewMotion motion) {
		clear();
		
		if (motion != null) {
			mView = motion.mView;
			mEvent = motion.mEvent;
		}
	}
	public void copy(ViewMotion motion) {
		clear();
		
		if (motion != null) {
			mView = motion.mView;
			mEvent = MotionEvent.obtain(motion.mEvent);
		}
	}
	public void clear() {
		if (mEvent != null && mRecycle) {
			mEvent.recycle();
		}
		mView = null;
		mEvent = null;
		mRecycle = false;
	}
	public boolean isEmpty() {
		return mView == null;
	}
	public View getView() {
		return mView;
	}
	public int getActionMasked() {
		return mEvent.getActionMasked();
	}
	public int getPointerCount() {
		return mEvent.getPointerCount();
	}
	public int getPointerId(int pointerIndex) {
		return mEvent.getPointerId(pointerIndex);
	}
	public int findPointerIndex(int pointerId) {
		return mEvent.findPointerIndex(pointerId);
	}
	public float getScreenX(int pointerIndex) {
		final PointF p = UiUtils.tempPointFs.acquire();
		final float x = copyScreenCoord(pointerIndex, p).x;
		UiUtils.tempPointFs.release(p);
		return x;
	}
	public float getScreenY(int pointerIndex) {
		final PointF p = UiUtils.tempPointFs.acquire();
		final float y = copyScreenCoord(pointerIndex, p).y;
		UiUtils.tempPointFs.release(p);
		return y;
	}
	public PointF copyScreenCoord(int pointerIndex, PointF out) {
		out.set(mEvent.getX(pointerIndex) + mView.getScrollX(), mEvent.getY(pointerIndex) + mView.getScrollY());
		UiUtils.transformPointToScreen(out, mView);
		return out;
	}
	public PointF transformPointFromScreen(PointF point) {
		UiUtils.transformPointFromScreen(point, mView);
		point.offset(-mView.getScrollX(), -mView.getScrollY()); // 转换到视口坐标
		return point;
	}
	public PointF transformOffsetFromScreen(PointF offset) {
		UiUtils.transformOffsetFromScreen(offset, mView);
		return offset;
	}

}
