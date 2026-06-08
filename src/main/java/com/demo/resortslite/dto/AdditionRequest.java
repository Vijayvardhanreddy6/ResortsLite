package com.demo.resortslite.dto;

import javax.validation.constraints.NotNull;

public class AdditionRequest {

    @NotNull(message = "number1 is required")
    private Double number1;

    @NotNull(message = "number2 is required")
    private Double number2;

    public AdditionRequest() {
    }

    public AdditionRequest(Double number1, Double number2) {
        this.number1 = number1;
        this.number2 = number2;
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
