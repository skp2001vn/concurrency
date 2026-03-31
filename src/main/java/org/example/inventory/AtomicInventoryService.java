package org.example.inventory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread-safe inventory service that uses atomic counters and compare-and-set
 * operations to handle purchases without explicit locks.
 */
public class AtomicInventoryService implements InventoryService {

    private final ConcurrentHashMap<String, AtomicInteger> inventory = new ConcurrentHashMap<>();

    @Override
    public void addProduct(String productId, int quantity) {
        inventory.put(productId, new AtomicInteger(quantity));
    }

    @Override
    public boolean purchase(String productId) {
        AtomicInteger stock = inventory.get(productId);
        if (stock == null) {
            return false;
        }
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
