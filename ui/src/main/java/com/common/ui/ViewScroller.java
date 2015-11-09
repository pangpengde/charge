package com.common.ui;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.ListIterator;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;

import com.common.sys.MainThread;

public abstract class ViewScroller implements Scrollable {
	// ### 值域 ###
	private static final int THUMB_MIN_WIDTH = 5; // dip
	private static final int THUMB_MIN_HEIGHT = 5; // dip
	private static final int THUMB_KEEP_VISIBLE = 1000;
	private static final int THUMB_TO_INVISIBLE = 200;
	private static final int THUMB_DEFAULT = 0;
	private static final int THUMB_SEEK = 1;
	private final ViewGroup mScrollableView;
	private final OverScroller mScroller;
	private final ViewGestureDetector mScrollDetector = new ViewGestureDetector();
	private final ViewGestureDetector mClickDetector = new ViewGestureDetector();
	private final TapGesture mClickGesture = new TapGesture();
	private final LongPressGesture mLongPressGesture = new LongPressGesture();
	private final Rect mOldViewportBounds = new Rect(0, 0, 0, 0);
	private final Rect mViewportBounds = new Rect(0, 0, 0, 0);
	private final RectF mViewportBoundsF = new RectF(0, 0, 0, 0);
	private final LinkedList<WeakReference<View>> mScrollSensitiveList = new LinkedList<WeakReference<View>>();
	private final LinkedList<ScrollObserver> mScrollObserverList = new LinkedList<ScrollObserver>();
	private final Rect mContentBounds = new Rect();
	private OverScrollMode mHorzOverScrollMode = OverScrollMode.ALWAYS;
	private OverScrollMode mVertOverScrollMode = OverScrollMode.ALWAYS;
	private int mMaxOverScrollWidth = 0;
	private int mMaxOverScrollHeight = 0;
	private ScrollState mScrollState = ScrollState.IDLE;
	private boolean mHorzSeeking = false;
	private boolean mVertSeeking = false;
	private boolean mHorzDragging = false;
	private boolean mVertDragging = false;
	private boolean mCanDragFling = true;
    private boolean mCanVertDrag = true;
    private boolean mCanHorzDrag = true;
	private DoSlide mRunningSlide = null;
	private long mIdleStartTime = System.currentTimeMillis();
	private long mScrollStartTime = 0;
	private Runnable mPendingScrollAfterLayout = null;
	private OnScrollListener mOnScrollListener = null;

	// ### 滚动条相关值域 ###
	private boolean mSeekEnabled = false;
	private boolean mThumbEnabled = false;
	private final Rect mVertThumbBounds = new Rect();
	private final Rect mVertThumbMargin = new Rect();
	private final Drawable[] mVertThumbDrawable = new Drawable[2];
	private final Rect mHorzThumbBounds = new Rect();
	private final Rect mHorzThumbMargin = new Rect();
	private final Drawable[] mHorzThumbDrawable = new Drawable[2];
	
	// ### 构造函数 ###
	protected ViewScroller(ViewGroup view) {
		mScrollableView = view;
		mScroller = new OverScroller(mScrollableView.getContext());
		mScrollDetector.pushGesture(new ScrollGesture());

		final DisplayMetrics dm = mScrollableView.getResources().getDisplayMetrics();
		mOldViewportBounds.set(0, 0, dm.widthPixels, dm.heightPixels);
		mViewportBounds.set(mOldViewportBounds);
		mViewportBoundsF.set(mOldViewportBounds);

		// TODO 图标
		mVertThumbMargin.set(0, UiUtils.dip2px(mScrollableView.getContext(), 2), UiUtils.dip2px(mScrollableView.getContext(), 2), UiUtils.dip2px(mScrollableView.getContext(), 6));
//		mVertThumbDrawable[THUMB_DEFAULT] = mScrollableView.getResources().getDrawable(R.drawable.general__shared__thumb_default_vert);
//		mVertThumbDrawable[THUMB_SEEK] = mScrollableView.getResources().getDrawable(R.drawable.general__shared__thumb_seek_vert);
		
		mHorzThumbMargin.set(UiUtils.dip2px(mScrollableView.getContext(), 2), 0, UiUtils.dip2px(mScrollableView.getContext(), 6), UiUtils.dip2px(mScrollableView.getContext(), 2));
//		mHorzThumbDrawable[THUMB_DEFAULT] = mScrollableView.getResources().getDrawable(R.drawable.general__shared__thumb_default_horz);
		
		mClickDetector.pushGesture(mClickGesture);
		mClickDetector.pushGesture(mLongPressGesture);
		mClickDetector.setGestureListener(new ClickListener());
	}
	
	// ### 方法 ###
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (mScrollableView.isEnabled() == false)
			return false;

