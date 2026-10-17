package com.soundmarketplace.sound_marketplace;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

public class DisabledIfDockerUnavailableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                return ConditionEvaluationResult.enabled("Docker environment is available");
            }
        } catch (Throwable ignored) {
        }
        return ConditionEvaluationResult.disabled("Docker environment is unavailable; skipping Testcontainers test.");
    }
}
