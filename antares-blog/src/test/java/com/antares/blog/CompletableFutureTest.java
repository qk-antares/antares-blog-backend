package com.antares.blog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;

@SpringBootTest
public class CompletableFutureTest {
    private ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(2,8,1000,
                    TimeUnit.SECONDS,
                    new LinkedBlockingDeque<>(100000),
                    Executors.defaultThreadFactory(),
                    new ThreadPoolExecutor.AbortPolicy());

    @Test
    public void test(){
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            CompletableFuture<Void> future11 = CompletableFuture.runAsync(() -> {
                System.out.println("1.1");
            }, threadPoolExecutor);
            CompletableFuture<Void> future12 = CompletableFuture.runAsync(() -> {
                System.out.println("1.2");
            }, threadPoolExecutor);
            CompletableFuture<Void> future13 = CompletableFuture.runAsync(() -> {
                System.out.println("1.3");
            }, threadPoolExecutor);
            CompletableFuture<Void> nestedFuture = CompletableFuture.allOf(future11, future12, future13)
                    .thenRun(() -> System.out.println("over1"));
            nestedFuture.join();
        }, threadPoolExecutor);

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            System.out.println("2");
        }, threadPoolExecutor);

        CompletableFuture<Void> future3 = CompletableFuture.runAsync(() -> {
            System.out.println("3");
        }, threadPoolExecutor);

        CompletableFuture.allOf(future1, future2, future3).join();
        System.out.println("over");
    }
}
