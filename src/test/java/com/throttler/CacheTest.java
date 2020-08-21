package com.throttler;

import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CacheTest {
    @Test
    public void testCache(){
        final int cacheSize = 3;
        LinkedHashMap<String, Integer> cache = new LinkedHashMap<>(cacheSize, 1, true){
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > cacheSize;
            }
        };

        for(int i = 0; i < 5; i++){
            cache.put("" + (char) (i + 65), i);
            cache.get("A");
            System.out.println(cache);
        }

    }

    long startTime = System.nanoTime();
    long t(){
        return (System.nanoTime() - startTime)/1000000;
    }
    void log(String s){
        System.out.println("[" + t() + "] " + s);
    }

    @Test
    public void testFutures() throws InterruptedException {
        List<Integer> ints = List.of(1, 2, 3, 4, 5);
        startTime = System.nanoTime();
        ints.stream().forEach(i ->
            CompletableFuture.supplyAsync(() -> {
                log("Before " + i);
                try {
                    Thread.sleep(i*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log("After " + i);
                return i;
            })
        );
        log("Before main");
        Thread.sleep(8000);
        log("After main");
    }

    int x = 0;

    void proc(){
        try{
            return;
        } finally {
            log("In final");
            x = 1;
        }
    }

    @Test
    public void testFuture() throws InterruptedException, ExecutionException {
        Future<Void> f = CompletableFuture.supplyAsync(() -> {
            log("Raising ");
            throw new NullPointerException();
        }).thenAccept(sla -> log("f: " + sla));
        Thread.sleep(1000);
//        log("f: " + f.get());
    }
}
