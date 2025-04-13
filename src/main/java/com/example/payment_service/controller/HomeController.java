package com.example.payment_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @GetMapping
    public String index() {
        logger.info("Accessed index page");
        return "index";
    }

    @GetMapping("/success")
    public String success() {
        logger.info("Accessed success page");
        return "success";
    }
}
