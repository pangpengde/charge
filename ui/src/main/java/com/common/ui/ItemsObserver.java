package com.common.ui;

public interface ItemsObserver {
	void onItemsAdded(int addCount, int addTo);
	void onItemsRemoved(int removeFrom, int removeCount);
	void onItemsMoved(int moveFrom, int moveCount, int moveTo);
	void onItemsModified(int modifyFrom, int modifyCount);
	void onItemsChanged(int itemCount);
}
