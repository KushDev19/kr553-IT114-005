package Project;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ConnectPanel extends JPanel {

    private JTextField usernameField;
    private JTextField hostField;
    private JTextField portField;
    private JButton connectButton;

    public ConnectPanel() {
        // Set up the layout
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Username label and field
        JLabel usernameLabel = new JLabel("Username:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(usernameLabel, gbc);

        usernameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        add(usernameField, gbc);

        // Host label and field
        JLabel hostLabel = new JLabel("Host:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(hostLabel, gbc);

        hostField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        add(hostField, gbc);

        // Port label and field
        JLabel portLabel = new JLabel("Port:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(portLabel, gbc);

        portField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        add(portField, gbc);

        // Connect button
        connectButton = new JButton("Connect");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(connectButton, gbc);

        // Add action listener to the connect button
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Retrieve input values
                String username = usernameField.getText().trim();
                String host = hostField.getText().trim();
                String portText = portField.getText().trim();

                // Validate inputs
                if (username.isEmpty() || username.contains(" ")) {
                    JOptionPane.showMessageDialog(ConnectPanel.this,
                            "Username cannot be empty or contain spaces.",
                            "Invalid Username", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (host.isEmpty()) {
                    JOptionPane.showMessageDialog(ConnectPanel.this,
                            "Host cannot be empty.",
                            "Invalid Host", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int port;
                try {
                    port = Integer.parseInt(portText);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(ConnectPanel.this,
                            "Port must be a number.",
                            "Invalid Port", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Initiate connection
                boolean connected = Client.INSTANCE.connectToServer(host, port, username);
                if (connected) {
                    // Transition to the chatroom UI panel
                    JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(ConnectPanel.this);
                    ChatRoomPanel chatRoomPanel = new ChatRoomPanel();
                    Client.INSTANCE.setChatRoomPanel(chatRoomPanel);
                    topFrame.setContentPane(chatRoomPanel);
                    topFrame.validate();
                    topFrame.repaint();

                    System.out.println("Connected as " + username + " to " + host + ":" + port);
                } else {
                    JOptionPane.showMessageDialog(ConnectPanel.this,
                            "Failed to connect to server.",
                            "Connection Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    // For testing purposes, create a main method to display the panel
    public static void main(String[] args) {
        JFrame frame = new JFrame("Connect to Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new ConnectPanel());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
