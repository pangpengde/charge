package com.common.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class HorzLineDrawable extends Drawable {
	private final Paint mPaint = new Paint();
	private float mHeight = 1.0f;
	private int mLeftPadding = 0;
	private int mRightPadding = 0;
	private int mColor = Color.TRANSPARENT;
	
	// ### 构造函数 ###
	public HorzLineDrawable(int color) {
		mColor = color;
	}
	
	// ### 属性 ###
	public void setHorzPadding(int leftPadding, int rightPadding) {
		mLeftPadding = leftPadding;
		mRightPadding = rightPadding;
	}
	public int getHeight() {
		return (int) mHeight;
	}
	public void setHeight(int height) {
		mHeight = height;
	}
	public int getColor() {
		return mColor;
	}
	public void setColor(int color) {
		mColor = color;
		invalidateSelf();
	}
	
	// ### 重写函数 ###
	@Override
	public void draw(Canvas canvas) {
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(mHeight);
		mPaint.setColor(mColor);
		mPaint.setAntiAlias(true);
		
		final Rect bounds = getBounds();
		canvas.drawLine(bounds.left + mLeftPadding, bounds.top + mHeight / 2.0f, bounds.right - mRightPadding, bounds.top + mHeight / 2.0f, mPaint);
	}
	@Override
	public int getIntrinsicHeight() {
		return (int) mHeight;
	}
	@Override
	public int getIntrinsicWidth() {
		return -1;
	}
	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
	@Override
	public void setAlpha(int alpha) {
		mPaint.setAlpha(alpha);
	}
	@Override
	public void setColorFilter(ColorFilter cf) {
		mPaint.setColorFilter(cf);
	}

}
