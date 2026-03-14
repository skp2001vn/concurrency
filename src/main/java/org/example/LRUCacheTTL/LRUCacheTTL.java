package org.example.LRUCacheTTL;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-Safe LRU Cache with TTL (expiration)
 * @param <K>
 * @param <V>
 */
public class LRUCacheTTL<K, V> {

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

    public LRUCacheTTL(int capacity) {
        this.capacity = capacity;
    }

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