		return mScrollDetector.onIntercept(mScrollableView, ev);
	}
	public boolean onTouchEvent(MotionEvent event) {
		if (mScrollableView.isEnabled() == false)
			return false;
		
		mScrollDetector.onTouch(mScrollableView, event);
		if (mScrollState == ScrollState.IDLE) {
			mClickDetector.onTouch(mScrollableView, event);
		} else {
			mClickDetector.reset(mScrollableView);
		}

		return true;
	}
	public void afterOnAttachedToWindow() {
		for (ViewParent parent = mScrollableView.getParent(); parent != null; parent = parent.getParent()) {
			if (parent instanceof ScrollObserver == false)
				continue;
			
			mScrollObserverList.add((ScrollObserver) parent);
		}
	}
	public void afterOnDetachedFromWindow() {
		abortRunningSlide();
		setScrollState(ScrollState.IDLE);
		mScrollObserverList.clear();
	}
	public void afterOnLayout(boolean changed, int l, int t, int r, int b) {
		if (changed) {
			// ATTENTION(by lizhan@duokan.com):
			// 此处调用的目的是保持当前视口位置, 更新视口尺寸.
			doScrollBy(0.0f, 0.0f);
		}
		
		// 布局彻底结束后, 修正视口位置.
		if (mPendingScrollAfterLayout == null) {
			mPendingScrollAfterLayout = new Runnable() {
				@Override
				public void run() {
					mPendingScrollAfterLayout = null;
					doRestrictScrollBy(0, 0);
				}
			};
			
			UiUtils.runAfterLayout(mScrollableView, mPendingScrollAfterLayout);
		}
	}
	public void afterDraw(Canvas canvas) {
		if (mThumbEnabled == false)
			return;
		
		final int alpha = alphaOfThumb();
		if (mHorzThumbBounds.isEmpty() == false) {
			final Drawable horzDrawable = horzThumbDrawable();
			
            canvas.save();
            canvas.translate(mViewportBounds.left, mViewportBounds.top);
			canvas.clipRect(mHorzThumbBounds);
			horzDrawable.setBounds(mHorzThumbBounds);
			horzDrawable.setAlpha(alpha);
			horzDrawable.draw(canvas);
			canvas.restore();
		}

		if (mVertThumbBounds.isEmpty() == false) {
			final Drawable vertDrawable = vertThumbDrawable();

			canvas.save();
            canvas.translate(mViewportBounds.left, mViewportBounds.top);
			canvas.clipRect(mVertThumbBounds);
			vertDrawable.setBounds(mVertThumbBounds);
			vertDrawable.setAlpha(alpha);
			vertDrawable.draw(canvas);
			canvas.restore();
		}
		
		if (alpha > 0) {
			mScrollableView.invalidate();
		}
	}
	public void afterRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
		if (disallowIntercept) {
			mScrollDetector.reset(mScrollableView);
		}
	}
	public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
		final Rect visibleBounds = getVisibleBoundsOnScreen();
		final boolean requestParent;
		if (visibleBounds.isEmpty()) {
			visibleBounds.set(getViewportBounds());
			requestParent = true;
		} else {
			requestParent = false;
		}
		
		if (visibleBounds.isEmpty())
			return true;
		
		final Rect requestRect = new Rect(rectangle);
		UiUtils.transformRect(requestRect, child, mScrollableView);
		
		final int deltaX, deltaY;
		if (requestRect.intersect(mContentBounds)) {
			deltaX = calcVisibleDeltaXToFitRequest(visibleBounds, requestRect);
			deltaY = calcVisibleDeltaYToFitRequest(visibleBounds, requestRect);
		} else {
			deltaX = 0;
			deltaY = 0;
		}
		
		if (deltaX != 0 || deltaY != 0) {
			if (immediate) {
				scrollBy(deltaX, deltaY);
			} else {
				scrollSmoothlyBy(deltaX, deltaY, UiUtils.ANIM_DURATION_SHORT, null, null);
			}
		}
		
		return !requestParent;
	}
	public boolean requestChildOnScreen(View child, boolean immediate) {
		return requestChildRectangleOnScreen(child, new Rect(0, 0, child.getWidth(), child.getHeight()), immediate);
	}
	public Rect getVisibleBoundsOnScreen() {
		final Rect visibleBounds = new Rect();
		UiUtils.getViewBounds(visibleBounds, mScrollableView.getRootView(), mScrollableView);
		visibleBounds.intersect(getViewportBounds());
		return visibleBounds;
	}
	public boolean isThumbVisible() {
		return alphaOfThumb() > 0;
	}
	public boolean isHorizontalFadingEdgeEnabled() {
		return false;
	}
	public boolean isHorizontalScrollBarEnabled() {
		return false;
	}
	public boolean isVerticalFadingEdgeEnabled() {
		return false;
	}
	public boolean isVerticalScrollBarEnabled() {
		return false;
	}
	public void setContentDimension(int width, int height) {
		setContentBounds(mContentBounds.left, mContentBounds.top, mContentBounds.left + width, mContentBounds.top + height);
	}
	public void setContentWidth(int width) {
		setContentBounds(mContentBounds.left, mContentBounds.top, mContentBounds.left + width, mContentBounds.bottom);
	}
	public void setContentHeight(int height) {
		setContentBounds(mContentBounds.left, mContentBounds.top, mContentBounds.right, mContentBounds.top + height);
	}
	public Rect getContentBounds() {
		return mContentBounds;
	}
	public void setContentBounds(Rect bounds) {
		setContentBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
	}
	public void setContentBounds(int left, int top, int right, int bottom) {
		if (mContentBounds.left == left && mContentBounds.top == top 
				&& mContentBounds.right == right && mContentBounds.bottom == bottom)
			return;
		
		mContentBounds.set(left, top, right, bottom);
		
		if (mScrollState == ScrollState.FLING) {
			mScroller.fling(
					mViewportBounds.left, mViewportBounds.top, 
					Math.round(Math.signum(mScroller.getCurrVelocityX())), Math.round(Math.signum(mScroller.getCurrVelocityY())), 
					minScrollX(), maxScrollX(), minScrollY(), maxScrollY(),
					maxOverScrollWidth(), maxOverScrollHeight()
			);
		} else {
			// TODO
		}
	}
	public void scrollBy(int dx, int dy) {
		abortRunningSlide();

		setScrollState(ScrollState.IDLE);
		scrollBy((float) dx, (float) dy);
	}
	public void scrollTo(int x, int y) {
		abortRunningSlide();

		setScrollState(ScrollState.IDLE);
		scrollTo((float) x, (float) y);
	}	
	public boolean shouldDelayChildPressedState() {
		return true;
	}
	public int computeHorizontalScrollExtent() {
		int visibleLeft = Math.max(mContentBounds.left, mViewportBounds.left);
		int visibleRight = Math.min(mViewportBounds.right, mContentBounds.right);
		int extent = Math.max(0, visibleRight - visibleLeft);
		return extent;
	}
	public int computeHorizontalScrollOffset() {
		int offset = Math.max(0, Math.min(mViewportBounds.left - mContentBounds.left, mContentBounds.width()));
		return offset;
	}
	public int computeHorizontalScrollRange() {
		return mContentBounds.width();
	}
	public int computeVerticalScrollExtent() {
		int visibleTop = Math.max(mContentBounds.top, mViewportBounds.top);
		int visibleBottom = Math.min(mViewportBounds.bottom, mContentBounds.bottom);
		int extent = Math.max(0, visibleBottom - visibleTop);
		return extent;
	}
	public int computeVerticalScrollOffset() {
		int offset = Math.max(0, Math.min(mViewportBounds.top - mContentBounds.top, mContentBounds.height()));
		return offset;
	}
	public int computeVerticalScrollRange() {
		return mContentBounds.height();
	}

	// ### Scrollable接口实现 ###
	@Override
	public int getContentWidth() {
		return mContentBounds.width();
	}
	@Override
	public int getContentHeight() {
		return mContentBounds.height();
	}
	@Override
	public boolean getThumbEnabled() {
		return mThumbEnabled;
	}
	@Override
	public void setThumbEnabled(boolean enabled) {
		mThumbEnabled = enabled;
		mScrollableView.invalidate();
	}
	@Override
	public boolean getSeekEnabled() {
		return mSeekEnabled;
	}
	@Override
	public void setSeekEnabled(boolean enabled) {
		mSeekEnabled = enabled;
	}
	@Override
	public boolean canDragFling() {
		return mCanDragFling;
	}
	@Override
	public void canDragFling(boolean can) {
		mCanDragFling = can;
	}
    @Override
    public boolean canVertDrag() {
        return mCanVertDrag;
    }
    @Override
    public void canVertDrag(boolean can) {
        mCanVertDrag = can;
    }
    @Override
    public boolean canHorzDrag() {
        return mCanHorzDrag;
    }
    @Override
    public void canHorzDrag(boolean can) {
        mCanHorzDrag = can;
    }
    @Override
	public int getHorizontalThumbMarginLeft() {
		return mHorzThumbMargin.left;
	}
	@Override
	public int getHorizontalThumbMarginTop() {
		return mHorzThumbMargin.top;
	}
	@Override
	public int getHorizontalThumbMarginRight() {
		return mHorzThumbMargin.right;
	}
	@Override
	public int getHorizontalThumbMarginBottom() {
		return mHorzThumbMargin.bottom;
	}
	@Override
	public void setHorizontalThumbMargin(int left, int top, int right, int bottom) {
		mHorzThumbMargin.set(left, top, right, bottom);
	}
	@Override
	public int getVerticalThumbMarginLeft() {
		return mVertThumbMargin.left;
	}
	@Override
	public int getVerticalThumbMarginTop() {
		return mVertThumbMargin.top;
	}
	@Override
	public int getVerticalThumbMarginRight() {
		return mVertThumbMargin.right;
	}
	@Override
	public int getVerticalThumbMarginBottom() {
		return mVertThumbMargin.bottom;
	}
	@Override
	public void setVerticalThumbMargin(int left, int top, int right, int bottom) {
		mVertThumbMargin.set(left, top, right, bottom);
	}
	@Override
	public Drawable getHorizontalThumbDrawable() {
		return mHorzThumbDrawable[THUMB_DEFAULT];
	}
	@Override
	public void setHorizontalThumbDrawable(Drawable drawable) {
		mHorzThumbDrawable[THUMB_DEFAULT] = drawable;
	}
	@Override
	public Drawable getVerticalThumbDrawable() {
		return mVertThumbDrawable[THUMB_DEFAULT];
	}
	@Override
	public void setVerticalThumbDrawable(Drawable drawable) {
		mVertThumbDrawable[THUMB_DEFAULT] = drawable;
	}
	@Override
	public Drawable getHorizontalSeekDrawable() {
		return mHorzThumbDrawable[THUMB_SEEK];
	}
	@Override
	public void setHorizontalSeekDrawable(Drawable drawable) {
		mHorzThumbDrawable[THUMB_SEEK] = drawable;
	}
	@Override
	public Drawable getVerticalSeekDrawable() {
		return mVertThumbDrawable[THUMB_SEEK];
	}
	@Override
	public void setVerticalSeekDrawable(Drawable drawable) {
		mVertThumbDrawable[THUMB_SEEK] = drawable;
	}
	@Override
	public ViewGestureDetector getScrollDetector() {
		return mScrollDetector;
	}
	@Override
	public ScrollState getScrollState() {
		return mScrollState;
	}
	@Override
	public int getIdleTime() {
		if (mScrollState == ScrollState.IDLE) {
			return (int) (System.currentTimeMillis() - mIdleStartTime);
		} else {
			return 0;
		}
	}
	@Override
	public int getScrollTime() {
		if (mScrollState != ScrollState.IDLE) {
			return (int) (System.currentTimeMillis() - mScrollStartTime);
		} else {
			return 0;
		}
	}
	@Override
	public int getScrollFinalX() {
		return mScroller.getFinalX();
	}
	@Override
	public int getScrollFinalY() {
		return mScroller.getFinalY();
	}
	@Override
	public void setScrollInterpolator(Interpolator interpolator) {
		mScroller.setInterpolator(interpolator);
	}
	@Override
	public void setScrollSensitive(View view, boolean sensitive) {
		ListIterator<WeakReference<View>> i = mScrollSensitiveList.listIterator();
		while (i.hasNext()) {
			View refView = i.next().get();
			if (refView == null) {
				i.remove();
				continue;
			} else if (refView == view) {
				if (sensitive == false) {
					i.remove();
				}
				return;
			}
		}
		
		if (sensitive) {
			mScrollSensitiveList.add(new WeakReference<View>(view));
		}
	}
	@Override
	public OverScrollMode getHorizontalOverScrollMode() {
		return mHorzOverScrollMode;
	}
	@Override
	public void setHorizontalOverScrollMode(OverScrollMode mode) {
		mHorzOverScrollMode = mode;
	}
	@Override
	public OverScrollMode getVerticalOverScrollMode() {
		return mVertOverScrollMode;
	}
	@Override
	public void setVerticalOverScrollMode(OverScrollMode mode) {
		mVertOverScrollMode = mode;
	}
	@Override
	public int getMaxOverScrollWidth() {
		return mMaxOverScrollWidth;
	}
	@Override
	public void setMaxOverScrollWidth(int width) {
		mMaxOverScrollWidth = width;
	}
	@Override
	public int getMaxOverScrollHeight() {
		return mMaxOverScrollHeight;
	}
	@Override
	public void setMaxOverScrollHeight(int height) {
		mMaxOverScrollHeight = height;
	}
	@Override
	public Rect getViewportBounds() {
		return mViewportBounds;
	}
	@Override
	public Rect copyViewportBounds() {
		return new Rect(mViewportBounds);
	}
	@Override
	public void setOnScrollListener(OnScrollListener listener) {
		mOnScrollListener = listener;
	}
	@Override
	public boolean canScrollHorizontally() {
		if (mContentBounds.width() > mViewportBounds.width())
			return true;
		
		switch (mHorzOverScrollMode) {
		case ALWAYS:
			return mMaxOverScrollWidth > 0; 
		case AUTO:
		case NEVER:
		default:
			return false;
		}
	}
	@Override
	public boolean canScrollVertically() {
		if (mContentBounds.height() > mViewportBounds.height())
			return true;
		
		switch (mVertOverScrollMode) {
		case ALWAYS:
			return mMaxOverScrollHeight > 0; 
		case AUTO:
		case NEVER:
		default:
			return false;
		}
	}
	@Override
	public boolean canOverScrollHorizontally() {
		return maxOverScrollWidth() > 0;
	}
	@Override
	public boolean canOverScrollVertically() {
		return maxOverScrollHeight() > 0;
	}
	@Override
	public boolean reachesContentLeft() {
		return mScrollableView.getScrollX() <= mContentBounds.left;
	}
	@Override
	public boolean reachesContentRight() {
		return mScrollableView.getScrollX() >= mContentBounds.right - mScrollableView.getWidth();
	}
	@Override
	public boolean reachesContentTop() {
		return mScrollableView.getScrollY() <= mContentBounds.top;
	}
	@Override
	public boolean reachesContentBottom() {
		return mScrollableView.getScrollY() >= mContentBounds.bottom - mScrollableView.getHeight();
	}
	@Override
	public boolean isChildViewable(int index) {
		if (index < 0 || index >= mScrollableView.getChildCount())
			return false;
		
		final View childView = mScrollableView.getChildAt(index);
		final boolean viewable = mViewportBounds.intersects(childView.getLeft(), childView.getTop(), childView.getRight(), childView.getBottom());
		return viewable;
	}
	@Override
	public void scrollSmoothly(float vx, float vy, final Runnable onFinish, final Runnable onCancel) {
		abortRunningSlide();
		mScroller.forceFinished(true);

		setScrollState(ScrollState.SMOOTH);
		slide(vx, vy, 
				new Runnable() {
					@Override
					public void run() { // onFinish
						setScrollState(ScrollState.IDLE);

						if (onFinish != null) {
							mScrollableView.post(onFinish);
						}
					}
				}, 
				new Runnable() {
					@Override
					public void run() { // onCancel
						if (onCancel != null) {
							mScrollableView.post(onCancel);
						}
					}
				}
		);
	}
	@Override
	public void scrollSmoothlyTo(int x, int y, int duration, final Runnable onFinish, final Runnable onCancel) {
		abortRunningSlide();
		mScroller.forceFinished(true);

		setScrollState(ScrollState.SMOOTH);
		slide(x - mViewportBoundsF.left, y - mViewportBoundsF.top, duration, false,
				new Runnable() {
					@Override
					public void run() { // onFinish
						setScrollState(ScrollState.IDLE);

						if (onFinish != null) {
							mScrollableView.post(onFinish);
						}
					}
				}, 
				new Runnable() {
					@Override
					public void run() { // onCancel
						if (onCancel != null) {
							mScrollableView.post(onCancel);
						}
					}
				}
		);
	}
	@Override
	public void scrollSmoothlyBy(int dx, int dy, int duration, final Runnable onFinish, final Runnable onCancel) {
		abortRunningSlide();
		mScroller.forceFinished(true);
		
		setScrollState(ScrollState.SMOOTH);
		slide(dx, dy, duration, false,
				new Runnable() { // onFinish
					@Override
					public void run() {
						setScrollState(ScrollState.IDLE);

						if (onFinish != null) {
							mScrollableView.post(onFinish);
						}
					}
				}, 
				new Runnable() { // onCancel
					@Override
					public void run() {
						if (onCancel != null) {
							mScrollableView.post(onCancel);
						}
					}
				}
		);
	}
	public final void forceScrollSmoothlyTo(int x, int y, int duration, final Runnable onFinish, final Runnable onCancel) {
		abortRunningSlide();
		mScroller.forceFinished(true);

		setScrollState(ScrollState.SMOOTH);
		slide(x - mViewportBoundsF.left, y - mViewportBoundsF.top, duration, true,
				new Runnable() {
					@Override
					public void run() { // onFinish
						if (onFinish != null) {
							onFinish.run();
						}
						setScrollState(ScrollState.IDLE);
					}
				}, 
				new Runnable() {
					@Override
					public void run() { // onCancel
						if (onCancel != null) {
							onCancel.run();
						}
					}
				}
		);
	}
	@Override
	public void forceScrollTo(int x, int y) {
		abortRunningSlide();
		setScrollState(ScrollState.IDLE);
		
		doScrollTo(x, y);
	}
	@Override
	public void springBack() {
		final int minScrollX = minScrollX();
		final int maxScrollX = maxScrollX();
		final int minScrollY = minScrollY();
		final int maxScrollY = maxScrollY();
		int x = Math.max(minScrollX, Math.min(mViewportBounds.left, maxScrollX));
		int y = Math.max(minScrollY, Math.min(mViewportBounds.top, maxScrollY));
		
		if (mViewportBounds.left != x || mViewportBounds.top != y) {
			scrollTo(x, y);
		}
	}
	@Override
	public void springBackSmoothly() {
		abortRunningSlide();
		setScrollState(ScrollState.SMOOTH);

		slide(0, 0, new Runnable() {
			@Override
			public void run() {
				setScrollState(ScrollState.IDLE);
			}
		}, null);
	}
	@Override
	public Point content2view(Point point) {
		point.x = point.x - mViewportBounds.left;
		point.y = point.y - mViewportBounds.top;
		return point;
	}
	@Override
	public Rect content2view(Rect rect) {
		rect.offset(-mViewportBounds.left, -mViewportBounds.top);
		return rect;
	}
	@Override
	public Point view2content(Point point) {
		point.x += mViewportBounds.left;
		point.y += mViewportBounds.top;
		return point;
	}
	@Override
	public Rect view2content(Rect rect) {
		rect.offset(mViewportBounds.left, mViewportBounds.top);
		return rect;
	}

	// ### 实现函数 ###
	protected void onScrollStateChanged(ScrollState oldState, ScrollState newState) {
		
	}
	protected void onScroll(boolean viewportChanged) {
		
	}
	protected void onTouchDown(PointF downPoint) {
		
	}
	protected void onTouchUp(PointF upPoint) {
		
	}
	protected void onTouchCancel(PointF cancelPoint) {
		
	}
	protected void onTap(PointF tapPoint) {
		mScrollableView.performClick();
	}
	protected void onLongPress(PointF pressPoint) {
		mScrollableView.performLongClick();
	}
	protected int getScrollSlop() {
		return UiUtils.getScaledTouchSlop(mScrollableView.getContext());
	}
	protected int getScrollOrder() {
		return 0;
	}
	protected final void scrollBy(float dx, float dy) {
		abortRunningSlide();
		doRestrictScrollBy(dx, dy);
	}
	protected final void scrollTo(float x, float y) {
		abortRunningSlide();
		doRestrictScrollTo(x, y);
	}
	protected final void doRestrictScrollBy(float dx, float dy) {
		doRestrictScrollTo(mViewportBoundsF.left + dx, mViewportBoundsF.top + dy);
	}
	protected final void doRestrictScrollTo(float x, float y) {
		final int minOverScrollX = minOverScrollX();
		final int maxOverScrollX = maxOverScrollX();
		final int minOverScrollY = minOverScrollY();
		final int maxOverScrollY = maxOverScrollY();
		x = Math.max(minOverScrollX, Math.min(x, maxOverScrollX));
		y = Math.max(minOverScrollY, Math.min(y, maxOverScrollY));

		doScrollTo(x, y);
	}
	protected final void doScrollBy(float dx, float dy) {
		doScrollTo(mViewportBoundsF.left + dx, mViewportBoundsF.top + dy);
	}
	protected final void doScrollTo(float x, float y) {
		mViewportBoundsF.set(x, y, x + mScrollableView.getWidth(), y + mScrollableView.getHeight());
		mViewportBounds.set(Math.round(mViewportBoundsF.left), Math.round(mViewportBoundsF.top), Math.round(mViewportBoundsF.right), Math.round(mViewportBoundsF.bottom));

		adjustViewport(mScrollState, mViewportBoundsF);
		mViewportBounds.set(Math.round(mViewportBoundsF.left), Math.round(mViewportBoundsF.top), Math.round(mViewportBoundsF.right), Math.round(mViewportBoundsF.bottom));

		boolean viewportChanged = mViewportBounds.left != mOldViewportBounds.left 
				|| mViewportBounds.top != mOldViewportBounds.top
				|| mViewportBounds.right != mOldViewportBounds.right 
				|| mViewportBounds.bottom != mOldViewportBounds.bottom;
		mOldViewportBounds.set(mViewportBounds);
		superViewScrollTo(mViewportBounds.left, mViewportBounds.top);
		
		updateVertThumbBounds();
		updateHorzThumbBounds();
		
		for (WeakReference<View> ref : mScrollSensitiveList) {
			View refView = ref.get();
			
			if (refView != null) {
				refView.invalidate();
			}
		}
		
		onScroll(viewportChanged);
		notifyScroll(viewportChanged);
	}
	protected void adjustViewport(ScrollState scrollState, RectF viewportBounds) {
		
	}
	protected void onFling(float flingX, float flingY, float vx, float vy, Runnable onFinish, Runnable onCancel) {
		final float _vx = -vx * scaleOfScrollX(-vx);
		final float _vy = -vy * scaleOfScrollY(-vy); 
		slide(_vx, _vy, onFinish, onCancel);
	}
	protected void onSeekStart() {
		
	}
	protected void onSeek(float posx, float posy) {
		scrollTo(minScrollX() + (maxScrollX() - minScrollX()) * posx, minScrollY() + (maxScrollY() - minScrollY()) * posy);
	}
	protected void onSeekEnd() {
		
	}
	protected void onDragStart(ScrollState prevState, float startX, float startY) {
		
	}
	protected void onDrag(float dx, float dy) {
		final float _dx = -dx * scaleOfScrollX(-dx);
		final float _dy = -dy * scaleOfScrollY(-dy);
		final float scrollX = mViewportBoundsF.left + _dx;
		final float scrollY = mViewportBoundsF.top + _dy; 
		scrollTo(scrollX, scrollY);
	}
	protected void onDragEnd(ScrollState nextState, float endX, float endY) {
		
	}
	protected void slide(float vx, float vy, Runnable onFinish, Runnable onCancel) {
		slide(vx, vy, minScrollX(), maxScrollX(), minScrollY(), maxScrollY(), onFinish, onCancel);
	}
	protected final void slide(float vx, float vy, int minX, int maxX, int minY, int maxY, Runnable onFinish, Runnable onCancel) {
		mScroller.fling(
				mViewportBounds.left, mViewportBounds.top, 
				Math.round(vx), Math.round(vy), 
				minX, maxX, minY, maxY, 
				maxOverScrollWidth(), maxOverScrollHeight()
		);
		mRunningSlide = new DoSlide(false, onFinish, onCancel);
		mScrollableView.post(mRunningSlide); 
	}
	protected void slide(float dx, float dy, int duration, boolean force, Runnable onFinish, Runnable onCancel) {
		if (force == false) {
			dx = Math.max(minOverScrollX() - mViewportBounds.left, Math.min(dx, maxOverScrollX() - mViewportBounds.left));
			dy = Math.max(minOverScrollY() - mViewportBounds.top, Math.min(dy, maxOverScrollY() - mViewportBounds.top));
		}
		
		mScroller.startScroll(
				mViewportBounds.left, mViewportBounds.top, 
				Math.round(dx), Math.round(dy), duration
		);
		mRunningSlide = new DoSlide(force, onFinish, onCancel);
		mScrollableView.post(mRunningSlide); 
	}
	protected final void slide(float vx, float vy, int endX, int endY, Runnable onFinish, Runnable onCancel) {
		mScroller.flyTo(mViewportBounds.left, mViewportBounds.top, endX, endY, Math.round(vx), Math.round(vy));
		mRunningSlide = new DoSlide(false, onFinish, onCancel);
		mScrollableView.post(mRunningSlide);
	}
	private final void abortRunningSlide() {
		mScroller.forceFinished(true);
		mRunningSlide = null;
	}
	private final void setScrollState(ScrollState state) {
		if (mScrollState != state) {
			ScrollState oldState = mScrollState;
			mScrollState = state;
			
			if (mScrollState == ScrollState.IDLE) {
				mIdleStartTime = System.currentTimeMillis();
			} else {
				mScrollStartTime = System.currentTimeMillis();
			}

			if (mScrollState == ScrollState.IDLE || mScrollState == ScrollState.SMOOTH) {
				mHorzSeeking = false;
				mVertSeeking = false;
				
				mHorzDragging = false;
				mVertDragging = false;
			}
			
			onScrollStateChanged(oldState, mScrollState);
			notifyScrollStateChanged(oldState, mScrollState);
		}
	}
	private final int alphaOfThumb() {
		final float opacity;
		if (mThumbEnabled == false) {
			opacity = 0.0f;
		} else if (getScrollState() == ScrollState.IDLE) {
			final int idleTime = getIdleTime();
			if (idleTime <= THUMB_KEEP_VISIBLE) {
				opacity = 1.0f;
			} else if (idleTime < THUMB_KEEP_VISIBLE + THUMB_TO_INVISIBLE) {
				opacity = (float) (THUMB_KEEP_VISIBLE + THUMB_TO_INVISIBLE - idleTime) / THUMB_TO_INVISIBLE;
			} else {
				opacity = 0.0f;
			}
		} else {
			opacity = 1.0f;
		}

		return (int) (255 * opacity);
	}
	protected float scaleOfScrollX(float vx) {
		final float minOverScrollX = minOverScrollX();
		final float maxOverScrollX = maxOverScrollX();
		final float minScrollX = minScrollX();
		final float maxScrollX = maxScrollX();
		final float scrollX = mViewportBoundsF.left;

		final float scaleX;
		if (Float.compare(scrollX, minScrollX) <= 0 && Float.compare(vx, 0) < 0) {
			if (Float.compare(minOverScrollX, mContentBounds.left) == 0) {
				scaleX = 0.0f;
			} else if (Float.compare(scrollX, minOverScrollX) <= 0) {
				scaleX = 0.0f;
			} else {
				scaleX = Math.abs((scrollX - minOverScrollX) / (minScrollX - minOverScrollX));
			}
		} else if (Float.compare(scrollX, maxScrollX) >= 0 && Float.compare(vx, 0) > 0) {
			if (Float.compare(maxOverScrollX, mContentBounds.right) == 0) {
				scaleX = 0.0f;
			} else if (Float.compare(scrollX, maxOverScrollX) >= 0) {
				scaleX = 0.0f;
			}  else {
				scaleX = Math.abs((scrollX - maxOverScrollX) / (maxScrollX - maxOverScrollX));
			}
		} else {
			scaleX = 1.0f;
		}
		
		assert Float.compare(scaleX, 0.0f) >= 0;
		return scaleX;
	}
	protected float scaleOfScrollY(float vy) {
		final float minOverScrollY = minOverScrollY();
		final float maxOverScrollY = maxOverScrollY();
		final float minScrollY = minScrollY();
		final float maxScrollY = maxScrollY();
		final float scrollY = mViewportBoundsF.top;

		final float scaleY;
		if (Float.compare(scrollY, minScrollY) <= 0 && Float.compare(vy, 0) < 0) {
			if (Float.compare(minOverScrollY, mContentBounds.top) == 0) {
				scaleY = 0.0f;
			} else if (Float.compare(scrollY, minOverScrollY) <= 0) {
				scaleY = 0.0f;
			} else {
				scaleY = Math.abs((scrollY - minOverScrollY) / (minScrollY - minOverScrollY));
			}
		} else if (Float.compare(scrollY, maxScrollY) >= 0 && Float.compare(vy, 0) > 0) {
			if (Float.compare(maxOverScrollY, mContentBounds.bottom) == 0) {
				scaleY = 0.0f;
			} else if (Float.compare(scrollY, maxOverScrollY) >= 0) {
				scaleY = 0.0f;
			}  else {
				scaleY = Math.abs((scrollY - maxOverScrollY) / (maxScrollY - maxOverScrollY));
			}
		} else {
			scaleY = 1.0f;
		}
		
		assert Float.compare(scaleY, 0.0f) >= 0;
		return scaleY;	
	}
	private float normalizedScrollX() {
		final int scrollRange = maxScrollX() - minScrollX();
		if (scrollRange == 0)
			return 0;
		
		return (mViewportBoundsF.left - minScrollX()) / (maxScrollX() - minScrollX());
	}
	private float normalizedScrollY() {
		final int scrollRange = maxScrollY() - minScrollY();
		if (scrollRange == 0)
			return 0;
		
		return (mViewportBoundsF.top - minScrollY()) / (maxScrollY() - minScrollY());
	}
	protected int minOverScrollX() {
		final int minOverScrollX = minScrollX() - maxOverScrollWidth();
		return minOverScrollX;
	}
	protected int maxOverScrollX() {
		final int maxOverScrollX = maxScrollX() + maxOverScrollWidth();
		return maxOverScrollX;
	}
	protected int minOverScrollY() {
		final int minOverScrollY = minScrollY() - maxOverScrollHeight();
		return minOverScrollY;
	}
	protected int maxOverScrollY() {
		final int maxOverScrollY = maxScrollY() + maxOverScrollHeight();
		return maxOverScrollY;
	}
	protected int maxOverScrollWidth() {
		switch (mHorzOverScrollMode) {
		case ALWAYS:
			return mMaxOverScrollWidth;
		case AUTO:
			return mContentBounds.width() > mViewportBounds.width() ? mMaxOverScrollWidth : 0;
		case NEVER:
		default:
			return 0;
		}
	}
	protected int maxOverScrollHeight() {
		switch (mVertOverScrollMode) {
		case ALWAYS:
			return mMaxOverScrollHeight;
		case AUTO:
			return mContentBounds.height() > mViewportBounds.height() ? mMaxOverScrollHeight : 0;
		case NEVER:
		default:
			return 0;
		}
	}
	protected int minScrollX() {
		return mContentBounds.left;
	}
	protected int minScrollY() {
		return mContentBounds.top;
	}
	protected int maxScrollX() {
		final int maxScrollX = Math.max(mContentBounds.left, mContentBounds.right - mViewportBounds.width());
		return maxScrollX;
	}
	protected int maxScrollY() {
		final int maxScrollY = Math.max(mContentBounds.top, mContentBounds.bottom - mViewportBounds.height());
		return maxScrollY;
	}
	private final void updateHorzThumbBounds() {
		final Drawable horzDrawable = horzThumbDrawable();
		final int scrollOffset = computeHorizontalScrollOffset();
		final int scrollExtent = computeHorizontalScrollExtent();
		final int scrollRange = computeHorizontalScrollRange();

		if (horzDrawable == null || scrollRange == 0 || scrollRange <= scrollExtent) {
			mHorzThumbBounds.setEmpty();
			
		} else {
			final float thumbPos = (float) scrollOffset / scrollRange;
			final float thumbLen = (float) scrollExtent / scrollRange;
			final int thumbMinWidth = horzMinThumbWidth();
			final int thumbMaxWidth = horzMaxThumbWidth();
			final int thumbWidth = Math.max(thumbMinWidth, Math.round(thumbMaxWidth * thumbLen));
			final int thumbHeight = horzDrawable.getIntrinsicHeight() > 0 ? horzDrawable.getIntrinsicHeight() : UiUtils.dip2px(mScrollableView.getContext(), THUMB_MIN_HEIGHT);
			int thumbLeft = mHorzThumbMargin.left + Math.round(thumbMaxWidth * thumbPos);
			int thumbRight = thumbLeft + thumbWidth;
			
			mHorzThumbBounds.set(thumbLeft, mScrollableView.getHeight() - mHorzThumbMargin.bottom - thumbHeight, thumbRight, mScrollableView.getHeight() - mHorzThumbMargin.bottom);
			if (mHorzThumbBounds.right > mScrollableView.getWidth() - mHorzThumbMargin.right) {
				mHorzThumbBounds.offset(mScrollableView.getWidth() - mHorzThumbMargin.right - mHorzThumbBounds.right, 0);
			}
		}
	}
	private final float horzSeekNormalizedOffset(int dx) {
		if (horzSeekRange() == 0)
			return 0;
		
		return (float) dx / horzSeekRange();
	}
	private final float vertSeekNormalizedOffset(int dy) {
		if (vertSeekRange() == 0)
			return 0;
		
		return (float) dy / vertSeekRange();
	}
	private final int horzSeekRange() {
		if (horzThumbIndex() != THUMB_SEEK)
			return 0;
		
		return horzMaxThumbWidth() - horzThumbDrawable().getIntrinsicWidth();
	}
	private final int vertSeekRange() {
		if (vertThumbIndex() != THUMB_SEEK)
			return 0;
		
		return vertMaxThumbHeight() - vertThumbDrawable().getIntrinsicHeight();
	}
	private final void updateVertThumbBounds() {
		final Drawable vertDrawable = vertThumbDrawable();
		final int scrollOffset = computeVerticalScrollOffset();
		final int scrollExtent = computeVerticalScrollExtent();
		final int scrollRange = computeVerticalScrollRange();
		
		if (vertDrawable == null || scrollRange == 0 || scrollRange <= scrollExtent) {
			mVertThumbBounds.setEmpty();
			
		} else {
			final float thumbPos = (float) scrollOffset / scrollRange;
			final float thumbLen = (float) scrollExtent / scrollRange;
			final int thumbMinHeight = vertMinThumbHeight();
			final int thumbMaxHeight = vertMaxThumbHeight();
			final int thumbWidth = vertDrawable.getIntrinsicWidth() > 0 ? vertDrawable.getIntrinsicWidth() : UiUtils.dip2px(mScrollableView.getContext(), THUMB_MIN_WIDTH);
			final int thumbHeight = Math.max(thumbMinHeight, Math.round(thumbMaxHeight * thumbLen));
			int thumbTop = mVertThumbMargin.top + Math.round(thumbMaxHeight * thumbPos);
			int thumbBottom = thumbTop + thumbHeight;
			
			mVertThumbBounds.set(mScrollableView.getWidth() - thumbWidth - mVertThumbMargin.right, thumbTop, mScrollableView.getWidth() - mVertThumbMargin.right, thumbBottom);
			if (mVertThumbBounds.bottom > mScrollableView.getHeight() - mVertThumbMargin.bottom) {
				mVertThumbBounds.offset(0, mScrollableView.getHeight() - mVertThumbMargin.bottom - mVertThumbBounds.bottom);
			}
		}
	}
	private final int horzMinThumbWidth() {
		final Drawable horzDrawable = horzThumbDrawable();
		return horzDrawable.getIntrinsicWidth() > 0 ? horzDrawable.getIntrinsicWidth() : UiUtils.dip2px(mScrollableView.getContext(), THUMB_MIN_WIDTH);
	}
	private final int vertMinThumbHeight() {
		final Drawable vertDrawable = vertThumbDrawable();
		return vertDrawable.getIntrinsicHeight() > 0 ? vertDrawable.getIntrinsicHeight() : UiUtils.dip2px(mScrollableView.getContext(), THUMB_MIN_HEIGHT);
	}
	private final Drawable horzThumbDrawable() {
		return mHorzThumbDrawable[horzThumbIndex()];
	}
	private final Drawable vertThumbDrawable() {
		return mVertThumbDrawable[vertThumbIndex()];
	}
	private final int horzThumbIndex() {
		if (mSeekEnabled == false)
			return THUMB_DEFAULT;

		if (getContentWidth() == 0 || horzMaxThumbWidth() == 0)
			return THUMB_DEFAULT;

		final Drawable seekDrawable = mHorzThumbDrawable[THUMB_SEEK];
		if (seekDrawable == null)
			return THUMB_DEFAULT;

		if (mHorzSeeking)
			return THUMB_SEEK;
		
		final float extentLen = mViewportBoundsF.width() / getContentWidth();
		final float maxLen = (float) seekDrawable.getIntrinsicWidth() / horzMaxThumbWidth();

		if (Float.compare(extentLen, maxLen) > 0)
			return THUMB_DEFAULT;
		
		return THUMB_SEEK;
	}
	private final int vertThumbIndex() {
		if (mSeekEnabled == false)
			return THUMB_DEFAULT;
		
		if (getContentHeight() == 0 || vertMaxThumbHeight() == 0)
			return THUMB_DEFAULT;
		
		final Drawable seekDrawable = mVertThumbDrawable[THUMB_SEEK];
		if (seekDrawable == null)
			return THUMB_DEFAULT;

		if (mVertSeeking)
			return THUMB_SEEK;
		
		final float extentLen = mViewportBoundsF.height() / getContentHeight();
		final float maxLen = (float) seekDrawable.getIntrinsicHeight() / vertMaxThumbHeight();

		if (Float.compare(extentLen, maxLen) > 0)
			return THUMB_DEFAULT;
		
		return THUMB_SEEK;
	}
	private final int horzMaxThumbWidth() {
		return mScrollableView.getWidth() - mHorzThumbMargin.left - mHorzThumbMargin.right;
	}
	private final int vertMaxThumbHeight() {
		return mScrollableView.getHeight() - mVertThumbMargin.top - mVertThumbMargin.bottom;
	}
	private final void requestParentDisallowInterceptTouchEvent(boolean disallow) {
		final ViewParent parent = mScrollableView.getParent();
		if (parent == null)
			return;
		
		parent.requestDisallowInterceptTouchEvent(disallow);
	}
    protected static int calcVisibleDeltaXToFitRequest(Rect visibleRect, Rect requestRect) {
    	if (visibleRect.left <= requestRect.left && visibleRect.right >= requestRect.right)
    		return 0; // 可见区域已经包含请求区域, 无需移动.
    	
    	if (visibleRect.left > requestRect.left && visibleRect.right < requestRect.right)
    		return 0; // 可见区域在请求区域中间, 不能确定移动方向, 不能移动.
    	
    	final int delta;
    	if (visibleRect.left < requestRect.left) {
    		// 可见区域在请求区域左侧, 向右移动至请求区域.
    		delta = Math.min(requestRect.left - visibleRect.left, requestRect.right - visibleRect.right); // 移动距离尽可能小
    	} else {
    		// 可见区域在请求区域右侧, 向左移动至请求区域.
    		delta = -Math.min(visibleRect.left - requestRect.left, visibleRect.right - requestRect.right); // 移动距离尽可能小
    	}
    	
        return delta;
    }
    protected static int calcVisibleDeltaYToFitRequest(Rect visibleRect, Rect requestRect) {
    	if (visibleRect.top <= requestRect.top && visibleRect.bottom >= requestRect.bottom)
    		return 0; // 可见区域已经包含请求区域, 无需移动.
    	
    	if (visibleRect.top > requestRect.top && visibleRect.bottom < requestRect.bottom)
    		return 0; // 可见区域在请求区域中间, 不能确定移动方向, 不能移动.
    	
    	final int delta;
    	if (visibleRect.top < requestRect.top) {
    		// 可见区域在请求区域上方, 向下移动至请求区域.
    		delta = Math.min(requestRect.top - visibleRect.top, requestRect.bottom - visibleRect.bottom); // 移动距离尽可能小
    	} else {
    		// 可见区域在请求区域下方, 向上移动至请求区域.
    		delta = -Math.min(visibleRect.top - requestRect.top, visibleRect.bottom - requestRect.bottom); // 移动距离尽可能小
    	}
    	
        return delta;
    }
	private final void notifyScrollStateChanged(ScrollState oldState, ScrollState newState) {
		if (mOnScrollListener != null) {
			mOnScrollListener.onScrollStateChanged(this, oldState, newState);
		}
		
		for (ScrollObserver each : mScrollObserverList) {
			each.onScrollStateChanged(this, oldState, newState);
		}
	}
	private final void notifyScroll(boolean viewportChanged) {
		if (mOnScrollListener != null) {
			mOnScrollListener.onScroll(this, viewportChanged);
		}
		
		for (ScrollObserver each : mScrollObserverList) {
			each.onScroll(this, viewportChanged);
		}
	}

	// ### 抽象实现函数 ###
	protected abstract void superViewScrollTo(int x, int y);
	
	// ### 内嵌类 ###
	private class DoSlide implements Runnable {
		private final boolean mForce;
		private final Runnable mOnFinish;
		private final Runnable mOnCancel;

		public DoSlide(boolean force, Runnable onFinish, Runnable onCancel) {
			mForce = force;
			mOnFinish = onFinish;
			mOnCancel = onCancel;
		}

		@Override
		public void run() {
			if (mRunningSlide != this) {
				MainThread.runLater(mOnCancel);
				return;
			}

			mScroller.computeScrollOffset();
			doScrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			
			if (mScroller.isFinished() == false) {
				mScrollableView.post(this);
			} else if (mForce == false && mScroller.springBack(mViewportBounds.left, mViewportBounds.top, minScrollX(), maxScrollX(), minScrollY(), maxScrollY())) {
				mScrollableView.post(this);
			} else if (mOnFinish != null) {
				MainThread.runLater(mOnFinish);
			}
		}
	}
	private class ScrollGesture extends ViewGesture {
		private final FlingGesture mFlingGesture = new FlingGesture();
		private final TranslateGesture mDragGesture = new TranslateGesture();

		public ScrollGesture() {
			mFlingGesture.setMinVelocity(0);
			mDragGesture.setTranslateSlop(0);
		}
		
		@Override
		protected void doRestart(View v, boolean reset) {
			mFlingGesture.restart(v, reset || mFlingGesture.keepDetecting() == false);
			mDragGesture.restart(v, reset || mDragGesture.keepDetecting() == false);
			mDragGesture.setDetectOrder(getScrollOrder());
		}
		@Override
		protected void doIntercept(View v, MotionEvent m, boolean delayed, GestureListener listener) {
			doDetect(v, m, delayed, listener);
		}
		@Override
		protected void doDetect(View v, MotionEvent m, boolean delayed, GestureListener listener) {
			if (m.getPointerCount() > 1 && mScrollState == ScrollState.IDLE) {
				keepDetecting(false);
				return;
			}
			
			if (m.getActionMasked() == MotionEvent.ACTION_DOWN) {
				// 优先判断滑块拖动
				if (mScrollState != ScrollState.DRAG && mScrollState != ScrollState.SEEK
						&& mSeekEnabled && isThumbVisible()) {
					final int horzThumbIndex = horzThumbIndex();
					final int vertThumbIndex = vertThumbIndex();
					
					if (horzThumbIndex == THUMB_SEEK
							&& mHorzThumbBounds.contains((int) m.getX(), (int) m.getY())) {
						mHorzSeeking = true;
						
					} else if (vertThumbIndex == THUMB_SEEK
							&& mVertThumbBounds.contains((int) m.getX(), (int) m.getY())) {
						
						mVertSeeking = true;
					}
				}

				if (mHorzSeeking || mVertSeeking) {
					mDragGesture.setTranslateSlop(0);
					mDragGesture.setMinAngle(0.0f);
					mDragGesture.setMaxAngle(360.0f);
					setScrollState(ScrollState.SEEK);
					requestParentDisallowInterceptTouchEvent(true);
					holdDetecting(true);
					onSeekStart();
					
				} else if (mScrollState == ScrollState.SMOOTH) {
					keepDetecting(false);
					return;
					
				} else if (mScrollState != ScrollState.IDLE && mCanDragFling) {
					// 非静止状态时, 直接切换到拖拽状态.
					onDragStart(mScrollState, m.getX(), m.getY());
					setScrollState(ScrollState.DRAG);
					scrollBy(0.0f, 0.0f);
					requestParentDisallowInterceptTouchEvent(true);
					holdDetecting(true);
				} else {
					// 静止状态时, 移动距离需要大于阀值时, 才切换到拖拽状态.
					mDragGesture.setTranslateSlop(getScrollSlop());
					
				}
			}
			
			if (keepDetecting() && skipNextDetecting() == false) {
				mDragGesture.detect(v, m, delayed, new TranslateGesture.GestureListener() {
					@Override
					public void onTouchUp(View v, PointF upPoint) {
						
					}
					@Override
					public void onTouchDown(View v, PointF downPoint) {
						
					}
					@Override
					public void onTouchCancel(View v, PointF cancelPoint) {
						
					}
					@Override
					public void onTranslate(ViewGesture g, View v, PointF origin, PointF translation) {
						// 判断是否开始拖拽
						if (mScrollState != ScrollState.DRAG && mScrollState != ScrollState.SEEK) {
							final double angle = UiUtils.calcAngle(new PointF(0, 0), translation);
							if (canScrollHorizontally() == false && canScrollVertically() == false) {
								mDragGesture.keepDetecting(false);
								mHorzDragging = false;
								mVertDragging = false;
							} else {
								mHorzDragging = checkHorzDragging(angle, (int) translation.x);
								mVertDragging = checkVertDragging(angle, (int) translation.y);
							}
							
							if (mHorzDragging || mVertDragging) {
								final ScrollState prevState = mScrollState;
								mDragGesture.setTranslateSlop(0);
								mDragGesture.setMinAngle(0.0f);
								mDragGesture.setMaxAngle(360.0f);
								setScrollState(ScrollState.DRAG);
								requestParentDisallowInterceptTouchEvent(true);
								holdDetecting(true);
								onDragStart(prevState, origin.x + translation.x, origin.y + translation.y);
							}
							
							return;
						}

						if (mScrollState == ScrollState.SEEK) {
							final float dx = mHorzSeeking == false ? 0 : horzSeekNormalizedOffset((int) translation.x);
							final float dy = mVertSeeking == false ? 0 : vertSeekNormalizedOffset((int) translation.y);
							final float posx = Math.max(0, Math.min(normalizedScrollX() + dx, 1.0f));
							final float posy = Math.max(0, Math.min(normalizedScrollY() + dy, 1.0f));
							onSeek(posx, posy);
							
						} else if (mScrollState == ScrollState.DRAG) {
							onDrag(mHorzDragging ? translation.x : 0, mVertDragging ? translation.y : 0);
						}
					}
					
					private boolean checkHorzDragging(double angle, int dx) {
                        if (!mCanHorzDrag)
                            return false;

						if (canScrollHorizontally() == false)
							return false;
						
						if (UiUtils.isLineBetween(angle, -50, 50) == false)
							return false;
						
						if (dx == 0)
							return false;
						
						if (canOverScrollHorizontally())
							return true;
						
						if (dx > 0 && reachesContentLeft() == false)
							return true;
						
						if (dx < 0 && reachesContentRight() == false)
							return true;
						
						return false;
					}
					private boolean checkVertDragging(double angle, int dy) {
                        if (!mCanVertDrag)
                            return false;

						if (canScrollVertically() == false)
							return false;
						
						if (UiUtils.isLineBetween(angle, 40, 140) == false)
							return false;
						
						if (dy == 0)
							return false;
						
						if (canOverScrollVertically())
							return true;
						
						if (dy > 0 && reachesContentTop() == false)
							return true;
						
						if (dy < 0 && reachesContentBottom() == false)
							return true;
						
						return false;
					}
				});
			}
			
			if (keepDetecting() && skipNextDetecting() == false) {
				mFlingGesture.detect(v, m, delayed, new FlingGesture.GestureListener() {
					@Override
					public void onTouchUp(View v, PointF upPoint) {
						
					}
					@Override
					public void onTouchDown(View v, PointF downPoint) {
						
					}
					@Override
					public void onTouchCancel(View v, PointF cancelPoint) {
						
					}
					@Override
					public void onFling(ViewGesture g, View v, PointF flingPoint, PointF velocity) {
						if (mScrollState == ScrollState.SEEK) {
							setScrollState(ScrollState.IDLE);
							onSeekEnd();
							
						} else if (mScrollState == ScrollState.DRAG) {
							setScrollState(ScrollState.FLING);
							ViewScroller.this.onFling(flingPoint.x, flingPoint.y, mHorzDragging ? velocity.x : 0, mVertDragging ? velocity.y : 0, new Runnable() {
								@Override
								public void run() {
									setScrollState(ScrollState.IDLE);
								}
							}, null);
							onDragEnd(mScrollState, flingPoint.x, flingPoint.y);
						}
					}
				});
			}
			
			if (m.getActionMasked() == MotionEvent.ACTION_UP) {
				requestParentDisallowInterceptTouchEvent(false);

				if (mScrollState == ScrollState.SEEK) {
					setScrollState(ScrollState.IDLE);
					onSeekEnd();
					
				} else if (mScrollState == ScrollState.DRAG) {
					
					setScrollState(ScrollState.FLING);
					ViewScroller.this.onFling(m.getX(), m.getY(), 0, 0, new Runnable() {
						@Override
						public void run() {
							setScrollState(ScrollState.IDLE);
						}
					}, null);
					onDragEnd(ScrollState.FLING, m.getX(), m.getY());
				}
			}
		}
	}
	private class ClickListener implements TapGesture.GestureListener, LongPressGesture.GestureListener {
		@Override
		public void onTouchDown(View v, PointF downPoint) {
			ViewScroller.this.onTouchDown(downPoint);
		}
		@Override
		public void onTouchUp(View v, PointF upPoint) {
			ViewScroller.this.onTouchUp(upPoint);
		}
		@Override
		public void onTouchCancel(View v, PointF cancelPoint) {
			ViewScroller.this.onTouchCancel(cancelPoint);
		}
		@Override
		public void onLongPress(View v, PointF pressPoint) {
			ViewScroller.this.onLongPress(pressPoint);
		}
		@Override
		public void onTap(ViewGesture g, View v, PointF tapPoint) {
			ViewScroller.this.onTap(tapPoint);
		}
		
	}
}
