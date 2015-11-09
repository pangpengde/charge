package com.common.sys;

import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class PooledThread {
	private static final ScheduledExecutorService sSingleThreadedExecutor = Executors.newSingleThreadScheduledExecutor();
	private static final ExecutorService sMultiThreadedExecutor = Executors.newCachedThreadPool();
	private static final LinkedList<QueuedTask<?>> sTaskQueue = new LinkedList<QueuedTask<?>>();
	
	// ### 方法 ###
	public static Future<?> runInQueue(Runnable runnable) {
		return runInQueue(runnable, "");
	}
	public static Future<?> runInQueue(Runnable runnable, final String queueName) {
		final QueuedTask<Void> task = new QueuedTask<Void>(queueName, runnable);
		
		sSingleThreadedExecutor.submit(new Runnable() {
			@Override
			public void run() {
				sTaskQueue.add(task);
				runNextQueuedTask(queueName);
			}
		});
	
		return task;
	}
	public static ScheduledFuture<?> runInQueueLater(Runnable runnable, final long delayMills) {
		return runInQueueLater(runnable, "", delayMills);
	}
	public static ScheduledFuture<?> runInQueueLater(Runnable runnable, final String queueName, final long delayMillis) {
		final ScheduledQueuedTask<Void> task = new ScheduledQueuedTask<Void>(queueName, runnable, System.currentTimeMillis() + delayMillis);
		
		sSingleThreadedExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				sTaskQueue.add(task);
				runNextQueuedTask(queueName);
			}
		}, delayMillis, TimeUnit.MICROSECONDS);
	
		return task;
	}
	public static Future<?> run(Runnable runnable) {
		return sMultiThreadedExecutor.submit(runnable);
	}
	public static ScheduledFuture<?> runLater(Runnable runnable, long delayMillis) {
		final ScheduledUnqueuedTask<Void> task = new ScheduledUnqueuedTask<Void>(runnable, System.currentTimeMillis() + delayMillis);
		
		sSingleThreadedExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				sMultiThreadedExecutor.submit(task);
			}
		}, delayMillis, TimeUnit.MICROSECONDS);
	
		return task;
	}
	
	public static <T> Future<?> callInQueue(Callable<T> callable) {
		return callInQueue(callable, "");
	}
	public static <T> Future<T> callInQueue(Callable<T> callable, final String queueName) {
		final QueuedTask<T> task = new QueuedTask<T>(queueName, callable);
		
		sSingleThreadedExecutor.submit(new Runnable() {
			@Override
			public void run() {
				sTaskQueue.add(task);
				runNextQueuedTask(queueName);
			}
		});
	
		return task;
	}
	public static <T> ScheduledFuture<T> callInQueueLater(Callable<T> callable, final long delayMills) {
		return callInQueueLater(callable, "", delayMills);
	}
	public static <T> ScheduledFuture<T> callInQueueLater(Callable<T> callable, final String queueName, final long delayMillis) {
		final ScheduledQueuedTask<T> task = new ScheduledQueuedTask<T>(queueName, callable, System.currentTimeMillis() + delayMillis);
		
		sSingleThreadedExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				sTaskQueue.add(task);
				runNextQueuedTask(queueName);
			}
		}, delayMillis, TimeUnit.MICROSECONDS);
	
		return task;
	}
	public static <T> Future<T> call(Callable<T> callable) {
		return sMultiThreadedExecutor.submit(callable);
	}
	public static <T> ScheduledFuture<T> callLater(Callable<T> callable, long delayMillis) {
		final ScheduledUnqueuedTask<T> task = new ScheduledUnqueuedTask<T>(callable, System.currentTimeMillis() + delayMillis);
		
		sSingleThreadedExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				sMultiThreadedExecutor.submit(task);
			}
		}, delayMillis, TimeUnit.MICROSECONDS);
	
		return task;
	}
	
	// ### 实现函数 ###
	private static void runNextQueuedTask(String queueName) {
		for (QueuedTask<?> each : sTaskQueue) {
			if (each.queueName.equals(queueName)) {
				// 保证相同队列中的任务按顺序依次执行
				if (each.running == false) {
					each.running = true;
					sMultiThreadedExecutor.submit(each);
				}
				break;
			}
		}
	}
	
	// ### 实现类 ###
	private static class QueuedTask<T> extends FutureTask<T> {
		public final String queueName;
		private boolean running = false;
		
		public QueuedTask(String queueName, Runnable runnable) {
			super(runnable, null);
			
			this.queueName = queueName;
		}
		public QueuedTask(String queueName, Callable<T> callable) {
			super(callable);
			
			this.queueName = queueName;
		}

		@Override
		public void run() {
			try {
				super.run();
				
			} finally {
				sSingleThreadedExecutor.submit(new Runnable() {
					@Override
					public void run() {
						sTaskQueue.remove(QueuedTask.this);
						running = false;
						
						runNextQueuedTask(queueName);
					}
				});
			}
		}
	}
	private static class ScheduledQueuedTask<T> extends QueuedTask<T> implements ScheduledFuture<T> {
		private final long mScheduledTime;
		
		public ScheduledQueuedTask(String queueName, Runnable runnable, long scheduledTime) {
			super(queueName, runnable);
			
			mScheduledTime = scheduledTime;
		}
		public ScheduledQueuedTask(String queueName, Callable<T> callable, long scheduledTime) {
			super(queueName, callable);
			
			mScheduledTime = scheduledTime;
		}
		
		@Override
		public long getDelay(TimeUnit unit) {
			final long delay = Math.max(0, mScheduledTime - System.currentTimeMillis());
			return unit.convert(delay, TimeUnit.MILLISECONDS);
		}
		@Override
		public int compareTo(Delayed another) {
			final long diff = getDelay(TimeUnit.MILLISECONDS) - another.getDelay(TimeUnit.MILLISECONDS);
			return diff == 0 ? 0 : diff > 0 ? 1 : -1;
		}
	}
	private static class ScheduledUnqueuedTask<T> extends FutureTask<T> implements ScheduledFuture<T> {
		private final long mScheduledTime;
		
		public ScheduledUnqueuedTask(Runnable runnable, long scheduledTime) {
			super(runnable, null);
			
			mScheduledTime = scheduledTime;
		}
		public ScheduledUnqueuedTask(Callable<T> callable, long scheduledTime) {
			super(callable);
			
			mScheduledTime = scheduledTime;
		}
		
		@Override
		public long getDelay(TimeUnit unit) {
			final long delay = Math.max(0, mScheduledTime - System.currentTimeMillis());
			return unit.convert(delay, TimeUnit.MILLISECONDS);
		}
		@Override
		public int compareTo(Delayed another) {
			final long diff = getDelay(TimeUnit.MILLISECONDS) - another.getDelay(TimeUnit.MILLISECONDS);
			return diff == 0 ? 0 : diff > 0 ? 1 : -1;
		}
	}

}
