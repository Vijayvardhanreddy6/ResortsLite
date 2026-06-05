package com.demo.resortslite;

import org.springframework.stereotype.Service;

@Service
public class CalculatorService {

    public double calculate(double num1, double num2, String operation) {
        switch (operation.toLowerCase()) {
            case "add":
                return num1 + num2;
            case "subtract":
                return num1 - num2;
            case "multiply":
                return num1 * num2;
            case "divide":
                if (num2 == 0) {
                    throw new ArithmeticException("Cannot divide by zero");
                }
                return num1 / num2;
            default:
                throw new IllegalArgumentException("Invalid operation: " + operation + ". Supported operations are: add, subtract, multiply, divide");
        }
    }

    public boolean isValidOperation(String operation) {
        if (operation == null) {
            return false;
        }
        String op = operation.toLowerCase();
        return op.equals("add") || op.equals("subtract") || op.equals("multiply") || op.equals("divide");
    }
}
