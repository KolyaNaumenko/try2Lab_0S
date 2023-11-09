import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        // Start the FunctionManager (server) in a separate thread
        new Thread(() -> FunctionManager.main(null)).start();

        try {
            // Sleep for a moment to ensure the server is ready
            Thread.sleep(10000);

            // Create a FunctionCalculator (client) for function f with x = 5
            Thread fThread = new Thread(() -> {
                FunctionCalculator functionF = new FunctionCalculator("f", 5);
                functionF.run();
            });

            // Create a FunctionCalculator (client) for function g with x = 10
            Thread gThread = new Thread(() -> {
                FunctionCalculator functionG = new FunctionCalculator("g", 10);
                functionG.run();
            });

            // Start the threads for function f and function g
            fThread.start();
            gThread.start();

            // Wait for both threads to finish
            fThread.join();
            gThread.join();

            // Get the result and print it
            String result = FunctionCalculator.getResult();
            System.out.println("Expression Result: " + result);
            if ("відмова".equals(result)) {
                System.out.println("Reason: Too many non-critical failures.");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}