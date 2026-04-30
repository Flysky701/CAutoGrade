package com.autograding.service;

import com.autograding.entity.SystemConfig;
import com.autograding.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    private final Map<String, Object> configCache = new ConcurrentHashMap<>();

    private static final Map<String, Map<String, Object>> DEFAULTS = new LinkedHashMap<>();

    static {
        Map<String, Object> llm = new LinkedHashMap<>();
        llm.put("provider", "deepseek");
        llm.put("apiKey", "");
        llm.put("model", "deepseek-chat");
        llm.put("temperature", 0.3);
        llm.put("maxTokens", 2048);
        llm.put("timeout", 30);

        Map<String, Object> sandbox = new LinkedHashMap<>();
        sandbox.put("compileTimeout", 5);
        sandbox.put("runTimeout", 5);
        sandbox.put("memoryLimitMB", 256);
        sandbox.put("maxConcurrent", 4);
        sandbox.put("enableNetwork", false);

        Map<String, Object> scoring = new LinkedHashMap<>();
        scoring.put("correctnessWeight", 60);
        scoring.put("styleWeight", 20);
        scoring.put("efficiencyWeight", 20);
        scoring.put("deviationThreshold", 15);
        scoring.put("fallbackToRules", true);

        DEFAULTS.put("llm", llm);
        DEFAULTS.put("sandbox", sandbox);
        DEFAULTS.put("scoring", scoring);
    }

    public SystemConfigService(SystemConfigMapper systemConfigMapper, ObjectMapper objectMapper) {
        this.systemConfigMapper = systemConfigMapper;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        for (Map.Entry<String, Map<String, Object>> entry : DEFAULTS.entrySet()) {
            String key = entry.getKey();
            SystemConfig existing = findByKey(key);
            if (existing != null) {
                configCache.put(key, deserialize(existing.getConfigValue()));
            } else {
                Map<String, Object> defaultValue = entry.getValue();
                persistToDb(key, defaultValue);
                configCache.put(key, defaultValue);
            }
        }
        log.info("System config initialized, keys: {}", configCache.keySet());
    }

    public Map<String, Object> getAllConfig() {
        return new LinkedHashMap<>(configCache);
    }

    @SuppressWarnings("unchecked")
    public void updateConfig(String key, Map<String, Object> value) {
        if (!DEFAULTS.containsKey(key)) {
            throw new IllegalArgumentException("Unknown config key: " + key);
        }
        persistToDb(key, value);
        configCache.put(key, value);
    }

    private void persistToDb(String key, Map<String, Object> value) {
        String json = serialize(value);
        SystemConfig existing = findByKey(key);
        if (existing != null) {
            existing.setConfigValue(json);
            existing.setUpdatedAt(LocalDateTime.now());
            systemConfigMapper.updateById(existing);
        } else {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(json);
            config.setUpdatedAt(LocalDateTime.now());
            systemConfigMapper.insert(config);
        }
    }

    private SystemConfig findByKey(String key) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key).last("limit 1");
        return systemConfigMapper.selectOne(wrapper);
    }

    private String serialize(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize config", e);
        }
    }

    private Map<String, Object> deserialize(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize config json, falling back to default", e);
            return null;
        }
    }
}
