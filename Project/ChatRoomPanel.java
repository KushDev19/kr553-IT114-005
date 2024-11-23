package Project;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.List;

public class ChatRoomPanel extends JPanel {

    private JTextPane chatHistoryPane;
    private StyledDocument chatDocument;
    private JTextField messageInputField;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;

    public ChatRoomPanel() {
        setLayout(new BorderLayout());

        // Chat history pane (center)
        chatHistoryPane = new JTextPane();
        chatHistoryPane.setEditable(false);
        chatDocument = chatHistoryPane.getStyledDocument();
        JScrollPane chatScrollPane = new JScrollPane(chatHistoryPane);

        // User list (right side)
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 0));

        // Message input panel (bottom)
        JPanel messagePanel = new JPanel(new BorderLayout());
        messageInputField = new JTextField();
        sendButton = new JButton("Send");

        messagePanel.add(messageInputField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        // Add components to the main panel
        add(chatScrollPane, BorderLayout.CENTER);
        add(userScrollPane, BorderLayout.EAST);
        add(messagePanel, BorderLayout.SOUTH);

        // Add action listener to send button and message input field
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        messageInputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    }

    // Method to send a message
    private void sendMessage() {
        String message = messageInputField.getText().trim();
        if (!message.isEmpty()) {
            // Clear the input field
            messageInputField.setText("");

            // Send the message to the server
            Client.INSTANCE.sendMessageToServer(message);
        }
    }

    // Method to append a chat message with specified color
    public void appendChatMessageWithColor(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setForeground(style, color);
            try {
                chatDocument.insertString(chatDocument.getLength(), message + "\n", style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    // Method to update user list
    public void updateUserList(List<String> users) {
        userListModel.clear();
        for (String user : users) {
            userListModel.addElement(user);
        }
    }

    // For testing purposes, create a main method to display the panel
    public static void main(String[] args) {
        JFrame frame = new JFrame("Chat Room");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new ChatRoomPanel());
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Example test for appending colored messages
        ChatRoomPanel panel = (ChatRoomPanel) frame.getContentPane();
        panel.appendChatMessageWithColor("This is a normal message.", Color.BLACK);
        panel.appendChatMessageWithColor("This is a message from you!", Color.BLUE);
        panel.appendChatMessageWithColor("[Private] This is a private message.", Color.MAGENTA);
    }
}
