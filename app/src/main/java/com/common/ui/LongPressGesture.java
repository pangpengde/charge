package com.common.ui;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class LongPressGesture extends ViewGesture {
	private MotionEvent mDownMotion = null;
	private MotionEvent mLastMotion = null;
	private DoCheckLongPress mPendingCheck = null;
	private boolean mCheckLongPress = false;

	@Override
	protected void doRestart(View v, boolean reset) {
		if (mDownMotion != null) {
			mDownMotion.recycle();
			mDownMotion = null;
		}

		if (mLastMotion != null) {
			mLastMotion.recycle();
			mLastMotion = null;
		}

		mPendingCheck = null;
		mCheckLongPress = false;
	}
	@Override
	protected void doDetect(View v, MotionEvent m, boolean delayed, ViewGesture.GestureListener listener) {
		// 判断是否需要探测手势
		if (listener instanceof GestureListener == false) {
			keepDetecting(false);
			mPendingCheck = null;
			return;
		}
		GestureListener longPresslistener = (GestureListener) listener;

		// 多指触控, 放弃探测.
		if (m.getPointerCount() > 1) {
			keepDetecting(false);
			mPendingCheck = null;
			return;
		}

		// 记录第一次下压动作
		if (mDownMotion == null && m.getActionMasked() == MotionEvent.ACTION_DOWN) {
			mDownMotion = MotionEvent.obtainNoHistory(m);
			mPendingCheck = new DoCheckLongPress(v);
			v.postDelayed(mPendingCheck, ViewConfiguration.getLongPressTimeout());
			return;
		}
		
		if (mDownMotion == null) {
			keepDetecting(false);
			mPendingCheck = null;
			return;
		}

		// 记录最后的动作
		if (mLastMotion != null) {
			mLastMotion.recycle();
			mLastMotion = null;
		}
		mLastMotion = MotionEvent.obtainNoHistory(m);
		
		PointF p0 = new PointF(mDownMotion.getRawX(), mDownMotion.getRawY());
		PointF p1 = new PointF(mLastMotion.getRawX(), mLastMotion.getRawY());
		
		// 判断是否溢出
		if (calcDistance(p0, p1) > getScaledTouchSlop(v)) {
			keepDetecting(false);
			mPendingCheck = null;
			return;
		}
		
		if (mCheckLongPress) {
			longPresslistener.onLongPress(v, new PointF(mLastMotion.getX(0), mLastMotion.getY(0)));
			keepDetecting(false);
			mPendingCheck = null;
		}

	}

	// ### 内嵌类 ###
	public static interface GestureListener extends ViewGesture.GestureListener {
		void onLongPress(View v, PointF pressPoint);
	}
	private class DoCheckLongPress implements Runnable {
		private final View mView;
		
		public DoCheckLongPress(View v) {
			mView = v;
		}
		
		@Override
		public void run() {
			if (mPendingCheck != this)
				return;
			mPendingCheck = null;
			
			final MotionEvent lastMotion = mLastMotion != null ? mLastMotion : mDownMotion;
			// 超时前, 已经终止探测, 但还没有重置.
			if (keepDetecting() == false || lastMotion == null 
					|| lastMotion.getActionMasked() == MotionEvent.ACTION_UP
					|| lastMotion.getActionMasked() == MotionEvent.ACTION_CANCEL)
				return;
				
			mCheckLongPress = true;
			MotionEvent m = MotionEvent.obtainNoHistory(lastMotion);
			m.setAction(MotionEvent.ACTION_MOVE);
			mView.dispatchTouchEvent(m);
			m.recycle();
			mCheckLongPress = false;
		}
	}
}
