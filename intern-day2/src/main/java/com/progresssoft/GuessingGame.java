package com.progresssoft;

import java.util.Random;
import java.util.Scanner;

public class GuessingGame {
    private final int secretNum;
    private final Scanner scanner;

    public GuessingGame(){
        Random random = new Random();
        this.scanner = new Scanner(System.in);
        this.secretNum = random.nextInt(100)+1;

    }
    public void startGame(){
        int userNum = 0;
        System.out.println("** Number Guessing game **");
        System.out.println("Guess the number: ");

        while (userNum != secretNum){
            userNum = scanner.nextInt();
            if (userNum > secretNum) {
                System.out.println("too high");
            }else if (userNum < secretNum){
                System.out.println("too low");
            }else{
                System.out.println("Correct!");
            }
        }
        scanner.close();
    }
}
