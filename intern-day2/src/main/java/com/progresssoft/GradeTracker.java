package com.progresssoft;

import java.util.ArrayList;
import java.util.Scanner;

public class GradeTracker {

    private ArrayList<Student> classroom;

    public GradeTracker(){
        classroom = new ArrayList<>();

    }
    public void enterStud(){
        Scanner scanner = new Scanner(System.in);
        while (true){
            System.out.println("Enter student name ('end' to exit):");
            String name = scanner.nextLine().trim();

            if (name.equalsIgnoreCase("end")){
                break;
            }
            System.out.println("Enter "+name+"'s grade:");
            double grade = scanner.nextDouble();
            scanner.nextLine();
            classroom.add(new Student(name,grade));
            System.out.println("Added.");
        }
        System.out.println("Total students recorded = "+classroom.size()+"\n");
    }

    public double calcAvg(){
        try {
            double sum = 0;
            for (Student s : classroom) {
                sum += s.getGrade();
            }
            return sum / classroom.size();
        } catch (ArithmeticException | IndexOutOfBoundsException | NullPointerException e) {
            System.err.println("Error calculating average: " + e.getMessage());
            return 0.0;
        }

    }
    public String findTopStudent(){
        try {
            Student topStudent = classroom.get(0);
            for (Student s : classroom) {
                if (s.getGrade() > topStudent.getGrade()) {
                    topStudent = s;
                }
            }
            return topStudent.getName();
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            System.err.println("Error finding top student: " + e.getMessage());
            return "No students available";
        }
    }
}
