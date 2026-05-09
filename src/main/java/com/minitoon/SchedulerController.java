package com.minitoon;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SchedulerController {

    @Scheduled(fixedRate = 3600000)
    public void autoGenerate() {

        System.out.println("AI Auto Video Generation Started!");
    }
}
