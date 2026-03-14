package org.example.Inventory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InventoryAtomicService {

    private ConcurrentHashMap<String, AtomicInteger> inventory = new ConcurrentHashMap<>();

    public void addProduct(String productId, int quantity) {
        inventory.put(productId, new AtomicInteger(quantity));
    }

    public boolean purchase(String productId) {
        AtomicInteger stock = inventory.get(productId);
        while (true) {
            int current = stock.get();
            if (current <= 0) {
                return false;
            }
            if (stock.compareAndSet(current, current - 1)) {
                return true;
            }
        }
    }
}