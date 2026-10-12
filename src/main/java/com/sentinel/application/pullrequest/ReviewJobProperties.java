package com.sentinel.application.pullrequest;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param retention how long finished jobs stay available for polling
 */
@ConfigurationProperties(prefix = "sentinel.pull-request.jobs")
public record ReviewJobProperties(@DefaultValue("24h") Duration retention) {
}