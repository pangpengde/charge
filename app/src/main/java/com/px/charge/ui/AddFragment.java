package com.px.charge.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.px.charge.R;
import com.px.charge.dao.AccountBook;

/**
 * Created by pangpengde on 15/8/10.
 */
public class AddFragment extends BaseFragment {
    public static final String TAG = AddFragment.class.getName();

    private EditText mTitle;
    private EditText mNumber;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_main, container, false);

        mTitle = (EditText) rootView.findViewById(R.id.activity_main__title);
        mNumber = (EditText) rootView.findViewById(R.id.activity_main__num);
        rootView.findViewById(R.id.activity_main__ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mTitle.getText().toString().trim())) {
                    Toast.makeText(getActivity(), "请输入标题", Toast.LENGTH_LONG).show();
                } else if (TextUtils.isEmpty(mNumber.getText().toString().trim())) {
                    Toast.makeText(getActivity(), "请输入价钱", Toast.LENGTH_LONG).show();
                } else {
                    AccountBook.get().add(mTitle.getText().toString(), Float.valueOf(mNumber.getText().toString().trim()), "");
                    mTitle.getText().clear();
                    mNumber.getText().clear();
                }
            }
        });

        rootView.findViewById(R.id.activity_main__tt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
                getActivity().startActivity(intent);
            }
        });

        rootView.findViewById(R.id.activity_main__ali).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage("com.alipay.android.app");
                if (intent != null) {
                    getActivity().startActivity(intent);
                }
            }
        });

        return rootView;
    }
}
