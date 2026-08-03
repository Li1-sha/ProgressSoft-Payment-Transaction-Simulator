package org.example;

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
        double sum = 0;
        for(Student s : classroom)
        {
            sum += s.getGrade();
        }
        return sum / classroom.size();

    }
    public String findTopStudent(){
        Student topStudent = classroom.get(0);
        for(Student s: classroom){
            if(s.getGrade()> topStudent.getGrade()){
                topStudent = s;
            }
        }
        return topStudent.getName();
    }
}
