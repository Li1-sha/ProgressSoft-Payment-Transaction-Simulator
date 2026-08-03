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

        while (userNum != secretNum){
            userNum = scanner.nextInt();
            if (userNum > secretNum) {
            }else if (userNum < secretNum){
            }else{
            }
        }
        scanner.close();
    }
}
