package com.px.charge.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
        mNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().contains(".")) {
                    if (s.length() - 1 - s.toString().indexOf(".") > 2) {
                        s = s.toString().subSequence(0,
                                s.toString().indexOf(".") + 3);
                        mNumber.setText(s);
                        mNumber.setSelection(s.length());
                    }
                }
                if (s.toString().trim().substring(0).equals(".")) {
                    s = "0" + s;
                    mNumber.setText(s);
                    mNumber.setSelection(2);
                }
                if (s.toString().startsWith("0")
                        && s.toString().trim().length() > 1) {
                    if (!s.toString().substring(1, 2).equals(".")) {
                        mNumber.setText(s.subSequence(0, 1));
                        mNumber.setSelection(1);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
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
