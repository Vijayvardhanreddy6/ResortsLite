package com.demo.resortslite.dto;

public class CalculateRequest {
    private String operation;
    private Double number1;
    private Double number2;

    public CalculateRequest() {
    }

    public CalculateRequest(String operation, Double number1, Double number2) {
        this.operation = operation;
        this.number1 = number1;
        this.number2 = number2;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Double getNumber1() {
        return number1;
    }

    public void setNumber1(Double number1) {
        this.number1 = number1;
    }

    public Double getNumber2() {
        return number2;
    }

    public void setNumber2(Double number2) {
        this.number2 = number2;
    }
}
