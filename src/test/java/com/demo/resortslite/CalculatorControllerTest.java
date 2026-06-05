package com.demo.resortslite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalculatorController.class)
public class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalculatorService calculatorService;

    @Test
    public void testCalculateAddition() throws Exception {
        when(calculatorService.isValidOperation("add")).thenReturn(true);
        when(calculatorService.calculate(5.0, 3.0, "add")).thenReturn(8.0);

        mockMvc.perform(post("/api/calculate")
                .param("num1", "5.0")
                .param("num2", "3.0")
                .param("operation", "add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(8.0))
                .andExpect(jsonPath("$.operation").value("add"));
    }

    @Test
    public void testCalculateSubtraction() throws Exception {
        when(calculatorService.isValidOperation("subtract")).thenReturn(true);
        when(calculatorService.calculate(10.0, 4.0, "subtract")).thenReturn(6.0);

        mockMvc.perform(post("/api/calculate")
                .param("num1", "10.0")
                .param("num2", "4.0")
                .param("operation", "subtract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(6.0))
                .andExpect(jsonPath("$.operation").value("subtract"));
    }

    @Test
    public void testCalculateMultiplication() throws Exception {
        when(calculatorService.isValidOperation("multiply")).thenReturn(true);
        when(calculatorService.calculate(6.0, 7.0, "multiply")).thenReturn(42.0);

        mockMvc.perform(post("/api/calculate")
                .param("num1", "6.0")
                .param("num2", "7.0")
                .param("operation", "multiply"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(42.0))
                .andExpect(jsonPath("$.operation").value("multiply"));
    }

    @Test
    public void testCalculateDivision() throws Exception {
        when(calculatorService.isValidOperation("divide")).thenReturn(true);
        when(calculatorService.calculate(20.0, 4.0, "divide")).thenReturn(5.0);

        mockMvc.perform(post("/api/calculate")
                .param("num1", "20.0")
                .param("num2", "4.0")
                .param("operation", "divide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(5.0))
                .andExpect(jsonPath("$.operation").value("divide"));
    }

    @Test
    public void testDivisionByZero() throws Exception {
        when(calculatorService.isValidOperation("divide")).thenReturn(true);
        when(calculatorService.calculate(10.0, 0.0, "divide"))
                .thenThrow(new ArithmeticException("Cannot divide by zero"));

        mockMvc.perform(post("/api/calculate")
                .param("num1", "10.0")
                .param("num2", "0.0")
                .param("operation", "divide"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot divide by zero"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    public void testInvalidOperation() throws Exception {
        when(calculatorService.isValidOperation("invalid")).thenReturn(false);

        mockMvc.perform(post("/api/calculate")
                .param("num1", "5.0")
                .param("num2", "3.0")
                .param("operation", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    public void testInvalidNumberParameter() throws Exception {
        mockMvc.perform(post("/api/calculate")
                .param("num1", "invalid")
                .param("num2", "3.0")
                .param("operation", "add"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testMissingParameters() throws Exception {
        mockMvc.perform(post("/api/calculate")
                .param("num1", "5.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNegativeNumbers() throws Exception {
        when(calculatorService.isValidOperation("add")).thenReturn(true);
        when(calculatorService.calculate(-5.0, 3.0, "add")).thenReturn(-2.0);

        mockMvc.perform(post("/api/calculate")
                .param("num1", "-5.0")
                .param("num2", "3.0")
                .param("operation", "add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(-2.0))
                .andExpect(jsonPath("$.operation").value("add"));
    }

    @Test
    public void testCaseInsensitiveOperation() throws Exception {
        when(calculatorService.isValidOperation("ADD")).thenReturn(true);
        when(calculatorService.calculate(5.0, 3.0, "ADD")).thenReturn(8.0);

        mockMvc.perform(post("/api/calculate")
                .param("num1", "5.0")
                .param("num2", "3.0")
                .param("operation", "ADD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(8.0))
                .andExpect(jsonPath("$.operation").value("add"));
    }
}
