package com.common.ui;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;

public class ViewMotion {
	private View mView = null;
	private int mActionMasked = 0;
	private final LinkedList<Pointer> mPointerList = new LinkedList<Pointer>();
	
	public ViewMotion() {
		
	}
	public ViewMotion(View view, MotionEvent event) {
		mView = view;
		mActionMasked = event.getActionMasked();
		
		for (int n = 0; n < event.getPointerCount(); ++n) {
			final Pointer pointer = new Pointer();
			pointer.id = event.getPointerId(n);
			pointer.screenCoords.x = event.getX(n) + mView.getScrollX();
			pointer.screenCoords.y = event.getY(n) + mView.getScrollY();
			UiUtils.transformPointToScreen(pointer.screenCoords, mView);
			mPointerList.add(pointer);
		}
	}

	public void clear() {
		mView = null;
		mActionMasked = 0;
		mPointerList.clear();
	}
	public void set(ViewMotion motion) {
		clear();
		
		if (motion != null) {
			mView = motion.mView;
			mActionMasked = motion.mActionMasked;
			mPointerList.addAll(motion.mPointerList);
		}
	}
	public boolean isEmpty() {
		return mView == null;
	}
	public View getView() {
		return mView;
	}
	public int getActionMasked() {
		return mActionMasked;
	}
	public int getPointerCount() {
		return mPointerList.size();
	}
	public int getPointerId(int pointerIndex) {
		return mPointerList.get(pointerIndex).id;
	}
	public int findPointerIndex(int pointerId) {
		for (int n = 0; n < mPointerList.size(); ++n) {
			if (mPointerList.get(n).id == pointerId)
				return n;
		}
		return -1;
	}
	public float getScreenX(int pointerIndex) {
		final Pointer pointer = mPointerList.get(pointerIndex);
		return pointer.screenCoords.x;
	}
	public float getScreenY(int pointerIndex) {
		final Pointer pointer = mPointerList.get(pointerIndex);
		return pointer.screenCoords.y;
	}
	public PointF copyScreenCoords(int pointerIndex, PointF out) {
		final Pointer pointer = mPointerList.get(pointerIndex);
		out.set(pointer.screenCoords.x, pointer.screenCoords.y);
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

	private static class Pointer {
		public int id = 0;
		public final PointF screenCoords = new PointF();
	}
}
