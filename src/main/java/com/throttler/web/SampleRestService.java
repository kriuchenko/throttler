package com.throttler.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleRestService {
    @GetMapping("/hi")
    public String hi(@RequestParam(value = "token", required = false) String token){
        return "Hi " + token;
    }
}
