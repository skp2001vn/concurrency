package org.example.inventory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Business logic: tracks product stock and lets concurrent customers purchase
 * one unit without allowing the quantity to drop below zero.
 *
 * <p>Technique: stores per-product {@link AtomicInteger} counters in a
 * {@link ConcurrentHashMap} because each product's quantity can be updated
 * independently. A compare-and-set retry loop prevents overselling without
 * explicit locks and keeps successful purchases lightweight.
 */
public class AtomicInventoryService implements InventoryService {

    private final ConcurrentHashMap<String, AtomicInteger> inventory = new ConcurrentHashMap<>();

    /**
     * Creates an empty atomic inventory service.
     */
    public AtomicInventoryService() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addProduct(String productId, int quantity) {
        inventory.put(productId, new AtomicInteger(quantity));
    }

    /**
     * {@inheritDoc}
     */
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
