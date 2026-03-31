package org.example.Inventory;

public interface InventoryService {

    void addProduct(String productId, int quantity);

    boolean purchase(String productId);
}
