package com.common.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.px.charge.R;

/**
 * Created by pangpengde on 15/10/30.
 */
public class FloatHintEditText extends EditText {
    private Paint mHintPaint;
    private int mFloatingHintColor;

    // ### 构造函数 ###
    public FloatHintEditText(Context context) {
        this(context, null);
    }
    public FloatHintEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FloatingHintEditText, 0, 0);
        try {
            mFloatingHintColor = array.getColor(R.styleable.FloatingHintEditText_floatingHintColor,
                    context.getResources().getColor(android.R.color.holo_blue_dark));
        } finally {
            array.recycle();
        }

        initFloatingHint();
    }

    // ### 重载 ###
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText(getHint().toString(), getPaddingLeft(), 0, mHintPaint);
    }

    // ### 实现函数 ###
    private void initFloatingHint() {
        if (getPaddingTop() < 16) {
            setPadding(getPaddingLeft(), 16, getPaddingRight(), getPaddingBottom());
        }
        mHintPaint = new Paint();
        mHintPaint.setAntiAlias(true);
        mHintPaint.setTextSize(10 * getResources().getDisplayMetrics().density);
        mHintPaint.setColor(Color.TRANSPARENT);

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s.length() == 0) {
                    mHintPaint.setColor(mFloatingHintColor);
                    invalidate();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    mHintPaint.setColor(Color.TRANSPARENT);
                    invalidate();
                }
            }
        });
    }
}
