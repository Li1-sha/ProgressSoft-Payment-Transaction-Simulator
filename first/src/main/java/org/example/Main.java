package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
     static void main(String[] args) {
         GradeTracker tracker = new GradeTracker();
         tracker.enterStud();
         System.out.println("Class average: "+tracker.calcAvg());
         System.out.println("Top student: "+tracker.findTopStudent());

    }
}
