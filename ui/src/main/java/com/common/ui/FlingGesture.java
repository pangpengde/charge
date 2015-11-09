package com.common.ui;

import java.util.LinkedList;

import android.graphics.PointF;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

public class FlingGesture extends ViewGesture {
	private final TranslateGesture mTranslateGesture = new TranslateGesture();
	private long mSampleInterval = 300;
	private LinkedList<Pair<PointF, Long>> mSampleList = new LinkedList<Pair<PointF, Long>>();
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
		mTranslateGesture.restart(v, reset || !mTranslateGesture.keepDetecting());
		mSampleList.clear();
	}
	@Override
	protected void doDetect(View v, final MotionEvent m, boolean delayed, ViewGesture.GestureListener listener) {
		// 判断是否需要探测手势
		if (listener instanceof GestureListener == false) {
			keepDetecting(false);
			return;
		}
		GestureListener flingListener = (GestureListener) listener;

		mTranslateGesture.detect(v, m, delayed, new TranslateGesture.GestureListener() {
			@Override
			public void onTouchUp(View v, PointF point) {

			}
			@Override
			public void onTouchDown(View v, PointF point) {
				
			}
			@Override
			public void onTouchCancel(View v, PointF point) {
				
			}
			@Override
			public void onTranslate(ViewGesture g, View v, PointF origin, PointF translation) {
				long curTime = m.getEventTime();
				mSampleList.addLast(new Pair<PointF, Long>(translation, curTime));
			}
		});
		
		if (m.getActionMasked() == MotionEvent.ACTION_UP && mSampleList.isEmpty() == false) {
			PointF abs = new PointF(0.0f, 0.0f);
			PointF total = new PointF(0.0f, 0.0f);
			long beginTime = 0;
			long endTime = m.getEventTime();
			for (Pair<PointF, Long> sample : mSampleList) {
				long interval = endTime - sample.second;
				if (interval > mSampleInterval) {
					continue;
				}
				
				if (beginTime == 0)
					beginTime = sample.second;
				
				abs.x += Math.abs(sample.first.x);
				abs.y += Math.abs(sample.first.y);
				total.x += sample.first.x;
				total.y += sample.first.y;
			}
			
			if (Math.abs(total.x) < UiUtils.getScaledTouchSlop(v.getContext())
					&& Math.abs(total.y) < UiUtils.getScaledTouchSlop(v.getContext()))
				return;
			
			float interval = (endTime - beginTime) / 1000.0f;
			PointF velocity = new PointF(abs.x / interval, abs.y / interval);
			float minVelocity = Float.isNaN(mMinVelocity) ? UiUtils.getScaledMinFlingVelocity(v.getContext()) : mMinVelocity;
			float maxVelocity = Float.isNaN(mMaxVelocity) ? UiUtils.getScaledMaxFlingVelocity(v.getContext()) : mMaxVelocity;
			if (velocity.x < minVelocity)
				velocity.x = 0.0f;
			if (velocity.y < minVelocity)
				velocity.y = 0.0f;
			velocity.x = Math.min(velocity.x, maxVelocity);
			velocity.y = Math.min(velocity.y, maxVelocity);
			
			if (Float.compare(total.x, 0.0f) < 0)
				velocity.x = -velocity.x;
			if (Float.compare(total.y, 0.0f) < 0)
				velocity.y = -velocity.y;
			
			if (UiUtils.isLineBetween(new PointF(0.0f, 0.0f), total, mMinAngle, mMaxAngle) == false)
				return;
			
			if (Float.compare(Math.abs(velocity.x), 0.0f) != 0 || Float.compare(Math.abs(velocity.y), 0.0f) != 0) {
				flingListener.onFling(this, v, new PointF(m.getX(0), m.getY(0)), velocity);
			}

		}
	}
}
