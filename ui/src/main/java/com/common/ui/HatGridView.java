package com.common.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

public class HatGridView extends ViewGroup implements Scrollable {
    // ### 常量 ###
    public static final int STRETCH_NONE = 0;
    public static final int STRETCH_COLUMN_SPACING = 1;
    public static final int STRETCH_COLUMN_WIDTH = 2;

    // ### 值域 ###
    private final GridView mGridView;
    private final FrameLayout mFooterFrameView;
    private final FrameLayout mHeaderFrameView;
    private final FrameLayout mHatView;
    private final LinearLayout mHatLinearView;
    private final FrameLayout mHatBkFrameView;
    private final FrameLayout mHatTipFrameView;
    private final FrameLayout mHatBodyFrameView;
    private final FrameLayout mBrimFrameView;
    private final FrameLayout mTitleFrameView;
    private final ImageView mBackToTopView;
    private final Rect mGridPadding = new Rect();
    private final ProxyAdapter mProxyAdapter;
    private int mHeaderSink = 0;
    private int mFooterRise = 0;
    private boolean mClipGridToBrim = true;
    private boolean mHatPushable = true;
    private int mColumnCount = 1;
    private int mVerticalSpacing = 0;
    private int mHatHeight = 0;
    private boolean mHatTipDockable = false;
    private HatTipState mHatTipState = HatTipState.UNDOCKED;
    private DoExpand mRunningExpand = null;
    private Runnable mPendingHideFastToTop = null;
    private OnScrollListener mOnScrollListener = null;
    private OnHatTipStateChangeListener mOnHatTipStateChangeListener = null;

