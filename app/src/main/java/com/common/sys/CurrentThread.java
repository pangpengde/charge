package com.common.sys;

import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;


public abstract class CurrentThread {
	public static final Thread get() {
		return Thread.currentThread();
	}

	public static final void runOnIdle(final IdleRunnable runnable, final int timeout) {
		if (runnable == null)
			return;
		
		final Runnable doRun = new Runnable() {
			private boolean mDone = false;
			
			@Override
			public void run() {
				if (mDone)
					return;
				mDone = true;
				
				if (runnable.idleRun()) {
					runOnIdle(runnable, timeout);
				}
			}
		};
		
		runOnIdle(new IdleRunnable() {
			@Override
			public boolean idleRun() {
				doRun.run();
				return false;
			}
		});
		new Handler().postDelayed(doRun, timeout);
	}
	public static final void runOnIdle(final IdleRunnable runnable) {
		if (runnable == null)
			return;
		
		new Handler().post(new Runnable() {
			@Override
			public void run() {
				Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
					@Override
					public boolean queueIdle() {
						return runnable.idleRun();
					}
				});
			}
		});
	}
	public static final void run(Runnable runnable) {
		if (runnable == null)
			return;
		
		runnable.run();
	}
	public static final void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			
		}
	}
}
