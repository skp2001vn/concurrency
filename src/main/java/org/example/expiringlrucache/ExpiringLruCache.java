package org.example.expiringlrucache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Business logic: provides a capacity-limited cache where entries expire by TTL
 * and the least recently used live entry is evicted when capacity is exceeded.
 *
 * <p>Technique: combines a hash map for O(1)-style lookup with a doubly linked
 * list for recency tracking because eviction needs both fast access and ordered
 * removal. A single {@link ReentrantLock} keeps the map and list consistent
 * during compound updates.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class ExpiringLruCache<K, V> {

    private class Node {
        K key;
        V value;
        long expireAt;

        Node prev;
        Node next;

        Node(K key, V value, long expireAt) {
            this.key = key;
            this.value = value;
            this.expireAt = expireAt;
        }
    }

    private final int capacity;
    private final Map<K, Node> cache = new HashMap<>();

    private Node head;
    private Node tail;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates a cache with the given maximum number of entries.
     *
     * @param capacity the maximum number of live entries before LRU eviction occurs
     */
    public ExpiringLruCache(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Returns the value for a key, or {@code null} if the key is absent or expired.
     *
     * @param key the cache key
     * @return the cached value, or {@code null} if no valid entry exists
     */
    public V get(K key) {
        lock.lock();
        try {
            Node node = cache.get(key);
            if (node == null) {
                return null;
            }

            if (System.currentTimeMillis() > node.expireAt) {
                removeNode(node);
                cache.remove(key);
                return null;
            }

            moveToHead(node);
            return node.value;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stores or updates a value with the given time-to-live.
     *
     * @param key the cache key
     * @param value the value to store
     * @param ttlMillis the entry time-to-live in milliseconds
     */
    public void put(K key, V value, long ttlMillis) {
        lock.lock();
        try {
            Node node = cache.get(key);
            long expireAt = System.currentTimeMillis() + ttlMillis;
            if (node != null) {
                node.value = value;
                node.expireAt = expireAt;
                moveToHead(node);
                return;
            }

            Node newNode = new Node(key, value, expireAt);
            cache.put(key, newNode);
            addToHead(newNode);

            if (cache.size() > capacity) {
                Node removed = removeTail();
                cache.remove(removed.key);
            }
        } finally {
            lock.unlock();
        }
    }

    private void moveToHead(Node node) {
        removeNode(node);
        addToHead(node);
    }

    private void addToHead(Node node) {
        node.prev = null;
        node.next = head;

        if (head != null) {
            head.prev = node;
        }

        head = node;

        if (tail == null) {
            tail = node;
        }
    }

    private void removeNode(Node node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }

        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
    }

    private Node removeTail() {
        Node removed = tail;
        removeNode(tail);
        return removed;
    }
}
