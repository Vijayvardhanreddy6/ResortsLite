package com.demo.resortslite;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/calculate")
public class CalculatorController {

    @PostMapping("/add")
    public Map<String, Object> addNumbers(
            @RequestParam(required = false) Double number1,
            @RequestParam(required = false) Double number2) {

        Map<String, Object> response = new HashMap<>();

        // Input validation - ensure both parameters are provided
        if (number1 == null || number2 == null) {
            response.put("error", "Both number1 and number2 parameters are required");
            return response;
        }

        // Perform addition operation
        double result = number1 + number2;

        // Build response with operation details
        response.put("operation", "addition");
        response.put("number1", number1);
        response.put("number2", number2);
        response.put("result", result);

        return response;
    }
}
