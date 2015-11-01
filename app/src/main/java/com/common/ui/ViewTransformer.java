package com.common.ui;

import android.graphics.Matrix;
import android.view.View;

public interface ViewTransformer {
	Matrix getChildTransformMatrix(View child);
}
