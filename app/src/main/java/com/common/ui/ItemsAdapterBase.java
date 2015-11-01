package com.common.ui;

import java.util.LinkedList;


import android.view.View;
import android.view.ViewGroup;

public abstract class ItemsAdapterBase implements ItemsAdapter {
	// ### 值域 ###
	private final LinkedList<ItemsObserver> mObserverList = new LinkedList<ItemsObserver>();
	
	// ### 方法 ###
	public void notifyItemsAdded(int addTo, int addCount) {
		for (ItemsObserver observer : mObserverList) {
			observer.onItemsAdded(addTo, addCount);
		}
	}
	public void notifyItemsRemoved(int removeFrom, int removeCount) {
		for (ItemsObserver observer : mObserverList) {
			observer.onItemsRemoved(removeFrom, removeCount);
		}
	}
	public void notifyItemsMoved(int moveFrom, int moveCount, int moveTo) {
		for (ItemsObserver observer : mObserverList) {
			observer.onItemsMoved(moveFrom, moveCount, moveTo);
		}
	}
	public void notifyItemsModified(int modifyFrom, int modifyCount) {
		for (ItemsObserver observer : mObserverList) {
			observer.onItemsModified(modifyFrom, modifyCount);
		}
	}
	public void notifyItemsChanged() {
		for (ItemsObserver observer : mObserverList) {
			observer.onItemsChanged(getItemCount());
		}
	}
	
	// ### ItemsAdapter接口实现 ###
	@Override
	public void addObserver(ItemsObserver observer) {
		assert observer != null;
		
		if (mObserverList.contains(observer))
			return;
		
		mObserverList.add(observer);
	}
	@Override
	public void removeObserver(ItemsObserver observer) {
		assert observer != null;
		
		mObserverList.remove(observer);
	}
	@Override
	public View getEmptyView(View oldView, ViewGroup parentView) {
		return null;
	}
}
