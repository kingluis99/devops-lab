package com.mikelam.taskapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * Useful during the Kubernetes labs: hit this endpoint repeatedly and watch the
 * hostname change as the Service load-balances across pod replicas.
 */
@RestController
public class InfoController {

    @Value("${app.build-version:dev}")
    private String buildVersion;

    @Value("${app.environment:local}")
    private String environment;

    @GetMapping("/api/info")
    public Map<String, String> info() {
        return Map.of(
                "buildVersion", buildVersion,
                "environment", environment,
                "hostname", hostname()
        );
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
