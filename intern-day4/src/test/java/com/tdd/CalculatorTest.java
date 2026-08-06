package com.tdd;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CalculatorTest {

    @Test
    void testAdd() {
        Calculator calc = new Calculator();
        int result = calc.add(2, 3);
        assertEquals(5, result);
    }

    @Test
    void testAddTwoNegativeNumbers() {
        Calculator calc = new Calculator();
        int result = calc.add(-5, -3);
        assertEquals(-8, result);
    }

    @Test
    void testSubtract() {
        Calculator calc = new Calculator();
        int result = calc.subtract(10, 4);
        assertEquals(6, result);
    }

    @Test
    void testSubtractTwoNegativeNumbers() {
        Calculator calc = new Calculator();
        int result = calc.subtract(-10,-4);
        assertEquals(-6, result);
    }

    @Test
    void testMultiply(){
        Calculator calc = new Calculator();
        int result = calc.multiply(3,4);
        assertEquals(12,result);
    }

    @Test
    void testDivide(){
        Calculator calc = new Calculator();
        int result = calc.divide(10,2);
        assertEquals(5, result);
    }

    @Test
    void testDivideByZero(){
        Calculator calc = new Calculator();
        // Decision: throw ArithmeticException instead of returning a sentinel
        // because it clearly signals an error and matches Java's built-in behavior.
        assertThrows(ArithmeticException.class, () -> calc.divide(5, 0));
    }
}