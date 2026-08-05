package com.progresssoft;

import org.junit.Test;

import static org.junit.Assert.*;

public class StudentTest {

    @Test(expected = IllegalArgumentException.class)
    public void testStudentWithNullName() {
        new Student(null, 85.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStudentWithEmptyName() {
        new Student("", 85.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStudentWithNegativeGrade() {
        new Student("Alice", -5.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStudentWithGradeAbove100() {
        new Student("Bob", 105.0);
    }

    @Test
    public void testValidStudentCreation() {
        Student student = new Student("Alice", 95.5);
        assertEquals("Alice", student.getName());
        assertEquals(95.5, student.getGrade(), 0.001);
    }

    @Test
    public void testSetAndGetName() {
        Student student = new Student("Alice", 85.0);
        student.setName("Bob");
        assertEquals("Bob", student.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNameNull() {
        Student student = new Student("Alice", 85.0);
        student.setName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNameEmpty() {
        Student student = new Student("Alice", 85.0);
        student.setName("");
    }

    @Test
    public void testSetAndGetGrade() {
        Student student = new Student("Alice", 85.0);
        student.setGrade(95.0);
        assertEquals(95.0, student.getGrade(), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetGradeNegative() {
        Student student = new Student("Alice", 85.0);
        student.setGrade(-10.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetGradeAbove100() {
        Student student = new Student("Alice", 85.0);
        student.setGrade(110.0);
    }

    @Test
    public void testToString() {
        Student student = new Student("Alice", 95.5);
        String expected = "Student{name='Alice', grade=95.5}";
        assertEquals(expected, student.toString());
    }
}