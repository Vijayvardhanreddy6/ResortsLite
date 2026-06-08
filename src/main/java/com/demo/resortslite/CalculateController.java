package com.demo.resortslite;

import com.demo.resortslite.dto.CalculateRequest;
import com.demo.resortslite.dto.CalculateResponse;
import com.demo.resortslite.service.CalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CalculateController {

    @Autowired
    private CalculationService calculationService;

    @PostMapping("/calculate")
    public CalculateResponse calculate(@RequestBody CalculateRequest request) {
        Double result = calculationService.calculate(
            request.getOperation(),
            request.getNumber1(),
            request.getNumber2()
        );
        return new CalculateResponse(result);
    }
}
