package org.example.AsyncJobQueue;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.

        AsyncJobQueue queue =
                new AsyncJobQueue(3, 2);

        queue.submit(new Job("job1", () -> {System.out.println("Running job1");}));

        queue.submit(new Job("job2", () -> {throw new RuntimeException("failure");}));

        queue.submit(new Job("job3", () -> {System.out.println("Running job3");}));
    }
}