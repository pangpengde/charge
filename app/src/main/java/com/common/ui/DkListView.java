package com.common.ui;

import android.content.Context;
import android.util.AttributeSet;

public class DkListView extends GridItemsView {
	public DkListView(Context context) {
		this(context, null);
	}

	public DkListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		setNumColumns(1);
		setThumbEnabled(true);
		setMaxOverScrollHeight(UiUtils.getScaledOverScrollHeight(getContext()));
	}
}
