package com.common.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.px.charge.R;

public class DkWebListView extends FrameLayout implements Scrollable {
	// ### 值域 ###
	private static final int MIN_LOADING_ITEMS = 50;
	private static final int MAX_LOADING_ITEMS = 100;
	private final HatGridView mListView;
	private final PullDownRefreshView mPullRefreshView;
	private final ProxyAdapter mProxyAdapter;
    private OnScrollListener mOnScrollListener = null;
    private HatGridView.OnItemClickListener mOnItemClickListener = null;
    private HatGridView.OnItemLongPressListener mOnItemLongPressListener = null;
    
	// ### 构造函数 ###
	public DkWebListView(Context context) {
		this(context, null);
	}
	public DkWebListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mListView = newHatGridView(context);
		mListView.setHatTipView(new PullDownRefreshView(getContext()));
		mListView.setHatTipMargin(0, UiUtils.dip2px(getContext(), 60), 0, 0);
		
		mPullRefreshView = (PullDownRefreshView) mListView.getHatTipView();

		mListView.setOnScrollListener(new HatGridView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(Scrollable scrollable, ScrollState oldState, ScrollState newState) {
                if (newState == ScrollState.IDLE) {
                    if (mProxyAdapter.hasMoreItems()) {
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (getScrollState() == ScrollState.IDLE) {
                                    loadMoreItemsIfNeeded();
                                }
                            }
                        }, UiUtils.ANIM_DURATION_LONG_LONG);
                    }
                }

                if (newState == ScrollState.DRAG) {
                    if (mPullRefreshView.getVisibility() != View.VISIBLE) {
                        pullRefreshState(PullDownRefreshView.RefreshState.NO_REFRESH);

                    } else if (mPullRefreshView.pullRefreshState() != PullDownRefreshView.RefreshState.REFRESHING) {
                        if (mProxyAdapter.isLoading()) {
                            pullRefreshState(PullDownRefreshView.RefreshState.NO_REFRESH);
                        } else {
                            pullRefreshState(PullDownRefreshView.RefreshState.DOWN_TO_REFRESH);
                            mListView.setHatTipDockable(true);
                        }
                    }
                }

                if (oldState == ScrollState.DRAG) {
                    mListView.setHatTipDockable(mPullRefreshView.pullRefreshState() == PullDownRefreshView.RefreshState.RELEASE_TO_REFRESH
                            || mPullRefreshView.pullRefreshState() == PullDownRefreshView.RefreshState.REFRESHING);
                }

                if (mOnScrollListener != null) {
                    mOnScrollListener.onScrollStateChanged(scrollable, oldState, newState);
                }
            }

            @Override
            public void onScroll(Scrollable scrollable, boolean viewportChanged) {
                if (mOnScrollListener != null) {
                    mOnScrollListener.onScroll(scrollable, viewportChanged);
                }
            }
        });
		
		mListView.setOnHatTipStateChange(new HatGridView.OnHatTipStateChangeListener() {
            @Override
            public void onHatTipStateChange(HatGridView.HatTipState oldState, HatGridView.HatTipState newState) {
                if (newState == HatGridView.HatTipState.DOCKING) {
                    pullRefreshState(PullDownRefreshView.RefreshState.RELEASE_TO_REFRESH);
                } else if (newState == HatGridView.HatTipState.UNDOCKING) {
                    pullRefreshState(PullDownRefreshView.RefreshState.DOWN_TO_REFRESH);
                } else if (newState == HatGridView.HatTipState.DOCKED) {
                    pullRefreshState(PullDownRefreshView.RefreshState.REFRESHING);
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            doRefresh(false);
                        }
                    }, UiUtils.ANIM_DURATION_LONG_LONG);

                }
            }
        });
		
		addView(mListView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		setBackgroundColor(Color.WHITE);

		mProxyAdapter = new ProxyAdapter();
		mListView.setAdapter(mProxyAdapter);
	}

	protected HatGridView newHatGridView(Context context) {
		return new HatGridView(context) {
			@Override
			protected void onAdjustDrag(PointF offset) {
				DkWebListView.this.onAdjustDrag(offset);
			}
			@Override
			protected void onAdjustViewport(ScrollState scrollState, RectF viewportBounds) {
				DkWebListView.this.onAdjustViewport(scrollState, viewportBounds);
			}
		};
	}
	
	// ### 属性 ###
