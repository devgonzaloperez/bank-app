package com.eazybytes.gatewayserver.controllers;

import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class FallbackController {
    
    //@RequestMapping("/contactSupport")
    @GetMapping("/contactSupport")
    public Mono<String> contactSupport(){
        return Mono.just("An error occurred. Please try after some time or contact support team!");
    }
    
}
