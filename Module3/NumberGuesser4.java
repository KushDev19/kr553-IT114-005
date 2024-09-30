import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

//kr553 9/30/2024

public class NumberGuesser4 {
    private int maxLevel = 1;
    private int level = 1;
    private int strikes = 0;
    private int maxStrikes = 5;
    private int number = -1;
    private boolean pickNewRandom = true;
    private Random random = new Random();
    private String fileName = "ng4.txt";
    private String[] fileHeaders = { "Level", "Strikes", "Number", "MaxLevel", "Difficulty" };
    private String difficulty = "medium"; // default difficulty

    private void saveState() {
        String[] data = { level + "", strikes + "", number + "", maxLevel + "", difficulty };
        String output = String.join(",", data);
        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(String.join(",", fileHeaders));
            fw.write("\n"); // new line
            fw.write(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean loadState() {
        File file = new File(fileName);
        if (!file.exists()) {
            // Not providing output here as it's expected for a fresh start
            return false;
        }
        try (Scanner reader = new Scanner(file)) {
            int lineNumber = 0;
            while (reader.hasNextLine()) {
                String text = reader.nextLine();
                if (lineNumber == 1) {
                    String[] data = text.split(",");
                    if (data.length >= 4) {
                        String level = data[0];
                        String strikes = data[1];
                        String number = data[2];
                        String maxLevel = data[3];

                        int temp = strToNum(level);
                        if (temp > -1) {
                            this.level = temp;
                        }
                        temp = strToNum(strikes);
                        if (temp > -1) {
                            this.strikes = temp;
                        }
                        temp = strToNum(number);
                        if (temp > -1) {
                            this.number = temp;
                            pickNewRandom = false;
                        }
                        temp = strToNum(maxLevel);
                        if (temp > -1) {
                            this.maxLevel = temp;
                        }
                        if (difficulty != "medium") {
                            this.difficulty = difficulty;
                        } else {
                            this.difficulty = "medium"; // default if not found
                            this.level = 1;
                        }
                    }
                }
                lineNumber++;
            }
        } catch (FileNotFoundException e) { // specific exception
            e.printStackTrace();
        } catch (Exception e2) { // any other unhandled exception
            e2.printStackTrace();
        }
        setMaxStrikes();
        System.out.println("Loaded state");
        int range = 10 + ((level - 1) * 5);
        System.out.println("Difficulty: " + difficulty + " and Your Max Strikes are: " + maxStrikes);
        System.out.println("Welcome to level " + level);
        System.out.println(
                "I picked a random number between 1-" + (range) + ", let's see if you can guess.");
        return true;
        //kr553 9/30/24
    }

    private void setMaxStrikes() {
        if (difficulty.equalsIgnoreCase("easy")) {
            maxStrikes = 10;
        } else if (difficulty.equalsIgnoreCase("medium")) {
            maxStrikes = 5;
        } else if (difficulty.equalsIgnoreCase("hard")) {
            maxStrikes = 3;
        } else {
            // Default to medium if difficulty is unrecognized
            maxStrikes = 5;
            difficulty = "medium";
        }
    }

    /***
     * Gets a random number between 1 and level.
     * 
     * @param level (level to use as upper bounds)
     * @return number between bounds
     */
    private void generateNewNumber(int level) {
        int range = 10 + ((level - 1) * 5);
        System.out.println("Welcome to level " + level);
        System.out.println(
                "I picked a random number between 1-" + (range) + ", let's see if you can guess.");
        number = random.nextInt(range) + 1;
    }

    private void win() {
        System.out.println("That's right!");
        level++; // level up!
        strikes = 0;
    }

    private boolean processCommands(String message) {
        boolean processed = false;
        if (message.equalsIgnoreCase("quit")) {
            System.out.println("Tired of playing? No problem, see you next time.");
            processed = true;
        }
        // TODO add other conditions here
        return processed;
    }

    private void lose() {
        System.out.println("Uh oh, looks like you need to get some more practice.");
        System.out.println("The correct number was " + number);
        strikes = 0;
        level--;
        if (level < 1) {
            level = 1;
        }
    }

    //kr553 9/30/24
    private void processGuess(int guess) {
        if (guess < 0) {
            return;
        }
        System.out.println("You guessed " + guess);
        if (guess == number) {
            win();
            pickNewRandom = true;
        } else {
            System.out.println("That's wrong");
            strikes++;
            if (strikes >= maxStrikes) {
                lose();
                pickNewRandom = true;
            } else if(strikes >= 2){
                System.out.println("You have " +  (maxStrikes - strikes) + " strikes left.");

                if (guess < number) {
                    System.out.println("Hint: Higher");
                } else {
                    System.out.println("Hint: Lower");
                }
            }
        }
        saveState();
    }

    private int strToNum(String message) {
        int guess = -1;
        try {
            guess = Integer.parseInt(message.trim());
        } catch (NumberFormatException e) {
            System.out.println("You didn't enter a number, please try again");
        } catch (Exception e2) {
            System.out.println("Null message received");
        }
        return guess;
    }

    public void start() {
        try (Scanner input = new Scanner(System.in);) {
            System.out.println("Welcome to NumberGuesser4.0");
            System.out.println("To exit, type the word 'quit'.");
            System.out.println("Please select a difficulty: easy, medium, or hard");
            String diff = input.nextLine();
            if (diff.equalsIgnoreCase("easy") || diff.equalsIgnoreCase("medium")
            || diff.equalsIgnoreCase("hard")) {
                this.difficulty = diff.toLowerCase();
            } else {
                System.out.println("Invalid difficulty selected, defaulting to medium");
                this.difficulty = "medium";
            }
            setMaxStrikes();
            System.out.println("Difficulty set to: " + this.difficulty);

            boolean loaded = loadState();

            do {
                if (pickNewRandom) {
                    generateNewNumber(level);
                    saveState();
                    pickNewRandom = false;
                }
                System.out.println("Type a number and press enter");
                String message = input.nextLine();
                if (processCommands(message)) {
                    // command handled; don't proceed with game logic
                    break;
                }
                int guess = strToNum(message);
                processGuess(guess);
            } while (true);
        } catch (Exception e) {
            System.out.println("An unexpected error occurred. Goodbye.");
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        System.out.println("Thanks for playing!");
    }

    public static void main(String[] args) {
        NumberGuesser4 ng = new NumberGuesser4();
        ng.start();
    }
}
