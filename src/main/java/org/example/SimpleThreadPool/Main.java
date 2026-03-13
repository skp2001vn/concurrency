package org.example.SimpleThreadPool;

public class Main {

    public static void main(String[] args) {

        SimpleThreadPool pool = new SimpleThreadPool(3);
        for (int i = 0; i < 10; i++) {
            int taskId = i;
            pool.submit(() -> {
                System.out.println("Task " + taskId + " executed by " + Thread.currentThread().getName());
            });
        }
    }
}