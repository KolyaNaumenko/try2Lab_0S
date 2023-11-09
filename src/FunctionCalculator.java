import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.*;
class FunctionCalculator implements Runnable {
    private String functionName;
    private Integer x;
    private Socket clientSocket;
    private static int failureCount = 0;
    private static final int MAX_FAILURES = 3; // Максимальна кількість некритичних збоїв
    private static double resultF = Double.NaN;
    private static double resultG = Double.NaN;
    private static final Object lock = new Object();

    public FunctionCalculator(String functionName, Integer x) {
        this.functionName = functionName;
        this.x = x;
    }

    public FunctionCalculator(String functionName, Socket clientSocket) {
        this.functionName = functionName;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        if (clientSocket == null) {
            // If clientSocket is not set, create a new socket
            try {
                clientSocket = new Socket("localhost", 8080);
            } catch (IOException e) {
                handleFailure(e);
                return;
            }
        }

        try (
                ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            clientSocket.setSoTimeout(5000); // Set a timeout of 5 seconds

            // Sending the function name and x value to the server
            output.writeObject(functionName);
            output.writeObject(x);
            output.flush();

            // Receiving the result from the server
            double result = input.readDouble();
            System.out.println("Result for " + functionName + "(" + x + "): " + result);

            // Save the result for further calculation
            saveResult(functionName, result);

            // Reset failure count on successful calculation
            failureCount = 0;
        } catch (IOException e) {
            handleFailure(e);
        } finally {
            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    handleFailure(e);
                }
            }
        }
    }

    private void saveResult(String functionName, double result) {
        synchronized (lock) {
            if ("f".equals(functionName)) {
                resultF = result;
            } else if ("g".equals(functionName)) {
                resultG = result;
            }
        }
    }

    private void handleFailure(IOException e) {
        System.err.println("Calculation for " + functionName + "(" + x + ") failed.");
        e.printStackTrace();

        // Increment failure count
        failureCount++;

        // Check if the number of failures exceeds the threshold
        if (failureCount >= MAX_FAILURES) {
            System.err.println("Too many non-critical failures. Stopping calculations.");
            System.exit(1); // You may want to handle this differently based on your application
        }
    }

    public static String getResult() {
        synchronized (lock) {
            if (Double.isNaN(resultF) || Double.isNaN(resultG)) {
                return "відмова";
            } else {
                double sum = resultF + resultG;
                return "Сума результатів f та g: " + sum;
            }
        }
    }

}