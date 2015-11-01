package com.common.ui;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.px.charge.R;

public class PullDownRefreshView extends FrameLayout {
	// ### 值域 ###
	private final ImageView mPullRefreshIconView;
	private final TextView mPullDownTipView;
	private final TextView mReleaseTipView;
	private final TextView mRefreshingTipView;
	private final TextView mRefreshDoneTipView;
	private RefreshState mPullRefreshState = RefreshState.NO_REFRESH;
	
	// ### 内嵌类 ###
	public static enum UiStyle {
		NORMAL,
		DARK
	}
	public static enum RefreshState {
		NO_REFRESH,           // 初始状态
		DOWN_TO_REFRESH,      // 下拉刷新
		RELEASE_TO_REFRESH,   // 释放刷新
		REFRESHING,           // 刷新中
		REFRESH_DONE          // 刷新成功
	}
	
	// ### 构造函数 ###
	public PullDownRefreshView(Context context) {
		this(context, null);
	}
	public PullDownRefreshView(Context context, AttributeSet attrs) {
		super(context, attrs);

		setClipChildren(false);
		setClipToPadding(false);
		
		inflate(getContext(), R.layout.general__web_pull_refresh_view, this);

		mPullRefreshIconView = (ImageView) findViewById(R.id.general__web_pull_refresh_view__icon);
		mPullDownTipView = (TextView) findViewById(R.id.general__web_pull_refresh_view__pull_down_tip);
		mReleaseTipView = (TextView) findViewById(R.id.general__web_pull_refresh_view__release_tip);
		mRefreshingTipView = (TextView) findViewById(R.id.general__web_pull_refresh_view__refreshing_tip);
		mRefreshDoneTipView = (TextView) findViewById(R.id.general__web_pull_refresh_view__refreshed_tip);
    }
	
	// ### 方法 ###
	public final RefreshState pullRefreshState() {
		return mPullRefreshState;
	}
	public final void pullRefreshState(RefreshState newState) {
		final RefreshState oldState = mPullRefreshState;
		if (oldState == newState)
			return;
		
		switch (oldState) {
		case NO_REFRESH:
			if (newState == RefreshState.DOWN_TO_REFRESH) {
				mPullRefreshState = newState;
			}
			break;
		case DOWN_TO_REFRESH:
			if (newState == RefreshState.NO_REFRESH
					|| newState == RefreshState.RELEASE_TO_REFRESH) {
				mPullRefreshState = newState;
			}
			break;
		case RELEASE_TO_REFRESH:
			if (newState == RefreshState.DOWN_TO_REFRESH
					|| newState == RefreshState.REFRESHING) {
				mPullRefreshState = newState;
			}
			break;
		case REFRESHING:
			if (newState == RefreshState.REFRESH_DONE) {
				mPullRefreshState = newState;
			}
			break;
		case REFRESH_DONE:
			if (newState == RefreshState.NO_REFRESH
					|| newState == RefreshState.DOWN_TO_REFRESH) {
				mPullRefreshState = newState;
			}
			break;
		}
		
		if (oldState != mPullRefreshState) {
			onPullRefreshStateChanged(oldState, mPullRefreshState);
		}
	}
	
	// ### 实现函数 ###
	private final void onPullRefreshStateChanged(RefreshState oldState, RefreshState newState) {
		switch (newState) {
		case NO_REFRESH:
			clearPullRefreshAnim();
			mPullDownTipView.setVisibility(View.INVISIBLE);
			mReleaseTipView.setVisibility(View.INVISIBLE);
			mRefreshingTipView.setVisibility(View.INVISIBLE);
			mRefreshDoneTipView.setVisibility(View.INVISIBLE);
			break;
		case DOWN_TO_REFRESH:
			mPullDownTipView.setVisibility(View.VISIBLE);
			if (oldState == RefreshState.RELEASE_TO_REFRESH) {
				flyPullRefreshIconOut();
				mReleaseTipView.setVisibility(View.INVISIBLE);
			} else if (oldState == RefreshState.REFRESH_DONE) {
				clearPullRefreshAnim();
				mRefreshDoneTipView.setVisibility(View.INVISIBLE);
			}
			break;
		case RELEASE_TO_REFRESH:
			mReleaseTipView.setVisibility(View.VISIBLE);
			mPullDownTipView.setVisibility(View.INVISIBLE);
			flyPullRefreshIconIn();
			break;
		case REFRESHING:
			mRefreshingTipView.setVisibility(View.VISIBLE);
			mReleaseTipView.setVisibility(View.INVISIBLE);
			rotatePullRefreshIcon();
			break;
		case REFRESH_DONE:
			clearPullRefreshAnim();
			mRefreshDoneTipView.setVisibility(View.VISIBLE);
			mRefreshingTipView.setVisibility(View.INVISIBLE);
			break;
		}
	}
	private final void flyPullRefreshIconIn() {
		UiUtils.flyView(mPullRefreshIconView, 0.0f, 0.0f, -1.0f, 0.0f, UiUtils.ANIM_DURATION_SHORT, true, null);
	}
	private final void flyPullRefreshIconOut() {
		UiUtils.flyViewOutToTop(mPullRefreshIconView, null);
	}
	private final void rotatePullRefreshIcon() {
		((Animatable) mPullRefreshIconView.getDrawable()).start();
	}
	private final void clearPullRefreshAnim() {
		mPullRefreshIconView.getDrawable().setVisible(true, true);
		((Animatable) mPullRefreshIconView.getDrawable()).stop();
		mPullRefreshIconView.clearAnimation();
	}
}
