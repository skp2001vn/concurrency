package org.example.AdvancedJobQueue;

public class Main {

    public static void main(String[] args) {

        AdvancedJobQueue queue =
                new AdvancedJobQueue(3,3);

        queue.submit(new Job("job1", () -> {

            System.out.println("Running job1");

        },1,0));

        queue.submit(new Job("job2", () -> {

            throw new RuntimeException("failure");

        },1,0));

        queue.submit(new Job("job3", () -> {

            System.out.println("Running job3");

        },5,2000));
    }
}