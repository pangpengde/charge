package com.common.ui;

import java.util.LinkedList;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;


public class ViewGestureDetector implements OnTouchListener {
	private final LinkedList<ViewGesture> mGestureList = new LinkedList<ViewGesture>();
	private ViewGesture.GestureListener mGestureListener = null;
	private ViewGesture mHoldDetectingGesture = null;
	private boolean mIsEnabled = true;
	private boolean mDelayTouchUp = false;
	private DelayedTouchUp mDelayedTouchUp = null;
	
	// ### 构造函数 ###
	public ViewGestureDetector() {

	}

	// ### 属性 ###
	public boolean getIsEnabled() {
		return mIsEnabled;
	}
	public void setIsEnabled(boolean isEnabled) {
		mIsEnabled = isEnabled;
	}
	public void setGestureListener(ViewGesture.GestureListener listener) {
		mGestureListener = listener;
	}

	// ### 方法 ###
	public ViewGesture[] findGestures(Class<?>... classes) {
		LinkedList<ViewGesture> foundList = new LinkedList<ViewGesture>();
		for (ViewGesture g : mGestureList) {
			for (Class<?> c : classes) {
				if (c.isInstance(g)) {
					foundList.add(g);
				}
			}
		}
		return foundList.toArray(new ViewGesture[0]);
	}
	public ViewGesture[] enableAllGesturesExcept(ViewGesture... gestures) {
		LinkedList<ViewGesture> lastEnabledList = new LinkedList<ViewGesture>(); 
		for (ViewGesture g : mGestureList) {
			if (g.getIsEnabled()) {
				lastEnabledList.addLast(g);
			}
			g.setIsEnabled(true);
		}

		for (ViewGesture g : gestures) {
			g.setIsEnabled(false);
		}
		
		return lastEnabledList.toArray(new ViewGesture[0]);
	}
	public ViewGesture[] disableAllGesturesExcept(ViewGesture... gestures) {
		LinkedList<ViewGesture> lastDisabledList = new LinkedList<ViewGesture>(); 
		for (ViewGesture g : mGestureList) {
			if (g.getIsEnabled() == false) {
				lastDisabledList.addLast(g);
			}
			g.setIsEnabled(false);
		}

		for (ViewGesture g : gestures) {
			g.setIsEnabled(true);
		}
		
		return lastDisabledList.toArray(new ViewGesture[0]);
	}
	public void pushGesture(ViewGesture gesture) {
		assert gesture != null;

		mGestureList.addFirst(gesture);
	}
	public void reset(View v) {
		resetAllGestures(v);
		mDelayTouchUp = false;
		mDelayedTouchUp = null;
		mHoldDetectingGesture = null;
	}
	public void attach(View v) {
		v.setOnTouchListener(this);
	}
	public void detach(View v) {
		v.setOnTouchListener(null);
	}
	public ViewGesture getHoldDetectingGesture() {
		return mHoldDetectingGesture;
	}

	// ### OnTouchListener接口实现 ###
	@Override
	public boolean onTouch(View v, MotionEvent m) {
		return onTouch(v, m, false, false);
	}
	
	public boolean onIntercept(View v, MotionEvent m) {
		return onTouch(v, m, false, true);
	}
	

	// ### 实现函数 ###
	private boolean onTouch(View v, MotionEvent m, boolean delayed, boolean intercept) {
		if (mIsEnabled == false) {
			reset(v);
			return false;
		}

		if (m.getActionMasked() == MotionEvent.ACTION_DOWN) {
			if (mDelayedTouchUp != null) {
				mDelayedTouchUp.run();
				assert mDelayedTouchUp == null;
			}
			
			restartAllGestures(v);
			if (mGestureListener != null)
				mGestureListener.onTouchDown(v, new PointF(m.getX(), m.getY()));
			
		} else if (m.getActionMasked() == MotionEvent.ACTION_CANCEL) {
			if (mGestureListener != null)
				mGestureListener.onTouchCancel(v, new PointF(m.getX(), m.getY()));
			
			reset(v);
			return false;
		} else if (m.getActionMasked() == MotionEvent.ACTION_UP) {
			if (mDelayTouchUp && mDelayedTouchUp == null) {
				mDelayedTouchUp = new DelayedTouchUp(v, m, intercept);
				v.postDelayed(mDelayedTouchUp, ViewConfiguration.getDoubleTapTimeout());
				return false;
			}
		}
		
		mDelayTouchUp = false;
		final boolean handled = detectGesture(v, m, delayed, intercept, mGestureListener);
		if (m.getAction() == MotionEvent.ACTION_UP) {
			if (mGestureListener != null)
				mGestureListener.onTouchUp(v, new PointF(m.getX(), m.getY()));

			restartAllGestures(v);
		}
		return handled;
	}
	private boolean detectGesture(View v, MotionEvent m, boolean delayed, boolean intercept, ViewGesture.GestureListener listener) {
		boolean anyHolding = false;
		boolean anyDetected = false;
		
		// 处理需要持续探测的手势
		while (mHoldDetectingGesture != null) {
			if (mHoldDetectingGesture.getIsEnabled() == false) {
				mHoldDetectingGesture = null;
				break;
			}

			if (mHoldDetectingGesture.holdDetecting() == false) {
				mHoldDetectingGesture = null;
				break;
			}

			anyHolding = true;
			mHoldDetectingGesture.detect(v, m, delayed, intercept, listener);
			mDelayTouchUp = mHoldDetectingGesture.delayTouchUp();
			anyDetected = true;

			if (mHoldDetectingGesture.holdDetecting() == false) {
				mHoldDetectingGesture = null;
				restartAllGestures(v);
			}

			if (intercept)
				return anyHolding;
			else
				return anyDetected;
		}

		// 探测所有手势
		for (ViewGesture gesture : mGestureList) {
			assert gesture != null;

			if (gesture.getIsEnabled() == false)
				continue;

			if (gesture.keepDetecting()) {
				gesture.detect(v, m, delayed, intercept, listener);
				mDelayTouchUp |= gesture.delayTouchUp();
				anyDetected = true;
			}

			if (gesture.holdDetecting()) {
				mHoldDetectingGesture = gesture;
				resetAllGesturesExcept(v, mHoldDetectingGesture);
				anyHolding = true;
				break;
			}

			if (gesture.skipNextDetecting()) {
				break;
			}
		}
		
		if (intercept)
			return anyHolding;
		else
			return anyDetected;
	}
	private void restartAllGestures(View v) {
		for (ViewGesture g : mGestureList) {
			g.restart(v, !g.keepDetecting());
		}
	}
	private void resetAllGestures(View v) {
		resetAllGesturesExcept(v, null);
	}
	private void resetAllGesturesExcept(View v, ViewGesture excludedGesture) {
		for (ViewGesture gesture : mGestureList) {
			if (gesture == excludedGesture)
				continue;

			gesture.restart(v, true);
		}
	}
	
	// ### 内嵌类 ###
	private class DelayedTouchUp implements Runnable {
		private final View mView;
		private MotionEvent mMotion;
		private final boolean mIntercept;
		
		public DelayedTouchUp(View v, MotionEvent m, boolean intercept) {
			mView = v;
			mIntercept = intercept;
			mMotion = MotionEvent.obtain(m);
		}
		
		@Override
		public void run() {
			if (mDelayedTouchUp == this) {
				mDelayTouchUp = false;
				mDelayedTouchUp = null;
				onTouch(mView, mMotion, true, mIntercept);
			}

			if (mMotion != null) {
				mMotion.recycle();
				mMotion = null;
			}
		}
	}
}
