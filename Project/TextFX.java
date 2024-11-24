package Project;

import Project.TextFX.TextColor;

/**
 * Utility to provide TextColored and formatted text in the GUI using HTML.
 */
public abstract class TextFX {

    /**
     * Enum representing available text TextColors using HTML TextColor codes.
     */
    public enum TextColor {
        BLACK("black", java.awt.Color.BLACK),
        RED("red", java.awt.Color.RED),
        GREEN("green", java.awt.Color.GREEN),
        YELLOW("yellow", java.awt.Color.YELLOW),
        BLUE("blue", java.awt.Color.BLUE),
        MAGENTA("magenta", java.awt.Color.MAGENTA),
        CYAN("cyan", java.awt.Color.CYAN),
        WHITE("white", java.awt.Color.WHITE);
    
        private final String code;
        private final java.awt.Color awtColor;
    
        TextColor(String code, java.awt.Color awtColor) {
            this.code = code;
            this.awtColor = awtColor;
        }
    
        public String getCode() {
            return code;
        }
    
        public java.awt.Color getAwtColor() {
            return awtColor;
        }
    }
    
    

    /**
     * Applies the specified TextColor to the input text using HTML span.
     *
     * @param text  The text to TextColorize.
     * @param TextColor The TextColor to apply.
     * @return The TextColorized text with HTML span.
     */
    public static String TextColorize(String text, TextColor TextColor) {
        return "<span style='TextColor:" + TextColor.getCode() + ";'>" + text + "</span>";
    }

    public static String formatFlipResult(String senderName, String result) {
        return TextColorize(String.format("[Flip] %s flipped a coin: %s", escapeHTML(senderName), escapeHTML(result)),
                TextColor.YELLOW);
    }

    public static String formatRollResult(String senderName, int total, String details) {
        return TextColorize(String.format("[Roll] %s rolled: %d (%s)", escapeHTML(senderName), total, escapeHTML(details)),
                TextColor.CYAN);
    }

    /**
     * Formats text with bold, italic, underline, and TextColor tags based on
     * markdown-style symbols.
     *
     * @param text Text to format.
     * @return Formatted text string with HTML tags.
     */
    public static String formatText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
    
        // Applying formatting in stages while preserving existing tags.
    
        // Step 1: Apply bold, italic, and underline formatting
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");    // Bold
        text = text.replaceAll("\\*(.+?)\\*", "<i>$1</i>");          // Italic
        text = text.replaceAll("_(.+?)_", "<u>$1</u>");              // Underline
    
        // Step 2: Process color tags (#r, #g, #b)
        text = text.replaceAll("#r(.*?)r#", "<span style='color:red;'>$1</span>");    // Red
        text = text.replaceAll("#g(.*?)g#", "<span style='color:green;'>$1</span>");  // Green
        text = text.replaceAll("#b(.*?)b#", "<span style='color:blue;'>$1</span>");   // Blue
    
        // Step 3: Escape HTML special characters in the remaining text
        
    
        return text;
    }
    
    

    /**
     * Escapes HTML special characters in the text to prevent HTML injection.
     *
     * @param text The text to escape.
     * @return The escaped text.
     */
    public static String escapeHTML(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Main method for demonstrating the usage of formatting methods.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        // Example usage:
        System.out.println(TextFX.TextColorize("Hello, world!", TextColor.RED));
        System.out.println(TextFX.formatText("This is **bold** text."));
        System.out.println(TextFX.formatText("This is *italic* text."));
        System.out.println(TextFX.formatText("This is _underlined_ text."));
        System.out.println(TextFX.formatText("This is #r red r# text."));
        System.out.println(TextFX.formatText("This is **_#r bold, italic, underlined red text r#_**."));
    }
}
