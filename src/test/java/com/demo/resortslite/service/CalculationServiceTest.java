package com.demo.resortslite.service;

import com.demo.resortslite.exception.DivisionByZeroException;
import com.demo.resortslite.exception.InvalidNumberException;
import com.demo.resortslite.exception.InvalidOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CalculationServiceTest {

    private CalculationService calculationService;

    @BeforeEach
    public void setUp() {
        calculationService = new CalculationService();
    }

    @Test
    public void testAddition() {
        Double result = calculationService.calculate("add", 5.0, 3.0);
        assertEquals(8.0, result);
    }

    @Test
    public void testSubtraction() {
        Double result = calculationService.calculate("subtract", 10.0, 4.0);
        assertEquals(6.0, result);
    }

    @Test
    public void testMultiplication() {
        Double result = calculationService.calculate("multiply", 6.0, 7.0);
        assertEquals(42.0, result);
    }

    @Test
    public void testDivision() {
        Double result = calculationService.calculate("divide", 20.0, 4.0);
        assertEquals(5.0, result);
    }

    @Test
    public void testDivisionByZero() {
        Exception exception = assertThrows(DivisionByZeroException.class, () -> {
            calculationService.calculate("divide", 10.0, 0.0);
        });
        assertEquals("Cannot divide by zero", exception.getMessage());
    }

    @Test
    public void testInvalidOperation() {
        Exception exception = assertThrows(InvalidOperationException.class, () -> {
            calculationService.calculate("invalid", 5.0, 3.0);
        });
        assertTrue(exception.getMessage().contains("Invalid operation"));
    }

    @Test
    public void testNullNumber1() {
        Exception exception = assertThrows(InvalidNumberException.class, () -> {
            calculationService.calculate("add", null, 3.0);
        });
        assertTrue(exception.getMessage().contains("must be provided and valid"));
    }

    @Test
    public void testNullNumber2() {
        Exception exception = assertThrows(InvalidNumberException.class, () -> {
            calculationService.calculate("add", 5.0, null);
        });
        assertTrue(exception.getMessage().contains("must be provided and valid"));
    }

    @Test
    public void testNullOperation() {
        Exception exception = assertThrows(InvalidOperationException.class, () -> {
            calculationService.calculate(null, 5.0, 3.0);
        });
        assertEquals("Operation must be provided", exception.getMessage());
    }

    @Test
    public void testEmptyOperation() {
        Exception exception = assertThrows(InvalidOperationException.class, () -> {
            calculationService.calculate("", 5.0, 3.0);
        });
        assertEquals("Operation must be provided", exception.getMessage());
    }

    @Test
    public void testCaseInsensitiveOperation() {
        Double result1 = calculationService.calculate("ADD", 5.0, 3.0);
        assertEquals(8.0, result1);

        Double result2 = calculationService.calculate("Multiply", 6.0, 7.0);
        assertEquals(42.0, result2);
    }

    @Test
    public void testNegativeNumbers() {
        Double result = calculationService.calculate("add", -5.0, -3.0);
        assertEquals(-8.0, result);
    }

    @Test
    public void testDecimalNumbers() {
        Double result = calculationService.calculate("multiply", 2.5, 4.0);
        assertEquals(10.0, result);
    }
}
