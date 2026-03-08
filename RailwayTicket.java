/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.railwayticket;

/**
 *
 * @author Deepak Shyam
 */import javax.swing.*;
import java.sql.*;
import java.lang.management.*;
import java.util.concurrent.Semaphore;

public class Railwayticket extends JFrame {

    JTextField nameField, dateField, seatField;
    JComboBox<String> trainDropdown;
    JTextArea ticketArea;

    final String DB_URL = "jdbc:mysql://localhost:3306/railway?useSSL=false";
    final String DB_USER = "root";
    final String DB_PASS = "deepak@2006";

    Semaphore ticketSemaphore = new Semaphore(5);

    public Railwayticket() {

        setTitle("Railway Reservation System");
        setSize(550, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setBounds(20, 20, 80, 30);
        add(nameLabel);

        nameField = new JTextField();
        nameField.setBounds(100, 20, 150, 30);
        add(nameField);

        JLabel trainLabel = new JLabel("Train:");
        trainLabel.setBounds(20, 60, 80, 30);
        add(trainLabel);

        trainDropdown = new JComboBox<>();
        trainDropdown.setBounds(100, 60, 150, 30);
        add(trainDropdown);

        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setBounds(20, 100, 80, 30);
        add(dateLabel);

        dateField = new JTextField();
        dateField.setBounds(100, 100, 150, 30);
        add(dateField);

        JLabel seatLabel = new JLabel("Seat No:");
        seatLabel.setBounds(20, 140, 80, 30);
        add(seatLabel);

        seatField = new JTextField();
        seatField.setBounds(100, 140, 150, 30);
        add(seatField);

        JButton bookBtn = new JButton("Book Ticket");
        bookBtn.setBounds(280, 20, 220, 30);
        add(bookBtn);

        JButton viewBtn = new JButton("View Tickets");
        viewBtn.setBounds(280, 60, 220, 30);
        add(viewBtn);

        JButton cancelBtn = new JButton("Cancel Ticket");
        cancelBtn.setBounds(280, 100, 220, 30);
        add(cancelBtn);

        JButton trainsBtn = new JButton("View Trains");
        trainsBtn.setBounds(280, 140, 220, 30);
        add(trainsBtn);

        JButton sysInfoBtn = new JButton("System Info");
        sysInfoBtn.setBounds(280, 180, 220, 30);
        add(sysInfoBtn);

        ticketArea = new JTextArea();
        ticketArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(ticketArea);
        scrollPane.setBounds(20, 230, 500, 300);
        add(scrollPane);

        bookBtn.addActionListener(e -> bookTicket());
        viewBtn.addActionListener(e -> viewTickets());
        cancelBtn.addActionListener(e -> cancelTicket());
        trainsBtn.addActionListener(e -> viewTrains());
        sysInfoBtn.addActionListener(e -> showSystemInfo());

        loadTrains();

        setVisible(true);
    }

    Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    void loadTrains() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT train_no, train_name FROM train")) {

            while (rs.next()) {
                String train = rs.getString("train_no") + " - " + rs.getString("train_name");
                trainDropdown.addItem(train);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void bookTicket() {

        new Thread(() -> {

            String name = nameField.getText().trim();
            String trainEntry = (String) trainDropdown.getSelectedItem();
            String trainNo = trainEntry != null ? trainEntry.split(" - ")[0] : "";
            String date = dateField.getText().trim();
            String seat = seatField.getText().trim();

            if (name.isEmpty() || trainNo.isEmpty() || date.isEmpty() || seat.isEmpty()) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "All fields are required!"));
                return;
            }

            try {

                ticketSemaphore.acquire();

                try (Connection conn = getConnection();
                     PreparedStatement insertStmt = conn.prepareStatement(
                             "INSERT INTO tickets (name, train_no, travel_date, seat_no) VALUES (?, ?, ?, ?)")) {

                    insertStmt.setString(1, name);
                    insertStmt.setString(2, trainNo);
                    insertStmt.setString(3, date);
                    insertStmt.setString(4, seat);

                    insertStmt.executeUpdate();

                    try (PreparedStatement updateTrainStmt = conn.prepareStatement(
                            "UPDATE train SET booked_seats = booked_seats + 1 WHERE train_no = ?")) {

                        updateTrainStmt.setString(1, trainNo);
                        updateTrainStmt.executeUpdate();
                    }

                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Ticket booked successfully!");
                        nameField.setText("");
                        dateField.setText("");
                        seatField.setText("");
                    });

                } catch (SQLIntegrityConstraintViolationException dupEx) {

                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this, "Seat already booked!"));
                }

            } catch (Exception ex) {

                ex.printStackTrace();

                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Booking failed: " + ex.getMessage()));
            } finally {

                ticketSemaphore.release();
            }

        }).start();
    }

    void viewTickets() {

        new Thread(() -> {

            StringBuilder result = new StringBuilder();

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM tickets")) {

                while (rs.next()) {

                    result.append("Name: ").append(rs.getString("name"))
                            .append(", Train No: ").append(rs.getString("train_no"))
                            .append(", Date: ").append(rs.getString("travel_date"))
                            .append(", Seat: ").append(rs.getString("seat_no"))
                            .append("\n");
                }

                SwingUtilities.invokeLater(() -> ticketArea.setText(result.toString()));

            } catch (SQLException ex) {

                ex.printStackTrace();

                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Error retrieving tickets."));
            }

        }).start();
    }

    void viewTrains() {

        new Thread(() -> {

            StringBuilder result = new StringBuilder();

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM train")) {

                while (rs.next()) {

                    result.append("Train No: ").append(rs.getString("train_no"))
                            .append(", Name: ").append(rs.getString("train_name"))
                            .append(", Seats: ").append(rs.getInt("total_seats"))
                            .append(", Booked: ").append(rs.getInt("booked_seats"))
                            .append("\n");
                }

                SwingUtilities.invokeLater(() -> ticketArea.setText(result.toString()));

            } catch (SQLException ex) {

                ex.printStackTrace();
            }

        }).start();
    }

    void cancelTicket() {

        new Thread(() -> {

            String nameToCancel = JOptionPane.showInputDialog(this, "Enter name to cancel:");
            String trainToCancel = JOptionPane.showInputDialog(this, "Enter train number to cancel:");
            String seatToCancel = JOptionPane.showInputDialog(this, "Enter seat number to cancel:");

            if (nameToCancel == null || seatToCancel == null || trainToCancel == null)
                return;

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "DELETE FROM tickets WHERE name=? AND seat_no=? AND train_no=?")) {

                stmt.setString(1, nameToCancel);
                stmt.setString(2, seatToCancel);
                stmt.setString(3, trainToCancel);

                int rows = stmt.executeUpdate();

                if (rows > 0) {

                    try (PreparedStatement updateTrainStmt = conn.prepareStatement(
                            "UPDATE train SET booked_seats = booked_seats - 1 WHERE train_no=?")) {

                        updateTrainStmt.setString(1, trainToCancel);
                        updateTrainStmt.executeUpdate();
                    }
                }

                SwingUtilities.invokeLater(() -> {

                    if (rows > 0)
                        JOptionPane.showMessageDialog(this, "Ticket cancelled successfully!");
                    else
                        JOptionPane.showMessageDialog(this, "Ticket not found.");
                });

            } catch (SQLException ex) {

                ex.printStackTrace();
            }

        }).start();
    }

    void showSystemInfo() {

        String os = System.getProperty("os.name");
        String user = System.getProperty("user.name");
        String javaVersion = System.getProperty("java.version");
        String home = System.getProperty("user.home");

        long threadId = Thread.currentThread().getId();

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();

        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        JOptionPane.showMessageDialog(this,
                "OS: " + os + "\n" +
                        "User: " + user + "\n" +
                        "Java: " + javaVersion + "\n" +
                        "Home: " + home + "\n" +
                        "Thread ID: " + threadId + "\n" +
                        "Processors: " + osBean.getAvailableProcessors() + "\n" +
                        "Used Memory: " + usedMemory + " MB",
                "System Info",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> new Railwayticket());
    }
}
