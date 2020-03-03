package org.apache.flink.client.program.rest.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorUtils {
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void submitTask(Callable task) {
        executor.submit(task);
    }

    public static void submitTask(Runnable task) {
        executor.submit(task);
    }
}
