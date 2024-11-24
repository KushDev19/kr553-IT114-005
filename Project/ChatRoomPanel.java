package Project;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import java.io.IOException;
import javax.swing.text.html.HTMLDocument;

public class ChatRoomPanel extends JPanel {

    private JTextPane chatHistoryPane;
    private JScrollPane chatScrollPane;
    private JTextField messageInputField;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private Client client;

    public ChatRoomPanel() {
        client = Client.INSTANCE;
        client.setChatRoomPanel(this);
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Chat history pane (center)
        chatHistoryPane = new JTextPane();
        chatHistoryPane.setContentType("text/html"); // Set content type to HTML
        chatHistoryPane.setEditable(false);
        chatHistoryPane.setBackground(Color.WHITE);
        chatScrollPane = new JScrollPane(chatHistoryPane);

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

        // Action listener to send button and message input field
        sendButton.addActionListener(e -> sendMessage());
        messageInputField.addActionListener(e -> sendMessage());
    }

    // Method to send a message
    private void sendMessage() {
        String message = messageInputField.getText().trim();
        if (!message.isEmpty()) {
            // Clear the input field
            messageInputField.setText("");

            // Send the message to the server
            client.sendMessageToServer(message);
        }
    }

    // Updated method to append a chat message with specified color
    public void appendChatMessageWithColor(String message, java.awt.Color color) {
        System.out.println("Appending message to UI: " + message + " with color: " + color);
        SwingUtilities.invokeLater(() -> {
            HTMLEditorKit kit = (HTMLEditorKit) chatHistoryPane.getEditorKit();
            HTMLDocument doc = (HTMLDocument) chatHistoryPane.getDocument();
            String colorHex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            String htmlMessage = String.format("<span style='color:%s;'>%s</span><br>", colorHex, message);

            try {
                kit.insertHTML(doc, doc.getLength(), htmlMessage, 0, 0, null);
                chatHistoryPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    // Method to update user list
    public void updateUserList(java.util.List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : users) {
                userListModel.addElement(user);
            }
        });
    }

    // For testing purposes, create a main method to display the panel
    public static void main(String[] args) {
        JFrame frame = new JFrame("Chat Room");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ChatRoomPanel chatRoomPanel = new ChatRoomPanel();
        frame.setContentPane(chatRoomPanel);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Example test for appending colored messages
        chatRoomPanel.appendChatMessageWithColor("This is a normal message.", Color.BLACK);
        chatRoomPanel.appendChatMessageWithColor("This is a message from you!", Color.BLUE);
        chatRoomPanel.appendChatMessageWithColor("[Private] This is a private message.", Color.MAGENTA);

        // Example test for formatted messages
        chatRoomPanel.appendChatMessageWithColor("**This is bold text**", Color.BLACK);
        chatRoomPanel.appendChatMessageWithColor("*This is italic text*", Color.BLACK);
        chatRoomPanel.appendChatMessageWithColor("_This is underlined text_", Color.BLACK);
        chatRoomPanel.appendChatMessageWithColor("#rThis is red text r#", Color.BLACK);
        chatRoomPanel.appendChatMessageWithColor("**_#bThis is bold, italic, underlined, blue text b#_**", Color.BLACK);
    }
}
