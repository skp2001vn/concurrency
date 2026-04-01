package org.example.inventory;

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
