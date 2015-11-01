package com.common.sys;

import java.util.ArrayList;

public abstract class TempVars<T> {
	private final ArrayList<T> mVars = new ArrayList<T>(10);
	private int mTop = 0;
	private int mSize = 0;
	
	public T acquire() {
		final T var;
		if (mTop >= mSize - 1) {
			mVars.add(newVar());
			++mSize;
		}
		var = (T) mVars.get(mTop++);
		return var;
	}
	public void release(T var) {
		int n = mSize - 1;
		for (; n >= 0 && mVars.get(n) != var; --n);
		
		assert n >= 0;
		if (n < 0)
			return;
		
		clearVar(var);
		if (n != mTop - 1) {
			final T tmp = mVars.get(n);
			mVars.set(n, mVars.get(mTop - 1));
			mVars.set(mTop - 1, tmp);
		}
		--mTop;
	}
	
	protected abstract T newVar();
	protected abstract void clearVar(T var);
}
