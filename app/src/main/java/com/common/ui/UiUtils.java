package com.common.ui;

import java.util.Arrays;
import java.util.concurrent.Callable;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;

import com.common.sys.MainThread;
import com.common.sys.TempVars;

public abstract class UiUtils {
	public static final int ANIM_DURATION_SHORT = 200;
	public static final int ANIM_DURATION_NORMAL = 300;
	public static final int ANIM_DURATION_LONG = 500;
	public static final int ANIM_DURATION_LONG_LONG = 800;
	public static final int ANIM_DURATION_QUITE_LONG = 2000;
	public static final int OVER_SCROLL_WIDTH = 60; // dip
	public static final int OVER_SCROLL_HEIGHT = 60; // dip
	public static final TempVars<Camera> tempCameras = new TempVars<Camera>() {
		@Override
		protected Camera newVar() {
			final Camera var = new Camera();
			var.save();
			return var;
		}
		@Override
		protected void clearVar(Camera var) {
			var.restore();
			var.save();
		}
	};
	public static final TempVars<Paint> tempPaints = new TempVars<Paint>() {
		@Override
		protected Paint newVar() {
			return new Paint();
		}
		@Override
		protected void clearVar(Paint var) {
			var.reset();
		}
	};
	public static final TempVars<Transformation> tempTransformations = new TempVars<Transformation>() {
		@Override
		protected Transformation newVar() {
			return new Transformation();
		}
		@Override
		protected void clearVar(Transformation var) {
			var.clear();
		}
	};
	public static final TempVars<Matrix> tempMatrices = new TempVars<Matrix>() {
		@Override
		protected Matrix newVar() {
			return new Matrix();
		}
		@Override
		protected void clearVar(Matrix var) {
			var.reset();
		}
	};
	public static final TempVars<Point> tempPoints = new TempVars<Point>() {
		@Override
		protected Point newVar() {
			return new Point();
		}
		@Override
		protected void clearVar(Point var) {
			var.x = var.y = 0;
		}
	};
	public static final TempVars<PointF> tempPointFs = new TempVars<PointF>() {
		@Override
		protected PointF newVar() {
			return new PointF();
		}
		@Override
		protected void clearVar(PointF var) {
			var.x = var.y = 0.0f;
		}
	};
	public static final TempVars<Rect> tempRects = new TempVars<Rect>() {
		@Override
		protected Rect newVar() {
			return new Rect();
		}
		@Override
		protected void clearVar(Rect var) {
			var.setEmpty();
		}
	};
	public static final TempVars<RectF> tempRectFs = new TempVars<RectF>() {
		@Override
		protected RectF newVar() {
			return new RectF();
		}
		@Override
		protected void clearVar(RectF var) {
			var.setEmpty();
		}
	};
	public static final TempVars<float[]> temp2floats = new TempVars<float[]>() {
		@Override
		protected float[] newVar() {
			final float[] var = new float[2];
			Arrays.fill(var, 0.0f);
			return var;
		}
		@Override
		protected void clearVar(float[] var) {
			Arrays.fill(var, 0.0f);
		}
	};
	public static final TempVars<float[]> temp4floats = new TempVars<float[]>() {
		@Override
		protected float[] newVar() {
			final float[] var = new float[4];
			Arrays.fill(var, 0.0f);
			return var;
		}
		@Override
		protected void clearVar(float[] var) {
			Arrays.fill(var, 0.0f);
		}
	};
	public static final TempVars<float[]> temp9floats = new TempVars<float[]>() {
		@Override
		protected float[] newVar() {
			final float[] var = new float[9];
			Arrays.fill(var, 0.0f);
			return var;
		}
		@Override
		protected void clearVar(float[] var) {
			Arrays.fill(var, 0.0f);
		}
	};
	public static final TempVars<int[]> temp2ints = new TempVars<int[]>() {
		@Override
		protected int[] newVar() {
			final int[] var = new int[2];
			Arrays.fill(var, 0);
			return var;
		}
		@Override
		protected void clearVar(int[] var) {
			Arrays.fill(var, 0);
		}
	};
	private static float mDpiX = Float.NaN;
	private static float mDpiY = Float.NaN;

