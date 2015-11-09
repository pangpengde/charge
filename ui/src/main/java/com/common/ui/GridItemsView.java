package com.common.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import java.util.ArrayList;


public class GridItemsView extends ItemsView {
	// ### 常量 ###
	public static final int STRETCH_NONE = 0;
	public static final int STRETCH_COLUMN_SPACING = 1;
	public static final int STRETCH_COLUMN_WIDTH = 2;
	
	// ### 值域 ###
	private int mColumnWidth = 0;
	private int mColumnCount = 0;
	private int mRowWidth = 0;
	private int mRowCount = 0;
	private int mNumColumns = 0;
	private int mDesiredColumnWidth = 0;
	private int mSupposedCellHeight = 0;
	private Drawable mRowBackground = null;
	private Drawable mRowDivider = null;
	private int mRowSpacing = 0;
	private Drawable mColumnDivider = null;
	private int mDesiredColumnSpacing = 0;
	private int mColumnSpacing = 0;
	private int mGroupCount = 0;
	private int mStretchMode = STRETCH_COLUMN_WIDTH;
	private ArrayList<ItemGroup> mGroups = new ArrayList<ItemGroup>(1);
	
	// ### 构造函数 ###
	public GridItemsView(Context context) {
		this(context, null);
	}
	public GridItemsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setClipToPadding(false);
	}

	// ### 属性 ###
	public final int getStretchMode() {
		return mStretchMode;
	}
	public final void setStretchMode(int mode) {
		if (mStretchMode != mode) {
			mStretchMode = mode;
			
			invalidateStruct();
		}
	}
	public final int getDesiredColumnWidth() {
		return mDesiredColumnWidth;
	}
	public final void setDesiredColumnWidth(int width) {
		mDesiredColumnWidth = width;
		invalidateLayout();
	}
	public final int getNumColumns() {
		return mNumColumns;
	}
	public final void setNumColumns(int count) {
		mNumColumns = count;
		invalidateStruct();
	}
	public final int getColumnCount() {
		visualize();
		return mColumnCount;
	}
	public final Drawable getColumnDivider() {
		return mColumnDivider;
	}
	public final void setColumnDivider(Drawable divider) {
		setColumnDivider(divider, false);
	}
	public final void setColumnDivider(Drawable divider, boolean keepSpacing) {
		if (mColumnDivider != divider) {
			mColumnDivider = divider;
			
			if (keepSpacing == false) {
				setDesiredColumnSpacing(mColumnDivider == null ? 0 : mColumnDivider.getIntrinsicWidth());
			}
		}
	}
	public final int getDesiredColumnSpacing() {
		return mDesiredColumnSpacing;
	}
	public final void setDesiredColumnSpacing(int spacing) {
		if (mDesiredColumnSpacing != spacing) {
			mDesiredColumnSpacing = spacing;
			invalidateStruct();
		}
	}
	public final int getRowCount() {
		visualize();
		return mRowCount;
	}
	public final Drawable getRowBackground() {
		return mRowBackground;
	}
	public final void setRowBackground(int resId) {
		setRowBackground(getResources().getDrawable(resId));
	}
	public final void setRowBackground(Drawable background) {
		mRowBackground = background;
		invalidate();
	}
	public final Drawable getRowDivider() {
		return mRowDivider;
	}
	public final void setRowDivider(int resId) {
		setRowDivider(getResources().getDrawable(resId));
	}
	public final void setRowDivider(Drawable divider) {
		setRowDivider(divider, false);
	}
	public final void setRowDivider(Drawable divider, boolean keepSpacing) {
		if (mRowDivider != divider) {
			mRowDivider = divider;
			
			if (keepSpacing == false) {
				setRowSpacing(mRowDivider == null ? 0 : mRowDivider.getIntrinsicHeight());
			}
		}
	}
	public final int getRowSpacing() {
		return mRowSpacing;
	}
	public final void setRowSpacing(int spacing) {
		if (mRowSpacing != spacing) {
			mRowSpacing = spacing;
			invalidateStruct();
		}
	}
	public final int getGroupCount() {
		return mGroupCount;
	}
	public final int getGroupSize(int groupIndex) {
		final ItemGroup group = mGroups.get(groupIndex);
		return group.mEndCellIndex - group.mStartCellIndex;
	}
	public final int getGroupRowCount(int groupIndex) {
		final ItemGroup group = mGroups.get(groupIndex);
		return group.mEndRowIndex - group.mStartRowIndex;
	}
	public final int getGroupFirstRowIndex(int groupIndex) {
		final ItemGroup group = mGroups.get(groupIndex);
		return group.mStartRowIndex;
	}
	public final int[] getGroupPosition(int itemIndex) {
		final int groupCount = getGroupCount();
		
		int groupIndex = 0;
		int groupItemIndex = itemIndex;
		for (; groupIndex < groupCount; ++groupIndex) {
			final int groupSize = getGroupSize(groupIndex);
			if (groupItemIndex < groupSize)
				break;
			
			groupItemIndex -= groupSize;
		}
		
		return new int[] { groupIndex, groupItemIndex };
	}
	public final int getItemIndex(int groupIndex, int groupItemIndex) {
		int itemIndex = groupItemIndex;
		for (int n = 0; n < groupIndex - 1; ++n) {
			final int groupSize = getGroupSize(groupIndex);

			itemIndex += groupSize;
		}
		
		return itemIndex;
	}

	// ### 方法 ###
	public final int getRowIndex(int itemIndex) {
		visualize();
		return rowIndexOfCell(itemIndex);
	}
	public final int getColumnIndex(int itemIndex) {
		visualize();
		return columnIndexOfCell(itemIndex);
	}
	public final Rect getRowBounds(int rowIndex) {
		visualize();
		return calcRowBounds(rowIndex, new Rect());
	}
	public final Rect getColumnBounds(int columnIndex) {
		visualize();
		return calcColumnBounds(columnIndex, new Rect());
	}
	public final void requestGroupInRect(int groupIndex, Rect rect, int gravity) {
		if (rect.isEmpty() || rect.width() == 0 || rect.height() == 0)
			return;

		if (groupIndex < 0 || mGroups.size() <= groupIndex)
			return;

		final Rect itemBounds = getGroupBounds(groupIndex);
		final Rect dstRect = UiUtils.tempRects.acquire();

		Gravity.apply(gravity, itemBounds.width(), itemBounds.height(), view2content(rect), dstRect);
		scrollBy(itemBounds.left - dstRect.left, itemBounds.top - dstRect.top);

		UiUtils.tempRects.release(dstRect);
		springBack();
	}
	public final void requestGroupVisible(int groupIndex) {
		if (getViewportBounds().isEmpty() || getContentWidth() == 0 || getContentHeight() == 0)
			return;

		if (groupIndex < 0 || mGroups.size() <= groupIndex)
			return;

		Rect itemBounds = getGroupBounds(groupIndex);

		scrollTo(itemBounds.left, itemBounds.top);
		springBack();
	}
	public Rect getGroupBounds(int groupIndex) {
		View itemView = mGroups.get(groupIndex).mTitleView;
		Rect itemBounds = new Rect(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom());
		return itemBounds;
	}

	// ### 重写函数 ###
