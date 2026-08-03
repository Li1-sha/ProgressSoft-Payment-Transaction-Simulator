package com.progresssoft;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
     public static void main(String[] args) {
         //GuessingGame game = new GuessingGame();
         //game.startGame();
         GradeTracker tracker = new GradeTracker();
         System.out.println("Class average: "+tracker.calcAvg());
         System.out.println("Top student: "+tracker.findTopStudent());

    }
}
