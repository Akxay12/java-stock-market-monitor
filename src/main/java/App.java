import yahoofinance.YahooFinance;
import yahoofinance.Stock;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Queue;

public class App {

    static class StockEntry {
        private final BigDecimal price;
        private final LocalDateTime timestamp;
        private static final DateTimeFormatter FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        StockEntry(BigDecimal price, LocalDateTime timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return String.format("[%s] DJI: $%,.2f",
                    timestamp.format(FORMATTER), price);
        }
    }

    public static void main(String[] args) {
        Queue<StockEntry> priceQueue = new LinkedList<>();

        System.out.println("Starting Dow Jones Industrial Average (^DJI) tracker...");
        System.out.println("Fetching price every 5 seconds. Press Ctrl+C to stop.\n");

        while (true) {
            try {
                Stock stock = YahooFinance.get("^DJI");

                if (stock == null) {
                    System.err.println("Could not retrieve stock data for ^DJI. Retrying...");
                } else {
                    BigDecimal price = stock.getQuote().getPrice();
                    LocalDateTime timestamp = LocalDateTime.now();

                    StockEntry entry = new StockEntry(price, timestamp);
                    priceQueue.add(entry);

                    System.out.println("=== Queue Contents (" + priceQueue.size() + " entries) ===");
                    for (StockEntry e : priceQueue) {
                        System.out.println("  " + e);
                    }
                    System.out.println();
                }

                Thread.sleep(5000);

            } catch (IOException e) {
                System.err.println("Network error fetching stock data: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.err.println("Thread interrupted during retry wait: " + ie.getMessage());
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread interrupted: " + e.getMessage());
                break;
            }
        }

        System.out.println("Tracker stopped. Final queue contents:");
        for (StockEntry e : priceQueue) {
            System.out.println("  " + e);
        }
    }
}
