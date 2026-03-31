package org.example.Inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class LockBasedInventoryServiceTest {

    @Test
    void returnsFalseForUnknownProduct() {
        LockBasedInventoryService inventory = new LockBasedInventoryService();

        assertFalse(inventory.purchase("missing"));
    }

    @Test
    void allowsPurchasesUpToAvailableStock() {
        LockBasedInventoryService inventory = new LockBasedInventoryService();
        inventory.addProduct("p1", 2);

        assertTrue(inventory.purchase("p1"));
        assertTrue(inventory.purchase("p1"));
        assertFalse(inventory.purchase("p1"));
    }

    @Test
    void concurrentPurchasesDoNotExceedAvailableStock() throws Exception {
        LockBasedInventoryService inventory = new LockBasedInventoryService();

        int stock = 10;
        int attempts = 30;
        inventory.addProduct("p1", stock);

        int successes = runConcurrentPurchases(inventory, "p1", attempts);

        assertEquals(stock, successes);
        assertFalse(inventory.purchase("p1"));
    }

    private int runConcurrentPurchases(InventoryService inventory, String productId, int attempts)
            throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                tasks.add(() -> inventory.purchase(productId));
            }

            int successes = 0;
            List<Future<Boolean>> futures = executor.invokeAll(tasks);
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    successes++;
                }
            }
            return successes;
        } finally {
            executor.shutdownNow();
        }
    }
}
