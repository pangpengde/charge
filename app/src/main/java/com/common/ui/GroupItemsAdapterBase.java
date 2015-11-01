package com.common.ui;


public abstract class GroupItemsAdapterBase extends ItemsAdapterBase implements GroupItemsAdapter {
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
		for (int n = 0; n <= groupIndex - 1; ++n) {
			final int groupSize = getGroupSize(n);

			itemIndex += groupSize;
		}
		
		return itemIndex;
	}
}
