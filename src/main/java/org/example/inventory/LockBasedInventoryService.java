package org.example.inventory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe inventory service that uses a separate lock per product to
 * synchronize purchases while allowing unrelated products to be updated in
 * parallel.
 */
public class LockBasedInventoryService implements InventoryService {

    private final ConcurrentHashMap<String, Integer> inventory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void addProduct(String productId, int quantity) {
        inventory.put(productId, quantity);
        //can use one global lock, but using one lock per productId (fine-grained locking) allows much better parallelism.
        locks.put(productId, new ReentrantLock());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean purchase(String productId) {
        ReentrantLock lock = locks.get(productId);
        if (lock == null) {
            return false;
        }

        lock.lock();
        try {
            Integer stock = inventory.get(productId);

            if (stock == null || stock <= 0) {
                return false;
            }

            inventory.put(productId, stock - 1);
            return true;
        } finally {
            lock.unlock();
        }
    }
}
