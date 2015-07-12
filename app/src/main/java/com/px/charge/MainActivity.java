package com.px.charge;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

    private EditText mTitle;
    private EditText mNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitle = (EditText) findViewById(R.id.activity_main__title);
        mNumber = (EditText) findViewById(R.id.activity_main__num);
        findViewById(R.id.activity_main__ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mTitle.getText().toString().trim())) {

                } else if (TextUtils.isEmpty(mNumber.getText().toString().trim())) {

                } else {
                    
                }
            }
        });
    }
}