//	public final void setUiStyle(UiStyle style) {
////		mPullRefreshView.setUiStyle(style);
//	}
	public final ListAdapter getAdapter() {
		return mProxyAdapter.baseAdapter();
	}
	public final void setAdapter(ListAdapter adapter) {
		mProxyAdapter.baseAdapter(adapter);
        pullRefreshState(PullDownRefreshView.RefreshState.NO_REFRESH);
	}
	public final int getStretchMode() {
		return mListView.getStretchMode();
	}
	public final void setStretchMode(int mode) {
		mListView.setStretchMode(mode);
	}
	public final Drawable getItemsBackground() {
		return mListView.getItemsBackground();
	}
	public final void setItemsBackground(int resId) {
		mListView.setItemsBackground(resId);
	}
	public final void setItemsBackground(Drawable background) {
		mListView.setItemsBackground(background);
	}
	public final View getItemView(int index) {
		return mListView.getItemView(index);
	}
	public final int getNumColumns() {
		return mListView.getNumColumns();
	}
	public final void setNumColumns(int num) {
		mListView.setNumColumns(num);
	}
	public final int getColumnCount() {
		return mListView.getColumnCount();
	}
	public final Drawable getColumnDivider() {
		return mListView.getColumnDivider();
	}
	public final void setColumnDivider(Drawable divider) {
		mListView.setColumnDivider(divider);
	}
	public final void setColumnDivider(Drawable divider, boolean keepSpacing) {
		mListView.setColumnDivider(divider, keepSpacing);
	}
	public final int getColumnSpacing() {
		return mListView.getColumnSpacing();
	}
	public final void setColumnSpacing(int spacing) {
		mListView.setColumnSpacing(spacing);
	}
	public final int getRowCount() {
		return mListView.getRowCount();
	}
	public final Drawable getRowBackground() {
		return mListView.getRowBackground();
	}
	public final void setRowBackground(int resId) {
		mListView.setRowBackground(getResources().getDrawable(resId));
	}
	public final void setRowBackground(Drawable background) {
		mListView.setRowBackground(background);
	}	
	public final Drawable getRowDivider() {
		return mListView.getRowDivider();
	}
	public final void setRowDivider(int resId) {
		mListView.setRowDivider(resId);
	}
	public final void setRowDivider(Drawable divider) {
		mListView.setRowDivider(divider);
	}
	public final void setRowDivider(Drawable divider, boolean keepSpacing) {
		mListView.setRowDivider(divider, keepSpacing);
	}
	public final int getRowSpacing() {
		return mListView.getRowSpacing();
	}
	public final void setRowSpacing(int spacing) {
		mListView.setRowSpacing(spacing);
	}
	public final ListState getListState() {
		return mProxyAdapter.listState();
	}
	public final void setPullRefreshEnabled(boolean enabled) {
		mPullRefreshView.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
	}
	public final int getHeaderSink() {
		return mListView.getHeaderSink();
	}
	public final void setHeaderSink(int sink) {
		mListView.setHeaderSink(sink);
	}
	public final int getFooterRise() {
		return mListView.getFooterRise();
	}
	public final void setFooterRise(int rise) {
		mListView.setFooterRise(rise);
	}
	public final View setTitleView(int viewId) {
		return mListView.setTitleView(viewId);
	}
	public final void setTitleView(View view) {
		mListView.setTitleView(view);
	}
	public final View setHatBackgroundView(int viewId) {
		return mListView.setHatBackgroundView(viewId);
	}
	public final void setHatBackgroundView(View view) {
		mListView.setHatBackgroundView(view);
	}
	public final View getHatBackgroundView() {
		return mListView.getHatBackgroundView();
	}
	public final View setHatTipView(int viewId) {
		return mListView.setTitleView(viewId);
	}
	public final void setHatTipView(View view) {
		mListView.setHatTipView(view);
	}
	public final View setHatBodyView(int viewId) {
		return mListView.setHatBodyView(viewId);
	}
	public final void setHatBodyView(View view) {
		mListView.setHatBodyView(view);
	}
    public final View getHatBodyView() {
    	return mListView.getHatBodyView();
    }
	public final int getHatBodyVisibleHeight() {
		return mListView.getHatBodyVisibleHeight();
	}
	public final View getBrimView() {
		return mListView.getBrimView();
	}
	public final View setBrimView(int viewId) {
		return mListView.setBrimView(viewId);
	}
	public final void setBrimView(View view) {
		mListView.setBrimView(view);
	}
	public final void setClipGridToBrim(boolean clipToBrim) {
		mListView.setClipGridToBrim(clipToBrim);
	}
	public final int getHatVisibleHeight() {
		return mListView.getHatVisibleHeight();
	}
    public final void setFastToTopEnable(boolean enable) {
       mListView.setFastToTopEnabled(enable);
    }
	
	// ### 方法 ###
