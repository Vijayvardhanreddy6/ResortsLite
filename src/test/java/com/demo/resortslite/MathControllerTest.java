package com.demo.resortslite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MathController.class)
public class MathControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testAddValidIntegers() throws Exception {
        mockMvc.perform(post("/api/add")
                        .param("num1", "5")
                        .param("num2", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(8.0));
    }

    @Test
    public void testAddValidDecimals() throws Exception {
        mockMvc.perform(post("/api/add")
                        .param("num1", "5.5")
                        .param("num2", "3.2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(8.7));
    }

    @Test
    public void testAddNegativeNumbers() throws Exception {
        mockMvc.perform(post("/api/add")
                        .param("num1", "-5")
                        .param("num2", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(-2.0));
    }

    @Test
    public void testAddWithZero() throws Exception {
        mockMvc.perform(post("/api/add")
                        .param("num1", "0")
                        .param("num2", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(5.0));
    }

    @Test
    public void testAddLargeNumbers() throws Exception {
        mockMvc.perform(post("/api/add")
                        .param("num1", "999999999")
                        .param("num2", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(1000000000.0));
    }

    @Test
    public void testAddMissingNum1Parameter() throws Exception {
        mockMvc.perform(post("/api/add")
                        .param("num2", "3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddMissingNum2Parameter() throws Exception {
        mockMvc.perform(post("/api/add")
                        .param("num1", "5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddNonNumericInput() throws Exception {
        mockMvc.perform(post("/api/add")
                        .param("num1", "abc")
                        .param("num2", "3"))
                .andExpect(status().isBadRequest());
    }
}
