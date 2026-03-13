package org.example.Memoizer;

public class Main {

    public static void main(String[] args) throws Exception {

        Memoizer<String, String> memoizer = new Memoizer<>();
        Runnable task = () -> {
            try {
                String result = memoizer.compute("A", () -> {
                    Thread.sleep(2000);
                    return "computed result";
                });

                System.out.println(Thread.currentThread().getName()
                        + " -> " + result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        new Thread(task).start();
        new Thread(task).start();
        new Thread(task).start();
    }
}