package com.px.charge.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.common.ui.DkWebListView;
import com.common.ui.HorzLineDrawable;
import com.common.ui.UiUtils;
import com.px.charge.R;
import com.px.charge.dao.AccountBook;
import com.px.charge.dao.Charge;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

import rx.functions.Action1;

/**
 * Created by pangpengde on 15/8/16.
 */
public class ListFragment extends BaseFragment implements AccountBook.AccountBookChangedListener {
    private static final String TAG = "ListFragment";
    private static final int PAGE_COUNT = 25;
    private final LinkedList<Charge> mChargeList = new LinkedList<Charge>();
    private boolean mFirstAttach = false;
    private DkWebListView mListView;
    private DkWebListView.ListAdapter mAdapter = new DkWebListView.ListAdapter() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        @Override
        protected void onClearAllItems() {

        }

        @Override
        protected void onLoadMoreItems(int suggestedCount) {
            loadData(mChargeList.size(), suggestedCount);
        }

        @Override
        public int getItemCount() {
            return mChargeList.size();
        }

        @Override
        public Charge getItem(int index) {
            return mChargeList.get(index);
        }

        @Override
        public View getItemView(int index, View oldView, ViewGroup parentView) {
            final View view;
            if (oldView != null) {
                view = oldView;
            } else {
                view = LayoutInflater.from(getActivity()).inflate(R.layout.list_item_view, parentView, false);
            }
            final Charge charge = getItem(index);
            TextView dateView = (TextView) view.findViewById(R.id.date);
            dateView.setText(sdf.format(charge.getPaidDate()));
            TextView titleView = (TextView) view.findViewById(R.id.title);
            titleView.setText(charge.getTitle());
            TextView priceView = (TextView) view.findViewById(R.id.price);
            priceView.setText(String.valueOf(charge.getPrice()));
            return view;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savednceState) {
        Log.w("", "px onCreateView");
        mListView = new DkWebListView(getActivity());
        mListView.setAdapter(mAdapter);
        HorzLineDrawable drawable = new HorzLineDrawable(Color.rgb(0xf2, 0xf2, 0xf2));
        drawable.setHeight(UiUtils.dip2px(getContext(), 5));
        mListView.setRowDivider(drawable);
        return mListView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFirstAttach == false) {
            mFirstAttach = true;
            loadData(0, PAGE_COUNT);
        }
    }

    @Override
    public void onAttach(Activity activity) {

        AccountBook.get().addListener(this);
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        AccountBook.get().removeListener(this);
        super.onDetach();
    }

    private void loadData(int index, int pageCount) {
        AccountBook.get().query(index, pageCount).subscribe(new Action1<List<? extends Charge>>() {
            @Override
            public void call(List<? extends Charge> charges) {
                if (charges.size() > 0) {
                    mChargeList.addAll(charges);
                    mAdapter.notifyLoadingDone(true);
                } else {
                    mAdapter.notifyLoadingDone(false);
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                mAdapter.notifyLoadingError();
            }
        });
    }

    @Override
    public void onAccountBookChanged() {
        if (mListView != null) {
            mListView.refresh(true);
        }
    }
}
