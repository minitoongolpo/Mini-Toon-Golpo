package com.minitoon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FacebookController {

    @Value("${FACEBOOK_PAGE_ID:NOT_FOUND}")
    private String pageId;

    @Value("${FACEBOOK_ACCESS_TOKEN:NOT_FOUND}")
    private String accessToken;

    @GetMapping("/facebook-status")
    public String facebookStatus() {

        return "Facebook Connected! Page ID = " + pageId;
    }
}
