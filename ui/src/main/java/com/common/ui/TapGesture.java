package com.common.ui;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class TapGesture extends ViewGesture {
	private MotionEvent mFirstMotion = null;
	
	// ### 内嵌类 ###
	public static interface GestureListener extends ViewGesture.GestureListener {
		void onTap(ViewGesture g, View v, PointF tapPoint);
	}

	@Override
	protected void doRestart(View v, boolean reset) {
		setFirstMotion(null);
	}
	@Override
	protected void doDetect(View v, MotionEvent m, boolean delayed, ViewGesture.GestureListener listener) {
		// 判断是否需要探测手势
		if (listener instanceof GestureListener == false) {
			keepDetecting(false);
			return;
		}
		GestureListener tapListener = (GestureListener) listener;

		if (mFirstMotion == null) {
			if (m.getPointerCount() == 1)
				setFirstMotion(m);
			return;
		}
		
		if (m.getPointerCount() > 1) {
			keepDetecting(false);
			return;
		}
		
		if (m.getEventTime() - mFirstMotion.getEventTime() > ViewConfiguration.getJumpTapTimeout()) {
			keepDetecting(false);
			return;
		}
		
		PointF firstPoint = new PointF(mFirstMotion.getX(), mFirstMotion.getY());
		PointF currentPoint = new PointF(m.getX(), m.getY());
		if (calcDistance(firstPoint, currentPoint) > getScaledTouchSlop(v)) {
			keepDetecting(false);
			return;
		}
		
		if (m.getAction() == MotionEvent.ACTION_UP) {
			tapListener.onTap(this, v, currentPoint);
		}
	}
	private void setFirstMotion(MotionEvent m) {
		if (mFirstMotion != null) {
			mFirstMotion.recycle();
			mFirstMotion = null;
		}
		
		if (m != null) {
			mFirstMotion = MotionEvent.obtain(m);
		}
	}
}
