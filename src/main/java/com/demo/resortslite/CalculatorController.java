package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/calculate")
public class CalculatorController {

    @Autowired
    private CalculatorService calculatorService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> calculate(
            @RequestParam double num1,
            @RequestParam double num2,
            @RequestParam String operation) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate operation
            if (!calculatorService.isValidOperation(operation)) {
                response.put("error", "Invalid operation: " + operation + ". Supported operations are: add, subtract, multiply, divide");
                response.put("statusCode", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Perform calculation
            double result = calculatorService.calculate(num1, num2, operation);

            // Success response
            response.put("result", result);
            response.put("operation", operation.toLowerCase());
            return ResponseEntity.ok(response);

        } catch (ArithmeticException e) {
            // Handle division by zero
            response.put("error", e.getMessage());
            response.put("statusCode", 400);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            // Handle unexpected errors
            response.put("error", "An error occurred: " + e.getMessage());
            response.put("statusCode", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
