package org.example.TaskScheduler;

public class Main {

    public static void main(String[] args) {

        TaskScheduler scheduler = new TaskScheduler();

        scheduler.schedule(() -> System.out.println("Task 1 executed"), 2000);
        scheduler.schedule(() -> System.out.println("Task 2 executed"), 1000);
        scheduler.schedule(() -> System.out.println("Task 3 executed"), 3000);
    }
}