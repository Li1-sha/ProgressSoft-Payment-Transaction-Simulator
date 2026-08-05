package com.progresssoft;

import java.util.InputMismatchException;
import java.util.Random;
import java.util.Scanner;

public class GuessingGame {
    private final int secretNum;
    private final Scanner scanner;

    public int getSecretNum() {
        return secretNum;
    }

    public GuessingGame(){
        Random random = new Random();
        this.scanner = new Scanner(System.in);
        this.secretNum = random.nextInt(100)+1;

    }
    public void startGame(){
        int userNum = 0;
        int attempts = 0;
        System.out.println("** Number Guessing game **");
        System.out.println("Guess the number (1-100): ");

        while (userNum != secretNum) {
            try {
                userNum = scanner.nextInt();
                attempts++;

                if (userNum < 1 || userNum > 100) {
                    System.out.println("Please enter a number between 1 and 100!");
                    continue;
                }

                if (userNum > secretNum) {
                    System.out.println("Too high! Try again: ");
                } else if (userNum < secretNum) {
                    System.out.println("Too low! Try again: ");
                } else {
                    System.out.println("Correct! You got it in " + attempts + " attempts!");
                }
            } catch (InputMismatchException e) {
                System.err.println("Error: Please enter a valid number!");
                scanner.nextLine(); // Clear invalid input
                System.out.println("Guess the number (1-100): ");
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                scanner.nextLine();
                System.out.println("Guess the number (1-100): ");
            }
        }
        scanner.close();
    }
}