//	public ItemsPresenter asItemsPresenter() {
//		return new ItemsPresenter() {
//			@Override
//			public View getItemView(int index) {
//				return DkWebListView.this.getItemView(index);
//			}
//			@Override
//			public int getItemCount() {
//				return DkWebListView.this.getItemCount();
//			}
//		};
//	}
	public final boolean isItemVisible(int index) {
		return mListView.isItemVisible(index);
	}
    public final Rect getItemBounds(int index) {
		return mListView.getItemBounds(index);
	}
    public final int getListPaddingLeft() {
		return mListView.getGridPaddingLeft();
	}
	public final int getListPaddingRight() {
		return mListView.getGridPaddingRight();
	}
	public final int getListPaddingTop() {
		return mListView.getGridPaddingTop();
	}
	public final int getListPaddingBottom() {
		return mListView.getGridPaddingBottom();
	}
	public final void setListPadding(int left, int top, int right, int bottom) {
		mListView.setGridPadding(left, top, right, bottom);
	}
	public final int getItemCount() {
		return mListView.getItemCount();
	}
	public final void refresh() {
		refresh(false);
	}
	public final void refresh(boolean clear) {
		if (mPullRefreshView.pullRefreshState() == PullDownRefreshView.RefreshState.REFRESHING)
			return;

		doRefresh(clear);
	}
	public final PullDownRefreshView.RefreshState getPullRefreshState() {
		return mPullRefreshView.pullRefreshState();
	}
	public final boolean isLoading() {
		return mProxyAdapter.isLoading();
	}
	public final int getListScrollX() {
		return mListView.getGridScrollX();
	}
	public final int getListScrollY() {
		return mListView.getGridScrollY();
	}
	public final void setOnItemClickListener(HatGridView.OnItemClickListener listener) {
		mOnItemClickListener = listener;
		mListView.setOnItemClickListener(new HatGridView.OnItemClickListener() {
			@Override
			public void onItemClick(HatGridView gridView, View itemView, int index) {
				if (getListState() == ListState.LOADING_MORE && index == mProxyAdapter.getItemCount() - 1)
					return;
				
				if (mOnItemClickListener != null) {
					mOnItemClickListener.onItemClick(gridView, itemView, index);
				}
			}
		});
	}
	public final void setOnItemLongPressListener(HatGridView.OnItemLongPressListener listener) {
		mOnItemLongPressListener = listener;
		mListView.setOnItemLongPressListener(new HatGridView.OnItemLongPressListener() {
			
			@Override
			public void onItemLongPress(HatGridView gridView, View itemView, int index) {
				if (getListState() == ListState.LOADING_MORE && index == mProxyAdapter.getItemCount() - 1)
					return;
				if (mOnItemLongPressListener != null) {
					mOnItemLongPressListener.onItemLongPress(gridView, itemView, index);
				}
			}
		});
	}
	public final void requestGroupVisible(int groupIndex) {
		mListView.requestGroupVisible(groupIndex);
	}
	public final void requestItemVisible(int index) {
		mListView.requestItemVisible(index);
	}
    public final void requestItemInRect(int index, Rect rect, int gravity) {
        mListView.requestItemInRect(index, rect, gravity);
    }
	public final int getGroupCount() {
		return mListView.getGroupCount();
	}
	public final int getGroupSize(int groupIndex) {
		return mListView.getGroupSize(groupIndex);
	}
	public final int getGroupRowCount(int groupIndex) {
		return mListView.getGroupRowCount(groupIndex);
	}
	public final int getGroupFirstRowIndex(int groupIndex) {
		return mListView.getGroupFirstRowIndex(groupIndex);
	}
	public final int[] getGroupPosition(int itemIndex) {
		return mListView.getGroupPosition(itemIndex);
	}
	public final int getItemIndex(int groupIndex, int groupItemIndex) {
		return mListView.getItemIndex(groupIndex, groupItemIndex);
	}
	public final View getTitleView() {
		return mListView.getTitleView();
	}
	
	// ### Scrollable接口实现 ###
	@Override
	public int getContentWidth() {
		return mListView.getContentWidth();
	}
	@Override
	public int getContentHeight() {
		return mListView.getContentHeight();
	}
	@Override
	public boolean getThumbEnabled() {
		return mListView.getThumbEnabled();
	}
	@Override
	public void setThumbEnabled(boolean enabled) {
		mListView.setThumbEnabled(enabled);
	}
	@Override
	public boolean getSeekEnabled() {
		return mListView.getSeekEnabled();
	}
	@Override
	public void setSeekEnabled(boolean enabled) {
		mListView.setSeekEnabled(enabled);
	}
	@Override
	public boolean canDragFling() {
		return mListView.canDragFling();
	}
	@Override
	public void canDragFling(boolean can) {
		mListView.canDragFling(can);
	}
    @Override
    public boolean canVertDrag() {
        return mListView.canVertDrag();
    }
    @Override
    public void canVertDrag(boolean can) {
        mListView.canVertDrag(can);
    }
    @Override
    public boolean canHorzDrag() {
        return mListView.canHorzDrag();
    }
    @Override
    public void canHorzDrag(boolean can) {
        mListView.canHorzDrag(can);
    }
	@Override
	public int getHorizontalThumbMarginLeft() {
		return mListView.getHorizontalThumbMarginLeft();
	}
	@Override
	public int getHorizontalThumbMarginTop() {
		return mListView.getHorizontalThumbMarginTop();
	}
	@Override
	public int getHorizontalThumbMarginRight() {
		return mListView.getHorizontalThumbMarginRight();
	}
	@Override
	public int getHorizontalThumbMarginBottom() {
		return mListView.getHorizontalThumbMarginBottom();
	}
	@Override
	public void setHorizontalThumbMargin(int left, int top, int right, int bottom) {
		mListView.setHorizontalThumbMargin(left, top, right, bottom);
	}
	@Override
	public int getVerticalThumbMarginLeft() {
		return mListView.getVerticalThumbMarginLeft();
	}
	@Override
	public int getVerticalThumbMarginTop() {
		return mListView.getVerticalThumbMarginTop();
	}
	@Override
	public int getVerticalThumbMarginRight() {
		return mListView.getVerticalThumbMarginRight();
	}
	@Override
	public int getVerticalThumbMarginBottom() {
		return mListView.getVerticalThumbMarginBottom();
	}
	@Override
	public void setVerticalThumbMargin(int left, int top, int right, int bottom) {
		mListView.setVerticalThumbMargin(left, top, right, bottom);
	}
	@Override
	public Drawable getHorizontalThumbDrawable() {
		return mListView.getHorizontalThumbDrawable();
	}
	@Override
	public void setHorizontalThumbDrawable(Drawable drawable) {
		mListView.setHorizontalThumbDrawable(drawable);
	}
	@Override
	public Drawable getVerticalThumbDrawable() {
		return mListView.getVerticalThumbDrawable();
	}
	@Override
	public void setVerticalThumbDrawable(Drawable drawable) {
		mListView.setVerticalThumbDrawable(drawable);
	}
	@Override
	public Drawable getHorizontalSeekDrawable() {
		return mListView.getHorizontalSeekDrawable();
	}
	@Override
	public void setHorizontalSeekDrawable(Drawable drawable) {
		mListView.setHorizontalSeekDrawable(drawable);
	}
	@Override
	public Drawable getVerticalSeekDrawable() {
		return mListView.getVerticalSeekDrawable();
	}
	@Override
	public void setVerticalSeekDrawable(Drawable drawable) {
		mListView.setVerticalSeekDrawable(drawable);
	}
	@Override
	public ViewGestureDetector getScrollDetector() {
		return mListView.getScrollDetector();
	}
	@Override
	public final ScrollState getScrollState() {
		return mListView.getScrollState();
	}
	@Override
	public final int getIdleTime() {
		return mListView.getIdleTime();
	}
	@Override
	public final int getScrollTime() {
		return mListView.getScrollTime();
	}
	@Override
	public int getScrollFinalX() {
		return mListView.getScrollFinalX();
	}
	@Override
	public int getScrollFinalY() {
		return mListView.getScrollFinalY();
	}
	@Override
	public final void setScrollInterpolator(Interpolator interpolator) {
		mListView.setScrollInterpolator(interpolator);
	}
	@Override
	public void setScrollSensitive(View view, boolean sensitive) {
		mListView.setScrollSensitive(view, sensitive);
	}
	@Override
	public OverScrollMode getHorizontalOverScrollMode() {
		return mListView.getHorizontalOverScrollMode();
	}
	@Override
	public void setHorizontalOverScrollMode(OverScrollMode mode) {
		mListView.setHorizontalOverScrollMode(mode);
	}
	@Override
	public OverScrollMode getVerticalOverScrollMode() {
		return mListView.getVerticalOverScrollMode();
	}
	@Override
	public void setVerticalOverScrollMode(OverScrollMode mode) {
		mListView.setVerticalOverScrollMode(mode);
	}
	@Override
	public final int getMaxOverScrollWidth() {
		return mListView.getMaxOverScrollWidth();
	}
	@Override
	public final void setMaxOverScrollWidth(int width) {
		mListView.setMaxOverScrollWidth(width);
	}
	@Override
	public int getMaxOverScrollHeight() {
		return mListView.getMaxOverScrollHeight();
	}
	@Override
	public final void setMaxOverScrollHeight(int height) {
		mListView.setMaxOverScrollHeight(height);
	}
	@Override
	public final Rect getViewportBounds() {
		return mListView.getViewportBounds();
	}
	@Override
	public final Rect copyViewportBounds() {
		return mListView.copyViewportBounds();
	}
	@Override
	public final void setOnScrollListener(OnScrollListener listener) {
		mOnScrollListener = listener;
	}
	@Override
	public final boolean canScrollHorizontally() {
		return mListView.canScrollHorizontally();
	}
	@Override
	public final boolean canScrollVertically() {
		return mListView.canScrollVertically();
	}
	@Override
	public boolean canOverScrollHorizontally() {
		return mListView.canOverScrollHorizontally();
	}
	@Override
	public boolean canOverScrollVertically() {
		return mListView.canOverScrollVertically();
	}
	@Override
	public final boolean reachesContentLeft() {
		return mListView.reachesContentLeft();
	}
	@Override
	public final boolean reachesContentRight() {
		return mListView.reachesContentRight();
	}
	@Override
	public final boolean reachesContentTop() {
		return mListView.reachesContentTop();
	}
	@Override
	public final boolean reachesContentBottom() {
		return mListView.reachesContentBottom();
	}
	@Override
	public final boolean isChildViewable(int index) {
		return mListView.isChildViewable(index);
	}
	@Override
	public void scrollSmoothly(float vx, float vy, final Runnable onFinish, final Runnable onCancel) {
		mListView.scrollSmoothly(vx, vy, onFinish, onCancel);
	}
	@Override
	public final void scrollSmoothlyTo(int x, int y, int duration, final Runnable onFinish, final Runnable onCancel) {
		mListView.scrollSmoothlyTo(x, y, duration, onFinish, onCancel);
	}
	@Override
	public final void scrollSmoothlyBy(int dx, int dy, int duration, final Runnable onFinish, final Runnable onCancel) {
		mListView.scrollSmoothlyBy(dx, dy, duration, onFinish, onCancel);
	}
	@Override
	public void forceScrollTo(int x, int y) {
		mListView.forceScrollTo(x, y);
	}
	@Override
	public void forceScrollSmoothlyTo(int x, int y, int duration, Runnable onFinish, Runnable onCancel) {
		mListView.forceScrollSmoothlyTo(x, y, duration, onFinish, onCancel);
	}
	public final void scrollSmoothlyToFirstRow(int duration, final Runnable onFinish, final Runnable onCancel) {
		mListView.scrollSmoothlyToFirstRow(duration, onFinish, onCancel);
	}
	@Override
	public void springBack() {
		mListView.springBack();
	}
	@Override
	public void springBackSmoothly() {
		mListView.springBackSmoothly();
	}
	@Override
	public Point content2view(Point point) {
		return mListView.content2view(point);
	}
	@Override
	public Rect content2view(Rect rect) {
		return mListView.content2view(rect);
	}
	@Override
	public Point view2content(Point point) {
		return mListView.view2content(point);
	}
	@Override
	public Rect view2content(Rect rect) {
		return mListView.view2content(rect);
	}
	
	// ### 重写函数 ###
	@Override
	public void scrollBy(int dx, int dy) {
		mListView.scrollBy(dx, dy);
	}
	@Override
	public void scrollTo(int x, int y) {
		mListView.scrollTo(x, y);
	}
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		
		loadMoreItemsIfNeeded();
	}
	
	// ### 实现函数 ###
	protected void onAdjustDrag(PointF offset) {
		
	}
	protected void onAdjustViewport(ScrollState scrollState, RectF viewportBounds) {
		
	}
	protected boolean needLoadMoreItems() {
		return false;
	}
	private final void onItemsChanged(int itemCount) {
		loadMoreItemsIfNeeded();
		
		if (mPullRefreshView.pullRefreshState() == PullDownRefreshView.RefreshState.REFRESHING
				&& getListState() != ListState.LOADING_UPDATES) {
			pullRefreshState(PullDownRefreshView.RefreshState.REFRESH_DONE);
			postDelayed(new Runnable() {
				@Override
				public void run() {
					mListView.undockHatTip();
				}
					
			}, UiUtils.ANIM_DURATION_NORMAL);
		}
	}
	private final void doRefresh(boolean clear) {
		mProxyAdapter.refresh(clear || ((getListState() == ListState.UNKOWN || getListState() == ListState.EMPTY) 
				&& mPullRefreshView.pullRefreshState() != PullDownRefreshView.RefreshState.REFRESHING));
	}
	private final void loadMoreItemsIfNeeded() {
		if (getListState() != ListState.MORE_TO_LOAD)
			return;
		
		final int firstInvisibleIndex = mListView.getLastVisibleItemIndex() + 1;
		if (needLoadMoreItems() || mListView.getItemCount() - firstInvisibleIndex <= (mListView.getVisibleItemCount() + 1) * 3) {
			mProxyAdapter.loadMoreItems();
		}
	}
	private final void pullRefreshState(PullDownRefreshView.RefreshState newState) {
		mPullRefreshView.pullRefreshState(newState);
	}

	// ### 内嵌类 ###
	public static enum ListState {
		UNKOWN,
		EMPTY,
		ERROR,
		MORE_TO_LOAD,
		FIRST_LOADING,
		LOADING_MORE,
		LOADING_UPDATES,
		LOADING_COMPLETE
	}
	
	public static abstract class ListAdapter extends HatGridView.GridAdapter {
		private ListState mListState = ListState.UNKOWN;
		
		public final ListState getListState() {
			return mListState;
		}
		public final void notifyLoadingError() {
			mListState = getItemCount() > 0 ? ListState.LOADING_COMPLETE : ListState.ERROR;
			super.notifyItemsChanged();
		}
		public final void notifyLoadingDone(boolean hasMore) {
			if (getItemCount() > 0) {
				mListState = hasMore ? ListState.MORE_TO_LOAD : ListState.LOADING_COMPLETE;
			} else {
				mListState = ListState.EMPTY;
			}

			super.notifyItemsChanged();
		}
		
		// ### 实现函数 ###
		private final void refresh(boolean clear) {
			if (isLoading())
				return;

			if (clear == false) {
				mListState = ListState.LOADING_UPDATES;
				if (onLoadItemUpdates()) {
					return;					
				}
			}
			
			onClearAllItems();
			mListState = ListState.FIRST_LOADING;
			onLoadMoreItems(MIN_LOADING_ITEMS);
			super.notifyItemsChanged();
		}
		private final void loadMoreItems(int suggestedCount) {
			mListState = ListState.LOADING_MORE;
			onLoadMoreItems(suggestedCount);
		}
		private final boolean isLoading() {
			return mListState == ListState.FIRST_LOADING 
					|| mListState == ListState.LOADING_MORE
					|| mListState == ListState.LOADING_UPDATES;
		}
		
		// ### 可重写函数 ###
		protected boolean onLoadItemUpdates() {
			return false;
		}
		
		// ### 抽象函数 ###
		protected abstract void onClearAllItems();
		protected abstract void onLoadMoreItems(int suggestedCount);
	}
	private class ProxyAdapter extends HatGridView.GridAdapter implements ItemsObserver {
		private ListAdapter mBaseAdapter = null;
		
		public final ListAdapter baseAdapter() {
			return mBaseAdapter;
		}
		public final void baseAdapter(ListAdapter adapter) {
			if (mBaseAdapter != null) {
				mBaseAdapter.removeObserver(this);
			}
			
			mBaseAdapter = adapter;
			
			if (mBaseAdapter != null) {
				mBaseAdapter.addObserver(this);
			}

		}
		public final boolean isLoading() {
			if (mBaseAdapter == null)
				return false;
			
			return mBaseAdapter.isLoading();
		}
		public final boolean hasMoreItems() {
			return listState() == ListState.MORE_TO_LOAD || listState() == ListState.LOADING_MORE;
		}
		public final int listItemCount() {
			return (listState() == ListState.UNKOWN || listState() == ListState.EMPTY) ? 0 : mBaseAdapter.getItemCount();
		}
		public final ListState listState() {
			if (mBaseAdapter == null)
				return ListState.UNKOWN;
			
			return mBaseAdapter.getListState();
		}
		public final void refresh(boolean clear) {
			if (mBaseAdapter == null) {
				return;
			}

			mBaseAdapter.refresh(clear);
		}
		public final void loadMoreItems() {
			if (mBaseAdapter == null) {
				return;
			}

			mBaseAdapter.loadMoreItems(Math.max(MIN_LOADING_ITEMS, Math.min(mListView.getVisibleItemCount() * 3, MAX_LOADING_ITEMS)));
		}

		@Override
		public int getGroupCount() {
			if (mBaseAdapter == null)
				return 0;

			return mBaseAdapter.getGroupCount();
		}
		@Override
		public int getGroupSize(int groupIndex) {
			if (mBaseAdapter == null)
				return 0;

			return mBaseAdapter.getGroupSize(groupIndex);
		}
		@Override
		public View getGroupTitleView(int groupIndex, View oldView, ViewGroup parentView) {
			if (mBaseAdapter == null)
				return null;

			return mBaseAdapter.getGroupTitleView(groupIndex, oldView, parentView);
		}
		@Override
		public final int getItemCount() {
			return listItemCount();
		}
		@Override
		public final Object getItem(int index) {
			return mBaseAdapter.getItem(index);
		}
		@Override
		public final View getEmptyView(View oldView, ViewGroup parentView) {
			if (mBaseAdapter == null)
				return null;
			
			switch (listState()) {
			case UNKOWN:
			case FIRST_LOADING:
				final ImageView loadingView = (ImageView) LayoutInflater.from(getContext()).inflate(R.layout.general__web_first_loading_view, parentView, false);
				((Animatable) loadingView.getDrawable()).start();
				return loadingView;
			case EMPTY:
				final View emptyView = mBaseAdapter.getEmptyView(null, parentView);
				return emptyView;
			case ERROR:
				final View errorView = LayoutInflater.from(getContext()).inflate(R.layout.general__web_error_view, parentView, false);
				errorView.findViewById(R.id.general__dk_web_error_view__refresh).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						refresh(true);
					}
				});
				return errorView;
			default:
				assert false;
				return null;
			}
		}
		@Override
		public final View getHeaderView(int itemCount, View oldView, ViewGroup parentView) {
			if (mBaseAdapter == null)
				return null;

			return mBaseAdapter.getHeaderView(itemCount, oldView, parentView);
		}
		@Override
		public final View getFooterView(int itemCount, View oldView, ViewGroup parentView) {
			final View view;
			if (oldView != null && oldView.getId() == R.id.general__dk_web_loading_more_view__root) {
				view = oldView;
			} else {
				view = LayoutInflater.from(getContext()).inflate(R.layout.general__web_loading_more_view, parentView, false); 
			}

			final FrameLayout frameView = (FrameLayout) view.findViewById(R.id.general__web_loading_more_view__footer_frame);
			final View loadingView = view.findViewById(R.id.general__web_loading_more_view__loading);

			final View oldFooterView = frameView.getChildCount() > 0 ? frameView.getChildAt(0) : null;
			final View newFooterView = (mBaseAdapter == null || hasMoreItems()) ? 
					null : mBaseAdapter.getFooterView(itemCount, oldFooterView, frameView);
			
			if (oldFooterView != newFooterView) {
				if (oldFooterView != null) {
					frameView.removeView(oldFooterView);
				}
				if (newFooterView != null) {
					frameView.addView(newFooterView);
				}
			}

			loadingView.setVisibility(hasMoreItems() ? View.VISIBLE : View.GONE);
			return view;
		}
		@Override
		public final View getItemView(int index, View oldView, ViewGroup cellView) {
			assert mBaseAdapter != null;
			
			final View view = mBaseAdapter.getItemView(index, oldView, cellView);
			return view;
		}
		@Override
		public void onItemsAdded(int addCount, int addTo) {
			onItemsChanged(getItemCount());
		}
		@Override
		public void onItemsRemoved(int removeFrom, int removeCount) {
			onItemsChanged(getItemCount());
		}
		@Override
		public final void onItemsMoved(int moveFrom, int moveCount, int moveTo) {
			onItemsChanged(getItemCount());
		}
		@Override
		public final void onItemsModified(int modifyFrom, int modifyCount) {
			onItemsChanged(getItemCount());
		}
		@Override
		public final void onItemsChanged(int itemCount) {
			if (mBaseAdapter == null)
				return;
			
			notifyItemsChanged();
			DkWebListView.this.onItemsChanged(itemCount);
		}
	}
	
}
