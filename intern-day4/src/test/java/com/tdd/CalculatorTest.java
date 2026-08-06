package com.tdd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

public class CalculatorTest {

    private Calculator calc;

    @BeforeEach //This eliminates repeated new Calculator() in every test
    void setUp() {
        calc = new Calculator();   // fresh instance for each test
    }

    @ParameterizedTest
    @CsvSource({
            "2, 3, 5",
            "-5, -3, -8",
            "5, -3, 2",
            "-5, 3, -2",
            "0, 5, 5",
            "0, -5, -5",
            "0, 0, 0"
    })
    void testAdd() {
        int result = calc.add(2, 3);
        assertEquals(5, result);
    }

    @ParameterizedTest
    @CsvSource({
            "10, 4, 6",
            "-10, -4, -6",
            "5, -3, 8",
            "-5, 3, -8",
            "5, 0, 5",
            "0, 5, -5",
            "0, 0, 0"
    })
    void testSubtract() {
        int result = calc.subtract(10, 4);
        assertEquals(6, result);
    }

    @Test
    void testMultiply(){
        int result = calc.multiply(3,4);
        assertEquals(12,result);
    }

    @Test
    void testDivide(){
        int result = calc.divide(10,2);
        assertEquals(5, result);
    }

    @Test
    void testDivideByZero(){
        // Decision: throw ArithmeticException instead of returning a sentinel
        // because it clearly signals an error and matches Java's built-in behavior.
        assertThrows(ArithmeticException.class, () -> calc.divide(5, 0));
    }
}