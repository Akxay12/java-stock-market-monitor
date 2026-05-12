import yahoofinance.YahooFinance;
import yahoofinance.Stock;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class App {

    // Holds a single fetched data point: price + timestamp
    static class StockEntry {
        final BigDecimal price;
        final LocalDateTime timestamp;
        private static final DateTimeFormatter FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        StockEntry(BigDecimal price, LocalDateTime timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return String.format("[%s]  DJI: $%,.2f", timestamp.format(FORMATTER), price);
        }
    }

    public static void main(String[] args) {

        // ── Data storage ──────────────────────────────────────────────────────
        Queue<StockEntry> priceQueue = new LinkedList<>();

        // ── JFreeChart setup ──────────────────────────────────────────────────
        TimeSeries series = new TimeSeries("DJI Price");
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Dow Jones Industrial Average (^DJI) — Live",  // chart title
                "Time",                                         // x-axis label
                "Price (USD)",                                  // y-axis label
                dataset,
                false,   // no legend
                true,    // tooltips
                false    // no URLs
        );

        // Style the chart
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        DateAxis timeAxis = (DateAxis) plot.getDomainAxis();
        timeAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        timeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 11));

        plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 11));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setSeriesPaint(0, new Color(0, 102, 204));
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        plot.setRenderer(renderer);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(900, 420));
        chartPanel.setMouseWheelEnabled(true);

        // ── Status bar ────────────────────────────────────────────────────────
        JLabel statusLabel = new JLabel("Starting up — fetching first data point...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 13));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        // ── Queue log panel ───────────────────────────────────────────────────
        JTextArea queueArea = new JTextArea(7, 50);
        queueArea.setEditable(false);
        queueArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        queueArea.setBackground(new Color(245, 245, 245));

        JScrollPane scrollPane = new JScrollPane(queueArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Price Queue (all fetched prices)"));
        scrollPane.setPreferredSize(new Dimension(900, 160));

        // ── Main window ───────────────────────────────────────────────────────
        JFrame frame = new JFrame("DJI Stock Monitor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));
        frame.add(statusLabel, BorderLayout.NORTH);
        frame.add(chartPanel, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);  // centre on screen
        frame.setVisible(true);

        // ── Background thread: fetch every 5 seconds ──────────────────────────
        Thread fetchThread = new Thread(() -> {
            System.out.println("DJI tracker started. Fetching every 5 seconds...\n");

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Stock stock = YahooFinance.get("^DJI");

                    if (stock == null) {
                        SwingUtilities.invokeLater(() ->
                                statusLabel.setText("Could not retrieve data for ^DJI — retrying..."));
                    } else {
                        BigDecimal price = stock.getQuote().getPrice();

                        LocalDateTime timestamp = LocalDateTime.now();
                        StockEntry entry = new StockEntry(price, timestamp);
                        priceQueue.add(entry);

                        System.out.println("Fetched: " + entry);

                        // Update GUI on the Event Dispatch Thread
                        SwingUtilities.invokeLater(() -> {
                            // Add point to the live chart
                            series.addOrUpdate(new Second(new Date()), price);

                            // Refresh status bar
                            statusLabel.setText("Last update: " + entry
                                    + "   |   Total readings: " + priceQueue.size());

                            // Rebuild queue display
                            StringBuilder sb = new StringBuilder();
                            for (StockEntry e : priceQueue) {
                                sb.append(e).append("\n");
                            }
                            queueArea.setText(sb.toString());
                            // Auto-scroll to the latest entry
                            queueArea.setCaretPosition(queueArea.getDocument().getLength());
                        });
                    }



                    //  Demo fallback data used to visualize the live graph
                    // in case Yahoo Finance API rate limits requests🔽

//
//                    BigDecimal price = BigDecimal.valueOf(
//                            42000 + Math.random() * 500
//                    );
//
//                    LocalDateTime timestamp = LocalDateTime.now();
//                    StockEntry entry = new StockEntry(price, timestamp);
//                    priceQueue.add(entry);
//
//                    System.out.println("Fetched: " + entry);
//
//          // Update GUI on the Event Dispatch Thread
//
//                    SwingUtilities.invokeLater(() -> {
//
//                        // Add point to the live chart
//                        series.addOrUpdate(new Second(new Date()), price);
//
//                        // Refresh status bar
//                        statusLabel.setText("Last update: " + entry
//                                + "   |   Total readings: " + priceQueue.size());
//
//                        // Rebuild queue display
//                        StringBuilder sb = new StringBuilder();
//                        for (StockEntry e : priceQueue) {
//                            sb.append(e).append("\n");
//                        }
//
//                        queueArea.setText(sb.toString());
//
//                        // Auto-scroll to latest entry
//                        queueArea.setCaretPosition(
//                                queueArea.getDocument().getLength()
//                        );
//                    });




                    Thread.sleep(5000);

                }
                // Comment out this IOException catch block to view the demo graph
                catch (IOException e) {
                    System.err.println("Network error: " + e.getMessage());
                    SwingUtilities.invokeLater(() ->
                            statusLabel.setText("Network error: " + e.getMessage() + " — retrying in 5s"));
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Tracker stopped.");
                }
            }
        });

        fetchThread.setDaemon(true);  // stops automatically when the window is closed
        fetchThread.start();
    }
}
