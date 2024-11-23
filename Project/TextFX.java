package Project;

import Project.TextFX.Color;

/**
 * Utility to provide colored and formatted text in the terminal.
 * Important: This may not satisfy the text formatting feature/requirement for chatroom projects if HTML rendering is needed.
 */
public abstract class TextFX {

    /**
     * Enum representing available text colors using ANSI escape codes.
     */
    public enum Color {
        BLACK("\033[0;30m"),
        RED("\033[0;31m"),
        GREEN("\033[0;32m"),
        YELLOW("\033[0;33m"),
        BLUE("\033[0;34m"),
        PURPLE("\033[0;35m"),
        MAGENTA("\033[0;35m"),
        CYAN("\033[0;36m"),
        WHITE("\033[0;37m");

        private final String code;

        Color(String code) {
            this.code = code;
        }

        /**
         * Retrieves the ANSI code for the color.
         *
         * @return ANSI code as a String.
         */
        public String getCode() {
            return code;
        }
    }

    public static final String RESET = "\033[0m";

    /**
     * Applies the specified color to the input text.
     *
     * @param text  The text to colorize.
     * @param color The color to apply.
     * @return The colorized text.
     */
    public static String colorize(String text, Color color) {
        return color.getCode() + text + RESET;
    }

    public static String formatFlipResult(String senderName, String result) {
        return colorize(String.format("[Flip] %s flipped a coin: %s", senderName, result), Color.YELLOW);
    }
    
    public static String formatRollResult(String senderName, int total, String details) {
        return colorize(String.format("[Roll] %s rolled: %d (%s)", senderName, total, details), Color.CYAN);
    }
    

    /**
     * Formats text with bold, italic, underline, and color tags based on markdown-style symbols.
     *
     * @param text Text to format.
     * @return Formatted text string.
     */
    public static String formatText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Applying formatting in stages while preserving existing tags.

        // Step 1: Process color tags (#r, #g, #b)
        text = text.replaceAll("#r(.*?)r#", "\033[0;31m$1\033[0m"); // Red
        text = text.replaceAll("#g(.*?)g#", "\033[0;32m$1\033[0m"); // Green
        text = text.replaceAll("#b(.*?)b#", "\033[0;34m$1\033[0m"); // Blue

        // Step 2: Apply bold, italic, and underline formatting in a way that allows nesting.
        // Note that to handle nested styles properly, use non-greedy quantifiers.
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "\033[1m$1\033[22m");  // Bold
        text = text.replaceAll("\\*(.+?)\\*", "\033[3m$1\033[23m");        // Italic
        text = text.replaceAll("_(.+?)_", "\033[4m$1\033[24m");            // Underline

        return text;
    }

    /**
     * Makes the text bold using ANSI escape codes.
     *
     * @param text The text to make bold.
     * @return The bolded text.
     */
    public static String bold(String text) {
        return "\033[1m" + text + RESET;
    }

    /**
     * Underlines the text using ANSI escape codes.
     *
     * @param text The text to underline.
     * @return The underlined text.
     */
    public static String underline(String text) {
        return "\033[4m" + text + RESET;
    }

    /**
     * Makes the text bold and underlined using ANSI escape codes.
     *
     * @param text The text to format.
     * @return The bold and underlined text.
     */
    public static String boldUnderline(String text) {
        return "\033[1;4m" + text + RESET;
    }

    /**
     * Applies color and bold formatting to the text.
     *
     * @param text  The text to format.
     * @param color The color to apply.
     * @return The formatted text.
     */
    public static String boldColor(String text, Color color) {
        return color.getCode() + "\033[1m" + text + RESET;
    }

    /**
     * Applies color and underline formatting to the text.
     *
     * @param text  The text to format.
     * @param color The color to apply.
     * @return The formatted text.
     */
    public static String underlineColor(String text, Color color) {
        return color.getCode() + "\033[4m" + text + RESET;
    }

    /**
     * Applies bold, underline, and color formatting to the text.
     *
     * @param text  The text to format.
     * @param color The color to apply.
     * @return The formatted text.
     */
    public static String boldUnderlineColor(String text, Color color) {
        return color.getCode() + "\033[1;4m" + text + RESET;
    }

    /**
     * Main method for demonstrating the usage of formatting methods.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        // Example usage:
        System.out.println(TextFX.colorize("Hello, world!", Color.RED));
        System.out.println(TextFX.formatText("This is **bold** text."));
        System.out.println(TextFX.formatText("This is *italic* text."));
        System.out.println(TextFX.formatText("This is _underlined_ text."));
        System.out.println(TextFX.formatText("This is #r red r# text."));
        System.out.println(TextFX.formatText("This is **_#r bold, italic, underlined red text r#_**."));
    }
}
