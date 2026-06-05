package com.demo.resortslite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CalculatorServiceTest {

    private CalculatorService calculatorService;

    @BeforeEach
    public void setUp() {
        calculatorService = new CalculatorService();
    }

    @Test
    public void testAddition() {
        double result = calculatorService.calculate(5.0, 3.0, "add");
        assertEquals(8.0, result, 0.001);
    }

    @Test
    public void testSubtraction() {
        double result = calculatorService.calculate(10.0, 4.0, "subtract");
        assertEquals(6.0, result, 0.001);
    }

    @Test
    public void testMultiplication() {
        double result = calculatorService.calculate(6.0, 7.0, "multiply");
        assertEquals(42.0, result, 0.001);
    }

    @Test
    public void testDivision() {
        double result = calculatorService.calculate(20.0, 4.0, "divide");
        assertEquals(5.0, result, 0.001);
    }

    @Test
    public void testDivisionByZero() {
        ArithmeticException exception = assertThrows(ArithmeticException.class, () -> {
            calculatorService.calculate(10.0, 0.0, "divide");
        });
        assertEquals("Cannot divide by zero", exception.getMessage());
    }

    @Test
    public void testInvalidOperation() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            calculatorService.calculate(5.0, 3.0, "invalid");
        });
        assertTrue(exception.getMessage().contains("Invalid operation"));
    }

    @Test
    public void testIsValidOperation() {
        assertTrue(calculatorService.isValidOperation("add"));
        assertTrue(calculatorService.isValidOperation("subtract"));
        assertTrue(calculatorService.isValidOperation("multiply"));
        assertTrue(calculatorService.isValidOperation("divide"));
        assertTrue(calculatorService.isValidOperation("ADD"));
        assertTrue(calculatorService.isValidOperation("SUBTRACT"));
        assertFalse(calculatorService.isValidOperation("invalid"));
        assertFalse(calculatorService.isValidOperation(null));
    }

    @Test
    public void testNegativeNumbers() {
        double result = calculatorService.calculate(-5.0, 3.0, "add");
        assertEquals(-2.0, result, 0.001);

        result = calculatorService.calculate(-10.0, -5.0, "multiply");
        assertEquals(50.0, result, 0.001);
    }

    @Test
    public void testDecimalNumbers() {
        double result = calculatorService.calculate(5.5, 2.5, "add");
        assertEquals(8.0, result, 0.001);

        result = calculatorService.calculate(10.0, 3.0, "divide");
        assertEquals(3.333, result, 0.001);
    }
}
