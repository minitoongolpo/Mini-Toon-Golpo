package com.minitoon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InstagramController {

    @Value("${INSTAGRAM_ACCOUNT_ID:NOT_FOUND}")
    private String instagramId;

    @Value("${INSTAGRAM_ACCESS_TOKEN:NOT_FOUND}")
    private String accessToken;

    @GetMapping("/instagram-status")
    public String instagramStatus() {

        return "Instagram Connected! ID = " + instagramId;
    }
}