//	@Override
//	public void setPaddingRelative(int start, int top, int end, int bottom) {
//		super.setPaddingRelative(start, top, end, bottom);
//		invalidateArrange();
//	}
	@Override
	protected int hitTestCell(Point point) {
		for (int r = 0; r < mRowCount; ++r) {
			final int rowFirst = cellIndex(r, 0);
			assert rowFirst >= 0;
			
			final int rowBottom = getCellBottom(rowFirst);
			if (rowBottom <= point.y)
				continue;
			
			final int rowTop = getCellTop(rowFirst);
			if (rowTop > point.y)
				break;
			
			for (int c = 0; c < mColumnCount; ++c) {
				final int index = cellIndex(r, c);

				if (inCell(index, point)) {
					return index;
				}
			}
			
			break;
		}
		
		return -1;
	}
	@Override
	protected int[] hitTestCells(Rect rect) {
		int firstIndex = -1;
		int lastIndex = -1;
		for (int r = 0; r < mRowCount; ++r) {
			final int index = cellIndex(r, 0);
			assert index >= 0;
			
			final boolean hits = intersectsCell(index, rect);
			if (hits == false) {
				if (firstIndex >= 0)
					break;
				else
					continue;
			}
			
			if (firstIndex < 0) {
				firstIndex = index;
			}
			
			lastIndex = cellIndex(r, mColumnCount - 1);
			assert lastIndex >= 0;
		}
		
		if (firstIndex < 0)
			return new int[0];
		
		final int[] indices = new int[lastIndex - firstIndex + 1];
		for (int n = firstIndex; n <= lastIndex; ++n) {
			indices[n - firstIndex] = n;
		}
		return indices;
	}
	@Override
	protected void drawItemsBackground(Canvas canvas) {
		super.drawItemsBackground(canvas);
		
		final Rect bounds = UiUtils.tempRects.acquire();
		if (mRowBackground != null) {
			final int[] indices = visibleCellIndices();
			if (indices.length > 0) {
				final int firstIndex = rowIndexOfCell(indices[0]);
				final int lastIndex = rowIndexOfCell(indices[indices.length - 1]);
				for (int r = firstIndex; r <= lastIndex; ++r) {
					calcRowBounds(r, bounds);
					mRowBackground.setLevel(r);
					mRowBackground.setBounds(bounds);
					mRowBackground.draw(canvas);
				}
			}
		}
		UiUtils.tempRects.release(bounds);
	}
	@Override
	protected void drawItemsForeground(Canvas canvas) {
		super.drawItemsForeground(canvas);
		
		final Rect bounds1 = UiUtils.tempRects.acquire();
		final Rect bounds2 = UiUtils.tempRects.acquire();
		
		if (mRowDivider != null) {
			final int[] indices = visibleCellIndices();
			if (indices.length > 0) {
				final int firstIndex = rowIndexOfCell(indices[0]);
				final int lastIndex = rowIndexOfCell(indices[indices.length - 1]);
				for (int r = firstIndex; r <= lastIndex; ++r) {
					final int groupIndex = groupIndexOfRow(r);
					if (followedByRowSpacing(groupIndex, r - mGroups.get(groupIndex).mStartRowIndex) == false)
						continue;
					
					calcRowBounds(r, bounds1);
					calcRowBounds(r + 1, bounds2);
					
					if (Rect.intersects(bounds1, getViewportBounds()) == false
							|| Rect.intersects(bounds2, getViewportBounds()) == false)
						break;
					
					final int top = (bounds2.top + bounds1.bottom - mRowDivider.getIntrinsicHeight()) / 2;
					mRowDivider.setBounds(bounds1.left, top, bounds1.right, top + mRowDivider.getIntrinsicHeight());
					mRowDivider.draw(canvas);
				}
			}
		}
		
		if (mColumnDivider != null) {
			for (int n = 0; n < mGroups.size(); ++n) {
				final ItemGroup group = mGroups.get(n);
				if (group.mEndRowIndex - group.mStartRowIndex < 1)
					continue;
				
				calcRowBounds(group.mStartRowIndex, bounds1);
				calcRowBounds(group.mEndRowIndex - 1, bounds2);
				final int top = bounds1.top;
				final int bottom = bounds2.bottom;

				for (int c = 0; c < mColumnCount - 1; ++c) {
					calcColumnBounds(c, bounds1);
                    calcColumnBounds(c + 1, bounds2);
					
					if (Rect.intersects(bounds1, getViewportBounds()) == false
							|| Rect.intersects(bounds2, getViewportBounds()) == false)
						break;
					
					final int left = (bounds2.left + bounds1.right - mColumnDivider.getIntrinsicWidth()) / 2;
					mColumnDivider.setBounds(left, top, left + mColumnDivider.getIntrinsicWidth(), bottom);
					mColumnDivider.draw(canvas);
				}
			}
		}
		
		UiUtils.tempRects.release(bounds1);
		UiUtils.tempRects.release(bounds2);
	}
	@Override
	protected void onArrange() {
		final int rowLeft = getCellsMarginLeft();
		int rowTop = getCellsMarginTop();
		for (int n = 0; n < mGroups.size(); ++n) {
			final ItemGroup group = mGroups.get(n);
			// 布局组标题
			if (group.mTitleView != null) {
				final LayoutParams lp = (LayoutParams) group.mTitleView.getLayoutParams();
				final int left = rowLeft + lp.leftMargin;
				final int top = rowTop + lp.topMargin;
				group.mTitleView.layout(left, top, left + group.mTitleView.getMeasuredWidth(), top + group.mTitleView.getMeasuredHeight());
				rowTop = top + group.mTitleView.getMeasuredHeight() + lp.bottomMargin;
			}
			
			for (int r = 0; r < group.mEndRowIndex - group.mStartRowIndex; ++r) {
				// 计算行高
				int rowHeight = 0;
				for (int c = 0; c < mColumnCount; ++c) {
					final int index = group.mStartCellIndex + r * mColumnCount + c;
					if (index >= group.mEndCellIndex)
						break;
					
					// 如果单元高度没有测量, 初始化测量规则.
                    if (getCellMeasuredHeight(index) < 0) {
						setCellWidthMeasureSpec(index, MeasureSpec.makeMeasureSpec(mColumnWidth, MeasureSpec.EXACTLY));
						setCellHeightMeasureSpec(index, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
					}
					
					final int measuredHeight = getCellMeasuredHeight(index);
					rowHeight = Math.max(rowHeight, measuredHeight >= 0 ? measuredHeight : mSupposedCellHeight);
				}
				
				int columnLeft = rowLeft;
				for (int c = 0; c < mColumnCount; ++c) {
					final int index = group.mStartCellIndex + r * mColumnCount + c;
					if (index >= group.mEndCellIndex)
						break;

					final int left = columnLeft;
					final int top = rowTop;
					final int right = left + mColumnWidth;
					final int bottom = top + rowHeight;
					
					setCellWidthMeasureSpec(index, MeasureSpec.makeMeasureSpec(mColumnWidth, MeasureSpec.EXACTLY));
					if (getCellMeasuredHeight(index) >= 0) {
						// 当单元高度完成测量后, 就可以使用行高来最终决定单元高度了. 
						setCellHeightMeasureSpec(index, MeasureSpec.makeMeasureSpec(rowHeight, MeasureSpec.EXACTLY));
					}
					
					arrangeCell(index, left, top, right, bottom);
					columnLeft = right + mColumnSpacing;
				}
				rowTop += rowHeight;
				
				if (followedByRowSpacing(n, r)) {
					rowTop += mRowSpacing;
				}
			}
		}
		
		setContentDimension(mRowWidth + getCellsMarginHorizontal(), rowTop + getCellsMarginBottom());
	}
    @Override
	protected int onStruct(int widthSpec, int heightSpec) {
		final int widthMode = MeasureSpec.getMode(widthSpec);
		final int widthSize = MeasureSpec.getSize(widthSpec);
		final int horzMargin = getCellsMarginHorizontal();
		final int vertMargin = getCellsMarginVertical();

		// 保存旧的结构信息
		final int oldColumnWidth = mColumnWidth;
		
		// 更新分组信息
		mGroupCount = getAdapter() instanceof GroupItemsAdapter ?
					((GroupItemsAdapter) getAdapter()).getGroupCount() : 0;
        // TODO mCells在adpater调用itemChanged之后才会更新, 这里的mGroups也应该保持相同逻辑
		if (mGroupCount < 1) {
			for (int n = mGroups.size() - 1; n >= 0; --n) {
				final ItemGroup group = mGroups.get(n);
				if (group.mTitleView != null) {
					removeViewInLayout(group.mTitleView);
				}
				mGroups.remove(n);
			}
			
			final ItemGroup group = new ItemGroup();
			group.mStartCellIndex = 0;
			group.mEndCellIndex = group.mStartCellIndex + getItemCount();
			group.mTitleView = null;
			mGroups.add(group);
		} else {
			final GroupItemsAdapter adapter = (GroupItemsAdapter) getAdapter();
			mGroups.ensureCapacity(mGroupCount);
			for (int n = 0; n < mGroupCount; ++n) {
				final ItemGroup group;
				if (n < mGroups.size()) {
					group = mGroups.get(n);
				} else {
					group = new ItemGroup();
					mGroups.add(group);
				}

				group.mStartCellIndex = n < 1 ? 0 : mGroups.get(n - 1).mEndCellIndex;
				group.mEndCellIndex = group.mStartCellIndex + adapter.getGroupSize(n);

				final View oldTitleView = group.mTitleView;
				group.mTitleView = adapter.getGroupTitleView(n, oldTitleView, this);
				if (group.mTitleView != oldTitleView) {
					if (oldTitleView != null) {
						removeViewInLayout(oldTitleView);
					}
					if (group.mTitleView != null) {
						final LayoutParams lp;
						if (group.mTitleView.getLayoutParams() == null) {
							lp = (LayoutParams) generateDefaultLayoutParams();
						} else if (group.mTitleView.getLayoutParams() instanceof LayoutParams) {
							lp = (LayoutParams) group.mTitleView.getLayoutParams();
						} else {
							lp = (LayoutParams) generateLayoutParams(group.mTitleView.getLayoutParams());
						}
						
						addViewInLayout(group.mTitleView, -1, lp);
					}
				}
			}
			for (int n = mGroups.size() - 1; n >= mGroupCount; --n) {
				final ItemGroup group = mGroups.get(n);
				if (group.mTitleView != null) {
					removeViewInLayout(group.mTitleView);
				}
				mGroups.remove(n);
			}
		}
		
		// 测量第一个单元视图
		final int cellWidth;
		final int cellHeight;
		if (getItemCount() > 0) {
			setCellWidthMeasureSpec(0, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			setCellHeightMeasureSpec(0, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			measureCell(0);
			
			cellWidth = widthMode == MeasureSpec.UNSPECIFIED ? 
					getCellMeasuredWidth(0) : Math.min(getCellMeasuredWidth(0), widthSize - horzMargin);
			cellHeight = getCellMeasuredHeight(0);
		} else {
			cellWidth = 0;
			cellHeight = 0;
		}

		final int columnCount;
		final int columnWidth;
		final int columnSpacing;
		if (mStretchMode == STRETCH_COLUMN_SPACING) {
			// 优先确定列宽
			if (mDesiredColumnWidth > 0) {
				// 指定了期望的列宽
				columnWidth = mDesiredColumnWidth;
			} else {
				// 没有指定期望的列宽
				columnWidth = cellWidth;
			}
			
			// 然后确定列数
			if (mNumColumns > 0) {
				// 直接指定了列数
				columnCount = mNumColumns;
			} else if (widthMode == MeasureSpec.UNSPECIFIED) {
				// 没有指定列数, 且没有限定行宽.
				columnCount = 1;
			} else {
				// 没有指定列数, 但限定了行宽.
				columnCount = (widthSize - horzMargin + mDesiredColumnSpacing) / (columnWidth + mDesiredColumnSpacing);
			}
			
			// 最后确定列间距
			if (widthMode == MeasureSpec.UNSPECIFIED) {
				// 没有限定行宽
				columnSpacing = mDesiredColumnSpacing;
			} else {
				// 限定了行宽
				columnSpacing = columnCount < 2 ? 0 : (widthSize - horzMargin - columnWidth * columnCount) / (columnCount - 1);
			}
			
		} else { // mStretchMode == STRETCH_COLUMN_WIDTH
			// 优先确定列数
			if (mNumColumns > 0) {
				// 直接指定了列数
				columnCount = mNumColumns;
			} else if (widthMode == MeasureSpec.UNSPECIFIED) {
				// 没有指定列数, 且没有限定行宽.
				columnCount = 1;
			} else if (mDesiredColumnWidth > 0) {
				// 没有指定列数, 但限定了行宽, 并指定了期望的列宽.
				final int bestCount = (widthSize - horzMargin + mDesiredColumnSpacing) / (mDesiredColumnWidth + mDesiredColumnSpacing);
				columnCount = Math.max(1, bestCount); // 保证至少有一列
			} else {
				// 没有指定列数, 但限定了行宽, 但没有指定期望的列宽.
				final int cellCount = (widthSize - horzMargin + mDesiredColumnSpacing) / (cellWidth + mDesiredColumnSpacing);
				columnCount = Math.max(1, cellCount);
			}
			assert columnCount > 0;
			
			// 然后确定列宽
			if (widthMode != MeasureSpec.UNSPECIFIED) {
				// 限定了行宽
				columnWidth = (widthSize - horzMargin + mDesiredColumnSpacing) / columnCount - mDesiredColumnSpacing;
			} else if (mDesiredColumnWidth > 0) {
				// 没有限定行宽, 但指定了期望的列宽.
				columnWidth = mDesiredColumnWidth;
			} else {
				// 没有限定行宽, 且没有指定期望的列宽.
				columnWidth = cellWidth;
			}
			
			// 最后确定列间距
			columnSpacing = mDesiredColumnSpacing;
		}
		
		mColumnCount = columnCount;
		if (columnSpacing < 0) {
			mColumnWidth = mRowWidth / columnCount;
			mColumnSpacing = 0;
		} else {
			mColumnWidth = columnWidth;
			mColumnSpacing = columnSpacing;
		}

		// 确定行数
		mRowCount = 0;
		for (int n = 0; n < mGroups.size(); ++n) {
			final ItemGroup group = mGroups.get(n);
			final int rowCount = (group.mEndCellIndex - group.mStartCellIndex + mColumnCount - 1) / mColumnCount;
			group.mStartRowIndex = mRowCount;
			group.mEndRowIndex = group.mStartRowIndex + rowCount;
			mRowCount += rowCount;
		}
		mRowWidth = mColumnWidth * mColumnCount + mColumnSpacing * (mColumnCount - 1);

		// 使用确定好的列宽, 重新测量第一个单元视图, 以便估算出更精确的单元高度.
		if (getItemCount() > 0) {
			setCellWidthMeasureSpec(0, MeasureSpec.makeMeasureSpec(columnWidth, MeasureSpec.EXACTLY));
			setCellHeightMeasureSpec(0, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			measureCell(0);
			
			mSupposedCellHeight = getCellMeasuredHeight(0);
		} else {
			
			mSupposedCellHeight = cellHeight;
		}
		
		// 确定单元格总高度/宽度
		int contentHeight = vertMargin;
		for (int n = 0; n < mGroups.size(); ++n) {
			final ItemGroup group = mGroups.get(n);
			final int rowCount = group.mEndRowIndex - group.mStartRowIndex;

			if (group.mTitleView != null) {
				final LayoutParams lp = (LayoutParams) group.mTitleView.getLayoutParams();
				group.mTitleView.measure(
						MeasureSpec.makeMeasureSpec(mRowWidth - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY), 
						lp.height > 0 ? MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY) : MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED) 
				);
				contentHeight += group.mTitleView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
			}
			
			if (rowCount > 0) {
				contentHeight += cellHeight * rowCount + mRowSpacing * (rowCount - 1);

				if (followedByRowSpacing(n, rowCount - 1)) {
					contentHeight += mRowSpacing;
				}
			}
		}
		
		setContentDimension(mRowWidth + getCellsMarginHorizontal(), contentHeight);
		if (getItemCount() < 1 && mGroupCount < 1)
			return STRUCT_EMPTY;
		else
			return (mColumnWidth != oldColumnWidth) ? STRUCT_CHANGED : STRUCT_DONE;
	}
	
	// ### 实现函数 ###
	protected final Rect calcRowBounds(int rowIndex, Rect bounds) {
		final int firstInRow = cellIndex(rowIndex, 0);
		bounds.left = getCellLeft(firstInRow);
		bounds.top = getCellTop(firstInRow);
		bounds.right = bounds.left + mRowWidth;
		bounds.bottom = getCellBottom(firstInRow);
		return bounds;
	}
	protected final Rect calcColumnBounds(int columnIndex, Rect bounds) {
		final int lastInFirstColumn = cellIndex(mRowCount - 1, 0);
		bounds.left = getCellsMarginLeft() + (mColumnWidth + mColumnSpacing) * columnIndex;
		bounds.top = getCellsMarginTop();
		bounds.right = bounds.left + mColumnWidth;
		bounds.bottom = getCellBottom(lastInFirstColumn);
		return bounds;
	}
	protected final int rowIndexOfCell(int cellIndex) {
		final int groupIndex = groupIndexOfCell(cellIndex);
		final ItemGroup group = mGroups.get(groupIndex);
		return group.mStartRowIndex + (cellIndex - group.mStartCellIndex) / mColumnCount;
	}
	protected final int columnIndexOfCell(int cellIndex) {
		final int groupIndex = groupIndexOfCell(cellIndex);
		final ItemGroup group = mGroups.get(groupIndex);
		return (cellIndex - group.mStartCellIndex) % mColumnCount;
	}
	protected final int cellIndex(int rowIndex, int columnIndex) {
		final int groupIndex = groupIndexOfRow(rowIndex);
		if (groupIndex < 0)
			return -1;
		
		final ItemGroup group = mGroups.get(groupIndex);
		final int cellIndex = group.mStartCellIndex + mColumnCount * (rowIndex - group.mStartRowIndex) + columnIndex;
		return Math.max(group.mStartCellIndex, Math.min(cellIndex, group.mEndCellIndex - 1));
	}
	protected final int groupIndexOfCell(int cellIndex) {
		for (int n = 0; n < mGroups.size(); ++n) {
			final ItemGroup group = mGroups.get(n);
			if (cellIndex >= group.mStartCellIndex && cellIndex < group.mEndCellIndex) {
				return n;
			}
		}
		
		return -1;
	}
	protected final int groupIndexOfRow(int rowIndex) {
		for (int n = 0; n < mGroups.size(); ++n) {
			final ItemGroup group = mGroups.get(n);
			if (rowIndex >= group.mStartRowIndex && rowIndex < group.mEndRowIndex) {
				return n;
			}
		}
		
		return -1;
	}
	protected final boolean followedByRowSpacing(int groupIndex, int groupRowIndex) {
		final ItemGroup group = mGroups.get(groupIndex);
		if (groupRowIndex + 1 < group.mEndRowIndex - group.mStartRowIndex) {
			return true;
		}

		for (int n = groupIndex + 1; n < mGroups.size(); ++n) {
			final ItemGroup nextGroup = mGroups.get(n);
			if (nextGroup.mTitleView != null) {
				return false;
			}
				
			if (nextGroup.mEndRowIndex - nextGroup.mStartRowIndex > 0) {
				return true;
			}
		}

		return false;
	}

    // ### 内嵌类 ###
	private static class ItemGroup {
		public int mStartCellIndex = 0;
		public int mEndCellIndex = 0;
		public int mStartRowIndex = 0;
		public int mEndRowIndex = 0;
		public View mTitleView = null;
	}
}
