package com.demo.resortslite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalculatorController.class)
public class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testAddNumbers_ValidInputs() throws Exception {
        mockMvc.perform(post("/api/calculate/add")
                .param("number1", "5.0")
                .param("number2", "3.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("addition"))
                .andExpect(jsonPath("$.number1").value(5.0))
                .andExpect(jsonPath("$.number2").value(3.0))
                .andExpect(jsonPath("$.result").value(8.0));
    }

    @Test
    public void testAddNumbers_NegativeNumbers() throws Exception {
        mockMvc.perform(post("/api/calculate/add")
                .param("number1", "-10.5")
                .param("number2", "3.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("addition"))
                .andExpect(jsonPath("$.number1").value(-10.5))
                .andExpect(jsonPath("$.number2").value(3.5))
                .andExpect(jsonPath("$.result").value(-7.0));
    }

    @Test
    public void testAddNumbers_DecimalValues() throws Exception {
        mockMvc.perform(post("/api/calculate/add")
                .param("number1", "2.25")
                .param("number2", "3.75"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("addition"))
                .andExpect(jsonPath("$.result").value(6.0));
    }

    @Test
    public void testAddNumbers_ZeroValues() throws Exception {
        mockMvc.perform(post("/api/calculate/add")
                .param("number1", "0")
                .param("number2", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(0.0));
    }

    @Test
    public void testAddNumbers_MissingNumber1() throws Exception {
        mockMvc.perform(post("/api/calculate/add")
                .param("number2", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("Both number1 and number2 parameters are required"));
    }

    @Test
    public void testAddNumbers_MissingNumber2() throws Exception {
        mockMvc.perform(post("/api/calculate/add")
                .param("number1", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("Both number1 and number2 parameters are required"));
    }

    @Test
    public void testAddNumbers_BothParametersMissing() throws Exception {
        mockMvc.perform(post("/api/calculate/add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("Both number1 and number2 parameters are required"));
    }

    @Test
    public void testAddNumbers_LargeNumbers() throws Exception {
        mockMvc.perform(post("/api/calculate/add")
                .param("number1", "999999.99")
                .param("number2", "0.01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("addition"))
                .andExpect(jsonPath("$.result").value(1000000.0));
    }
}
