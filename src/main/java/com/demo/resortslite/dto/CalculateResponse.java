package com.demo.resortslite.dto;

public class CalculateResponse {
    private Double result;

    public CalculateResponse() {
    }

    public CalculateResponse(Double result) {
        this.result = result;
    }

    public Double getResult() {
        return result;
    }

    public void setResult(Double result) {
        this.result = result;
    }
}
