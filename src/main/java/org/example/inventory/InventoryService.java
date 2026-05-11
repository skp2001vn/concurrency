package org.example.inventory;

/**
 * Business logic: defines stock operations for product inventory examples where
 * concurrent customers may add stock or purchase one unit.
 *
 * <p>Technique: implementations demonstrate per-product locking and atomic
 * compare-and-set counters because stock updates are compound operations. These
 * strategies prevent overselling while showing the tradeoff between explicit
 * locks and lock-free retry loops.
 */
public interface InventoryService {

    /**
     * Adds or replaces the stock quantity for a product.
     *
     * @param productId the product identifier
     * @param quantity the available quantity
     */
    void addProduct(String productId, int quantity);

    /**
     * Attempts to purchase one unit of the given product.
     *
     * @param productId the product identifier
     * @return {@code true} if stock was decremented, otherwise {@code false}
     */
    boolean purchase(String productId);
}
