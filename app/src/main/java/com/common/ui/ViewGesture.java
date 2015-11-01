package com.common.ui;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public abstract class ViewGesture {
	private boolean mKeepDetecting = true;
	private boolean mHoldDetecting = false;
	private boolean mSkipNextDetecting = false;
	private boolean mDelayTouchUp = false;
	private boolean mIsEnabled = true;
	
	// ### 内嵌类 ###
	public static interface GestureListener {
		void onTouchDown(View v, PointF downPoint);
		void onTouchUp(View v, PointF upPoint);
		void onTouchCancel(View v, PointF cancelPoint);
	}
	
	// ### 属性 ###
	public final boolean getIsEnabled() {
		return mIsEnabled;
	}
	public final void setIsEnabled(boolean isEnabled) {
		mIsEnabled = isEnabled;
	}
	
	// ### 方法 ###
	public final void restart(View v, boolean reset) {
		mKeepDetecting = true;
		mSkipNextDetecting = false;
		mHoldDetecting = false;
		mDelayTouchUp = false;
		doRestart(v, reset);
	}
	public final void detect(View v, MotionEvent m, boolean delayed, GestureListener listener) {
		detect(v, m, delayed, false, listener);
	}
	public final void detect(View v, MotionEvent m, boolean delayed, boolean intercept, GestureListener listener) {
		mSkipNextDetecting = false;
		if (mKeepDetecting == false)
			return;
		if (intercept) {
			doIntercept(v, m, delayed, listener);
		} else {
			doDetect(v, m, delayed, listener);
		}
	}
	public final boolean keepDetecting() {
		return mKeepDetecting;
	}
	protected final void keepDetecting(boolean keepDetecting) {
		mKeepDetecting = keepDetecting;
		
		if (mKeepDetecting == false) {
			mHoldDetecting = false;
			mDelayTouchUp = false;
		}
	}
	public final boolean holdDetecting() {
		return mHoldDetecting;
	}
	protected final void holdDetecting(boolean holdDetecting) {
		mHoldDetecting = holdDetecting;
		
		if (mHoldDetecting)
			mKeepDetecting = true;
	}
	public final boolean skipNextDetecting() {
		return mSkipNextDetecting;
	}
	protected final void skipNextDetecting(boolean skipNextDetecting) {
		mSkipNextDetecting = skipNextDetecting;
	}
	public final boolean delayTouchUp() {
		return mDelayTouchUp;
	}
	protected final void delayTouchUp(boolean delayTouchUp) {
		mDelayTouchUp = delayTouchUp;
	}
	
	// ### 抽象方法 ###
	protected abstract void doRestart(View v, boolean reset);
	protected abstract void doDetect(View v, MotionEvent m, boolean delayed, GestureListener listener);
	
	// ### 实现函数 ###
	protected void doIntercept(View v, MotionEvent m, boolean delayed, GestureListener listener) {
		
	}
	protected boolean isLineBetween(PointF p1, PointF p2, double minDegree, double maxDegree) {
		double angle = normalizeDegree(calcLineAngle(p1, p2), minDegree, minDegree + 360.0);
		if (Double.compare(angle, minDegree) >= 0 && Double.compare(angle, maxDegree) <= 0)
			return true;
		
		angle = normalizeDegree(180 - calcLineAngle(p1, p2), minDegree, minDegree + 360.0);
		if (Double.compare(angle, minDegree) >= 0 && Double.compare(angle, maxDegree) <= 0)
			return true;
		return false;
	}
	protected int normalizeDegree(int degree, int minDegree, int maxDegree) {
		return (int) normalizeDegree((double) degree, (double) minDegree, (double) maxDegree);
	}
	protected double normalizeDegree(double degree, double minDegree, double maxDegree) {
		assert minDegree < maxDegree;
		assert Double.compare(Math.abs(maxDegree - minDegree), 360) == 0;
		
		while (degree < minDegree || degree > maxDegree) {
			if (Double.compare(degree, minDegree) < 0)
				degree += 360.0;
			else
				degree -= 360.0;
		}
		
		return degree;
	}
	/**
	 * 计算视图坐标系下, 过(p1, p2)的直线与X轴正半轴逆时针方向的夹角(0-180).
	 * 
	 * @param p1 直线上一点.
	 * @param p2 直线上另一点.
	 * @return 角度夹角.
	 */
	protected double calcLineAngle(PointF p1, PointF p2) {
		double angle = calcAngle(p1, p2);
		if (Double.compare(angle, 180.0) > 0) {
			angle = angle - 180.0;
		}
		return angle;
	}
	/**
	 * 计算视图坐标系下, 向量(origin, point)与X轴正半轴逆时针方向的夹角(0-360).
	 * 
	 * @param origin 向量的原点.
	 * @param point  向量的另外一点.
	 * @return 角度夹角.
	 */
	protected double calcAngle(PointF origin, PointF point) {
		return Math.toDegrees(calcRadian(origin, point));
	}
	/**
	 * 计算视图坐标系下, 向量(origin, point)与X轴正半轴逆时针方向的夹角.
	 * 
	 * @param origin 向量的原点.
	 * @param point  向量的另外一点.
	 * @return 弧度夹角.
	 */
	protected double calcRadian(PointF origin, PointF point) {
		assert origin != null && point != null;
		
		// 变换为笛卡尔坐标
		PointF p0 = new PointF(origin.x, -origin.y);
		PointF p1 = new PointF(point.x, -point.y);
		
		if (p1.x == p0.x) {
	        if (p1.y > p0.y)
	            return Math.PI * 0.5;
	        else
	        	return Math.PI * 1.5;
		} else if (p1.y == p0.y) {
	        if (p1.x > p0.x)
	            return 0;
	        else
	        	return Math.PI;
		} else {
	        double atan = Math.atan((double) (p1.y - p0.y) / (p1.x - p0.x));
	        if ((p1.x < p0.x) && (p1.y > p0.y))
	            return atan + Math.PI;
	        else if ((p1.x < p0.x) && (p1.y < p0.y))
	            return atan + Math.PI;
	        else if ((p1.x > p0.x) && (p1.y < p0.y))
	            return atan + 2 * Math.PI;
	        else
	        	return atan;
	    }
	}
	protected double calcDistance(PointF p0, PointF p1) {
		return Math.sqrt(Math.pow(p0.x - p1.x, 2.0) + Math.pow(p0.y - p1.y, 2.0));
	}
	protected int getScaledMinFlingVelocity(View v) {
		return ViewConfiguration.get(v.getContext()).getScaledMinimumFlingVelocity();
	}
	protected int getScaledMaxFlingVelocity(View v) {
		return ViewConfiguration.get(v.getContext()).getScaledMaximumFlingVelocity();
	}
	protected int getLongPressTimeout() {
		return ViewConfiguration.getLongPressTimeout();
	}
	protected int getScaledTouchSlop(View v) {
		return ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
	}
	protected long getJumpTapTimeout() {
		return ViewConfiguration.getJumpTapTimeout();
	}
	protected int dip2px(View v, int dip) {
		return Math.round(v.getContext().getResources().getDisplayMetrics().density * dip); 

	}
}
