package org.example.ExpiringCache;

public class Main {

    public static void main(String[] args) throws Exception {

        ExpiringCache<String, String> cache = new ExpiringCache<>();

        cache.put("user1", "Alice", 2000);
        System.out.println(cache.get("user1"));

        Thread.sleep(3000);
        System.out.println(cache.get("user1"));
    }
}