package com.demo.resortslite;

import com.demo.resortslite.dto.AdditionRequest;
import com.demo.resortslite.dto.AdditionResponse;
import com.demo.resortslite.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/calculate")
public class CalculateController {

    private static final Logger logger = LoggerFactory.getLogger(CalculateController.class);

    @PostMapping("/add")
    public ResponseEntity<?> add(@Valid @RequestBody AdditionRequest request, BindingResult bindingResult) {
        try {
            // Log the incoming request
            logger.info("Addition request received: number1={}, number2={}", request.getNumber1(), request.getNumber2());

            // Validate request
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "Invalid request parameters";
                logger.warn("Validation error: {}", errorMessage);
                ErrorResponse errorResponse = new ErrorResponse(errorMessage, "VALIDATION_ERROR");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Perform addition
            Double result = request.getNumber1() + request.getNumber2();
            logger.info("Addition successful: {} + {} = {}", request.getNumber1(), request.getNumber2(), result);

            // Return success response
            AdditionResponse response = new AdditionResponse(result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Log the error
            logger.error("Error processing addition request", e);

            // Return error response
            ErrorResponse errorResponse = new ErrorResponse("An unexpected error occurred", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
