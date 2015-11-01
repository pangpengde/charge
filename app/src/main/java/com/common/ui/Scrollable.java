package com.common.ui;


import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Interpolator;

public interface Scrollable {
	// ### 接口属性 ###
	int getContentWidth();
	int getContentHeight();
	boolean getThumbEnabled();
	void setThumbEnabled(boolean enabled);
	boolean getSeekEnabled();
	void setSeekEnabled(boolean enabled);
	boolean canDragFling();
	void canDragFling(boolean can);
    boolean canVertDrag();
    void canVertDrag(boolean can);
    boolean canHorzDrag();
    void canHorzDrag(boolean can);
	int getHorizontalThumbMarginLeft();
	int getHorizontalThumbMarginTop();
	int getHorizontalThumbMarginRight();
	int getHorizontalThumbMarginBottom();
	void setHorizontalThumbMargin(int left, int top, int right, int bottom);
	int getVerticalThumbMarginLeft();
	int getVerticalThumbMarginTop();
	int getVerticalThumbMarginRight();
	int getVerticalThumbMarginBottom();
	void setVerticalThumbMargin(int left, int top, int right, int bottom);
	Drawable getHorizontalThumbDrawable();
	void setHorizontalThumbDrawable(Drawable drawable);
	Drawable getVerticalThumbDrawable();
	void setVerticalThumbDrawable(Drawable drawable);
	Drawable getHorizontalSeekDrawable();
	void setHorizontalSeekDrawable(Drawable drawable);
	Drawable getVerticalSeekDrawable();
	void setVerticalSeekDrawable(Drawable drawable);
	ViewGestureDetector getScrollDetector();
	ScrollState getScrollState();
	int getIdleTime();
	int getScrollTime();
	int getScrollFinalX();
	int getScrollFinalY();
	void setScrollInterpolator(Interpolator interpolator);
	void setScrollSensitive(View view, boolean sensitive);
	OverScrollMode getHorizontalOverScrollMode();
	void setHorizontalOverScrollMode(OverScrollMode mode);
	OverScrollMode getVerticalOverScrollMode();
	void setVerticalOverScrollMode(OverScrollMode mode);
	int getMaxOverScrollWidth();
	void setMaxOverScrollWidth(int width);
	int getMaxOverScrollHeight();
	void setMaxOverScrollHeight(int height);
	Rect getViewportBounds();
	Rect copyViewportBounds();
	void setOnScrollListener(OnScrollListener listener);
	
	// ### 接口方法 ###
	boolean canScrollHorizontally();
	boolean canScrollVertically();
	boolean canOverScrollHorizontally();
	boolean canOverScrollVertically();
	boolean reachesContentLeft();
	boolean reachesContentRight();
	boolean reachesContentTop();
	boolean reachesContentBottom();
	boolean isChildViewable(int index);
	void scrollSmoothly(float vx, float vy, final Runnable onFinish, final Runnable onCancel);
	void scrollSmoothlyTo(int x, int y, int duration, final Runnable onFinish, final Runnable onCancel);
	void scrollSmoothlyBy(int dx, int dy, int duration, final Runnable onFinish, final Runnable onCancel);
	void forceScrollTo(int x, int y);
	void forceScrollSmoothlyTo(int x, int y, int duration, final Runnable onFinish, final Runnable onCancel);
	void springBack();
	void springBackSmoothly();
	Point content2view(Point point);
	Rect content2view(Rect rect);
	Point view2content(Point point);
	Rect view2content(Rect rect);

	// ### 内嵌类 ###
	static enum ScrollState {
		IDLE,
		SEEK,
		DRAG,
		FLING,
		SMOOTH
	}
	static enum OverScrollMode {
		ALWAYS,
		AUTO,
		NEVER
	}
	static interface OnScrollListener {
		void onScrollStateChanged(Scrollable scrollable, ScrollState oldState, ScrollState newState);
		void onScroll(Scrollable scrollable, boolean viewportChanged);
	}
	static interface ScrollObserver extends OnScrollListener {
		
	}
}