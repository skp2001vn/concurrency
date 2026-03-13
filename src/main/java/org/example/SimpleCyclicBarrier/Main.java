package org.example.SimpleCyclicBarrier;

public class Main {

    public static void main(String[] args) {

        SimpleCyclicBarrier barrier = new SimpleCyclicBarrier(3);

        Runnable task = () -> {
            try {
                System.out.println(Thread.currentThread().getName() + " reached barrier");

                barrier.await();

                System.out.println(Thread.currentThread().getName() + " passed barrier");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        for (int i = 0; i < 3; i++) {
            new Thread(task).start();
        }
    }
}