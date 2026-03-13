package org.example.LRUCacheTTL;

public class Main {

    public static void main(String[] args) throws Exception {

        LRUCacheTTL<Integer, String> cache = new LRUCacheTTL<>(2);

        cache.put(1, "A", 2000);
        cache.put(2, "B", 5000);
        System.out.println(cache.get(1)); // A

        Thread.sleep(3000);
        System.out.println(cache.get(1)); // null (expired)
    }
}