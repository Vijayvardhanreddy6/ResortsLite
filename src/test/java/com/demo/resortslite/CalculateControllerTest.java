package com.demo.resortslite;

import com.demo.resortslite.dto.CalculateRequest;
import com.demo.resortslite.service.CalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalculateController.class)
public class CalculateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CalculationService calculationService;

    @Test
    public void testCalculateAddition() throws Exception {
        CalculateRequest request = new CalculateRequest("add", 5.0, 3.0);
        when(calculationService.calculate("add", 5.0, 3.0)).thenReturn(8.0);

        mockMvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(8.0));
    }

    @Test
    public void testCalculateSubtraction() throws Exception {
        CalculateRequest request = new CalculateRequest("subtract", 10.0, 4.0);
        when(calculationService.calculate("subtract", 10.0, 4.0)).thenReturn(6.0);

        mockMvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(6.0));
    }

    @Test
    public void testCalculateMultiplication() throws Exception {
        CalculateRequest request = new CalculateRequest("multiply", 6.0, 7.0);
        when(calculationService.calculate("multiply", 6.0, 7.0)).thenReturn(42.0);

        mockMvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(42.0));
    }

    @Test
    public void testCalculateDivision() throws Exception {
        CalculateRequest request = new CalculateRequest("divide", 20.0, 4.0);
        when(calculationService.calculate("divide", 20.0, 4.0)).thenReturn(5.0);

        mockMvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(5.0));
    }

    @Test
    public void testCalculateDivisionByZero() throws Exception {
        CalculateRequest request = new CalculateRequest("divide", 10.0, 0.0);
        when(calculationService.calculate(eq("divide"), eq(10.0), eq(0.0)))
                .thenThrow(new com.demo.resortslite.exception.DivisionByZeroException("Cannot divide by zero"));

        mockMvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot divide by zero"));
    }

    @Test
    public void testCalculateInvalidOperation() throws Exception {
        CalculateRequest request = new CalculateRequest("invalid", 5.0, 3.0);
        when(calculationService.calculate(eq("invalid"), eq(5.0), eq(3.0)))
                .thenThrow(new com.demo.resortslite.exception.InvalidOperationException("Invalid operation: invalid. Supported operations are: add, subtract, multiply, divide"));

        mockMvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testCalculateWithNullNumbers() throws Exception {
        CalculateRequest request = new CalculateRequest("add", null, 3.0);
        when(calculationService.calculate(eq("add"), any(), eq(3.0)))
                .thenThrow(new com.demo.resortslite.exception.InvalidNumberException("Both number1 and number2 must be provided and valid"));

        mockMvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testCalculateWithNegativeNumbers() throws Exception {
        CalculateRequest request = new CalculateRequest("add", -5.0, -3.0);
        when(calculationService.calculate("add", -5.0, -3.0)).thenReturn(-8.0);

        mockMvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(-8.0));
    }

    @Test
    public void testCalculateWithDecimalNumbers() throws Exception {
        CalculateRequest request = new CalculateRequest("multiply", 2.5, 4.0);
        when(calculationService.calculate("multiply", 2.5, 4.0)).thenReturn(10.0);

        mockMvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(10.0));
    }
}
