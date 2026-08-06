package com.tdd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

public class CalculatorTest {

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
        Calculator calc = new Calculator();
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
        Calculator calc = new Calculator();
        int result = calc.subtract(10, 4);
        assertEquals(6, result);
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