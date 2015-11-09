package com.common.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;

import com.common.sys.CurrentThread;
import com.common.sys.MainThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;

public abstract class ItemsView extends ViewGroup implements ItemsObserver, Scrollable, ViewTreeObserver.OnPreDrawListener {
    // ### 值域 ###
    protected static final int STRUCT_EMPTY = -1;
    protected static final int STRUCT_DONE = 0;
    protected static final int STRUCT_CHANGED = 1;
    private final Scroller mScroller;
    private final ArrayList<ItemCell> mCells = new ArrayList<ItemCell>();
    private final ArrayList<ItemCell> mFilledCells = new ArrayList<ItemCell>();
    private final LinkedList<Integer> mPinnedCellIndexList = new LinkedList<Integer>();
    private final Rect mItemsBackgroundPadding = new Rect();
    private Drawable mItemsBackground = null;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private int mItemCount = 0;
    private boolean mVisualizeValid = true;
    private boolean mArrangeValid = true;
    private boolean mStructValid = true;
    private boolean mInVisualize = false;
    private int mWidthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    private int mHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    private View mEmptyView = null;
    private boolean mEmpty = false;
    private int mContentWidth = 0;
    private int mContentHeight = 0;
    private int[] mVisibleCellIndices = new int[0];
    private int[] mPreviewCellIndices = new int[0];
    private ScrollState mScrollState = ScrollState.IDLE;
    private ItemsAdapter mAdapter = null;
    private int mTouchingItemIndex = -1;
    private int mPressedItemIndex = -1;
    private Runnable mPendingPress = null;
    private Runnable mPendingUnpress = null;
    private OnItemClickListener mOnItemClickListener = null;
    private OnItemLongPressListener mOnItemLongPressListener = null;

