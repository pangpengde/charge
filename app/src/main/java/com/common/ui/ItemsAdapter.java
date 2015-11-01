package com.common.ui;


import android.view.View;
import android.view.ViewGroup;

public interface ItemsAdapter {
	void addObserver(ItemsObserver observer);
	void removeObserver(ItemsObserver observer);
	int getItemCount();
	Object getItem(int index);
	View getItemView(int index, View oldView, ViewGroup parentView);
	View getEmptyView(View oldView, ViewGroup parentView);
}
