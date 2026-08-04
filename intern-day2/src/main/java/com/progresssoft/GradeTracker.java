package com.progresssoft;

import java.util.ArrayList;
import java.util.Scanner;

public class GradeTracker {

    private ArrayList<Student> classroom;

    public GradeTracker() {
        classroom = new ArrayList<>();
    }

    public void enterStud(Student student) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Enter student name ('end' to exit):");
            String name = scanner.nextLine().trim();

            if (name.equalsIgnoreCase("end")) {
                break;
            }
            System.out.println("Enter " + name + "'s grade:");
            double grade = scanner.nextDouble();
            scanner.nextLine();
            classroom.add(new Student(name, grade));
            System.out.println("Added.");
        }
        System.out.println("Total students recorded = " + classroom.size() + "\n");
    }

    public double calcAvg() {
        if (classroom.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (Student s : classroom) {
            sum += s.getGrade();
        }
        return sum / classroom.size();
    }

    public String findTopStudent() {
        if (classroom.isEmpty()) {
            return "No students available";
        }

        Student topStudent = classroom.get(0);
        for (Student s : classroom) {
            if (s.getGrade() > topStudent.getGrade()) {
                topStudent = s;
            }
        }
        return topStudent.getName();
    }

    public int getStudentCount() {
        return classroom.size();
    }

    public boolean isEmpty() {
        return classroom.isEmpty();
    }

    public void addStudent(Student student) {
        if (student == null) {
            return; // Test expects to silently ignore and return without adding
        }
        classroom.add(student);
    }

    public void addStudent(String name, double grade) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        if (grade < 0 || grade > 100) {
            return;
        }
        classroom.add(new Student(name, grade));
    }

    public Student getStudent(int index) {
        try {
            return classroom.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public void clearAllStudents() {
        classroom.clear();
    }
}