    // ### 构造函数 ###
    public ItemsView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mScroller = newScroller();
        mScroller.setScrollInterpolator(new AccelerateDecelerateInterpolator());
        setWillNotDraw(false);
        setClipChildren(false);
        setStaticTransformationsEnabled(true);
    }

    // ### 属性 ###
    public final void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public final void setOnItemLongPressListener(OnItemLongPressListener listener) {
        mOnItemLongPressListener = listener;
    }

    public final ItemsAdapter getAdapter() {
        return mAdapter;
    }

    public final void setAdapter(ItemsAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.removeObserver(this);
        }

        mAdapter = adapter;

        if (mAdapter != null) {
            mAdapter.addObserver(this);
        }

        invalidateStruct();
        invalidateLayout();
    }

    public final Drawable getItemsBackground() {
        return mItemsBackground;
    }

    public final void setItemsBackground(int resId) {
        setItemsBackground(getResources().getDrawable(resId));
    }

    public final void setItemsBackground(Drawable background) {
        if (mItemsBackground != background) {
            mItemsBackground = background;

            final int l = mItemsBackgroundPadding.left;
            final int t = mItemsBackgroundPadding.top;
            final int r = mItemsBackgroundPadding.right;
            final int b = mItemsBackgroundPadding.bottom;

            if (mItemsBackground == null) {
                mItemsBackgroundPadding.setEmpty();
            } else {
                mItemsBackground.getPadding(mItemsBackgroundPadding);
            }

            if (mItemsBackgroundPadding.left != l || mItemsBackgroundPadding.top != t || mItemsBackgroundPadding.right != r || mItemsBackgroundPadding.bottom != b) {
                invalidateStruct();
            }
        }
    }

    public final int getItemCount() {
        return mItemCount;
    }

    public final View getItemView(int index) {
        visualize();
        final ItemCell cell = getCell(index);
        return cell.mItemView;
    }

    public final View[] getItemViews() {
        visualize();
        View[] itemViews = new View[mFilledCells.size()];
        for (int n = 0; n < itemViews.length; ++n) {
            itemViews[n] = mFilledCells.get(n).mItemView;
        }
        return itemViews;
    }

    public final View[] getVisibleItemViews() {
        int[] indices = getVisibleItemIndices();
        View[] itemViews = new View[indices.length];
        for (int i = 0; i < indices.length; i++) {
            itemViews[i] = getItemView(indices[i]);
        }
        return itemViews;
    }

    public final int[] getVisibleItemIndices() {
        visualize();
        return mVisibleCellIndices;
    }

    public final int getFirstVisibleItemIndex() {
        visualize();
        return mVisibleCellIndices.length > 0 ? mVisibleCellIndices[0] : -1;
    }

    public final int getLastVisibleItemIndex() {
        visualize();
        return mVisibleCellIndices.length > 0 ? mVisibleCellIndices[mVisibleCellIndices.length - 1] : -1;
    }

    public final int getVisibleItemCount() {
        visualize();
        return mVisibleCellIndices.length;
    }

    public final int getPreviewWidth() {
        return mPreviewWidth;
    }

    public final void setPreviewWidth(int width) {
        mPreviewWidth = width;
        invalidateVisualize();
    }

    public final int getPreviewHeight() {
        return mPreviewHeight;
    }

    public final void setPreviewHeight(int height) {
        mPreviewHeight = height;
        invalidateVisualize();
    }

    // ### 方法 ###
    public final Rect getItemBounds(int index) {
        final boolean oldState = forceItemVisual(index, true);
        visualize();

        final ItemCell cell = getCell(index);
        final View itemView = cell.mItemView;
        assert itemView != null;
        Rect itemBounds = new Rect(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom());
        itemBounds.offset(cell.mLeft, cell.mTop);
        itemBounds.offset(-cell.mCellView.getScrollX(), -cell.mCellView.getScrollY());
        forceItemVisual(index, oldState);
        return itemBounds;
    }

    public final int[] hitTestVisibleItems(Rect rect) {
        final int[] visibleIndices = getVisibleItemIndices();
        view2content(rect);

        final ArrayList<Integer> hitList = new ArrayList<Integer>();
        for (int n = 0; n < visibleIndices.length; ++n) {
            if (Rect.intersects(getItemBounds(visibleIndices[n]), rect)) {
                hitList.add(visibleIndices[n]);
            }
        }

        final int[] hitIndices = new int[hitList.size()];
        for (int n = 0; n < hitIndices.length; ++n) {
            hitIndices[n] = hitList.get(n);
        }

        return hitIndices;
    }

    public final int hitTestItemCell(int x, int y) {
        visualize();
        Point point2view = new Point(x, y);
        view2content(point2view);
        return hitTestCell(point2view);
    }

    public final int[] hitTestItemCells(Rect rect) {
        visualize();
        Rect rect2view = new Rect(rect);
        view2content(rect2view);
        return hitTestCells(rect2view);
    }

    public final boolean forceItemVisual(int index, boolean force) {
        final ItemCell cell = getCell(index);
        final boolean oldState = cell.forceVisual();
        if (cell.forceVisual() == force)
            return oldState;

        cell.forceVisual(force);
        if (force) {
            pinCell(index, true);
        } else if (cell.mTransformation == null) {
            pinCell(index, false);
        }

        return oldState;
    }

    public final void setItemOffset(int index, int dx, int dy) {
        final ItemCell cell = getCell(index);

        cell.setOffset(dx, dy);
        if (cell.mTransformation != null) {
            pinCell(index, true);
        } else if (cell.forceVisual() == false) {
            pinCell(index, false);
        }

        visualize();
    }

    public final void setItemAlpha(int index, float alpha) {
        final ItemCell cell = getCell(index);

        cell.setAlpha(alpha);
        if (cell.mTransformation != null) {
            pinCell(index, true);
        } else if (cell.forceVisual() == false) {
            pinCell(index, false);
        }

        visualize();
    }

    public final void requestItemVisible(int index) {
        visualize();
        if (checkCell(index) == false)
            return;

        if (getViewportBounds().isEmpty() || getContentWidth() == 0 || getContentHeight() == 0)
            return;

        if (isItemVisible(index))
            return;

        final Rect itemBounds = getItemBounds(index);
        scrollTo(itemBounds.left, itemBounds.top);
        springBack();
    }

    public final void requestItemInRect(int index, Rect rect, int gravity) {
        visualize();
        if (checkCell(index) == false)
            return;

        if (rect.isEmpty() || rect.width() == 0 || rect.height() == 0)
            return;

        final Rect itemBounds = getItemBounds(index);
        final Rect dstRect = UiUtils.tempRects.acquire();

        Gravity.apply(gravity, itemBounds.width(), itemBounds.height(), view2content(rect), dstRect);
        scrollBy(itemBounds.left - dstRect.left, itemBounds.top - dstRect.top);

        UiUtils.tempRects.release(dstRect);
        springBack();
    }

    public final boolean isItemVisible(int index) {
        visualize();
        if (checkCell(index) == false)
            return false;

        return getCell(index).visible();
    }

    // ### Scrollable接口实现 ###
    @Override
    public final int getContentWidth() {
        return mScroller.getContentWidth();
    }

    @Override
    public final int getContentHeight() {
        return mScroller.getContentHeight();
    }

    @Override
    public final boolean getThumbEnabled() {
        return mScroller.getThumbEnabled();
    }

    @Override
    public final void setThumbEnabled(boolean enabled) {
        mScroller.setThumbEnabled(enabled);
    }

    @Override
    public boolean getSeekEnabled() {
        return mScroller.getSeekEnabled();
    }

    @Override
    public void setSeekEnabled(boolean enabled) {
        mScroller.setSeekEnabled(enabled);
    }

    @Override
    public boolean canDragFling() {
        return mScroller.canDragFling();
    }

    @Override
    public void canDragFling(boolean can) {
        mScroller.canDragFling(can);
    }

    @Override
    public boolean canVertDrag() {
        return mScroller.canVertDrag();
    }

    @Override
    public void canVertDrag(boolean can) {
        mScroller.canVertDrag(can);
    }

    @Override
    public boolean canHorzDrag() {
        return mScroller.canHorzDrag();
    }

    @Override
    public void canHorzDrag(boolean can) {
        mScroller.canHorzDrag(can);
    }

    @Override
    public int getHorizontalThumbMarginLeft() {
        return mScroller.getHorizontalThumbMarginLeft();
    }

    @Override
    public int getHorizontalThumbMarginTop() {
        return mScroller.getHorizontalThumbMarginTop();
    }

    @Override
    public int getHorizontalThumbMarginRight() {
        return mScroller.getHorizontalThumbMarginRight();
    }

    @Override
    public int getHorizontalThumbMarginBottom() {
        return mScroller.getHorizontalThumbMarginBottom();
    }

    @Override
    public void setHorizontalThumbMargin(int left, int top, int right, int bottom) {
        mScroller.setHorizontalThumbMargin(left, top, right, bottom);
    }

    @Override
    public int getVerticalThumbMarginLeft() {
        return mScroller.getVerticalThumbMarginLeft();
    }

    @Override
    public int getVerticalThumbMarginTop() {
        return mScroller.getVerticalThumbMarginTop();
    }

    @Override
    public int getVerticalThumbMarginRight() {
        return mScroller.getVerticalThumbMarginRight();
    }

    @Override
    public int getVerticalThumbMarginBottom() {
        return mScroller.getVerticalThumbMarginBottom();
    }

    @Override
    public void setVerticalThumbMargin(int left, int top, int right, int bottom) {
        mScroller.setVerticalThumbMargin(left, top, right, bottom);
    }

    @Override
    public Drawable getHorizontalThumbDrawable() {
        return mScroller.getHorizontalThumbDrawable();
    }

    @Override
    public void setHorizontalThumbDrawable(Drawable drawable) {
        mScroller.setHorizontalThumbDrawable(drawable);
    }

    @Override
    public Drawable getVerticalThumbDrawable() {
        return mScroller.getVerticalThumbDrawable();
    }

    @Override
    public void setVerticalThumbDrawable(Drawable drawable) {
        mScroller.setVerticalThumbDrawable(drawable);
    }

    @Override
    public Drawable getHorizontalSeekDrawable() {
        return mScroller.getHorizontalSeekDrawable();
    }

    @Override
    public void setHorizontalSeekDrawable(Drawable drawable) {
        mScroller.setHorizontalSeekDrawable(drawable);
    }

    @Override
    public Drawable getVerticalSeekDrawable() {
        return mScroller.getVerticalSeekDrawable();
    }

    @Override
    public void setVerticalSeekDrawable(Drawable drawable) {
        mScroller.setVerticalSeekDrawable(drawable);
    }

    @Override
    public ViewGestureDetector getScrollDetector() {
        return mScroller.getScrollDetector();
    }

    @Override
    public final ScrollState getScrollState() {
        return mScroller.getScrollState();
    }

    @Override
    public final int getIdleTime() {
        return mScroller.getIdleTime();
    }

    @Override
    public final int getScrollTime() {
        return mScroller.getScrollTime();
    }

    @Override
    public int getScrollFinalX() {
        return mScroller.getScrollFinalX();
    }

    @Override
    public int getScrollFinalY() {
        return mScroller.getScrollFinalY();
    }

    @Override
    public final void setScrollInterpolator(Interpolator interpolator) {
        mScroller.setScrollInterpolator(interpolator);
    }

    @Override
    public final void setScrollSensitive(View view, boolean sensitive) {
        mScroller.setScrollSensitive(view, sensitive);
    }

    @Override
    public OverScrollMode getHorizontalOverScrollMode() {
        return mScroller.getHorizontalOverScrollMode();
    }

    @Override
    public void setHorizontalOverScrollMode(OverScrollMode mode) {
        mScroller.setHorizontalOverScrollMode(mode);
    }

    @Override
    public OverScrollMode getVerticalOverScrollMode() {
        return mScroller.getVerticalOverScrollMode();
    }

    @Override
    public void setVerticalOverScrollMode(OverScrollMode mode) {
        mScroller.setVerticalOverScrollMode(mode);
    }

    @Override
    public final int getMaxOverScrollWidth() {
        return mScroller.getMaxOverScrollWidth();
    }

    @Override
    public final void setMaxOverScrollWidth(int width) {
        mScroller.setMaxOverScrollWidth(width);
    }

    @Override
    public final int getMaxOverScrollHeight() {
        return mScroller.getMaxOverScrollHeight();
    }

    @Override
    public final void setMaxOverScrollHeight(int height) {
        mScroller.setMaxOverScrollHeight(height);
    }

    @Override
    public final Rect getViewportBounds() {
        return mScroller.getViewportBounds();
    }

    @Override
    public final Rect copyViewportBounds() {
        return mScroller.copyViewportBounds();
    }

    @Override
    public final void setOnScrollListener(OnScrollListener listener) {
        mScroller.setOnScrollListener(listener);
    }

    @Override
    public final boolean canScrollHorizontally() {
        return mScroller.canScrollHorizontally();
    }

    @Override
    public final boolean canScrollVertically() {
        return mScroller.canScrollVertically();
    }

    @Override
    public boolean canOverScrollHorizontally() {
        return mScroller.canOverScrollHorizontally();
    }

    @Override
    public boolean canOverScrollVertically() {
        return mScroller.canOverScrollVertically();
    }

    @Override
    public final boolean reachesContentLeft() {
        return mScroller.reachesContentLeft();
    }

    @Override
    public final boolean reachesContentRight() {
        return mScroller.reachesContentRight();
    }

    @Override
    public final boolean reachesContentTop() {
        return mScroller.reachesContentTop();
    }

    @Override
    public final boolean reachesContentBottom() {
        return mScroller.reachesContentBottom();
    }

    @Override
    public final boolean isChildViewable(int index) {
        return mScroller.isChildViewable(index);
    }

    @Override
    public void scrollSmoothly(float vx, float vy, final Runnable onFinish, final Runnable onCancel) {
        mScroller.scrollSmoothly(vx, vy, onFinish, onCancel);
    }

    @Override
    public final void scrollSmoothlyTo(int x, int y, int duration, final Runnable onFinish, final Runnable onCancel) {
        mScroller.scrollSmoothlyTo(x, y, duration, onFinish, onCancel);
    }

    @Override
    public final void scrollSmoothlyBy(int dx, int dy, int duration, final Runnable onFinish, final Runnable onCancel) {
        mScroller.scrollSmoothlyBy(dx, dy, duration, onFinish, onCancel);
    }

    @Override
    public final void forceScrollTo(int x, int y) {
        mScroller.forceScrollTo(x, y);
    }

    @Override
    public final void forceScrollSmoothlyTo(int x, int y, int duration, Runnable onFinish, Runnable onCancel) {
        mScroller.forceScrollSmoothlyTo(x, y, duration, onFinish, onCancel);
    }

    @Override
    public final void springBack() {
        mScroller.springBack();
    }

    @Override
    public final void springBackSmoothly() {
        mScroller.springBackSmoothly();
    }

    @Override
    public final Point content2view(Point point) {
        return mScroller.content2view(point);
    }

    @Override
    public final Rect content2view(Rect rect) {
        return mScroller.content2view(rect);
    }

    @Override
    public final Point view2content(Point point) {
        return mScroller.view2content(point);
    }

    @Override
    public final Rect view2content(Rect rect) {
        return mScroller.view2content(rect);
    }

    // ### ItemsObserver接口实现 ###
    @Override
    public void onItemsAdded(int addCount, int addTo) {
        final int[] pos = saveViewportPos();
        itemsAdded(addTo, addCount);
        visualize();
        restoreViewportPos(pos);
    }

    @Override
    public void onItemsRemoved(int removeFrom, int removeCount) {
        final int[] pos = saveViewportPos();
        itemsRemoved(removeFrom, removeCount);
        visualize();
        restoreViewportPos(pos);
    }

    @Override
    public void onItemsMoved(int fromIndex, int toIndex, int moveCount) {
        final int[] pos = saveViewportPos();
        itemsMoved(fromIndex, toIndex, moveCount);
        visualize();
        restoreViewportPos(pos);
    }

    @Override
    public void onItemsModified(int modifyFrom, int modifyCount) {
        final int[] pos = saveViewportPos();
        itemsModified(modifyFrom, modifyCount);
        visualize();
        restoreViewportPos(pos);
    }

    @Override
    public void onItemsChanged(int itemCount) {
        final int[] pos = saveViewportPos();
        itemsChanged(itemCount);
        visualize();
        restoreViewportPos(pos);
        springBack();
    }

    // ### ViewTreeObserver.OnPreDrawObserver ###
    @Override
    public boolean onPreDraw() {
        if (mVisualizeValid == false) {
            visualize();
            return !isLayoutRequested();
        }
        return true;
    }

    // ### 重写函数 ###
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        getViewTreeObserver().addOnPreDrawListener(this);
        mScroller.afterOnAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        getViewTreeObserver().removeOnPreDrawListener(this);
        mScroller.afterOnDetachedFromWindow();

        mPendingPress = null;
        mPendingUnpress = null;
        mTouchingItemIndex = -1;
        mPressedItemIndex = -1;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mScroller.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mScroller.onTouchEvent(event);
    }

    @Override
    public void draw(Canvas canvas) {
        drawItemsBackground(canvas);
        super.draw(canvas);
        drawItemsForeground(canvas);
        drawThumbs(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mWidthSpec != widthMeasureSpec || mHeightSpec != heightMeasureSpec) {
            mWidthSpec = widthMeasureSpec;
            mHeightSpec = heightMeasureSpec;
            invalidateStruct();
        }

        visualize();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mScroller.afterOnLayout(changed, l, t, r, b);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (left != getPaddingLeft() || top != getPaddingTop() || right != getPaddingRight() || bottom != getPaddingBottom()) {
            super.setPadding(left, top, right, bottom);
            invalidateStruct();
        }
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        if (child instanceof ItemCellView == false)
            return false;

        final ItemCellView cellView = (ItemCellView) child;
        final Transformation cellTransformation = cellView.mCell.mTransformation;

        if (cellTransformation == null
                || (cellTransformation.getTransformationType() & Transformation.TYPE_ALPHA) != Transformation.TYPE_ALPHA)
            return false;

        t.clear();
        t.setAlpha(cellTransformation.getAlpha());
        t.setTransformationType(Transformation.TYPE_ALPHA);
        return true;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
        mScroller.afterRequestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean isHorizontalFadingEdgeEnabled() {
        return mScroller.isHorizontalFadingEdgeEnabled();
    }

    @Override
    public boolean isHorizontalScrollBarEnabled() {
        return mScroller.isHorizontalScrollBarEnabled();
    }

    @Override
    public boolean isVerticalFadingEdgeEnabled() {
        return mScroller.isVerticalFadingEdgeEnabled();
    }

    @Override
    public boolean isVerticalScrollBarEnabled() {
        return mScroller.isVerticalScrollBarEnabled();
    }

    @Override
    public void scrollBy(int dx, int dy) {
        mScroller.scrollBy(dx, dy);
    }

    @Override
    public void scrollTo(int x, int y) {
        mScroller.scrollTo(x, y);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return mScroller.shouldDelayChildPressedState();
    }

    @Override
    protected int computeHorizontalScrollExtent() {
        return mScroller.computeHorizontalScrollExtent();
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return mScroller.computeHorizontalScrollOffset();
    }

    @Override
    protected int computeHorizontalScrollRange() {
        return mScroller.computeHorizontalScrollRange();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return mScroller.computeVerticalScrollExtent();
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return mScroller.computeVerticalScrollOffset();
    }

    @Override
    protected int computeVerticalScrollRange() {
        return mScroller.computeVerticalScrollRange();
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child instanceof ItemCellView == false) {
            return super.drawChild(canvas, child, drawingTime);
        }

        final ItemCellView cellView = (ItemCellView) child;
        final Transformation cellTransformation = cellView.mCell.mTransformation;

        if (cellTransformation == null
                || (cellTransformation.getTransformationType() & Transformation.TYPE_MATRIX) != Transformation.TYPE_MATRIX)
            return super.drawChild(canvas, child, drawingTime);

        // ATTENTION(by lizhan@duokan.com):
        // 在MIUI系统上(android 4.1.1), 子视图如果本身不在可见区域内, 而是通过变换(scroll/transformation)移动到可见区域内, 调用super.drawChild(...)
        // 不会引发绘制. 所以这里在调用super.drawChild(...)之前, 提前应用变换, 使子视图可见, 从而绕过这个问题. (这应该是MIUI的BUG, android原生系统无此问题!)
        canvas.save();
        canvas.concat(cellTransformation.getMatrix());
        final boolean more = super.drawChild(canvas, child, drawingTime);
        canvas.restore();
        return more;
    }

    @Override
    public void forceLayout() {
        super.forceLayout();
        invalidateStruct();
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        invalidateStruct();
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return lp instanceof ViewGroup.MarginLayoutParams ? new LayoutParams((ViewGroup.MarginLayoutParams) lp) : new LayoutParams(lp);
    }

    // ### 实现函数 ###
    protected void drawItemsBackground(Canvas canvas) {
        if (mItemCount > 0 && mItemsBackground != null) {
            final Rect contentBounds = mScroller.getContentBounds();
            mItemsBackground.setBounds(contentBounds.left + getPaddingLeft(), contentBounds.top + getPaddingTop(), contentBounds.right - getPaddingRight(), contentBounds.bottom - getPaddingBottom());
            mItemsBackground.draw(canvas);
        }
    }

    protected void drawItemsForeground(Canvas canvas) {

    }

    protected boolean drawThumbs(Canvas canvas) {
        final boolean drawn = mScroller.isThumbVisible();
        mScroller.afterDraw(canvas);
        return drawn;
    }

    protected int[] saveViewportPos() { // 保存视口当前相对位置
        final int viewportIndex;
        final int viewportOffsetX;
        final int viewportOffsetY;
        if (mVisibleCellIndices.length > 0) {
            viewportIndex = mVisibleCellIndices[0];
            final ItemCell cell = getCell(viewportIndex);
            viewportOffsetX = getViewportBounds().left - cell.mLeft;
            viewportOffsetY = getViewportBounds().top - cell.mTop;
        } else if (mFilledCells.size() > 0) {
            final ItemCell cell = mFilledCells.get(0);
            int index = 0;
            while (index < mCells.size() && getCell(index) != cell) {
                ++index;
            }
            viewportIndex = index;
            viewportOffsetX = getViewportBounds().left - cell.mLeft;
            viewportOffsetY = getViewportBounds().top - cell.mTop;
        } else {
            viewportIndex = -1;
            viewportOffsetX = getViewportBounds().left;
            viewportOffsetY = getViewportBounds().top;
        }

        return new int[]{viewportIndex, viewportOffsetX, viewportOffsetY};
    }

    protected void restoreViewportPos(int[] pos) {
        // 保持视口相对位置不变
        final int viewportIndex = pos[0];
        final int viewportOffsetX = pos[1];
        final int viewportOffsetY = pos[2];

        if (viewportIndex < 0) {
            mScroller.doScrollTo(viewportOffsetX, viewportOffsetY);
        } else {
            mScroller.doScrollToCell(viewportIndex, viewportOffsetX, viewportOffsetY);
        }
    }

    protected Scroller newScroller() {
        return new Scroller();
    }

    private final void cancelPressing() {
        mPendingPress = null;
        mPendingUnpress = null;

        if (mPressedItemIndex >= 0) {
            final ItemCell cell = getCell(mPressedItemIndex);
            if (cell.mCellView != null) {
                cell.mCellView.setPressed(false);
            }
            mPressedItemIndex = -1;
        }
    }

    private final boolean isPressing() {
        return mPendingPress != null || mPressedItemIndex >= 0;
    }

    private final void pendPress(final int index) {
        assert mPendingPress == null;

        mPendingPress = new Runnable() {
            @Override
            public void run() {
                if (mPendingPress != this)
                    return;

                final ItemCell cell = getCell(index);
                if (cell != null) {
                    mPressedItemIndex = index;
                    if (cell.mCellView != null) {
                        cell.mCellView.setPressed(true);
                    }
                }
                mPendingPress = null;
            }
        };

        MainThread.runLater(mPendingPress, UiUtils.getTapTimeout());
    }

    private final void pendUnpress(final Runnable postUnpress) {
        assert mPendingUnpress == null;

        // 立即切换到下压状态
        if (mPendingPress != null) {
            mPendingPress.run();
            mPendingPress = null;
        }

        assert mPressedItemIndex >= 0;
        if (mPressedItemIndex >= 0) {
            mPendingUnpress = new Runnable() {
                @Override
                public void run() {
                    if (mPendingUnpress != this)
                        return;

                    final ItemCell cell = getCell(mPressedItemIndex);
                    if (cell != null) {
                        if (cell.mCellView != null) {
                            cell.mCellView.setPressed(false);
                        }

                        CurrentThread.run(postUnpress);
                    }
                    mPressedItemIndex = -1;
                    mPendingUnpress = null;
                }
            };

            MainThread.runLater(mPendingUnpress, UiUtils.getPressedStateDuration());
        }
    }

    private final void itemsAdded(int addCount, int addTo) {
        if (addCount <= 0)
            return;

        cancelPressing();
        mTouchingItemIndex = -1;

        final ItemCell[] newCells = new ItemCell[addCount];
        for (int n = 0; n < newCells.length; ++n) {
            newCells[n] = new ItemCell();
        }

        mCells.addAll(addTo, Arrays.asList(newCells));
        invalidateStruct();
    }

    private final void itemsRemoved(int removeFrom, int removeCount) {
        if (removeCount <= 0)
            return;

        cancelPressing();
        mTouchingItemIndex = -1;

        moveCells(removeFrom, removeCount, mCells.size() - removeCount);
        invalidateArrange();
    }

    private final void itemsMoved(final int moveFrom, final int moveCount, final int moveTo) {
        if (moveCount <= 0 || moveFrom == moveTo)
            return;

        cancelPressing();
        mTouchingItemIndex = -1;

        moveCells(moveFrom, moveCount, moveTo);
        invalidateStruct();
    }

    private final void itemsModified(int modifiedFrom, int modifiedCount) {
        for (int n = modifiedFrom; n < modifiedFrom + modifiedCount; ++n) {
            final ItemCell cell = getCell(n);
            cell.itemChanged(true);
            cell.mMeasuredWidth = -1;
            cell.mMeasuredHeight = -1;
        }
        invalidateStruct();
    }

    private final void itemsChanged(int count) {
        cancelPressing();
        mTouchingItemIndex = -1;

        mItemCount = count;
        mCells.ensureCapacity(mItemCount);
        for (int n = 0; n < mItemCount; ++n) {
            if (n < mCells.size()) {
                final ItemCell cell = getCell(n);
                cell.itemChanged(true);
                cell.mMeasuredWidth = -1;
                cell.mMeasuredHeight = -1;
            } else {
                final ItemCell cell = new ItemCell();
                mCells.add(cell);
            }
        }

        invalidateStruct();
    }

    private final void moveCells(final int moveFrom, final int moveCount, final int moveTo) {
        if (moveFrom == moveTo)
            return;

        final ItemCell[] movedCells = mCells.subList(moveFrom, moveFrom + moveCount).toArray(new ItemCell[0]);
        if (moveFrom < moveTo) {
            // 向后移动, 需要先把后面的项向前移动.
            // 确定需要先移动的项
            final int shiftBegin = moveFrom + moveCount;
            final int shiftEnd = Math.min(moveTo + moveCount, mCells.size());
            for (int src = shiftBegin, dst = moveFrom; src < shiftEnd; ++src, ++dst) {
                mCells.set(dst, mCells.get(src));
            }

            for (int src = 0, dst = shiftEnd - moveCount; src < movedCells.length; ++src, ++dst) {
                mCells.set(dst, movedCells[src]);
            }
        } else {
            // 向前移动, 需要先把前面的项向后移动.
            // 确定需要先移动的项
            final int shiftBegin = moveTo;
            final int shiftEnd = moveFrom;
            // 从后向前移动, 防止移动过程中, 先移动的项覆盖尚未移动的项.
            for (int src = shiftEnd - 1, dst = moveFrom + moveCount - 1; src >= shiftBegin; --src, --dst) {
                mCells.set(dst, mCells.get(src));
            }

            for (int src = 0, dst = moveTo; src < movedCells.length; ++src, ++dst) {
                mCells.set(dst, movedCells[src]);
            }
        }
    }

    protected final void invalidateLayout() {
        super.requestLayout();
    }

    protected final void invalidateStruct() {
        if (mStructValid) {
            mStructValid = false;
            invalidateArrange();
        }
    }

    protected final void invalidateArrange() {
        if (mArrangeValid) {
            mArrangeValid = false;
            invalidateVisualize();
        }
    }

    protected final void invalidateVisualize() {
        if (mVisualizeValid) {
            mVisualizeValid = false;
            invalidate();
        }
    }

    protected final void visualize() {
        if (mInVisualize)
            return;

        final int measuredWidthOld = getMeasuredWidth();
        final int measuredHeightOld = getMeasuredHeight();

        if (mVisualizeValid) {
            setMeasuredDimension(measuredWidthOld, measuredHeightOld);
            return;
        }

        mInVisualize = true;

        do {
            arrange();

            updateVisibleCells();
            for (int n = 0; n < mVisibleCellIndices.length; ++n) {
                final int visibleIndex = mVisibleCellIndices[n];
                visualizeCell(visibleIndex);
            }

            for (int n = 0; n < mPreviewCellIndices.length; ++n) {
                final int previewIndex = mPreviewCellIndices[n];
                visualizeCell(previewIndex);
            }

            for (int index : mPinnedCellIndexList) {
                visualizeCell(index);
            }

            mVisualizeValid = true;

        } while (mStructValid == false || mArrangeValid == false || mVisualizeValid == false);

        // 隐藏不可见的单元视图
        for (int n = 0; n < mFilledCells.size(); ++n) {
            final ItemCell cell = mFilledCells.get(n);
            assert cell.mCellView != null;
            if (cell.visible() == false && cell.pin() == false && cell.mCellView.getVisibility() == View.VISIBLE) {
                cell.mCellView.setVisibility(View.INVISIBLE);
            }
        }

        // 重新计算测量尺寸
        final int widthMode = MeasureSpec.getMode(mWidthSpec);
        final int widthSize = MeasureSpec.getSize(mWidthSpec);
        final int heightMode = MeasureSpec.getMode(mHeightSpec);
        final int heightSize = MeasureSpec.getSize(mHeightSpec);

        final int measuredWidth;
        if (widthMode == MeasureSpec.EXACTLY)
            measuredWidth = widthSize;
        else if (widthMode == MeasureSpec.AT_MOST)
            measuredWidth = Math.min(mContentWidth, widthSize);
        else
            measuredWidth = mContentWidth;

        final int measuredHeight;
        if (heightMode == MeasureSpec.EXACTLY)
            measuredHeight = heightSize;
        else if (heightMode == MeasureSpec.AT_MOST)
            measuredHeight = Math.min(mContentHeight, heightSize);
        else
            measuredHeight = mContentHeight;

        setMeasuredDimension(measuredWidth, measuredHeight);
        if (measuredWidth != measuredWidthOld || measuredHeight != measuredHeightOld) {
            invalidateLayout();
        }

        mInVisualize = false;
    }

    protected final int[] visibleCellIndices() {
        return mVisibleCellIndices;
    }

    private final void updateVisibleCells() {
        final Rect viewportBounds = getViewportBounds();

        // 重置所有单元可视状态
        for (int n = 0; n < mCells.size(); ++n) {
            final ItemCell cell = getCell(n);
            cell.visible(false);
            cell.preview(false);
        }

        if (mPreviewWidth == 0 && mPreviewHeight == 0) {
            mVisibleCellIndices = hitTestCells(viewportBounds);
            for (int n = 0; n < mVisibleCellIndices.length; ++n) {
                final ItemCell cell = getCell(mVisibleCellIndices[n]);
                cell.visible(true);
            }

            if (mPreviewCellIndices.length > 0) {
                mPreviewCellIndices = new int[0];
            }
        } else {
            final Rect previewBounds = new Rect(viewportBounds);
            previewBounds.inset(-mPreviewWidth, -mPreviewHeight);

            final int[] indices = hitTestCells(previewBounds);
            final ArrayList<Integer> visibleCellIndices = new ArrayList<Integer>(indices.length);
            final ArrayList<Integer> previewCellIndices = new ArrayList<Integer>(indices.length);

            for (int n = 0; n < indices.length; ++n) {
                final int index = indices[n];
                final ItemCell cell = getCell(index);

                if (viewportBounds.intersects(cell.mLeft, cell.mTop, cell.mRight, cell.mBottom)) {
                    visibleCellIndices.add(index);
                    cell.visible(true);
                } else {
                    previewCellIndices.add(index);
                    cell.preview(true);
                }
            }

            mVisibleCellIndices = new int[visibleCellIndices.size()];
            for (int n = 0; n < mVisibleCellIndices.length; ++n) {
                mVisibleCellIndices[n] = visibleCellIndices.get(n);
            }

            mPreviewCellIndices = new int[previewCellIndices.size()];
            for (int n = 0; n < mPreviewCellIndices.length; ++n) {
                mPreviewCellIndices[n] = previewCellIndices.get(n);
            }
        }
    }

    private final void visualizeCell(int index) {
        assert mStructValid;
        assert mArrangeValid;
        assert mAdapter != null;

        final ItemCell cell = getCell(index);
        final int arrangedWidth = cell.mRight - cell.mLeft;
        final int arrangedHeight = cell.mBottom - cell.mTop;
        final boolean needsMeasure = fillCell(index);

        if (cell.mCellView.getVisibility() != View.VISIBLE) {
            cell.mCellView.setVisibility(View.VISIBLE);
        }

        // 重新测量单元视图
        if (needsMeasure) {
            assert cell.layoutValid() == false;

            measureCell(index);

            // 测量宽度与排列结果不符, 需要重新排列.
            if (cell.mMeasuredWidth != arrangedWidth) {
                invalidateArrange();
            }

            // 测量高度与排列结果不符, 需要重新排列.
            if (cell.mMeasuredHeight != arrangedHeight) {
                invalidateArrange();
            }
        }

        // 排列和测量的结果相同时, 才开始调整位置或布局.
        if (cell.mMeasuredWidth == arrangedWidth && cell.mMeasuredHeight == arrangedHeight) {
            if (cell.layoutValid() == false) {
                cell.mCellView.layout(cell.mLeft, cell.mTop, cell.mRight, cell.mBottom);
                cell.layoutValid(true);
            } else {
                cell.mCellView.offsetLeftAndRight(cell.mLeft - cell.mCellView.getLeft());
                cell.mCellView.offsetTopAndBottom(cell.mTop - cell.mCellView.getTop());
            }
        }
    }

    protected final void measureCell(int index) {
        final ItemCell cell = getCell(index);

        fillCell(index);
        cell.mCellView.measure(cell.mWidthMeasureSpec, cell.mHeightMeasureSpec);
        cell.mMeasuredWidth = cell.mCellView.getMeasuredWidth();
        cell.mMeasuredHeight = cell.mCellView.getMeasuredHeight();
    }

    private final boolean fillCell(int index) {
        assert mStructValid;

        final ItemCell cell = getCell(index);
        final int arrangedWidth = cell.mRight - cell.mLeft;
        final int arrangedHeight = cell.mBottom - cell.mTop;
        final boolean needsRefresh = cell.itemChanged() || cell.mCellView == null;
        final boolean needsMeasure = needsRefresh || cell.mMeasuredWidth != arrangedWidth || cell.mMeasuredHeight != arrangedHeight;

        // 重用一个不可见的单元视图
        if (cell.mCellView == null) {
            assert cell.layoutValid() == false;

            // 尝试重用一个旧的单元视图
            for (int n = 0; n < mFilledCells.size(); ++n) {
                final ItemCell filledCell = mFilledCells.get(n);
                assert filledCell.mItemView != null;

                // 不重用动画中的视图
                if (filledCell.mItemView.getAnimation() != null)
                    continue;

                // 不重用可见/预览/固定中的视图
                if (filledCell.visible() || filledCell.preview() || filledCell.pin())
                    continue;

                cell.mCellView = filledCell.mCellView;
                cell.mCellView.mCell = cell;
                cell.mItemView = filledCell.mItemView;
                filledCell.mCellView = null;
                filledCell.mItemView = null;
                filledCell.layoutValid(false);
                mFilledCells.set(n, cell);
                break;
            }
        }

        // 创建新的单元视图
        if (cell.mCellView == null) {
            assert cell.layoutValid() == false;

            // 创建一个新的单元视图
            final ItemCellView cellView = newCellView();
            cell.mCellView = cellView;
            cell.mCellView.mCell = cell;
            mFilledCells.add(cell);
            addViewInLayout(cellView, -1, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT), true);
        }

        // 刷新元素视图
        assert cell.mCellView != null;
        if (needsRefresh) {
            assert cell.layoutValid() == false;

            final View itemView = mAdapter.getItemView(index, cell.mItemView, cell.mCellView);
            assert itemView != null;
            if (cell.mItemView == null) {
                // 创建了新的元素视图(没有元素视图可以重用)
                cell.mCellView.addView(itemView);
                cell.mItemView = itemView;
            } else if (cell.mItemView != itemView) {
                // 替换了旧的元素视图(没能重用旧的元素视图)
                if (cell.mItemView.getAnimation() == null) {
                    cell.mCellView.removeView(cell.mItemView);
                }
                cell.mCellView.addView(itemView);
                cell.mItemView = itemView;
            } else {
                // 重用了旧的元素视图
            }
        }

        cell.itemChanged(false);
        cell.layoutValid(cell.layoutValid() && !needsMeasure);
        return needsMeasure;
    }

    private final void pinCell(int index, boolean pin) {
        final ItemCell cell = getCell(index);
        if (cell.pin() == pin)
            return;

        cell.pin(pin);
        if (pin) {
            if (mPinnedCellIndexList.contains(index) == false) {
                mPinnedCellIndexList.add(index);
            }
        } else {
            ListIterator<Integer> i = mPinnedCellIndexList.listIterator();
            while (i.hasNext()) {
                if (i.next() == index) {
                    i.remove();
                    break;
                }
            }
        }
        invalidateVisualize();
    }

    private final void arrange() {
        if (mArrangeValid)
            return;

        struct();
        if (mEmpty == false) {
            onArrange();

        } else {

            arrangeEmpty();
        }
        mArrangeValid = true;
    }

    private final void arrangeEmpty() {
        if (mEmptyView == null)
            return;

        final Rect layoutBounds = UiUtils.tempRects.acquire();
        final Rect emptyBounds = UiUtils.tempRects.acquire();

        layoutBounds.set(mScroller.getContentBounds());
        layoutBounds.left += getPaddingLeft();
        layoutBounds.top += getPaddingTop();
        layoutBounds.right -= getPaddingRight();
        layoutBounds.bottom -= getPaddingBottom();

        final LayoutParams emptyParams = (LayoutParams) mEmptyView.getLayoutParams();
        Gravity.apply(emptyParams.gravity, mEmptyView.getMeasuredWidth(), mEmptyView.getMeasuredHeight(), layoutBounds, emptyBounds);

        switch (emptyParams.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.LEFT:
                emptyBounds.offset(emptyParams.leftMargin, 0);
                break;
            case Gravity.RIGHT:
                emptyBounds.offset(-emptyParams.rightMargin, 0);
                break;
            case Gravity.CENTER_HORIZONTAL:
            default:
                emptyBounds.offset(emptyParams.leftMargin - emptyParams.rightMargin, 0);
                break;
        }

        switch (emptyParams.gravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.TOP:
                emptyBounds.offset(0, emptyParams.topMargin);
                break;
            case Gravity.BOTTOM:
                emptyBounds.offset(0, -emptyParams.bottomMargin);
                break;
            case Gravity.CENTER_VERTICAL:
            default:
                emptyBounds.offset(0, emptyParams.topMargin - emptyParams.bottomMargin);
                break;
        }

        mEmptyView.layout(emptyBounds.left, emptyBounds.top, emptyBounds.right, emptyBounds.bottom);
        UiUtils.tempRects.release(layoutBounds);
        UiUtils.tempRects.release(emptyBounds);
    }

    private final void struct() {
        if (mStructValid)
            return;

        final int result = onStruct(mWidthSpec, mHeightSpec);
        if (result != STRUCT_DONE) {
            for (int n = 0; n < mItemCount; ++n) {
                final ItemCell cell = getCell(n);
                cell.mMeasuredWidth = -1;
                cell.mMeasuredHeight = -1;
                cell.layoutValid(false);
            }
        }
        mEmpty = (result & STRUCT_EMPTY) == STRUCT_EMPTY;

        if (mEmptyView != null) {
            removeViewInLayout(mEmptyView);
        }

        if (mEmpty) {
            mEmptyView = mAdapter != null ? mAdapter.getEmptyView(mEmptyView, this) : null;
            if (mEmptyView != null) {

                final LayoutParams emptyParams;
                if (mEmptyView.getLayoutParams() == null) {
                    emptyParams = generateDefaultLayoutParams();
                } else if (mEmptyView.getLayoutParams() instanceof LayoutParams) {
                    emptyParams = (LayoutParams) mEmptyView.getLayoutParams();
                } else if (mEmptyView.getLayoutParams() instanceof MarginLayoutParams) {
                    emptyParams = new LayoutParams((MarginLayoutParams) mEmptyView.getLayoutParams());
                } else {
                    emptyParams = new LayoutParams(mEmptyView.getLayoutParams());
                }

                addViewInLayout(mEmptyView, -1, emptyParams, true);
            }

            structEmpty();
        } else {

            mEmptyView = null;
        }

        mStructValid = true;
    }

    private final void structEmpty() {
        final int widthMode = MeasureSpec.getMode(mWidthSpec);
        final int widthSize = MeasureSpec.getSize(mWidthSpec);
        final int horzPadding = getPaddingLeft() + getPaddingRight();
        final int heightMode = MeasureSpec.getMode(mHeightSpec);
        final int heightSize = MeasureSpec.getSize(mHeightSpec);
        final int vertPadding = getPaddingTop() + getPaddingBottom();

        if (mEmptyView == null) {
            setContentDimension(horzPadding, vertPadding);
            return;
        }

        final LayoutParams emptyParams = (LayoutParams) mEmptyView.getLayoutParams();
        assert emptyParams != null;
        final int emptyHorzMargin = emptyParams.leftMargin + emptyParams.rightMargin;
        final int emptyVertMargin = emptyParams.topMargin + emptyParams.bottomMargin;

        final int emptyWidthSpec = getChildMeasureSpec(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, emptyParams.width);
        final int emptyHeightSpec = getChildMeasureSpec(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, emptyParams.height);
        mEmptyView.measure(emptyWidthSpec, emptyHeightSpec);

        final int desiredWidth = Math.max(horzPadding + emptyHorzMargin + mEmptyView.getMeasuredWidth(), getSuggestedMinimumWidth());
        final int desiredHeight = Math.max(vertPadding + emptyVertMargin + mEmptyView.getMeasuredHeight(), getSuggestedMinimumHeight());

        final int measuredWidth;
        switch (widthMode) {
            case MeasureSpec.EXACTLY:
                measuredWidth = widthSize;
                break;
            case MeasureSpec.AT_MOST:
                measuredWidth = Math.min(widthSize, desiredWidth);
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
                measuredWidth = desiredWidth;
                break;
        }

        final int measuredHeight;
        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                measuredHeight = heightSize;
                break;
            case MeasureSpec.AT_MOST:
                measuredHeight = Math.min(heightSize, desiredHeight);
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
                measuredHeight = desiredHeight;
                break;
        }

        if (emptyParams.width == ViewGroup.LayoutParams.MATCH_PARENT || (emptyParams.height == ViewGroup.LayoutParams.MATCH_PARENT)) {
            final int emptyWidthSpec2 = emptyParams.width == ViewGroup.LayoutParams.MATCH_PARENT ?
                    MeasureSpec.makeMeasureSpec(measuredWidth - horzPadding - emptyHorzMargin, MeasureSpec.EXACTLY) : emptyWidthSpec;
            final int emptyHeightSpec2 = emptyParams.height == ViewGroup.LayoutParams.MATCH_PARENT ?
                    MeasureSpec.makeMeasureSpec(measuredHeight - vertPadding - emptyVertMargin, MeasureSpec.EXACTLY) : emptyHeightSpec;

            mEmptyView.measure(emptyWidthSpec2, emptyHeightSpec2);
        }

        setContentDimension(Math.max(measuredWidth, horzPadding + emptyHorzMargin + mEmptyView.getMeasuredWidth()),
                Math.max(measuredHeight, vertPadding + emptyVertMargin + mEmptyView.getMeasuredHeight()));
    }

    protected ItemCellView newCellView() {
        final ItemCellView cellView = new ItemCellView(getContext());
        return cellView;
    }

    protected final void arrangeCell(int index, int left, int top, int right, int bottom) {
        assert mAdapter != null;
        final ItemCell cell = getCell(index);

        cell.mLeft = left;
        cell.mTop = top;
        cell.mRight = right;
        cell.mBottom = bottom;
    }

    protected final void setContentBounds(int left, int top, int right, int bottom) {
        mContentWidth = right - left;
        mContentHeight = bottom - top;
        mScroller.setContentBounds(left, top, right, bottom);
    }

    protected final void setContentDimension(int width, int height) {
        mContentWidth = width;
        mContentHeight = height;
        mScroller.setContentWidth(mContentWidth);
        mScroller.setContentHeight(mContentHeight);
    }

    protected final boolean inCell(int index, Point point) {
        final ItemCell cell = getCell(index);
        return point.x >= cell.mLeft && point.y >= cell.mTop && point.x < cell.mRight && point.y < cell.mBottom;
    }

    protected final boolean intersectsCell(int index, Rect rect) {
        final ItemCell cell = getCell(index);
        return rect.intersects(cell.mLeft, cell.mTop, cell.mRight, cell.mBottom);
    }

    protected final int getCellLeft(int index) {
        final ItemCell cell = getCell(index);
        return cell.mLeft;
    }

    protected final int getCellTop(int index) {
        final ItemCell cell = getCell(index);
        return cell.mTop;
    }

    protected final int getCellRight(int index) {
        final ItemCell cell = getCell(index);
        return cell.mRight;
    }

    protected final int getCellBottom(int index) {
        final ItemCell cell = getCell(index);
        return cell.mBottom;
    }

    protected final void setCellWidthMeasureSpec(int index, int widthMeasureSpec) {
        final ItemCell cell = getCell(index);
        cell.mWidthMeasureSpec = widthMeasureSpec;
    }

    protected final void setCellHeightMeasureSpec(int index, int heightMeasureSpec) {
        final ItemCell cell = getCell(index);
        cell.mHeightMeasureSpec = heightMeasureSpec;
    }

    protected final int getCellMeasuredWidth(int index) {
        final ItemCell cell = getCell(index);
        return cell.mMeasuredWidth;
    }

    protected final int getCellMeasuredHeight(int index) {
        final ItemCell cell = getCell(index);
        return cell.mMeasuredHeight;
    }

    protected final boolean isCellItemChanged(int index) {
        final ItemCell cell = getCell(index);
        return cell.itemChanged();
    }

    protected final ItemCell getCell(int index) {
        if (checkCell(index) == false)
            return null;

        final ItemCell cell = mCells.get(index);
        assert cell != null;
        return cell;
    }

    protected final boolean checkCell(int index) {
        return index >= 0 && index < mCells.size();
    }

    protected final int getCellsMarginHorizontal() {
        return getCellsMarginLeft() + getCellsMarginRight();
    }

    protected final int getCellsMarginVertical() {
        return getCellsMarginTop() + getCellsMarginBottom();
    }

    protected final int getCellsMarginLeft() {
        return getPaddingLeft() + mItemsBackgroundPadding.left;
    }

    protected final int getCellsMarginTop() {
        return getPaddingTop() + mItemsBackgroundPadding.top;
    }

    protected final int getCellsMarginRight() {
        return getPaddingRight() + mItemsBackgroundPadding.right;
    }

    protected final int getCellsMarginBottom() {
        return getPaddingBottom() + mItemsBackgroundPadding.bottom;
    }

    // ### 抽象实现函数 ###
    protected abstract int hitTestCell(Point point);

    protected abstract int[] hitTestCells(Rect rect);

    protected abstract void onArrange();

    protected abstract int onStruct(int widthSpec, int heightSpec);

    // ### 内嵌类 ###
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public int gravity = Gravity.CENTER;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);

            this.gravity = gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super((ViewGroup.MarginLayoutParams) source);
            gravity = source.gravity;
        }
    }

    public static interface OnItemClickListener {
        void onItemClick(ItemsView itemsView, View itemView, int index);
    }

    public static interface OnItemLongPressListener {
        void onItemLongPress(ItemsView itemsView, View itemView, int index);
    }

    private static class ItemCell {
        private final static int FLAG_ITEM_CHANGED = 0x01;
        private final static int FLAG_LAYOUT_VALID = 0x02;
        private final static int FLAG_FORCE_VISUAL = 0x04;
        private final static int FLAG_VISIBLE = 0x08;
        private final static int FLAG_PIN = 0x10;
        private final static int FLAG_PREVIEW = 0x20;

        private ItemCellView mCellView = null;
        private View mItemView = null;
        private int mWidthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        private int mHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        private int mMeasuredWidth = -1;
        private int mMeasuredHeight = -1;
        private int mLeft = 0;
        private int mTop = 0;
        private int mBottom = 0;
        private int mRight = 0;
        private int mFlags = FLAG_ITEM_CHANGED;
        private Transformation mTransformation = null;

        public boolean itemChanged() {
            return (mFlags & FLAG_ITEM_CHANGED) == FLAG_ITEM_CHANGED;
        }

        public void itemChanged(boolean changed) {
            mFlags = changed ? (mFlags | FLAG_ITEM_CHANGED) : (mFlags & ~FLAG_ITEM_CHANGED);
        }

        public boolean layoutValid() {
            return (mFlags & FLAG_LAYOUT_VALID) == FLAG_LAYOUT_VALID;
        }

        public void layoutValid(boolean valid) {
            mFlags = valid ? (mFlags | FLAG_LAYOUT_VALID) : (mFlags & ~FLAG_LAYOUT_VALID);
        }

        public boolean forceVisual() {
            return (mFlags & FLAG_FORCE_VISUAL) == FLAG_FORCE_VISUAL;
        }

        public void forceVisual(boolean force) {
            mFlags = force ? (mFlags | FLAG_FORCE_VISUAL) : (mFlags & ~FLAG_FORCE_VISUAL);
        }

        public boolean visible() {
            return (mFlags & FLAG_VISIBLE) == FLAG_VISIBLE;
        }

        public void visible(boolean visible) {
            mFlags = visible ? (mFlags | FLAG_VISIBLE) : (mFlags & ~FLAG_VISIBLE);
        }

        public boolean pin() {
            return (mFlags & FLAG_PIN) == FLAG_PIN;
        }

        public void pin(boolean pin) {
            mFlags = pin ? (mFlags | FLAG_PIN) : (mFlags & ~FLAG_PIN);
        }

        public boolean preview() {
            return (mFlags & FLAG_PREVIEW) == FLAG_PREVIEW;
        }

        public void preview(boolean preview) {
            mFlags = preview ? (mFlags | FLAG_PREVIEW) : (mFlags & ~FLAG_PREVIEW);
        }

        public void setOffset(int dx, int dy) {
            if (dx == 0 && dy == 0) {
                if (mTransformation != null) {
                    if (Float.compare(mTransformation.getAlpha(), 1.0f) == 0) {
                        mTransformation = null;
                    } else {
                        mTransformation.getMatrix().reset();
                        mTransformation.setTransformationType(Transformation.TYPE_ALPHA);
                    }
                }
            } else {
                if (mTransformation == null) {
                    mTransformation = new Transformation();
                }
                mTransformation.getMatrix().reset();
                mTransformation.getMatrix().preTranslate(dx, dy);
                mTransformation.setTransformationType(mTransformation.getTransformationType() | Transformation.TYPE_MATRIX);
            }

            if (mCellView != null) {
                mCellView.invalidate();
            }
        }

        public void setAlpha(float alpha) {
            if (Float.compare(alpha, 1.0f) == 0) {
                if (mTransformation != null) {
                    if ((mTransformation.getTransformationType() & Transformation.TYPE_MATRIX) != Transformation.TYPE_MATRIX) {
                        mTransformation = null;
                    } else {
                        mTransformation.setAlpha(alpha);
                        mTransformation.setTransformationType(Transformation.TYPE_MATRIX);
                    }
                }
            } else {
                if (mTransformation == null) {
                    mTransformation = new Transformation();
                }
                mTransformation.setAlpha(alpha);
                mTransformation.setTransformationType(mTransformation.getTransformationType() | Transformation.TYPE_ALPHA);
            }

            if (mCellView != null) {
                mCellView.invalidate();
            }
        }
    }

    protected class ItemCellView extends FrameLayout {
        private ItemCell mCell = null;

        public ItemCellView(Context context) {
            this(context, null);
        }

        public ItemCellView(Context context, AttributeSet attrs) {
            super(context, attrs);

            setVisibility(View.INVISIBLE);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);

            for (int n = getChildCount() - 2; n >= 0; --n) {
                final View itemView = getChildAt(n);

                if (itemView.getAnimation() == null) {
                    removeViewInLayout(itemView);
                }
            }
        }

        @Override
        public final void forceLayout() {
            super.forceLayout();
            if (mCell == null)
                return;

            mCell.mMeasuredWidth = -1;
            mCell.mMeasuredHeight = -1;
            mCell.layoutValid(false);
            invalidateArrange();
        }

        @Override
        public final void requestLayout() {
            forceLayout();
        }
    }

    protected class Scroller extends ViewScroller {
        public Scroller() {
            super(ItemsView.this);
        }

        public final void doScrollToCell(int index, float offsetX, float offsetY) {
            if (mItemCount > 0) {
                final float x, y;
                index = Math.max(0, Math.min(index, mItemCount - 1));
                final ItemCell cell = getCell(index);

                x = cell.mLeft + offsetX;
                y = cell.mTop + offsetY;
                doScrollTo(x, y);
            }
        }

        @Override
        protected void onScrollStateChanged(ScrollState oldState, ScrollState newState) {
            if (oldState == ScrollState.IDLE && newState == ScrollState.DRAG) {
                cancelPressing();
            }
        }

        @Override
        protected void onScroll(boolean viewportChanged) {
            if (viewportChanged) {
                invalidateVisualize();
            }
            visualize();
        }

        @Override
        protected void onTouchDown(PointF downPoint) {
            if (mScrollState != ScrollState.IDLE)
                return;

            if (mOnItemClickListener == null && mOnItemLongPressListener == null)
                return;

            if (isPressing())
                return;

            final Point point = new Point(Math.round(downPoint.x), Math.round(downPoint.y));
            view2content(point);

            mTouchingItemIndex = hitTestCell(point);
            if (mTouchingItemIndex >= 0) {
                pendPress(mTouchingItemIndex);
            }
        }

        @Override
        protected void onTouchUp(PointF upPoint) {
            if (mTouchingItemIndex < 0)
                return;

            // !onTouchCancel && !onTap
            cancelPressing();
            mTouchingItemIndex = -1;
        }

        @Override
        protected void onTouchCancel(PointF cancelPoint) {
            cancelPressing();
            mTouchingItemIndex = -1;
        }

        @Override
        protected void onTap(PointF tapPoint) {
            if (mOnItemClickListener == null || mTouchingItemIndex < 0) {
                super.onTap(tapPoint);
                return;
            }

            final int index = mTouchingItemIndex;
            pendUnpress(new Runnable() {
                @Override
                public void run() {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(ItemsView.this, getItemView(index), index);
                    }
                }
            });
            mTouchingItemIndex = -1;
        }

        @Override
        protected void onLongPress(PointF pressPoint) {
            if (mOnItemLongPressListener == null || mTouchingItemIndex < 0) {
                super.onLongPress(pressPoint);
                return;
            }

            mOnItemLongPressListener.onItemLongPress(ItemsView.this, getItemView(mTouchingItemIndex), mTouchingItemIndex);
        }

        @Override
        protected void superViewScrollTo(int x, int y) {
            ItemsView.super.scrollTo(x, y);
        }
    }
}
