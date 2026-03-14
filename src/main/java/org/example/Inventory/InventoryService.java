package org.example.Inventory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class InventoryService {

    private final ConcurrentHashMap<String, Integer> inventory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void addProduct(String productId, int quantity) {
        inventory.put(productId, quantity);
        locks.put(productId, new ReentrantLock());
    }

    public boolean purchase(String productId) {
        ReentrantLock lock = locks.get(productId);

        lock.lock();
        try {
            int stock = inventory.get(productId);

            if (stock <= 0) {
                return false;
            }

            inventory.put(productId, stock - 1);
            return true;
        } finally {
            lock.unlock();
        }
    }
}