	// ### 消息处理相关方法 ###
	public static final void callPreDraw(final View view, final Callable<Boolean> callable) {
		if (view == null || callable == null)
			return;

		new ViewPreDrawObserver(view) {
			@Override
			protected boolean onViewPreDraw(View view) {
				try {
					return callable.call();
					
				} catch (Exception e) {

					return true;
				}
			}
		};
	}
	public static final void runPreDraw(final View view, final Runnable runnable) {
		if (view == null || runnable == null)
			return;

		callPreDraw(view, new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				runnable.run();
				return true;
			}
		});
	}
	public static final void runAfterLayout(final View view, final Runnable runnable) {
		if (view == null || runnable == null)
			return;

		callPreDraw(view, new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				runnable.run();
				return false;
			}
		});
	}


	// ### 视图坐标变换相关方法 ###
	public static Rect getViewBounds(Rect bounds, View view, View relativeToView) {
		final RectF boundsF = tempRectFs.acquire();
		getViewBounds(boundsF, view, relativeToView);
		boundsF.round(bounds);
		tempRectFs.release(boundsF);
		return bounds;
	}
	public static RectF getViewBounds(RectF boundsF, View view, View relativeToView) {
		boundsF.set(view.getScrollX(), view.getScrollY(), view.getScrollX() + view.getWidth(), view.getScrollY() + view.getHeight());
		transformRect(boundsF, view, relativeToView);
		return boundsF;
	}
	public static boolean getVisibleBounds(RectF visibleBounds, View view) {
		final RectF r = tempRectFs.acquire();
		
		visibleBounds.set(view.getScrollX(), view.getScrollY(), view.getScrollX() + view.getWidth(), view.getScrollY() + view.getHeight());
		for (ViewParent parent = view.getParent(); parent != null; parent = parent.getParent()) {
			if (parent instanceof View == false)
				break;
			
			final View parentView = (View) parent;
			r.set(parentView.getScrollX(), parentView.getScrollY(), parentView.getScrollX() + parentView.getWidth(), parentView.getScrollY() + parentView.getHeight());
			transformRect(r, parentView, view);
			
			if (visibleBounds.intersect(r) == false) {
				visibleBounds.setEmpty();
				break;
			}
		}
		
		tempRectFs.release(r);
		return !visibleBounds.isEmpty();
	}
	public static MotionEvent obtainMotionEvent(MotionEvent ev, View fromView, View toView) {
		final Matrix m = tempMatrices.acquire();
		transformMatrix(m, fromView, toView);
		m.preTranslate(fromView.getScrollX(), fromView.getScrollY()); // 转换到fromView的内容坐标
		m.postTranslate(-toView.getScrollX(), -toView.getScrollY()); // 转换到toView的视口坐标
		
		final MotionEvent newEvent = obtainMotionEvent(ev, m);
		tempMatrices.release(m);
		return newEvent;
	}
	@SuppressWarnings("deprecation")
	public static MotionEvent obtainMotionEvent(MotionEvent ev, Matrix m) {
		final long downTime = ev.getDownTime();
		final long eventTime = ev.getEventTime();
		final int action = ev.getAction();
		final int pointerCount = ev.getPointerCount();
		final int pointerIds[] = new int[pointerCount];
		final PointerCoords pointerCoords[] = new PointerCoords[pointerCount];
		final int metaState = ev.getMetaState();
		final float xPrecision = ev.getXPrecision();
		final float yPrecision = ev.getYPrecision();
		final int deviceId = ev.getDeviceId();
		final int edgeFlags = ev.getEdgeFlags();
		final int source = ev.getSource();
		final int flags = ev.getFlags();
		
		final PointF point = tempPointFs.acquire(); 
		for (int n = 0; n < pointerCount; ++n) {
			pointerIds[n] = ev.getPointerId(n);
			pointerCoords[n] = new PointerCoords();
			ev.getPointerCoords(n, pointerCoords[n]);
			point.set(pointerCoords[n].x, pointerCoords[n].y);
			premultiplyPoint(m, point);
			pointerCoords[n].x = point.x;
			pointerCoords[n].y = point.y;
		}
		tempPointFs.release(point);
		
		return MotionEvent.obtain(downTime, eventTime, action, pointerCount, pointerIds, pointerCoords, metaState, xPrecision, yPrecision, deviceId, edgeFlags, source, flags);
	}
	public static Rect[] transformRects(Rect rects[], View fromView, View toView) {
		for (Rect rect : rects) {
			transformRect(rect, fromView, toView);
		}
		return rects;
	}
	public static Rect transformRect(Rect rect, View fromView, View toView) {
		final RectF rectF = tempRectFs.acquire();
		rectF.set(rect);
		transformRect(rectF, fromView, toView);
		rectF.round(rect);
		tempRectFs.release(rectF);
		return rect;
	}
	public static Point transformOffset(Point offset, View fromView, View toView) {
		final PointF offsetF = tempPointFs.acquire();
		offsetF.set(offset.x, offset.y);
		transformOffset(offsetF, fromView, toView);
		offset.x = Math.round(offsetF.x);
		offset.y = Math.round(offsetF.y);
		tempPointFs.release(offsetF);
		return offset;
	}
	public static Point transformPoint(Point point, View fromView, View toView) {
		final PointF pointF = tempPointFs.acquire();
		pointF.set(point.x, point.y);
		transformPoint(pointF, fromView, toView);
		point.x = Math.round(pointF.x);
		point.y = Math.round(pointF.y);
		tempPointFs.release(pointF);
		return point;
	}
	public static RectF transformRect(RectF rectF, View fromView, View toView) {
		final Matrix m = tempMatrices.acquire();
		final float[] tv = temp4floats.acquire();
		final float[] bv = temp4floats.acquire();
		
		transformMatrix(m, fromView, toView);
		
		tv[0] = rectF.left;
		tv[1] = rectF.top;
		tv[2] = rectF.right;
		tv[3] = rectF.top;
		bv[0] = rectF.left;
		bv[1] = rectF.bottom;
		bv[2] = rectF.right;
		bv[3] = rectF.bottom;
		
		m.mapPoints(tv);
		m.mapPoints(bv);
		
		rectF.left = Math.min(Math.min(tv[0], tv[2]), Math.min(bv[0], bv[2]));
		rectF.top = Math.min(Math.min(tv[1], tv[3]), Math.min(bv[1], bv[3]));
		rectF.right = Math.max(Math.max(tv[0], tv[2]), Math.max(bv[0], bv[2]));
		rectF.bottom = Math.max(Math.max(tv[1], tv[3]), Math.max(bv[1], bv[3]));
		
		tempMatrices.release(m);
		temp4floats.release(tv);
		temp4floats.release(bv);
		return rectF;
	}
	public static PointF transformOffset(PointF offsetF, View fromView, View toView) {
		final Matrix m = tempMatrices.acquire();
		final float[] v = temp2floats.acquire();
		
		transformMatrix(m, fromView, toView);
		v[0] = offsetF.x;
		v[1] = offsetF.y;
		m.mapPoints(v);
		offsetF.x = v[0];
		offsetF.y = v[1];

		v[0] = 0.0f;
		v[1] = 0.0f;
		m.mapPoints(v);
		
		offsetF.x -= v[0];
		offsetF.y -= v[1];

		tempMatrices.release(m);
		temp2floats.release(v);
		return offsetF;
	}
	public static PointF transformPoint(PointF pointF, View fromView, View toView) {
		final Matrix m = tempMatrices.acquire();
		final float[] v = temp2floats.acquire();
		
		transformMatrix(m, fromView, toView);
		v[0] = pointF.x;
		v[1] = pointF.y;
		m.mapPoints(v);
		pointF.x = v[0];
		pointF.y = v[1];
		
		tempMatrices.release(m);
		temp2floats.release(v);
		return pointF;
	}
	public static Rect transformRectFromScreen(Rect rect, View toView) {
		final RectF rectF = tempRectFs.acquire();
		
		rectF.set(rect);
		transformRectFromScreen(rectF, toView);
		rectF.round(rect);
		
		tempRectFs.release(rectF);
		return rect;
	}
	public static Point transformOffsetFromScreen(Point offset, View toView) {
		final PointF offsetF = tempPointFs.acquire();
		
		offsetF.x = offset.x;
		offsetF.y = offset.y;
		transformOffsetFromScreen(offsetF, toView);
		offset.x = Math.round(offsetF.x);
		offset.y = Math.round(offsetF.y);
		
		tempPointFs.release(offsetF);
		return offset;
	}
	public static Point transformPointFromScreen(Point point, View toView) {
		final PointF pointF = tempPointFs.acquire();
		
		pointF.x = point.x;
		pointF.y = point.y;
		transformPointFromScreen(pointF, toView);
		point.x = Math.round(pointF.x);
		point.y = Math.round(pointF.y);
		
		tempPointFs.release(pointF);
		return point;
	}
	public static RectF transformRectFromScreen(RectF rectF, View toView) {
		final Matrix m = tempMatrices.acquire();
		final Matrix w = tempMatrices.acquire();
		final float[] tv = temp4floats.acquire();
		final float[] bv = temp4floats.acquire();
		
		tv[0] = rectF.left;
		tv[1] = rectF.top;
		tv[2] = rectF.right;
		tv[3] = rectF.top;
		bv[0] = rectF.left;
		bv[1] = rectF.bottom;
		bv[2] = rectF.right;
		bv[3] = rectF.bottom;
		
		transformMatrixToScreen(m, toView);
		m.invert(w);
		w.mapPoints(tv);
		w.mapPoints(bv);
		
		rectF.left = Math.min(Math.min(tv[0], tv[2]), Math.min(bv[0], bv[2]));
		rectF.top = Math.min(Math.min(tv[1], tv[3]), Math.min(bv[1], bv[3]));
		rectF.right = Math.max(Math.max(tv[0], tv[2]), Math.max(bv[0], bv[2]));
		rectF.bottom = Math.max(Math.max(tv[1], tv[3]), Math.max(bv[1], bv[3]));
		
		tempMatrices.release(m);
		tempMatrices.release(w);
		temp4floats.release(tv);
		temp4floats.release(bv);
		return rectF;
	}
	public static PointF transformOffsetFromScreen(PointF offsetF, View toView) {
		final PointF originF = tempPointFs.acquire();
		transformPointFromScreen(offsetF, toView);
		transformPointFromScreen(originF, toView);
		offsetF.x -= originF.x;
		offsetF.y -= originF.y;
		tempPointFs.release(originF);
		return offsetF;
	}
	public static PointF transformPointFromScreen(PointF pointF, View toView) {
		final Matrix m = tempMatrices.acquire();
		final Matrix w = tempMatrices.acquire();
		final float[] v = temp2floats.acquire();
		
		transformMatrixToScreen(m, toView);
		m.invert(w);
		
		v[0] = pointF.x;
		v[1] = pointF.y;
		w.mapPoints(v);
		pointF.x = v[0];
		pointF.y = v[1];
		
		tempMatrices.release(m);
		tempMatrices.release(w);
		temp2floats.release(v);
		return pointF;
	}
	public static Rect transformRectToScreen(Rect rect, View fromView) {
		final RectF rectF = tempRectFs.acquire();
		
		rectF.set(rect);
		transformRectToScreen(rectF, fromView);
		rectF.round(rect);
		
		tempRectFs.release(rectF);
		return rect;
	}
	public static Point transformOffsetToScreen(Point offset, View fromView) {
		final PointF pointF = tempPointFs.acquire();
		
		pointF.x = offset.x;
		pointF.y = offset.y;
		transformOffsetToScreen(pointF, fromView);
		offset.x = Math.round(pointF.x);
		offset.y = Math.round(pointF.y);
		
		tempPointFs.release(pointF);
		return offset;
	}
	public static Point transformPointToScreen(Point point, View fromView) {
		final PointF pointF = tempPointFs.acquire();
		
		pointF.x = point.x;
		pointF.y = point.y;
		transformPointToScreen(pointF, fromView);
		point.x = Math.round(pointF.x);
		point.y = Math.round(pointF.y);
		
		tempPointFs.release(pointF);
		return point;
	}
	public static RectF transformRectToScreen(RectF rectF, View fromView) {
		final Matrix m = tempMatrices.acquire();
		final float[] tv = temp4floats.acquire();
		final float[] bv = temp4floats.acquire();
		
		tv[0] = rectF.left;
		tv[1] = rectF.top;
		tv[2] = rectF.right;
		tv[3] = rectF.top;
		bv[0] = rectF.left;
		bv[1] = rectF.bottom;
		bv[2] = rectF.right;
		bv[3] = rectF.bottom;
		
		transformMatrixToScreen(m, fromView);
		m.mapPoints(tv);
		m.mapPoints(bv);
		
		rectF.left = Math.min(Math.min(tv[0], tv[2]), Math.min(bv[0], bv[2]));
		rectF.top = Math.min(Math.min(tv[1], tv[3]), Math.min(bv[1], bv[3]));
		rectF.right = Math.max(Math.max(tv[0], tv[2]), Math.max(bv[0], bv[2]));
		rectF.bottom = Math.max(Math.max(tv[1], tv[3]), Math.max(bv[1], bv[3]));
		
		tempMatrices.release(m);
		temp4floats.release(tv);
		temp4floats.release(bv);
		return rectF;
	}
	public static PointF transformOffsetToScreen(PointF offsetF, View fromView) {
		final PointF originF = tempPointFs.acquire();
		transformPointToScreen(offsetF, fromView);
		transformPointToScreen(originF, fromView);
		offsetF.x -= originF.x;
		offsetF.y -= originF.y;
		tempPointFs.release(originF);
		return offsetF;
	}
	public static PointF transformPointToScreen(PointF pointF, View fromView) {
		final Matrix m = tempMatrices.acquire();
		final float[] v = temp2floats.acquire();
		
		transformMatrixToScreen(m, fromView);
		v[0] = pointF.x;
		v[1] = pointF.y;
		m.mapPoints(v);
		pointF.x = v[0];
		pointF.y = v[1];
		
		tempMatrices.release(m);
		temp2floats.release(v);
		return pointF;
	}
	public static Matrix transformMatrix(Matrix m, View fromView, View toView) {
		final Matrix m0 = tempMatrices.acquire();
		final Matrix m1 = tempMatrices.acquire();
		
		transformMatrixToScreen(m0, fromView);
		transformMatrixToScreen(m1, toView);
		m1.invert(m);
		m.preConcat(m0);

		
		tempMatrices.release(m0);
		tempMatrices.release(m1);
		return m;
	}
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static Matrix transformMatrixToScreen(Matrix m, View fromView) {
		if (fromView == null)
			return m;

		// EXPLAIN(by lizhan@xiaomi.com): 
		// 坐标变换规则:
		// parentCoords = parent content coords = childPosMatrix * childTransformMatrix * childViewMatrix * childScrollMatrix * childCoords
		// childCoords = child content coords
		// viewportCoords(onTouchEvent时默认使用的坐标) = scrollMatrix * contentCoords
		// contentCoords(onDraw时默认使用的坐标) = user defined coords
		//
		// 参见源码(View.class):
		//    void getBoundsOnScreen(Rect outRect) {
		//        if (mAttachInfo == null) {
		//            return;
		//        }
		//
		//        RectF position = mAttachInfo.mTmpTransformRect;
		//        position.set(0, 0, mRight - mLeft, mBottom - mTop); // 视口坐标系
		//
		//        if (!hasIdentityMatrix()) {
		//            getMatrix().mapRect(position);
		//        }
		//
		//        position.offset(mLeft, mTop); // 这一步后, position变换到了父视图坐标系.
		//
		//		  // 将坐标从父视图坐标系变换到根视图坐标系, 这里可以知道: parentCoords = posM * viewM * scrollM * childCoords.
		//        ViewParent parent = mParent;
		//        while (parent instanceof View) {
		//            View parentView = (View) parent;
		//
		//            position.offset(-parentView.mScrollX, -parentView.mScrollY);
		//
		//            if (!parentView.hasIdentityMatrix()) {
		//                parentView.getMatrix().mapRect(position);
		//            }
		//
		//            position.offset(parentView.mLeft, parentView.mTop);
		//
		//            parent = parentView.mParent;
		//        }
		//
		//        if (parent instanceof ViewRootImpl) {
		//            ViewRootImpl viewRootImpl = (ViewRootImpl) parent;
		//            position.offset(0, -viewRootImpl.mCurScrollY);
		//        }
		//
		//		  // 将坐标从根视图坐标系变换到窗口坐标系
		//        position.offset(mAttachInfo.mWindowLeft, mAttachInfo.mWindowTop);
		//
		//        outRect.set((int) (position.left + 0.5f), (int) (position.top + 0.5f),
		//                (int) (position.right + 0.5f), (int) (position.bottom + 0.5f));
		//    }
		
		while (true) {
			// childScrollMatrix * childCoords
			m.postTranslate(-fromView.getScrollX(), -fromView.getScrollY());
			
			// childViewMatrix * childScrollMatrix * childCoords
			if (Build.VERSION.SDK_INT >= 11) {
				m.postConcat(fromView.getMatrix()); 
			}

			final ViewParent parent = fromView.getParent();
			// childTransformMatrix * childViewMatrix * childScrollMatrix * childCoords
			if (parent instanceof ViewTransformer) {
				final ViewTransformer transformer = (ViewTransformer) parent;
				m.postConcat(transformer.getChildTransformMatrix(fromView));
			}

			// childPosMatrix * childTransformMatrix * childViewMatrix * childScrollMatrix * childCoords
			m.postTranslate(fromView.getLeft(), fromView.getTop());

			if (parent instanceof View == false)
				break;

			fromView = (View) parent;
		}
		
		final int[] v = temp2ints.acquire();
		v[0] = 0;
		v[1] = 0;
		fromView.getLocationOnScreen(v);
		m.postTranslate(v[0], v[1]);
		temp2ints.release(v);
		
		return m;
	}
	public static Point premultiplyOffset(Matrix m, Point offset) {
		final PointF p0 = tempPointFs.acquire();
		final PointF p1 = tempPointFs.acquire();
		
		p0.x = offset.x;
		p0.y = offset.y;
		p1.x = 0;
		p1.y = 0;
		
		premultiplyPoint(m, p0);
		premultiplyPoint(m, p1);
		
		offset.x = Math.round(p0.x - p1.x);
		offset.y = Math.round(p0.y - p1.y);
		
		tempPointFs.release(p0);
		tempPointFs.release(p1);
		return offset;
	}
	public static Point premultiplyPoint(Matrix m, Point point) {
		final PointF pointF = tempPointFs.acquire();
		
		pointF.x = point.x;
		pointF.y = point.y;
		premultiplyPoint(m, pointF);
		point.x = Math.round(pointF.x);
		point.y = Math.round(pointF.y);
		
		tempPointFs.release(pointF);
		return point;
	}
	public static Rect premultiplyRect(Matrix m, Rect rect) {
		final RectF rectF = tempRectFs.acquire();
		rectF.set(rect);
		premultiplyRect(m, rectF);
		rectF.round(rect);
		tempRectFs.release(rectF);
		return rect;
	}
	public static PointF premultiplyOffset(Matrix m, PointF offset) {
		final PointF p0 = tempPointFs.acquire();
		final PointF p1 = tempPointFs.acquire();
		
		p0.x = offset.x;
		p0.y = offset.y;
		p1.x = 0;
		p1.y = 0;
		
		premultiplyPoint(m, p0);
		premultiplyPoint(m, p1);
		
		offset.x = p0.x - p1.x;
		offset.y = p0.y - p1.y;
		
		tempPointFs.release(p0);
		tempPointFs.release(p1);
		return offset;
	}
	public static PointF premultiplyPoint(Matrix m, PointF pointF) {
		final float[] v = temp2floats.acquire();
		
		v[0] = pointF.x;
		v[1] = pointF.y;
		m.mapPoints(v);
		pointF.x = v[0];
		pointF.y = v[1];
		
		temp2floats.release(v);
		return pointF;
	}
	public static RectF premultiplyRect(Matrix m, RectF rectF) {
		final float[] tv = temp4floats.acquire();
		final float[] bv = temp4floats.acquire();
		
		tv[0] = rectF.left;
		tv[1] = rectF.top;
		tv[2] = rectF.right;
		tv[3] = rectF.top;
		bv[0] = rectF.left;
		bv[1] = rectF.bottom;
		bv[2] = rectF.right;
		bv[3] = rectF.bottom;
		
		m.mapPoints(tv);
		m.mapPoints(bv);
		
		rectF.left = Math.min(Math.min(tv[0], tv[2]), Math.min(bv[0], bv[2]));
		rectF.top = Math.min(Math.min(tv[1], tv[3]), Math.min(bv[1], bv[3]));
		rectF.right = Math.max(Math.max(tv[0], tv[2]), Math.max(bv[0], bv[2]));
		rectF.bottom = Math.max(Math.max(tv[1], tv[3]), Math.max(bv[1], bv[3]));
		
		temp4floats.release(tv);
		temp4floats.release(bv);
		return rectF;
	}

	// ### 常用计算方法 ###
	public static boolean isLineBetween(PointF p1, PointF p2, double minDegree, double maxDegree) {
		double angle = normalizeDegree(calcLineAngle(p1, p2), minDegree, minDegree + 360.0);
		return isLineBetween(angle, minDegree, maxDegree);
	}
	public static boolean isLineBetween(double angle, double minDegree, double maxDegree) {
		double na = normalizeDegree(angle, minDegree, minDegree + 360.0);
		if (Double.compare(na, minDegree) >= 0 && Double.compare(na, maxDegree) < 0)
			return true;
		
		na = normalizeDegree(angle + 180, minDegree, minDegree + 360.0);
		if (Double.compare(na, minDegree) >= 0 && Double.compare(na, maxDegree) < 0)
			return true;
		
		return false;
	}
	public static int compareDegree(double degree1, double degree2) {
		return Double.compare(normalizeDegree(degree1, 0, 360), normalizeDegree(degree2, 0, 360));
	}
	public static int normalizeDegree(int degree, int minDegree, int maxDegree) {
		return (int) normalizeDegree((double) degree, (double) minDegree, (double) maxDegree);
	}
	public static double normalizeDegree(double degree, double minDegree, double maxDegree) {
		assert minDegree < maxDegree;
		assert Double.compare(Math.abs(maxDegree - minDegree), 360) == 0;
		
		while (Double.compare(degree, minDegree) < 0 || Double.compare(degree, maxDegree) >= 0) {
			if (Double.compare(degree, minDegree) < 0)
				degree += 360.0;
			else
				degree -= 360.0;
		}
		
		return degree;
	}
	/**
	 * 计算视图坐标系下, 过(p1, p2)的直线与X轴正半轴逆时针方向的夹角(0-180).
	 * 
	 * @param p1 直线上一点.
	 * @param p2 直线上另一点.
	 * @return 角度夹角.
	 */
	public static double calcLineAngle(PointF p1, PointF p2) {
		double angle = calcAngle(p1, p2);
		if (Double.compare(angle, 180.0) > 0) {
			angle = angle - 180.0;
		}
		return angle;
	}
	/**
	 * 计算视图坐标系下, 向量(origin, point)与X轴正半轴逆时针方向的夹角(0-360).
	 * 
	 * @param origin 向量的原点.
	 * @param point  向量的另外一点.
	 * @return 角度夹角.
	 */
	public static double calcAngle(PointF origin, PointF point) {
		return Math.toDegrees(calcRadian(origin, point));
	}
	/**
	 * 计算视图坐标系下, 向量(origin, point)与X轴正半轴逆时针方向的夹角.
	 * 
	 * @param origin 向量的原点.
	 * @param point  向量的另外一点.
	 * @return 弧度夹角.
	 */
	public static double calcRadian(PointF origin, PointF point) {
		assert origin != null && point != null;
		
		// 变换为笛卡尔坐标
		PointF p0 = new PointF(origin.x, -origin.y);
		PointF p1 = new PointF(point.x, -point.y);
		
		if (p1.x == p0.x) {
	        if (p1.y > p0.y)
	            return Math.PI * 0.5;
	        else
	        	return Math.PI * 1.5;
		} else if (p1.y == p0.y) {
	        if (p1.x > p0.x)
	            return 0;
	        else
	        	return Math.PI;
		} else {
	        double atan = Math.atan((double) (p1.y - p0.y) / (p1.x - p0.x));
	        if ((p1.x < p0.x) && (p1.y > p0.y))
	            return atan + Math.PI;
	        else if ((p1.x < p0.x) && (p1.y < p0.y))
	            return atan + Math.PI;
	        else if ((p1.x > p0.x) && (p1.y < p0.y))
	            return atan + 2 * Math.PI;
	        else
	        	return atan;
	    }
	}
	public static double calcDistance(PointF p0, PointF p1) {
		return Math.sqrt(Math.pow(p0.x - p1.x, 2.0) + Math.pow(p0.y - p1.y, 2.0));
	}
	public static int getScaledMinFlingVelocity(Context context) {
		return ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
	}
	public static int getScaledMaxFlingVelocity(Context context) {
		return ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
	}
	public static int getLongPressTimeout() {
		return ViewConfiguration.getLongPressTimeout();
	}
	public static int getScaledTouchSlop(Context context) {
		return ViewConfiguration.get(context).getScaledTouchSlop();
	}
	public static int getScaledPagingTouchSlop(Context context) {
		return ViewConfiguration.get(context).getScaledPagingTouchSlop();
	}
	public static int getJumpTapTimeout() {
		return ViewConfiguration.getJumpTapTimeout();
	}
	public static int getTapTimeout() {
		return ViewConfiguration.getTapTimeout();
	}
	public static int getDoubleTapTimeout() {
		return ViewConfiguration.getDoubleTapTimeout();
	}
	public static int getPressedStateDuration() {
		return ViewConfiguration.getPressedStateDuration();
	}
	public static int getScaledOverScrollWidth(Context context) {
		return dip2px(context, OVER_SCROLL_WIDTH);
	}
	public static int getScaledOverScrollHeight(Context context) {
		return dip2px(context, OVER_SCROLL_HEIGHT);
	}
	public static int dip2px(Context context, float dip) { 
		final float density = context.getResources().getDisplayMetrics().density; 
		return Math.round(dip * density); 
	}
	public static float px2dip(Context context, int px) { 
		final float density = context.getResources().getDisplayMetrics().density; 
		return px / density; 
	}
	public static float dip2pxX(Context context, float dipX) {
		return dipX * getDpiX(context) / 160.0f;
	}
	public static float dip2pxY(Context context, float dipY) {
		return dipY * getDpiY(context) / 160.0f;
	}
	public static float px2dipX(Context context, float px) {
		return px * 160.0f / getDpiX(context);
	}
	public static float px2dipY(Context context, float px) {
		return px * 160.0f / getDpiY(context);
	}
	public static int roundDip2pxX(Context context, float dipX) {
		return Math.round(dip2pxX(context, dipX));
	}
	public static int roundDip2pxY(Context context, float dipY) {
		return Math.round(dip2pxY(context, dipY));
	}
	public static int roundPx2dipX(Context context, float px) {
		return Math.round(px2dipX(context, px));
	}
	public static int roundPx2dipY(Context context, float px) {
		return Math.round(px2dipY(context, px));
	}
	public static float getDpiX(Context context) {
		initDpiXy(context);
		return mDpiX;
	}
	public static float getDpiY(Context context) {
		initDpiXy(context);
		return mDpiY;
	}
	private static void initDpiXy(Context context) {
		if (Float.isNaN(mDpiX) == false && Float.isNaN(mDpiY) == false)
			return;
		
		final DisplayMetrics dm = context.getResources().getDisplayMetrics();	
		final int xyDpi = Math.round((float) Math.sqrt(dm.xdpi * dm.ydpi));

		final float densitySize = (float) Math.sqrt(Math.pow((float) dm.widthPixels / dm.densityDpi, 2) + Math.pow((float) dm.heightPixels / dm.densityDpi, 2));
		final float xySize = (float) Math.sqrt(Math.pow((float) dm.widthPixels / xyDpi, 2) + Math.pow((float) dm.heightPixels / xyDpi, 2));
		
		if (Float.compare(Math.abs(xySize / densitySize - 1.0f), 0.3f) <= 0) {
			mDpiX = dm.xdpi;
			mDpiY = dm.ydpi;
		} else {
			mDpiX = mDpiY = dm.densityDpi;
		}
	}
	public static int getScreenWidth(Context context) {
		WindowManager windowManager = (WindowManager) context.getSystemService("window");
		DisplayMetrics dm = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(dm);
		return dm.widthPixels;
	}
	public static int getScreenHeight(Context context) {
		WindowManager windowManager = (WindowManager) context.getSystemService("window");
		DisplayMetrics dm = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}
	
	// ### 绘制相关函数  ###
	public static boolean isLightColor(int color) {
		final float opacity = Color.alpha(color) / 255.0f;
		final float gray = (Color.red(color) * 0.299f + Color.green(color) * 0.578f + Color.blue(color) * 0.144f) * opacity;
		return Float.compare(gray, 255 * 0.75f) >= 0;
	}
	public static void drawDrawable(Canvas canvas, Drawable drawable, Rect dstRect, int gravity) {
		final int width;
		if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL) {
			width = dstRect.width();
		} else {
			width = drawable.getIntrinsicWidth();
		}

		final int height;
		if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL) {
			height = dstRect.height();
		} else {
			height = drawable.getIntrinsicHeight();
		}
		
		final int x;
		if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.LEFT) {
			x = dstRect.left;
		} else if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.RIGHT) {
			x = dstRect.right - width;
		} else {
			x = (dstRect.left + dstRect.right - width + 1) / 2;
		}
		
		final int y;
		if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.TOP) {
			y = dstRect.top;
		} else if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
			y = dstRect.bottom - height;
		} else {
			y = (dstRect.top + dstRect.bottom - height + 1) / 2;
		}

		drawable.setBounds(x, y, x + width, y + height);
		drawable.draw(canvas);
	}
	
	// ### 动画相关函数 ###
	public static void fadeViewIn(View view, final Runnable onEnd) {
		fadeView(view, 0.0f, 1.0f, ANIM_DURATION_SHORT, false, onEnd);
	}
	public static void fadeViewOut(View view, final Runnable onEnd) {
		fadeView(view, 1.0f, 0.0f, ANIM_DURATION_SHORT, false, onEnd);
	}
	public static void fadeView(View view, float fromAlpha, float toAlpha, int duration, boolean fillAfter, final Runnable onEnd) {
		fadeView(view, fromAlpha, toAlpha, duration, fillAfter, new AccelerateDecelerateInterpolator(), onEnd);
	}
	public static void fadeView(View view, float fromAlpha, float toAlpha, int duration, boolean fillAfter, Interpolator interplator, final Runnable onEnd) {
		final AlphaAnimation anim = new AlphaAnimation(fromAlpha, toAlpha);
		anim.setDuration(duration);
		anim.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				
			}
			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}
			@Override
			public void onAnimationEnd(Animation animation) {
				MainThread.runLater(onEnd);
			}
		});
		anim.setInterpolator(interplator);
		anim.setFillEnabled(fillAfter);
		anim.setFillAfter(fillAfter);
		view.startAnimation(anim);
	}
	public static void flyViewInFromLeft(View view, final Runnable onEnd) {
		flyView(view, -1.0f, 0.0f, 0.0f, 0.0f, ANIM_DURATION_SHORT, false, onEnd);
	}
	public static void flyViewInFromRight(View view, final Runnable onEnd) {
		flyView(view, 1.0f, 0.0f, 0.0f, 0.0f, ANIM_DURATION_SHORT, false, onEnd);
	}
	public static void flyViewInFromTop(View view, final Runnable onEnd) {
		flyView(view, 0.0f, 0.0f, -1.0f, 0.0f, ANIM_DURATION_SHORT, false, onEnd);
	}
	public static void flyViewInFromBottom(View view, final Runnable onEnd) {
		flyView(view, 0.0f, 0.0f, 1.0f, 0.0f, ANIM_DURATION_SHORT, false, onEnd);
	}
	public static void flyViewOutToLeft(View view, final Runnable onEnd) {
		flyView(view, 0.0f, -1.0f, 0.0f, 0.0f, ANIM_DURATION_SHORT, false, onEnd);
	}
	public static void flyViewOutToRight(View view, final Runnable onEnd) {
		flyView(view, 0.0f, 1.0f, 0.0f, 0.0f, ANIM_DURATION_SHORT, false, onEnd);
	}
	public static void flyViewOutToTop(View view, final Runnable onEnd) {
		flyView(view, 0.0f, 0.0f, 0.0f, -1.0f, ANIM_DURATION_SHORT, false, onEnd);
	}
	public static void flyViewOutToBottom(View view, final Runnable onEnd) {
		flyView(view, 0.0f, 0.0f, 0.0f, 1.0f, ANIM_DURATION_SHORT, false, onEnd);
	}
	public static void flyView(View view, float fromX, float toX, float fromY, float toY, int duration, boolean fillAfter, final Runnable onEnd) {
		final TranslateAnimation anim = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, fromX, Animation.RELATIVE_TO_SELF, toX, 
				Animation.RELATIVE_TO_SELF, fromY, Animation.RELATIVE_TO_SELF, toY
		);
		anim.setDuration(duration);
		anim.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				
			}
			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}
			@Override
			public void onAnimationEnd(Animation animation) {
				MainThread.runLater(onEnd);
			}
		});
		anim.setInterpolator(new AccelerateDecelerateInterpolator());
		anim.setFillEnabled(fillAfter);
		anim.setFillAfter(fillAfter);
		view.startAnimation(anim);		
	}
	
	// ### 内嵌类 ###
	private static class ViewPreDrawObserver implements ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
		private final View mView;
		
		public ViewPreDrawObserver(View view) {
			mView = view;
			
			if (mView.getWindowToken() != null) {
				mView.getViewTreeObserver().addOnPreDrawListener(this);
			}
			mView.addOnAttachStateChangeListener(this);
		}
		
		@Override
		public final boolean onPreDraw() {
			mView.getViewTreeObserver().removeOnPreDrawListener(this);
			mView.removeOnAttachStateChangeListener(this);

			return onViewPreDraw(mView);
		}
		@Override
		public final void onViewAttachedToWindow(View v) {
			mView.getViewTreeObserver().addOnPreDrawListener(this);
		}
		@Override
		public final void onViewDetachedFromWindow(View v) {
			mView.getViewTreeObserver().removeOnPreDrawListener(this);
			mView.removeOnAttachStateChangeListener(this);
		}
		
		protected boolean onViewPreDraw(View view) {
			return false;
		}
	};
}
