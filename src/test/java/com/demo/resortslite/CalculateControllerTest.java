package com.demo.resortslite;

import com.demo.resortslite.dto.AdditionRequest;
import com.demo.resortslite.dto.AdditionResponse;
import com.demo.resortslite.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalculateController.class)
public class CalculateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testAddition_ValidIntegers() throws Exception {
        AdditionRequest request = new AdditionRequest(5.0, 3.0);

        MvcResult result = mockMvc.perform(post("/api/calculate/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(8.0))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AdditionResponse response = objectMapper.readValue(responseBody, AdditionResponse.class);
        assertEquals(8.0, response.getResult());
    }

    @Test
    public void testAddition_ValidDecimals() throws Exception {
        AdditionRequest request = new AdditionRequest(3.5, 2.7);

        MvcResult result = mockMvc.perform(post("/api/calculate/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(6.2))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AdditionResponse response = objectMapper.readValue(responseBody, AdditionResponse.class);
        assertEquals(6.2, response.getResult(), 0.0001);
    }

    @Test
    public void testAddition_NegativeNumbers() throws Exception {
        AdditionRequest request = new AdditionRequest(-10.0, 5.0);

        mockMvc.perform(post("/api/calculate/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(-5.0));
    }

    @Test
    public void testAddition_BothNegative() throws Exception {
        AdditionRequest request = new AdditionRequest(-7.0, -3.0);

        mockMvc.perform(post("/api/calculate/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(-10.0));
    }

    @Test
    public void testAddition_Zero() throws Exception {
        AdditionRequest request = new AdditionRequest(0.0, 5.0);

        mockMvc.perform(post("/api/calculate/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(5.0));
    }

    @Test
    public void testAddition_LargeNumbers() throws Exception {
        AdditionRequest request = new AdditionRequest(1000000.0, 2000000.0);

        mockMvc.perform(post("/api/calculate/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(3000000.0));
    }

    @Test
    public void testAddition_MissingNumber1() throws Exception {
        String requestJson = "{\"number2\": 5.0}";

        mockMvc.perform(post("/api/calculate/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    public void testAddition_MissingNumber2() throws Exception {
        String requestJson = "{\"number1\": 5.0}";

        mockMvc.perform(post("/api/calculate/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    public void testAddition_EmptyBody() throws Exception {
        String requestJson = "{}";

        mockMvc.perform(post("/api/calculate/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    public void testAddition_InvalidJson() throws Exception {
        String requestJson = "{invalid json}";

        mockMvc.perform(post("/api/calculate/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddition_NullValues() throws Exception {
        String requestJson = "{\"number1\": null, \"number2\": null}";

        mockMvc.perform(post("/api/calculate/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    public void testAddition_VerySmallDecimals() throws Exception {
        AdditionRequest request = new AdditionRequest(0.0001, 0.0002);

        mockMvc.perform(post("/api/calculate/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(0.0003));
    }
}