    // ### 构造函数 ###
    public HatGridView(Context context) {
        this(context, null);
    }
    public HatGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);

        // 标题框
        mTitleFrameView = new FrameLayout(context);

        // 帽子视图
        mHatView = new FrameLayout(context) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (mHatPushable == false) {
                    final int maxCover = getHatVisibleHeight(); // 帽子底部可以被网格遮住的最大高度
                    final int gridPushY = mGridView.getScrollY(); // 网格被推入的高度
                    final int hatPushY = mHatView.getScrollY() + hatOffset(); // 帽子被推入的高度
                    final int clipBottom = getHeight() - Math.min(gridPushY - hatPushY, maxCover);
                    canvas.clipRect(0, 0, getWidth(), clipBottom);
                }
                super.dispatchDraw(canvas);
            }
        };

        // 帽子背景框
        mHatBkFrameView = new FrameLayout(context) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                canvas.clipRect(0, getScrollY(), getWidth(), getScrollY() + getHeight() - mBrimFrameView.getHeight());
                super.dispatchDraw(canvas);
            }
        };
        FrameLayout.LayoutParams bkParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mHatView.addView(mHatBkFrameView, bkParams);

        // 帽子布局
        mHatLinearView = new LinearLayout(context) {
            @Override
            public final boolean onInterceptTouchEvent(MotionEvent ev) {
                final MotionEvent gridEvent = UiUtils.obtainMotionEvent(ev, this, mGridView);
                final boolean handled = mGridView.onInterceptTouchEvent(gridEvent);
                gridEvent.recycle();
                return handled;
            }
            @Override
            public final boolean onTouchEvent(MotionEvent ev) {
                final MotionEvent gridEvent = UiUtils.obtainMotionEvent(ev, this, mGridView);
                final boolean handled = mGridView.onTouchEvent(gridEvent);
                gridEvent.recycle();
                return handled;
            }
            @Override
            public final void dispatchDraw(Canvas canvas) {
                final int clipTop2Linear = titleBottom2Hat() - mHatLinearView.getTop();
                canvas.clipRect(0, clipTop2Linear, getWidth(), getHeight());
                super.dispatchDraw(canvas);
            }

        };
        FrameLayout.LayoutParams linearParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        linearParams.gravity = Gravity.BOTTOM;
        mHatLinearView.setOrientation(LinearLayout.VERTICAL);
        mHatLinearView.setClipChildren(false);
        mHatLinearView.setClipToPadding(false);
        mHatView.addView(mHatLinearView, linearParams);

        // 帽尖框
        mHatTipFrameView = new FrameLayout(context);
        mHatTipFrameView.setClipChildren(false);
        mHatTipFrameView.setClipToPadding(false);
        mHatTipFrameView.setMinimumHeight(UiUtils.getScaledOverScrollHeight(getContext()));
        mHatLinearView.addView(mHatTipFrameView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 帽身框
        mHatBodyFrameView = new FrameLayout(context);
        mHatBodyFrameView.setClipChildren(false);
        mHatBodyFrameView.setClipToPadding(false);
        mHatLinearView.addView(mHatBodyFrameView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 帽檐框
        mBrimFrameView = new FrameLayout(context);
        mHatLinearView.addView(mBrimFrameView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 页眉框
        mHeaderFrameView = new FrameLayout(context) {
            @Override
            public final boolean onInterceptTouchEvent(MotionEvent ev) {
                final MotionEvent gridEvent = UiUtils.obtainMotionEvent(ev, this, mGridView);
                final boolean handled = mGridView.onInterceptTouchEvent(gridEvent);
                gridEvent.recycle();
                return handled;
            }
            @Override
            public final boolean onTouchEvent(MotionEvent ev) {
                final MotionEvent gridEvent = UiUtils.obtainMotionEvent(ev, this, mGridView);
                final boolean handled = mGridView.onTouchEvent(gridEvent);
                gridEvent.recycle();
                return handled;
            }
        };

        // 页脚框
        mFooterFrameView = new FrameLayout(context) {
            @Override
            public final boolean onInterceptTouchEvent(MotionEvent ev) {
                final MotionEvent gridEvent = UiUtils.obtainMotionEvent(ev, this, mGridView);
                final boolean handled = mGridView.onInterceptTouchEvent(gridEvent);
                gridEvent.recycle();
                return handled;
            }
            @Override
            public final boolean onTouchEvent(MotionEvent ev) {
                final MotionEvent gridEvent = UiUtils.obtainMotionEvent(ev, this, mGridView);
                final boolean handled = mGridView.onTouchEvent(gridEvent);
                gridEvent.recycle();
                return handled;
            }

        };

        // 网格视图
        mGridView = new GridView(context);
        mGridView.setThumbEnabled(true);
        mGridView.setRowSpacing(mVerticalSpacing);
        mGridView.setNumColumns(mColumnCount);
        mGridView.setOnScrollListener(new ItemsView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(Scrollable scrollable, ScrollState oldState, ScrollState newState) {
                if (newState == ScrollState.IDLE) {
                    if (mHatTipState == HatTipState.DOCKING) {
                        hatTipState(HatTipState.DOCKED);
                    } else if (mHatTipState == HatTipState.UNDOCKING) {
                        hatTipState(HatTipState.UNDOCKED);
                    }
                }
                updateViewWhenScrollStateChanged(oldState, newState);
                if (mOnScrollListener != null) {
                    mOnScrollListener.onScrollStateChanged(scrollable, oldState, newState);
                }
            }

            @Override
            public void onScroll(Scrollable scrollable, boolean viewportChanged) {
                final int gridScrollY = mGridView.getViewportBounds().top;
                final int gridPushY = gridScrollY; // 网格被推入的高度
                final int hatPushY = mHatView.getScrollY() + hatOffset(); // 帽子被推入的高度
                if (mHatPushable || gridPushY < hatPushY) {
                    final int maxHatScrollY = -hatOffset() + hatBodyHeight();
                    int hatScrollY = -hatOffset() + gridScrollY;
                    hatScrollY = Math.max(0, Math.min(hatScrollY, maxHatScrollY));

                    mHatView.scrollTo(0, hatScrollY);
                    mHatBkFrameView.scrollTo(0, -hatScrollY / 2);
                    mHatLinearView.invalidate();
                } else {
                    mHatView.invalidate();
                }

                mHeaderFrameView.offsetTopAndBottom((mGridView.getPaddingTop() - headerHeight() + headerSink() - gridScrollY) - mHeaderFrameView.getTop());
                mFooterFrameView.offsetTopAndBottom((mGridView.getContentHeight() - mGridPadding.bottom - footerHeight() - footerRise() - gridScrollY) - mFooterFrameView.getTop());
//				mHeaderFrameView.scrollTo(0, -(mGridView.getPaddingTop() - headerHeight() + headerSink()) + gridScrollY);
//				mFooterFrameView.scrollTo(0, -(mGridView.getContentHeight() - mGridPadding.bottom - footerHeight() - footerRise()) + gridScrollY);

                if (mOnScrollListener != null) {
                    mOnScrollListener.onScroll(scrollable, viewportChanged);
                }
            }
        });

        addView(mGridView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(mHeaderFrameView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(mFooterFrameView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(mHatView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(mTitleFrameView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mProxyAdapter = new ProxyAdapter();
        mGridView.setAdapter(mProxyAdapter);

        mBackToTopView = new ImageView(context);
        mBackToTopView.setScaleType(ScaleType.CENTER);
//        mBackToTopView.setImageResource(R.drawable.general__hat_grid_view__back_to_top);
//        mBackToTopView.setBackgroundResource(R.drawable.general__shared__button_circular_48dip);
        addView(mBackToTopView, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mBackToTopView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideBackToTopView();
                scrollSmoothlyTo(0, 0, UiUtils.ANIM_DURATION_NORMAL, null, null);
            }
        });
        mBackToTopView.setEnabled(false);
        mBackToTopView.setVisibility(View.INVISIBLE);
    }

    // ### 属性 ###
    public final ItemsAdapter getAdapter() {
        return mProxyAdapter.getBaseAdapter();
    }
    public final void setAdapter(GridAdapter adapter) {
        mProxyAdapter.setBaseAdapter(adapter);
    }
    public final int getStretchMode() {
        return mGridView.getStretchMode();
    }
    public final void setStretchMode(int mode) {
        mGridView.setStretchMode(mode);
    }
    public final Drawable getItemsBackground() {
        return mGridView.getItemsBackground();
    }
    public final void setItemsBackground(int resId) {
        mGridView.setItemsBackground(resId);
    }
    public final void setItemsBackground(Drawable background) {
        mGridView.setItemsBackground(background);
    }
    public final View getItemView(int index) {
        return mGridView.getItemView(index);
    }
    public final int getNumColumns() {
        return mGridView.getNumColumns();
    }
    public final void setNumColumns(int num) {
        mGridView.setNumColumns(num);
    }
    public final int getColumnCount() {
        return mGridView.getColumnCount();
    }
    public final Drawable getColumnDivider() {
        return mGridView.getColumnDivider();
    }
    public final void setColumnDivider(Drawable divider) {
        mGridView.setColumnDivider(divider);
    }
    public final void setColumnDivider(Drawable divider, boolean keepSpacing) {
        mGridView.setColumnDivider(divider, keepSpacing);
    }
    public final int getColumnSpacing() {
        return mGridView.getDesiredColumnSpacing();
    }
    public final void setColumnSpacing(int spacing) {
        mGridView.setDesiredColumnSpacing(spacing);
    }
    public final int getRowCount() {
        return mGridView.getRowCount();
    }
    public final Drawable getRowBackground() {
        return mGridView.getRowBackground();
    }
    public final void setRowBackground(int resId) {
        mGridView.setRowBackground(getResources().getDrawable(resId));
    }
    public final void setRowBackground(Drawable background) {
        mGridView.setRowBackground(background);
    }
    public final Drawable getRowDivider() {
        return mGridView.getRowDivider();
    }
    public final void setRowDivider(int resId) {
        mGridView.setRowDivider(resId);
    }
    public final void setRowDivider(Drawable divider) {
        mGridView.setRowDivider(divider);
    }
    public final void setRowDivider(Drawable divider, boolean keepSpacing) {
        mGridView.setRowDivider(divider, keepSpacing);
    }
    public final int getRowSpacing() {
        return mGridView.getRowSpacing();
    }
    public final void setRowSpacing(int spacing) {
        mGridView.setRowSpacing(spacing);
    }
    public final ScrollState getGridScrollState() {
        return mGridView.getScrollState();
    }
    public final int getGridScrollX() {
        return mGridView.getScrollX();
    }
    public final int getGridScrollY() {
        return mGridView.getScrollY();
    }
    public final void setOnItemClickListener(final OnItemClickListener listener) {
        mGridView.setOnItemClickListener(new ItemsView.OnItemClickListener() {
            @Override
            public void onItemClick(ItemsView itemsView, View itemView, int index) {
                listener.onItemClick(HatGridView.this, itemView, index);
            }
        });
    }
    public final void setOnItemLongPressListener(final OnItemLongPressListener listener) {
        mGridView.setOnItemLongPressListener(new ItemsView.OnItemLongPressListener() {
            @Override
            public void onItemLongPress(ItemsView itemsView, View itemView, int index) {
                listener.onItemLongPress(HatGridView.this, itemView, index);
            }
        });
    }
    public final OnHatTipStateChangeListener getOnHatTipStateChange() {
        return mOnHatTipStateChangeListener;
    }
    public final void setOnHatTipStateChange(OnHatTipStateChangeListener listener) {
        mOnHatTipStateChangeListener = listener;
    }
    public final int getHeaderSink() {
        return mHeaderSink;
    }
    public final void setHeaderSink(int sink) {
        mHeaderSink = sink;
        requestLayout();
    }
    public final int getFooterRise() {
        return mFooterRise;
    }
    public final void setFooterRise(int rise) {
        mFooterRise = rise;
        requestLayout();
    }
    public final View getTitleView() {
        if (mTitleFrameView.getChildCount() > 0) {
            return mTitleFrameView.getChildAt(0);
        }
        return null;
    }
    public final View setTitleView(int viewId) {
        mTitleFrameView.removeAllViews();
        mTitleFrameView.setClickable(false);

        if (viewId != 0) {
            final View titleView = LayoutInflater.from(getContext()).inflate(viewId, mTitleFrameView, false);
            mTitleFrameView.addView(titleView);
            mTitleFrameView.setClickable(true);
            return titleView;
        }

        return null;
    }
    public final void setTitleView(View view) {
        mTitleFrameView.removeAllViews();
        mTitleFrameView.setClickable(false);

        if (view != null) {
            if (view.getLayoutParams() == null) {
                view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            }

            mTitleFrameView.addView(view);
            mTitleFrameView.setClickable(true);
        }

    }
    public final View setHatBackgroundView(int viewId) {
        mHatBkFrameView.removeAllViews();

        if (viewId != 0) {
            final View bkView = LayoutInflater.from(getContext()).inflate(viewId, mHatBkFrameView, false);
            mHatBkFrameView.addView(bkView);
            return bkView;
        }

        return null;
    }
    public final void setHatBackgroundView(View view) {
        mHatBkFrameView.removeAllViews();

        if (view != null) {
            if (view.getLayoutParams() == null) {
                view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            mHatBkFrameView.addView(view);
        }

    }
    public final View getHatTipView() {
        if (mHatTipFrameView.getChildCount() > 0) {
            return mHatTipFrameView.getChildAt(0);
        }
        return null;
    }
    public final View setHatTipView(int viewId) {
        mHatTipFrameView.removeAllViews();

        if (viewId != 0) {
            final View tipView = LayoutInflater.from(getContext()).inflate(viewId, mHatTipFrameView, false);
            mHatTipFrameView.addView(tipView);
            return tipView;
        }

        return null;
    }
    public final void setHatTipView(View view) {
        mHatTipFrameView.removeAllViews();

        if (view != null) {
            if (view.getLayoutParams() == null) {
                view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            mHatTipFrameView.addView(view);
        }

    }
    public final boolean getHatTipDockable() {
        return mHatTipDockable;
    }
    public final void setHatTipDockable(boolean dockable) {
        if (mHatTipDockable != dockable) {
            mHatTipDockable = dockable;

            if (mHatTipDockable == false && mHatTipState == HatTipState.DOCKED) {
                springBackSmoothly();
            }
        }
    }
    public final void setHatTipMargin(int left, int top, int right, int bottom) {
        mHatTipFrameView.setPadding(left, top, right, bottom);
    }
    public final View setHatBodyView(int viewId) {
        mHatBodyFrameView.removeAllViews();

        if (viewId != 0) {
            final View bodyView = LayoutInflater.from(getContext()).inflate(viewId, mHatBodyFrameView, false);
            mHatBodyFrameView.addView(bodyView);
            return bodyView;
        }

        return null;
    }
    public final void setHatBodyView(View view) {
        mHatBodyFrameView.removeAllViews();

        if (view != null) {
            if (view.getLayoutParams() == null) {
                view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            mHatBodyFrameView.addView(view);
        }

    }
    public final View getHatBodyView() {
        if (mHatBodyFrameView.getChildCount() >= 1) {
            return mHatBodyFrameView.getChildAt(0);
        } else {
            return null;
        }
    }
    public final int getHatBodyVisibleHeight() {
        return mHatView.getHeight() - titleBottom2Hat() - mBrimFrameView.getHeight();
    }
    public final View getBrimView() {
        if (mBrimFrameView.getChildCount() > 0) {
            return mBrimFrameView.getChildAt(0);
        }
        return null;
    }
    public final View setBrimView(int viewId) {
        mBrimFrameView.removeAllViews();

        if (viewId != 0) {
            final View brimView = LayoutInflater.from(getContext()).inflate(viewId, mBrimFrameView, false);
            mBrimFrameView.addView(brimView);
            return brimView;
        }

        return null;
    }
    public final void setBrimView(View view) {
        mBrimFrameView.removeAllViews();

        if (view != null) {
            if (view.getLayoutParams() == null) {
                view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            mBrimFrameView.addView(view);
        }

    }
    public final View getHatView() {
        return mHatLinearView;
    }
    public final int getHatVisibleHeight() {
        return mHatView.getHeight() - titleBottom2Hat();
    }
    public final View getHatBackgroundView() {
        return mHatBkFrameView.getChildAt(0);
    }
    public final void setFastToTopEnabled(boolean enabled) {
        mBackToTopView.setEnabled(enabled);
    }

    // ### 方法 ###
    public final int getGridPaddingLeft() {
        return mGridPadding.left;
    }
    public final int getGridPaddingRight() {
        return mGridPadding.right;
    }
    public final int getGridPaddingTop() {
        return mGridPadding.top;
    }
    public final int getGridPaddingBottom() {
        return mGridPadding.bottom;
    }
    public final void setGridPadding(int left, int top, int right, int bottom) {
        mGridPadding.set(left, top, right, bottom);
        mHeaderFrameView.setPadding(left, 0, right, 0);
        mFooterFrameView.setPadding(left, 0, right, 0);
        requestLayout();
        invalidate();
    }
    public final int getItemCount() {
        return mGridView.getItemCount();
    }
    public final View[] getItemViews() {
        return mGridView.getItemViews();
    }
    public final int[] getVisibleItemIndices() {
        return mGridView.getVisibleItemIndices();
    }
    public final int getFirstVisibleItemIndex() {
        return mGridView.getFirstVisibleItemIndex();
    }
    public final int getLastVisibleItemIndex() {
        return mGridView.getLastVisibleItemIndex();
    }
    public final int getVisibleItemCount() {
        return mGridView.getVisibleItemCount();
    }
    public final int getPreviewHeight() {
        return mGridView.getPreviewHeight();
    }
    public final void setPreviewHeight(int height) {
        mGridView.setPreviewHeight(height);
    }
    public void requestGroupVisible(int groupIndex) {
        mGridView.requestGroupVisible(groupIndex);

        // 防止元素被帽檐遮挡
        Rect itemBounds = mGridView.getGroupBounds(groupIndex);
        if (itemBounds.top < mGridView.getViewportBounds().top + mBrimFrameView.getHeight()) {
            mGridView.scrollBy(0, itemBounds.top - (mGridView.getViewportBounds().top + mBrimFrameView.getHeight()));
        }
        mGridView.springBack();
    }
    public final void requestItemVisible(int index) {
        if (index < 0 || index >= mGridView.getItemCount())
            return;

        mGridView.requestItemVisible(index);

        // 防止元素被帽檐遮挡
        Rect itemBounds = mGridView.getItemBounds(index);
        if (itemBounds.top < mGridView.getViewportBounds().top + mBrimFrameView.getHeight()) {
            mGridView.scrollBy(0, itemBounds.top - (mGridView.getViewportBounds().top + mBrimFrameView.getHeight()));
        }
        mGridView.springBack();
    }
    public final void requestItemInRect(int index, Rect rect, int gravity) {
        mGridView.requestItemInRect(index, rect, gravity);
//
//        Rect itemBounds = mGridView.getItemBounds(index);
//        if (itemBounds.top < mGridView.getViewportBounds().top + mBrimFrameView.getHeight()) {
//            mGridView.scrollBy(0, itemBounds.top - (mGridView.getViewportBounds().top + mBrimFrameView.getHeight()));
//        }
//        mGridView.springBack();
    }
    public final int getGroupCount() {
        return mGridView.getGroupCount();
    }
    public final int getGroupSize(int groupIndex) {
        return mGridView.getGroupSize(groupIndex);
    }
    public final int getGroupRowCount(int groupIndex) {
        return mGridView.getGroupRowCount(groupIndex);
    }
    public final int getGroupFirstRowIndex(int groupIndex) {
        return mGridView.getGroupFirstRowIndex(groupIndex);
    }
    public final int[] getGroupPosition(int itemIndex) {
        return mGridView.getGroupPosition(itemIndex);
    }
    public final int getItemIndex(int groupIndex, int groupItemIndex) {
        return mGridView.getItemIndex(groupIndex, groupItemIndex);
    }
    public final boolean isHatTipVisible() {
        return mGridView.getScrollY() < hatTipBottom2Grid() - mTitleFrameView.getHeight();
    }
    public final boolean isHatTipFullyVisible() {
        return mGridView.getScrollY() < hatTipTop2Grid() - mTitleFrameView.getHeight();
    }
    public final boolean isItemVisible(int index) {
        return mGridView.isItemVisible(index);
    }
    public final Rect getItemBounds(int index) {
        return mGridView.getItemBounds(index);
    }
    public final int hitTestItemCell(int x, int y) {
        return mGridView.hitTestItemCell(x, y);
    }
    public final int[] hitTestVisibleItems(Rect rect) {
        return mGridView.hitTestVisibleItems(rect);
    }
    public final void setClipGridToBrim(boolean clipToBrim) {
        mClipGridToBrim = clipToBrim;
        invalidate();
    }
    public final void setHatPushable(boolean pushable) {
        mHatPushable = pushable;
        scrollBy(0, 0);
    }
    public final void undockHatTip() {
        if (mHatTipState == HatTipState.DOCKED) {
            mHatTipState = HatTipState.UNDOCKING;

            if (getScrollState() == ScrollState.IDLE) {
                springBackSmoothly();
            }
        }
    }
    public final boolean collapse() {
        if (mRunningExpand == null || mRunningExpand.mIsCollapsing)
            return false;

        final AlphaAnimation collapseAnim = new AlphaAnimation(mRunningExpand.mExpandProgress, 0.0f);
        collapseAnim.initialize(0, 0, 0, 0);
        collapseAnim.setDuration(UiUtils.ANIM_DURATION_LONG);

        if (mRunningExpand.mTopMaskView != null) {
            final AlphaAnimation inAnim = new AlphaAnimation(mRunningExpand.mExpandProgress, 0.0f);
            inAnim.setDuration(UiUtils.ANIM_DURATION_LONG);
            inAnim.setFillAfter(true);
            mRunningExpand.mTopMaskView.startAnimation(inAnim);
        }

        if (mRunningExpand.mBottomMaskView != null) {
            final AlphaAnimation inAnim = new AlphaAnimation(mRunningExpand.mExpandProgress, 0.0f);
            inAnim.setDuration(UiUtils.ANIM_DURATION_LONG);
            inAnim.setFillAfter(true);
            mRunningExpand.mBottomMaskView.startAnimation(inAnim);
        }

        mRunningExpand.mIsCollapsing = true;
        mRunningExpand.mExpandAnim = collapseAnim;
        removeCallbacks(mRunningExpand);
        post(mRunningExpand);
        return true;
    }
    public final boolean expandAt(int index, View subView, int height, View topMaskView, View bottomMaskView, Runnable onExpanded, Runnable onCollapsed) {
        if (mRunningExpand != null)
            return false;

        mGridView.setEnabled(false);
        subView.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

        final int rowIndex = index / mGridView.getColumnCount();
        final Rect rowBounds = mGridView.getRowBounds(rowIndex);
        final Rect viewportBounds = mGridView.getViewportBounds();
        final int expandFrom = rowBounds.bottom;
        final int expandUp = expandFrom - (viewportBounds.bottom - height);
        final int expandDown = viewportBounds.bottom - expandFrom;

        final Rect gridBounds = new Rect(0, 0, mGridView.getWidth(), mGridView.getHeight());
        if (expandUp >= 0) {
            gridBounds.bottom += expandUp;
        } else {
            gridBounds.top += expandUp;
        }
        final int[] expandItemIndices = mGridView.hitTestItemCells(gridBounds);
        for (int n = 0; n < expandItemIndices.length; ++n) {
            final int i = expandItemIndices[n];
            mGridView.forceItemVisual(i, true);
        }

        final AlphaAnimation expandAnim = new AlphaAnimation(0.0f, 1.0f);
        expandAnim.initialize(0, 0, 0, 0);
        expandAnim.setDuration(UiUtils.ANIM_DURATION_LONG);
        expandAnim.start();

        if (topMaskView != null) {
            final AlphaAnimation inAnim = new AlphaAnimation(0.0f, 1.0f);
            inAnim.setDuration(UiUtils.ANIM_DURATION_LONG);
            inAnim.setFillAfter(true);
            topMaskView.startAnimation(inAnim);
        }

        if (bottomMaskView != null) {
            final AlphaAnimation inAnim = new AlphaAnimation(0.0f, 1.0f);
            inAnim.setDuration(UiUtils.ANIM_DURATION_LONG);
            inAnim.setFillAfter(true);
            bottomMaskView.startAnimation(inAnim);
        }

//		for (int n = 0; n < expandItemIndices.length; ++n) {
//			final int i = expandItemIndices[n]; 
//			final View itemView = mGridView.getItemView(i);
//			final AlphaAnimation alphaAnim = new AlphaAnimation(1.0f, 0.2f);
//			alphaAnim.setDuration(UIUtils.ANIM_DURATION_LONG);
//			alphaAnim.setFillAfter(true);
//			itemView.startAnimation(alphaAnim);
//		}

        mRunningExpand = new DoExpand();
        mRunningExpand.mExpandAt = index;
        mRunningExpand.mSubView = subView;
        mRunningExpand.mTopMaskView = topMaskView;
        mRunningExpand.mBottomMaskView = bottomMaskView;
        mRunningExpand.mOnExpanded = onExpanded;
        mRunningExpand.mOnCollapsed = onCollapsed;
        mRunningExpand.mScrollFrom = viewportBounds.top;
        mRunningExpand.mExpandFrom = expandFrom;
        mRunningExpand.mExpandUp = expandUp;
        mRunningExpand.mExpandDown = expandDown;
        mRunningExpand.mExpandItemIndices = expandItemIndices;
        mRunningExpand.mExpandAnim = expandAnim;

        mRunningExpand.mSubFrameView = new SubFrameView(getContext(), null, subView, topMaskView, bottomMaskView);
        addViewInLayout(mRunningExpand.mSubFrameView, -1, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mRunningExpand.mSubFrameView.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
        mRunningExpand.mSubFrameView.layout(0, 0, getWidth(), getHeight());
        mRunningExpand.run();
        return true;
    }
    public final void scrollSmoothlyToFirstRow(int duration, final Runnable onFinish, final Runnable onCancel) {
        mGridView.forceScrollSmoothlyTo(0, mGridView.getPaddingTop() - mTitleFrameView.getHeight(), duration,
                new Runnable() {
                    @Override
                    public void run() {
                        if (onFinish != null) {
                            onFinish.run();
                        }
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        if (onCancel != null) {
                            onCancel.run();
                        }
                    }
                }
        );
    }

    // ### Scrollable接口实现 ###
    @Override
    public int getContentWidth() {
        return mGridView.getContentWidth();
    }
    @Override
    public int getContentHeight() {
        return mGridView.getContentHeight();
    }
    @Override
    public boolean getThumbEnabled() {
        return mGridView.getThumbEnabled();
    }
    @Override
    public void setThumbEnabled(boolean enabled) {
        mGridView.setThumbEnabled(enabled);
    }
    @Override
    public boolean getSeekEnabled() {
        return mGridView.getSeekEnabled();
    }
    @Override
    public void setSeekEnabled(boolean enabled) {
        mGridView.setSeekEnabled(enabled);
    }
    @Override
    public boolean canDragFling() {
        return mGridView.canDragFling();
    }
    @Override
    public void canDragFling(boolean can) {
        mGridView.canDragFling(can);
    }
    @Override
    public boolean canVertDrag() {
        return mGridView.canVertDrag();
    }
    @Override
    public void canVertDrag(boolean can) {
        mGridView.canVertDrag(can);
    }
    @Override
    public boolean canHorzDrag() {
        return mGridView.canHorzDrag();
    }
    @Override
    public void canHorzDrag(boolean can) {
        mGridView.canHorzDrag(can);
    }
    @Override
    public int getHorizontalThumbMarginLeft() {
        return mGridView.getHorizontalThumbMarginLeft();
    }
    @Override
    public int getHorizontalThumbMarginTop() {
        return mGridView.getHorizontalThumbMarginTop();
    }
    @Override
    public int getHorizontalThumbMarginRight() {
        return mGridView.getHorizontalThumbMarginRight();
    }
    @Override
    public int getHorizontalThumbMarginBottom() {
        return mGridView.getHorizontalThumbMarginBottom();
    }
    @Override
    public void setHorizontalThumbMargin(int left, int top, int right, int bottom) {
        mGridView.setHorizontalThumbMargin(left, top, right, bottom);
    }
    @Override
    public int getVerticalThumbMarginLeft() {
        return mGridView.getVerticalThumbMarginLeft();
    }
    @Override
    public int getVerticalThumbMarginTop() {
        return mGridView.getVerticalThumbMarginTop();
    }
    @Override
    public int getVerticalThumbMarginRight() {
        return mGridView.getVerticalThumbMarginRight();
    }
    @Override
    public int getVerticalThumbMarginBottom() {
        return mGridView.getVerticalThumbMarginBottom();
    }
    @Override
    public void setVerticalThumbMargin(int left, int top, int right, int bottom) {
        mGridView.setVerticalThumbMargin(left, top, right, bottom);
    }
    @Override
    public Drawable getHorizontalThumbDrawable() {
        return mGridView.getHorizontalThumbDrawable();
    }
    @Override
    public void setHorizontalThumbDrawable(Drawable drawable) {
        mGridView.setHorizontalThumbDrawable(drawable);
    }
    @Override
    public Drawable getVerticalThumbDrawable() {
        return mGridView.getVerticalThumbDrawable();
    }
    @Override
    public void setVerticalThumbDrawable(Drawable drawable) {
        mGridView.setVerticalThumbDrawable(drawable);
    }
    @Override
    public Drawable getHorizontalSeekDrawable() {
        return mGridView.getHorizontalSeekDrawable();
    }
    @Override
    public void setHorizontalSeekDrawable(Drawable drawable) {
        mGridView.setHorizontalSeekDrawable(drawable);
    }
    @Override
    public Drawable getVerticalSeekDrawable() {
        return mGridView.getVerticalSeekDrawable();
    }
    @Override
    public void setVerticalSeekDrawable(Drawable drawable) {
        mGridView.setVerticalSeekDrawable(drawable);
    }
    @Override
    public ViewGestureDetector getScrollDetector() {
        return mGridView.getScrollDetector();
    }
    @Override
    public final ScrollState getScrollState() {
        return mGridView.getScrollState();
    }
    @Override
    public final int getIdleTime() {
        return mGridView.getIdleTime();
    }
    @Override
    public final int getScrollTime() {
        return mGridView.getScrollTime();
    }
    @Override
    public int getScrollFinalX() {
        return mGridView.getScrollFinalX();
    }
    @Override
    public int getScrollFinalY() {
        return mGridView.getScrollFinalY();
    }
    @Override
    public final void setScrollInterpolator(Interpolator interpolator) {
        mGridView.setScrollInterpolator(interpolator);
    }
    @Override
    public void setScrollSensitive(View view, boolean sensitive) {
        mGridView.setScrollSensitive(view, sensitive);
    }
    @Override
    public OverScrollMode getHorizontalOverScrollMode() {
        return mGridView.getHorizontalOverScrollMode();
    }
    @Override
    public void setHorizontalOverScrollMode(OverScrollMode mode) {
        mGridView.setHorizontalOverScrollMode(mode);
    }
    @Override
    public OverScrollMode getVerticalOverScrollMode() {
        return mGridView.getVerticalOverScrollMode();
    }
    @Override
    public void setVerticalOverScrollMode(OverScrollMode mode) {
        mGridView.setVerticalOverScrollMode(mode);
    }
    @Override
    public final int getMaxOverScrollWidth() {
        return mGridView.getMaxOverScrollWidth();
    }
    @Override
    public final void setMaxOverScrollWidth(int width) {
        mGridView.setMaxOverScrollWidth(width);
    }
    @Override
    public final int getMaxOverScrollHeight() {
        return mGridView.getMaxOverScrollHeight();
    }
    @Override
    public final void setMaxOverScrollHeight(int height) {
        mGridView.setMaxOverScrollHeight(height);
    }
    @Override
    public final Rect getViewportBounds() {
        return mGridView.getViewportBounds();
    }
    @Override
    public final Rect copyViewportBounds() {
        return mGridView.copyViewportBounds();
    }
    @Override
    public final void setOnScrollListener(OnScrollListener listener) {
        mOnScrollListener = listener;
    }
    @Override
    public final boolean canScrollHorizontally() {
        return mGridView.canScrollHorizontally();
    }
    @Override
    public final boolean canScrollVertically() {
        return mGridView.canScrollVertically();
    }
    @Override
    public boolean canOverScrollHorizontally() {
        return mGridView.canOverScrollHorizontally();
    }
    @Override
    public boolean canOverScrollVertically() {
        return mGridView.canOverScrollVertically();
    }
    @Override
    public final boolean reachesContentLeft() {
        return mGridView.reachesContentLeft();
    }
    @Override
    public final boolean reachesContentRight() {
        return mGridView.reachesContentRight();
    }
    @Override
    public final boolean reachesContentTop() {
        return mGridView.reachesContentTop();
    }
    @Override
    public final boolean reachesContentBottom() {
        return mGridView.reachesContentBottom();
    }
    @Override
    public final boolean isChildViewable(int index) {
        return mGridView.isChildViewable(index);
    }
    @Override
    public void scrollSmoothly(float vx, float vy, final Runnable onFinish, final Runnable onCancel) {
        mGridView.scrollSmoothly(vx, vy, onFinish, onCancel);
    }
    @Override
    public final void scrollSmoothlyTo(int x, int y, int duration, final Runnable onFinish, final Runnable onCancel) {
        mGridView.scrollSmoothlyTo(x, y, duration, onFinish, onCancel);
    }
    @Override
    public final void scrollSmoothlyBy(int dx, int dy, int duration, final Runnable onFinish, final Runnable onCancel) {
        mGridView.scrollSmoothlyBy(dx, dy, duration, onFinish, onCancel);
    }
    @Override
    public void forceScrollTo(int x, int y) {
        mGridView.forceScrollTo(x, y);
    }
    @Override
    public void forceScrollSmoothlyTo(int x, int y, int duration, Runnable onFinish, Runnable onCancel) {
        mGridView.forceScrollSmoothlyTo(x, y, duration, onFinish, onCancel);
    }
    @Override
    public void springBack() {
        mGridView.springBack();
    }
    @Override
    public void springBackSmoothly() {
        mGridView.springBackSmoothly();
    }
    @Override
    public Point content2view(Point point) {
        return mGridView.content2view(point);
    }
    @Override
    public Rect content2view(Rect rect) {
        return mGridView.content2view(rect);
    }
    @Override
    public Point view2content(Point point) {
        return mGridView.view2content(point);
    }
    @Override
    public Rect view2content(Rect rect) {
        return mGridView.view2content(rect);
    }

    // ### 重写函数 ###
    @Override
    public void scrollBy(int x, int y) {
        mGridView.scrollBy(x, y);
    }
    @Override
    public void scrollTo(int x, int y) {
        mGridView.scrollTo(x, y);
    }
    @Override
    public void setEnabled(boolean enabled) {
        mGridView.setEnabled(enabled);
        super.setEnabled(enabled);
    }
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        canvas.translate(-mGridView.getScrollX(), -mGridView.getScrollY());
        if (mGridView.superDrawThumbs(canvas)) {
            HatGridView.this.invalidate();
        }
        canvas.translate(mGridView.getScrollX(), mGridView.getScrollY());

    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int horzPadding = getPaddingLeft() + getPaddingRight();
        final int vertPadding = getPaddingTop() + getPaddingBottom();

        measureChild(mTitleFrameView, widthMeasureSpec, heightMeasureSpec);
        mHatLinearView.setPadding(0, mTitleFrameView.getMeasuredHeight(), 0, 0);

        measureChild(mHatView, widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        measureChild(mGridView, widthMeasureSpec, heightMeasureSpec);

        measureChild(mBackToTopView, widthMeasureSpec, heightMeasureSpec);

        final int maxWidth = Math.max(mTitleFrameView.getMeasuredWidth(), Math.max(mHatView.getMeasuredWidth(), mGridView.getMeasuredWidth()));
        final int maxHeight = Math.max(mTitleFrameView.getMeasuredHeight(), Math.max(mHatView.getMeasuredHeight() - mHatTipFrameView.getMeasuredHeight(), mGridView.getMeasuredHeight()));
        final int measuredWidth = resolveSize(Math.max(getSuggestedMinimumWidth(), maxWidth + horzPadding), widthMeasureSpec);
        final int measuredHeight = resolveSize(Math.max(getSuggestedMinimumHeight(), maxHeight + vertPadding), heightMeasureSpec);

        mTitleFrameView.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mTitleFrameView.getMeasuredHeight(), MeasureSpec.EXACTLY));
        mHatView.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mHatView.getMeasuredHeight(), MeasureSpec.EXACTLY));
        mHeaderFrameView.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        mFooterFrameView.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

        final int titleHeight = mTitleFrameView.getMeasuredHeight();
        final int bodyHeight = mHatBodyFrameView.getMeasuredHeight();
        final int brimHeight = mBrimFrameView.getMeasuredHeight();
        final int headerHeight = headerView() == null ? 0 : headerView().getMeasuredHeight();
        final int footerHeight = footerView() == null ? 0 : footerView().getMeasuredHeight();

        mGridView.setPadding(mGridPadding.left, titleHeight + bodyHeight + brimHeight + mGridPadding.top + headerHeight - headerSink(), mGridPadding.right, footerHeight  + mGridPadding.bottom - footerRise());
        mGridView.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(measuredHeight - vertPadding, MeasureSpec.EXACTLY));
        mGridView.setMaxOverScrollHeight(mHatLinearView.getMeasuredHeight() - titleHeight - bodyHeight - brimHeight);

        setMeasuredDimension(measuredWidth, measuredHeight);
    }
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int l = getPaddingLeft();
        final int t = getPaddingTop();
        final int r = getWidth() - getPaddingRight();
        final int b = getHeight() - getPaddingBottom();

        final boolean springBack = changed || mHatHeight != mHatView.getMeasuredHeight();
        mHatHeight = mHatView.getMeasuredHeight();

        mTitleFrameView.layout(l, t, l + mTitleFrameView.getMeasuredWidth(), t + mTitleFrameView.getMeasuredHeight());
        mHatView.layout(l, t, l + mHatView.getMeasuredWidth(), t + mHatView.getMeasuredHeight());
        mHeaderFrameView.layout(l, t, l + mHeaderFrameView.getMeasuredWidth(), t + mHeaderFrameView.getMeasuredHeight());
        mFooterFrameView.layout(l, t, l + mFooterFrameView.getMeasuredWidth(), t + mFooterFrameView.getMeasuredHeight());
        mGridView.layout(l, t, l+ mGridView.getMeasuredWidth(), t + mGridView.getMeasuredHeight());
        mBackToTopView.layout(r - mBackToTopView.getMeasuredWidth(), b - mBackToTopView.getMeasuredHeight(), r, b);

        // TODO(by lizhan@duokan.com): ugly!!! :(
//        final ThemeFeature themeFeature = ManagedContext.of(getContext()).queryFeature(ThemeFeature.class);
//        if (themeFeature != null) {
//            mBackToTopView.offsetTopAndBottom(-themeFeature.getTheme().getPagePaddingBottom());
//        }

        if (springBack) {
            mGridView.springBack();

        } else if (getScrollState() == ScrollState.IDLE) {
            mGridView.scrollBy(0, 0);
        }

        mGridView.setVerticalThumbMargin(0, mTitleFrameView.getHeight() + UiUtils.dip2px(getContext(), 2), UiUtils.dip2px(getContext(), 2), UiUtils.dip2px(getContext(), 6));
    }

    // ### 实现函数 ###
    protected void onAdjustDrag(PointF offset) {

    }
    protected void onAdjustViewport(ScrollState scrollState, RectF viewportBounds) {

    }
    private void hatTipState(HatTipState state) {
        if (mHatTipState != state) {
            final HatTipState oldState = mHatTipState;

            mHatTipState = state;
            if (mOnHatTipStateChangeListener != null) {
                mOnHatTipStateChangeListener.onHatTipStateChange(oldState, mHatTipState);
            }
        }
    }
    private int headerSink() {
        return headerView() == null ? 0 : mHeaderSink;
    }
    private int footerRise() {
        return footerView() == null ? 0 : mFooterRise;
    }
    private final int headerHeight() {
        return headerView() == null ? 0 : headerView().getHeight();
    }
    private final int footerHeight() {
        return footerView() == null ? 0 : footerView().getHeight();
    }
    private final View headerView() {
        if (mHeaderFrameView.getChildCount() > 0) {
            return mHeaderFrameView.getChildAt(0);
        }
        return null;
    }
    private final void headerView(View view) {
        mHeaderFrameView.removeAllViews();

        if (view != null) {
            if (view.getLayoutParams() == null) {
                view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            mHeaderFrameView.addView(view);
        }
    }
    private final View footerView() {
        if (mFooterFrameView.getChildCount() > 0) {
            return mFooterFrameView.getChildAt(0);
        }
        return null;
    }
    private final void footerView(View view) {
        mFooterFrameView.removeAllViews();

        if (view != null) {
            if (view.getLayoutParams() == null) {
                view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            mFooterFrameView.addView(view);
        }
    }
    private final int hatTipBottom2Grid() {
        final int top = hatTipTop2Grid() + hatTipHeight();
        return top;
    }
    private final int hatTipTop2Grid() {
        final int top = hatTipTop2Hat() + hatOffset();
        return top;
    }
    private final int hatTipTop2Hat() {
        final int top = mHatLinearView.getTop() + mHatTipFrameView.getTop() + (getHatTipView() == null ? 0 : getHatTipView().getTop());
        return top;
    }
    private final int hatTipHeight() {
        final int height = getHatTipView() == null ? 0 : getHatTipView().getHeight();
        return height;
    }
    private final int hatBodyHeight() {
        final int height = mHatBodyFrameView.getHeight();
        return height;
    }
    private final int hatOffset() {
        return -hatBodyTop2Hat() + mTitleFrameView.getHeight(); // HatBody投影位于Grid顶点下方TitleHeight处
    }
    private final int titleBottom2Hat() {
        final int bottom = mHatView.getScrollY() + mTitleFrameView.getBottom();
        return bottom;
    }
    private final int hatBodyTop2Hat() {
        final int top = mHatLinearView.getTop() + mHatBodyFrameView.getTop();
        return top;
    }

    private void showBackToTopView() {
        if (mBackToTopView.isEnabled() == false)
            return;
        if (mBackToTopView.getVisibility() == View.VISIBLE)
            return;

        mBackToTopView.clearAnimation();
        mBackToTopView.setVisibility(View.VISIBLE);
        UiUtils.fadeViewIn(mBackToTopView, null);
    }
    private void hideBackToTopView() {
        if (mBackToTopView.isEnabled() == false)
            return;
        if (mBackToTopView.getVisibility() == View.INVISIBLE)
            return;

        mBackToTopView.clearAnimation();
        mBackToTopView.setVisibility(View.INVISIBLE);
        UiUtils.fadeViewOut(mBackToTopView, null);
    }

    private void updateViewWhenScrollStateChanged(ScrollState oldState, ScrollState newState) {
        if (mBackToTopView.isEnabled() == false)
            return;

        if (mPendingHideFastToTop != null) {
            removeCallbacks(mPendingHideFastToTop);
            mPendingHideFastToTop = null;
        }
        if (newState == ScrollState.IDLE) {
            if (getHatBodyVisibleHeight() > 0) {
                hideBackToTopView();
            } else if (mPendingHideFastToTop == null) {
                mPendingHideFastToTop = new Runnable() {
                    public void run() {
                        hideBackToTopView();
                        mPendingHideFastToTop = null;
                    }
                };
                postDelayed(mPendingHideFastToTop, 2000);
            }
        }
    }

    // ### 内嵌类 ###
    public static enum HatTipState {
        UNDOCKED,
        UNDOCKING,
        DOCKING,
        DOCKED,
    }
    public static interface OnItemClickListener {
        void onItemClick(HatGridView gridView, View itemView, int index);
    }
    public static interface OnItemLongPressListener {
        void onItemLongPress(HatGridView gridView, View itemView, int index);
    }
    public static interface OnHatTipStateChangeListener {
        void onHatTipStateChange(HatTipState oldState, HatTipState newState);
    }
    public static abstract class GridAdapter extends GroupItemsAdapterBase {
        @Override
        public int getGroupCount() {
            return 0;
        }
        @Override
        public int getGroupSize(int groupIndex) {
            return 0;
        }
        @Override
        public View getGroupTitleView(int groupIndex, View oldView, ViewGroup parentView) {
            return null;
        }
        public View getHeaderView(int itemCount, View oldView, ViewGroup parentView) {
            return null;
        }
        public View getFooterView(int itemCount, View oldView, ViewGroup parentView) {
            return null;
        }
    }
    private class GridView extends GridItemsView {
        public GridView(Context context) {
            super(context);
        }

        public boolean superDrawThumbs(Canvas canvas) {
            return super.drawThumbs(canvas);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            if (mHatPushable) {
                final int clipTop;
                if (mClipGridToBrim) {
                    clipTop = getScrollY() + mHatView.getHeight() - mHatView.getScrollY();
                } else {
                    clipTop = getScrollY() + mHatView.getHeight() - mHatView.getScrollY() - mBrimFrameView.getHeight();
                }
                canvas.clipRect(0, clipTop, getWidth(), clipTop + getHeight());
            }
            super.dispatchDraw(canvas);
        }
        @Override
        protected boolean drawThumbs(Canvas canvas) {
            return false;
        }
        @Override
        protected Scroller newScroller() {
            return new Scroller() {
                @Override
                protected void adjustViewport(ScrollState scrollState, RectF viewportBounds) {
                    HatGridView.this.onAdjustViewport(scrollState, viewportBounds);
                }
                @Override
                protected void onDrag(float dx, float dy) {
                    final PointF offset = UiUtils.tempPointFs.acquire();
                    offset.set(dx, dy);
                    onAdjustDrag(offset);
                    dx = offset.x;
                    dy = offset.y;
                    UiUtils.tempPointFs.release(offset);
                    super.onDrag(dx, dy);

                    if (dy > 1) {
                        showBackToTopView();
                    } else if (dy < -1) {
                        hideBackToTopView();
                    }

                    if (mHatTipDockable == false) {
                        hatTipState(HatTipState.UNDOCKED);
                        return;
                    }

                    if (isHatTipFullyVisible()) {
                        if (mHatTipState != HatTipState.DOCKED) {
                            hatTipState(HatTipState.DOCKING);
                        }
                    } else {
                        if (mHatTipState != HatTipState.UNDOCKED) {
                            hatTipState(HatTipState.UNDOCKING);
                        }
                    }

                }
                @Override
                protected void onFling(float flingX, float flingY, float vx, float vy, Runnable onFinish, Runnable onCancel) {
                    if (mHatTipDockable == false) {
                        hatTipState(HatTipState.UNDOCKED);

                    } else if (vy >= 0 && isHatTipFullyVisible()) {
                        hatTipState(HatTipState.DOCKING);

                    } else {
                        hatTipState(HatTipState.UNDOCKING);
                    }

                    super.onFling(flingX, flingY, vx, vy, onFinish, onCancel);
                }
                @Override
                protected int maxOverScrollHeight() {
                    if (mHatTipState == HatTipState.DOCKED || mHatTipState == HatTipState.DOCKING) {
                        // 保证过滚动区域不会变大
                        return Math.max(super.maxOverScrollHeight() - hatTipHeight(), 0);
                    } else {
                        return super.maxOverScrollHeight();
                    }
                }
                @Override
                protected int minScrollY() {
                    if (mHatTipState == HatTipState.DOCKED || mHatTipState == HatTipState.DOCKING) {
                        return super.minScrollY() - hatTipHeight();
                    } else {
                        return super.minScrollY();
                    }
                }
            };
        }

    };
    private class ProxyAdapter extends GridAdapter implements ItemsObserver {
        private GridAdapter mBaseAdapter = null;

        public final GridAdapter getBaseAdapter() {
            return mBaseAdapter;
        }
        public final void setBaseAdapter(GridAdapter adapter) {
            if (mBaseAdapter != null) {
                mBaseAdapter.removeObserver(this);
            }

            mBaseAdapter = adapter;

            if (mBaseAdapter != null) {
                mBaseAdapter.addObserver(this);
            }
        }

        @Override
        public int getGroupCount() {
            return mBaseAdapter == null ? 0 : mBaseAdapter.getGroupCount();
        }
        @Override
        public int getGroupSize(int groupIndex) {
            return mBaseAdapter == null ? 0 : mBaseAdapter.getGroupSize(groupIndex);
        }
        @Override
        public View getGroupTitleView(int groupIndex, View oldView, ViewGroup parentView) {
            return mBaseAdapter == null ? null : mBaseAdapter.getGroupTitleView(groupIndex, oldView, parentView);
        }
        @Override
        public int getItemCount() {
            return mBaseAdapter == null ? 0 : mBaseAdapter.getItemCount();
        }
        @Override
        public View getEmptyView(View oldView, ViewGroup parentView) {
            return mBaseAdapter == null ? null : mBaseAdapter.getEmptyView(null, parentView);
        }
        @Override
        public View getHeaderView(int itemCount, View oldView, ViewGroup parentView) {
            return mBaseAdapter == null ? null : mBaseAdapter.getHeaderView(itemCount, oldView, parentView);
        }
        @Override
        public View getFooterView(int itemCount, View oldView, ViewGroup parentView) {
            return mBaseAdapter == null ? null : mBaseAdapter.getFooterView(itemCount, oldView, parentView);
        }
        @Override
        public View getItemView(int index, View oldView, ViewGroup parentView) {
            return mBaseAdapter == null ? null : mBaseAdapter.getItemView(index, oldView, parentView);
        }
        @Override
        public void onItemsAdded(int addCount, int addTo) {
            notifyItemsAdded(addCount, addTo);
        }
        @Override
        public void onItemsRemoved(int removeFrom, int removeCount) {
            notifyItemsRemoved(removeFrom, removeCount);
        }
        @Override
        public void onItemsMoved(int moveFrom, int moveCount, int moveTo) {
            notifyItemsMoved(moveFrom, moveCount, moveTo);
        }
        @Override
        public void onItemsModified(int modifiedFrom, int modifiedCount) {
            notifyItemsModified(modifiedFrom, modifiedCount);
        }
        @Override
        public void onItemsChanged(int itemCount) {
            final View oldHeaderView = headerView();
            final View newHeaderView = getHeaderView(itemCount, oldHeaderView, mHeaderFrameView);
            if (oldHeaderView != newHeaderView) {
                headerView(newHeaderView);
            }

            final View oldFooterView = footerView();
            final View newFooterView = getFooterView(itemCount, oldFooterView, mFooterFrameView);
            if (oldFooterView != newFooterView) {
                footerView(newFooterView);
            }

            notifyItemsChanged();
        }
        @Override
        public Object getItem(int index) {
            return mBaseAdapter == null ? null : mBaseAdapter.getItem(index);
        }
    }
    private class SubFrameView extends FrameLayout {
        private final View mSubView;
        private final View mTopMaskView;
        private final View mBottomMaskView;

        public SubFrameView(Context context, AttributeSet attrs, View subView, View topMaskView, View bottomMaskView) {
            super(context, attrs);

            setWillNotDraw(false);

            mSubView = subView;
            mTopMaskView = topMaskView;
            mBottomMaskView = bottomMaskView;

            final int gridWidth = HatGridView.this.getWidth();
            final int gridHeight = HatGridView.this.getHeight();
            final int subHeight = mRunningExpand.mExpandUp + mRunningExpand.mExpandDown;
            addView(mSubView, new LayoutParams(gridWidth, subHeight));

            if (mTopMaskView != null) {
                int maskHeight = gridHeight - subHeight + Math.max(0, mRunningExpand.mExpandUp);
                addView(mTopMaskView, new LayoutParams(gridWidth, maskHeight));
            }

            if (mBottomMaskView != null) {
                addView(mBottomMaskView, new FrameLayout.LayoutParams(gridWidth, mRunningExpand.mExpandDown));
            }
        }
        public void refresh() {
            Point subViewPos = new Point(0, mRunningExpand.mExpandFrom);
            mGridView.content2view(subViewPos);
            scrollTo(0, -(subViewPos.y - mRunningExpand.mSubView.getTop()));
            invalidate();
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            mSubView.layout(0, getHeight() - mSubView.getMeasuredHeight(), getWidth(), getHeight());

            if (mTopMaskView != null) {
                mTopMaskView.layout(0, Math.min(0, -mRunningExpand.mExpandUp), getWidth(), getHeight() - mSubView.getMeasuredHeight());
            }

            if (mBottomMaskView != null) {
                mBottomMaskView.layout(0, getHeight() - mSubView.getMeasuredHeight(), getWidth(), getHeight() - mSubView.getMeasuredHeight() + mRunningExpand.mExpandDown + Math.max(0, mRunningExpand.mExpandUp));
            }
        }
        @Override
        public void draw(Canvas canvas) {
            if (mTopMaskView != null) {
//				canvas.save();
//				canvas.clipRect(mTopMaskView.getLeft(), mTopMaskView.getTop(), mTopMaskView.getRight(), mSubView.getTop());
                drawChild(canvas, mTopMaskView, getDrawingTime());
//				canvas.restore();
            }

            if (mBottomMaskView != null) {
                canvas.save();
                canvas.clipRect(mBottomMaskView.getLeft(), mSubView.getTop() + mRunningExpand.mExpandHeight,
                        mBottomMaskView.getRight(), mSubView.getTop() + mRunningExpand.mExpandHeight + mBottomMaskView.getHeight());
                drawChild(canvas, mBottomMaskView, getDrawingTime());
                canvas.restore();
            }

            canvas.save();
            canvas.clipRect(mSubView.getLeft(), mSubView.getTop(),
                    mSubView.getRight(), mSubView.getTop() + mRunningExpand.mExpandHeight);
            drawChild(canvas, mSubView, getDrawingTime());
            canvas.restore();
        }
//		@Override
//		protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
//			canvas.clipRect(mClipRect.left + getScrollX(), mClipRect.top + getScrollY(), 
//					mClipRect.right + getScrollX(), mClipRect.bottom + getScrollY());
//			return super.drawChild(canvas, child, drawingTime);
//		}
    }
    private class DoExpand implements Runnable {
        public int mExpandAt = 0;
        public SubFrameView mSubFrameView = null;
        public View mSubView = null;
        public View mTopMaskView = null;
        public View mBottomMaskView = null;
        public Runnable mOnExpanded = null;
        public Runnable mOnCollapsed = null;
        public int mScrollFrom = 0;
        public int mExpandFrom = 0;
        public int mExpandUp = 0;
        public int mExpandDown = 0;
        public AlphaAnimation mExpandAnim = null;
        public int[] mExpandItemIndices = new int[0];
        public float mExpandProgress = 0.0f;
        public int mExpandHeight = 0;
        public boolean mIsCollapsing = false;

        @Override
        public void run() {
            Transformation out = new Transformation();
            boolean needsMore = mExpandAnim.getTransformation(AnimationUtils.currentAnimationTimeMillis(), out);

            mExpandProgress = out.getAlpha();
            final int expandUp = Math.round(mExpandUp * mExpandProgress);
            final int expandDown = Math.round(mExpandDown * mExpandProgress);
            mExpandHeight = expandUp + expandDown;

            // 调整mGridView单元显示位置
            mGridView.forceScrollTo(mGridView.getScrollX(), mScrollFrom + expandUp);
            for (int n = 0; n < mExpandItemIndices.length; ++n) {
                final int index = mExpandItemIndices[n];
                if (index / mGridView.getColumnCount() <= mExpandAt / mGridView.getColumnCount())
                    continue;

                mGridView.setItemOffset(index, 0, mExpandHeight);
            }

            // 调整mSubView显示位置及可见高度
            mSubFrameView.refresh();

            if (needsMore) {
                post(this);
            } else if (mIsCollapsing == false) {
                post(mRunningExpand.mOnExpanded);
            } else {
                for (int n = 0; n < mRunningExpand.mExpandItemIndices.length; ++n) {
                    final int index = mRunningExpand.mExpandItemIndices[n];
                    mGridView.forceItemVisual(index, false);
                }

                mGridView.setEnabled(true);
                removeViewInLayout(mRunningExpand.mSubFrameView);
                invalidate();
                post(mRunningExpand.mOnCollapsed);
                mRunningExpand = null;
            }
        }
    }
}
