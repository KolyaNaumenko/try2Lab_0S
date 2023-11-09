import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.*;
class SocketServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Socket Server is waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
                ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            clientSocket.setSoTimeout(5000); // Set a timeout of 5 seconds

            // Receiving the function name and x value from the client
            String functionName = (String) input.readObject();
            Integer x = (Integer) input.readObject();

            // Calculating the result based on the function name
            double result;
            try {
                result = calculateFunctionWithTimeout(functionName, x, 3000); // Set a timeout of 3 seconds
            } catch (TimeoutException e) {
                System.err.println("Calculation for " + functionName + "(" + x + ") timed out.");
                result = Double.NaN;
            }

            // Sending the result back to the client
            output.writeDouble(result);
            output.flush();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static double calculateFunctionWithTimeout(String functionName, Integer x, long timeoutMillis) throws TimeoutException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Double> future = executor.submit(() -> calculateFunction(functionName, x));

        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            future.cancel(true); // Cancel the task if it exceeds the timeout
            throw new TimeoutException();
        } finally {
            executor.shutdownNow(); // Shutdown the executor service
        }
    }

    private static double calculateFunction(String functionName, Integer x) {
        // Implement your actual calculation logic here
        double doubleX = x.doubleValue(); // Convert Integer to double
        if ("f".equals(functionName)) {
            return doubleX * doubleX;
        } else if ("g".equals(functionName)) {
            return 3 * doubleX - 5;
        } else {
            return Double.NaN; // Placeholder for unsupported function
        }
    }
}