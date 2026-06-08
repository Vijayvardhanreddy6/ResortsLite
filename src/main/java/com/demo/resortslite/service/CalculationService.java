package com.demo.resortslite.service;

import com.demo.resortslite.exception.DivisionByZeroException;
import com.demo.resortslite.exception.InvalidNumberException;
import com.demo.resortslite.exception.InvalidOperationException;
import org.springframework.stereotype.Service;

@Service
public class CalculationService {

    public Double calculate(String operation, Double number1, Double number2) {
        // Validate inputs
        if (number1 == null || number2 == null) {
            throw new InvalidNumberException("Both number1 and number2 must be provided and valid");
        }

        if (operation == null || operation.trim().isEmpty()) {
            throw new InvalidOperationException("Operation must be provided");
        }

        // Perform calculation based on operation type
        switch (operation.toLowerCase()) {
            case "add":
                return number1 + number2;
            case "subtract":
                return number1 - number2;
            case "multiply":
                return number1 * number2;
            case "divide":
                if (number2 == 0.0) {
                    throw new DivisionByZeroException("Cannot divide by zero");
                }
                return number1 / number2;
            default:
                throw new InvalidOperationException("Invalid operation: " + operation + ". Supported operations are: add, subtract, multiply, divide");
        }
    }
}
