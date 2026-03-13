package org.example.ConnectionPool;

public class Main {

    public static void main(String[] args) {

        ConnectionPool pool = new ConnectionPool(2, 5);

        Runnable task = () -> {

            try {

                Connection c = pool.acquire(3000);

                if (c == null) {
                    System.out.println("Timeout waiting for connection");
                    return;
                }

                System.out.println(
                        Thread.currentThread().getName()
                                + " got connection "
                                + c.getId());

                Thread.sleep(2000);

                pool.release(c);

            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        for (int i = 0; i < 6; i++) {
            new Thread(task).start();
        }
    }
}