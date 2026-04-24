package org.qiyu.live.msg.provider.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager {

    public static final ThreadPoolExecutor COMMON_ASYNC_POOL = new ThreadPoolExecutor(
            2,
            8,
            3,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1000),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable);
                    thread.setName("commonAsyncPool-" + ThreadLocalRandom.current().nextInt(10000));
                    return thread;
                }
            });

    private ThreadPoolManager() {
    }
}
