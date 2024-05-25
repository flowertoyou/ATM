import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.*;

public class ATMServer {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/Library";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "12345";
    private static final int PORT = 2525;
    private static String userIdClient;
    private static String passwordClient;

    private static final Logger logger = Logger.getLogger(ATMServer.class.getName());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("ATM Server is running...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {

                    if (inputLine.startsWith("HELO"))     //'HELO'报文
                    {
                        handleHELO(inputLine);
                        logger.info(userIdClient+"用户进入ATM系统");
                    }
                    else if (inputLine.startsWith("PASS"))
                    {
                        handlePASS(inputLine);
                        logger.info(userIdClient+"用户正在输入密码");
                    }
                    else if (inputLine.startsWith("BALA"))
                    {
                        handleBALA();
                        logger.info(userIdClient+"用户查询余额");
                    }
                    else if (inputLine.startsWith("WDRA"))
                    {
                        handleWDRA(inputLine);
                        logger.info(userIdClient+"用户取款");
                    }
                    else if (inputLine.equals("BYE"))
                    {
                        out.println("BYE");
                        logger.info(userIdClient+"用户退卡");
                        break;
                    }
                }

                clientSocket.close();
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }

        private void handleHELO(String inputLine) throws SQLException {
            userIdClient = inputLine.substring(5);
            if (checkUserIdExists(userIdClient)) {
                out.println("500 AUTH REQUIRE");
            } else {
                out.println("401 ERROR!");
            }
        }

        private void handlePASS(String inputLine) throws SQLException {
            passwordClient = inputLine.substring(5);
            if (checkPasswordMatch(userIdClient, passwordClient)) {
                out.println("525 OK!");
            } else {
                out.println("401 ERROR!");
            }
        }

        private void handleBALA() throws SQLException {
            double balance = getBalance(userIdClient);
            out.println("AMNT " + Double.toString(balance));
        }


        private void handleWDRA(String inputLine) throws SQLException {
            double amount = Double.parseDouble(inputLine.substring(5));
            double balance = getBalance(userIdClient);
            if (amount > balance) {
                out.println("401 ERROR!");
            } else {
                updateBalance(userIdClient, balance - amount);
                out.println("525 OK!");
            }
        }

        private boolean checkUserIdExists(String userId) throws SQLException {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement statement = conn.prepareStatement("SELECT * FROM client WHERE UserId = ?")) {
                statement.setString(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        }

        private boolean checkPasswordMatch(String userId, String password) throws SQLException {
            String password_from_db = null;
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement statement = conn.prepareStatement("SELECT Password FROM client WHERE UserId = ?")) {
                statement.setString(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        password_from_db = resultSet.getString("Password");
                    }
                }
            }
            // 判断密码是否匹配
            return password_from_db != null && password_from_db.equals(password);
        }


        private double getBalance(String userId) throws SQLException {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement statement = conn.prepareStatement("SELECT Balance FROM client WHERE UserId = ?")) {
                statement.setString(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getDouble("Balance");
                    } else {
                        throw new SQLException("User not found!");
                    }
                }
            }
        }

        private void updateBalance(String userId, double newBalance) throws SQLException {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement statement = conn.prepareStatement("UPDATE client SET Balance = ? WHERE UserId = ?")) {
                statement.setDouble(1, newBalance);
                statement.setString(2, userId);
                statement.executeUpdate();
            }
        }
    }
}
