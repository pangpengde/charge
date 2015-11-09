package com.common.ui;


import android.view.View;
import android.view.ViewGroup;

public interface GroupItemsAdapter extends ItemsAdapter {
	int getGroupCount();
	int getGroupSize(int groupIndex);
	View getGroupTitleView(int groupIndex, View oldView, ViewGroup parentView);
}
