package com.progresssoft;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GradeTrackerTest {
    private GradeTracker tracker;

    @Before
    public void setUp() {
        tracker = new GradeTracker();
    }

    @Test
    public void testCalcAvgWithEmptyRoster() {
        // Should catch exception and return 0.0
        double avg = tracker.calcAvg();
        assertEquals(0.0, avg, 0.001);
    }

    @Test
    public void testFindTopStudentWithEmptyRoster() {
        // Should catch exception and return "No students available"
        String result = tracker.findTopStudent();
        assertEquals("No students available", result);
    }

    @Test
    public void testCalcAvgWithValidData() {
        tracker.addStudent("Alice", 90.0);
        tracker.addStudent("Bob", 85.0);
        tracker.addStudent("Charlie", 95.0);
        double avg = tracker.calcAvg();
        assertEquals(90.0, avg, 0.001);
    }

    @Test
    public void testFindTopStudentWithValidData() {
        tracker.addStudent("Alice", 90.0);
        tracker.addStudent("Bob", 85.0);
        tracker.addStudent("Charlie", 95.0);
        String top = tracker.findTopStudent();
        assertEquals("Charlie", top);
    }

    @Test
    public void testAddStudent() {
        tracker.addStudent("David", 88.5);
        assertEquals(1, tracker.getStudentCount());
        assertFalse(tracker.isEmpty());
    }

    @Test
    public void testAddNullStudent() {
        tracker.addStudent((Student) null);
        assertEquals(0, tracker.getStudentCount());
    }

    @Test
    public void testAddStudentWithInvalidName() {
        tracker.addStudent("", 85.0);
        assertEquals(0, tracker.getStudentCount());

        tracker.addStudent(null, 85.0);
        assertEquals(0, tracker.getStudentCount());
    }

    @Test
    public void testAddStudentWithInvalidGrade() {
        tracker.addStudent("Eve", -10.0);
        assertEquals(0, tracker.getStudentCount());

        tracker.addStudent("Eve", 110.0);
        assertEquals(0, tracker.getStudentCount());
    }

    @Test
    public void testGetStudentCount() {
        assertEquals(0, tracker.getStudentCount());
        tracker.addStudent("Alice", 90.0);
        assertEquals(1, tracker.getStudentCount());
        tracker.addStudent("Bob", 85.0);
        assertEquals(2, tracker.getStudentCount());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(tracker.isEmpty());
        tracker.addStudent("Alice", 90.0);
        assertFalse(tracker.isEmpty());
    }
}