package com.winrisk.web;

import com.winrisk.game.HeadlessCli;
import com.winrisk.game.Play;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

/**
 * Web server entry point. {@code --headless-play} retains CLI parity: the same
 * jar can run a headless simulation instead of starting the server.
 */
@SpringBootApplication
@EnableScheduling
public class WinRiskApplication {

    public static void main(String[] args) {
        boolean headless = args != null && Arrays.stream(args)
                .anyMatch(arg -> "--headless-play".equalsIgnoreCase(arg)
                        || "headless-play".equalsIgnoreCase(arg));
        if (headless) {
            HeadlessCli.HeadlessConfig config = HeadlessCli.buildHeadlessConfig(args);
            System.out.println(new Play(config.getParams(), config.getMaxTurns())
                    .playResult()
                    .toReport());
            return;
        }
        SpringApplication.run(WinRiskApplication.class, args);
    }
}
