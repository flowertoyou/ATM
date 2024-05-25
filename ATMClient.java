import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.swing.JOptionPane;

public class ATMClient extends JFrame {
    private static final String SERVER_ADDRESS = "172.20.10.2";
    private static final int PORT = 2525;
    private JTextField userIdField;
    private JPasswordField passwordField;
    private JLabel messageLabel;
    private static Socket socket;
    private JButton enterButton;
    private JButton loginButton;
    private JLabel passwordLabel;
    private JLabel useridLabel;
    private String currentUserID;

    public ATMClient() {
        setTitle("ATM Client");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(null);

        useridLabel = new JLabel("User ID:");
        UserIdButtonListener listener = new UserIdButtonListener(this);
        useridLabel.setBounds(10, 20, 80, 25);
        panel.add(useridLabel);

        userIdField = new JTextField(20);
        userIdField.setBounds(100, 20, 165, 25);
        panel.add(userIdField);

        enterButton = new JButton("Enter");
        enterButton.setBounds(20, 50, 80, 25);
        enterButton.addActionListener(listener);
        panel.add(enterButton);

        passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(10, 20, 80, 25);
        panel.add(passwordLabel);
        passwordLabel.setVisible(false);

        passwordField = new JPasswordField(20);
        passwordField.setBounds(100, 20, 165, 25);
        panel.add(passwordField);
        passwordField.setVisible(false);

        loginButton = new JButton("Login");
        loginButton.setBounds(20, 50, 80, 25);
        loginButton.addActionListener(new LoginButtonListener());
        loginButton.setVisible(false);
        panel.add(loginButton);

        messageLabel = new JLabel("");
        messageLabel.setBounds(10, 140, 300, 25);
        panel.add(messageLabel);

        add(panel);
        setVisible(true);
    }

    class UserIdButtonListener implements ActionListener {
        private ATMClient atmclient;

        public UserIdButtonListener(ATMClient atmclient) {
            this.atmclient = atmclient;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
//            System.out.println("have done");
            String userId = userIdField.getText();
            try {
                Socket socket = new Socket(SERVER_ADDRESS, 2525); // 连接服务器地址和端口
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("HELO " + userId); // 发送HELO消息给服务器
                System.out.println("say hello");
                String response = in.readLine(); // 读取服务器返回的消息
                //System.out.println("have read: " + response);

                if (response.startsWith("500")) {
                    currentUserID = userId;
                    userIdField.setVisible(false);
                    enterButton.setVisible(false);
                    useridLabel.setVisible(false);
                    passwordLabel.setVisible(true);
                    passwordField.setVisible(true);
                    loginButton.setVisible(true);
                } else if (response.startsWith("401")) {
                    messageLabel.setText("Card number is incorrect. Please try again.");
                }

                out.close();
                in.close();

            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Connection error: " + ex.getMessage());
            }
        }
    }

    class LoginButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String password = new String(passwordField.getPassword());
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("PASS sp " + password); // 发送密码给服务器
                String response = in.readLine(); // 读取服务器返回的消息

                if (response.startsWith("525")) {
                    openNextFunctionality(); // 密码正确，执行登录操作
                } else if (response.startsWith("401")) {
                    messageLabel.setText("Incorrect password. Please try again."); // 密码错误，提示用户重新输入
                }

                out.close();
                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Connection error: " + ex.getMessage());
            }
        }
    }

    private void openNextFunctionality() {
        JFrame nextFrame = new JFrame("Next Functionality");
        JPanel panel = new JPanel();
        nextFrame.add(panel);
        panel.setLayout(new GridLayout(3, 1));

        JButton checkBalanceButton = new JButton("Check Balance");
        JButton withdrawButton = new JButton("Withdraw");
        JButton exitButton = new JButton("Exit");

        checkBalanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkBalance();
            }
        });

        withdrawButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                withdraw();
            }
        });

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exit();
            }
        });

        panel.add(checkBalanceButton);
        panel.add(withdrawButton);
        panel.add(exitButton);

        nextFrame.setSize(300, 200);
        nextFrame.setVisible(true);
    }

    private void checkBalance() {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String userId = userIdField.getText(); // Get user ID from UI
            out.println("BALA " + userId); // Send balance inquiry to server

            String response = in.readLine(); // Receive response from server

            if (response.startsWith("AMNT:")) {
                double balance = Double.parseDouble(response.substring(5)); // Parse balance information
                displayBalance(balance); // Display balance on UI
            } else {
                JOptionPane.showMessageDialog(null, "Failed to retrieve balance."); // Display error message on failure
            }

            out.close();
            in.close();

        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Connection error: " + ex.getMessage()); // Display connection error
        }
    }

    private void displayBalance(double balance) {
        JFrame balanceFrame = new JFrame("Balance");
        JPanel balancePanel = new JPanel();
        balanceFrame.add(balancePanel);

        JLabel balanceLabel = new JLabel("Your balance is: " + balance);
        balancePanel.add(balanceLabel);

        balanceFrame.setSize(300, 200);
        balanceFrame.setVisible(true);
    }

    private void withdraw() {
        try{
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Connected to server.");

            while (true) {
                System.out.print("Enter the amount to withdraw: ");
                String amountStr = userInput.readLine();

                if (amountStr == null || amountStr.isEmpty()) {
                    System.out.println("Invalid amount. Please enter a valid number.");
                    continue;
                }

                out.println("WDRA sp " + amountStr);

                String response = in.readLine();

                if (response.startsWith("401")) {
                    System.out.println("Insufficient balance. Withdrawal failed.");
                } else if (response.startsWith("525")) {
                    System.out.println("Withdrawal successful.");
                    break;
                } else {
                    System.out.println("Unknown response from server: " + response);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void exit() {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("BYE"); // Send "BYE" to server
            String response = in.readLine(); // Receive response from server

            if (response.equals("BYE")) {
                JOptionPane.showMessageDialog(null, "Card successfully ejected.");
                System.exit(0); // Exit the program after successfully ejecting the card
            } else {
                JOptionPane.showMessageDialog(null, "Error: Unable to eject card.");
            }

            out.close();
            in.close();
            socket.close();

        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Connection error: " + ex.getMessage());
        }
    }
    public static void main(String[] args) {
        try {
            socket = new Socket(SERVER_ADDRESS, PORT);
            new ATMClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}