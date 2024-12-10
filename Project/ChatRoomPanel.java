package Project;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.text.html.HTMLDocument;

public class ChatRoomPanel extends JPanel {

    private JTextPane chatHistoryPane;
    private JScrollPane chatScrollPane;
    private JTextField messageInputField;
    private JButton sendButton;
    private JButton exportChatButton; // Button for exporting chat history
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

        // Export chat button
        exportChatButton = new JButton("Export Chat");
        exportChatButton.addActionListener(e -> exportChatHistory());

        // Adding components to message panel
        messagePanel.add(messageInputField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        messagePanel.add(exportChatButton, BorderLayout.WEST); // Add export button to UI

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

    public void updateUserList(java.util.List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : users) {
                if (user.equals(client.getClientName())) {
                    userListModel.addElement("<html><b>" + user + "</b></html>"); // Highlight current user
                } else {
                    userListModel.addElement(user);
                }
            }
        });
    }
    
    
    // Method to export chat history
    private void exportChatHistory() {
        try {
            // Extract content from chatHistoryPane
            HTMLEditorKit kit = (HTMLEditorKit) chatHistoryPane.getEditorKit();
            HTMLDocument doc = (HTMLDocument) chatHistoryPane.getDocument();
            StringWriter writer = new StringWriter();
            kit.write(writer, doc, 0, doc.getLength());
            String htmlContent = writer.toString();

            // Convert HTML to plain text
            String plainText = htmlContent.replaceAll("<[^>]*>", ""); // Strip HTML tags

            // Create unique filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "chat_history_" + timestamp + ".txt";

            // Write to file
            File file = new File(fileName);
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
                bufferedWriter.write(plainText);
            }

            // Notify user
            JOptionPane.showMessageDialog(this, "Chat history saved to " + file.getAbsolutePath(), 
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error exporting chat history: " + e.getMessage(), 
                    "Export Failed", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
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
    }
}
