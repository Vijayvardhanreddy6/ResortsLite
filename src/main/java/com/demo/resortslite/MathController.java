package com.demo.resortslite;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MathController {

    @PostMapping("/add")
    public Map<String, Object> add(
            @RequestParam(required = true) Double num1,
            @RequestParam(required = true) Double num2) {

        Map<String, Object> response = new HashMap<>();
        response.put("result", num1 + num2);
        return response;
    }
}
