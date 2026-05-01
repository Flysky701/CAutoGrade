package com.autograding.service;

import com.autograding.entity.SystemConfig;
import com.autograding.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SystemConfigMapper systemConfigMapper;

    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SystemConfigService systemConfigService;

    @BeforeEach
    void setUp() {
        systemConfigService = new SystemConfigService(systemConfigMapper, objectMapper);
    }

    @Test
    void testInit_loadsFromDatabase() {
        SystemConfig llmConfig = new SystemConfig();
        llmConfig.setConfigKey("llm");
        llmConfig.setConfigValue("{\"provider\":\"openai\",\"apiKey\":\"sk-test\",\"model\":\"gpt-4\",\"temperature\":0.5,\"maxTokens\":4096,\"timeout\":60}");

        SystemConfig sandboxConfig = new SystemConfig();
        sandboxConfig.setConfigKey("sandbox");
        sandboxConfig.setConfigValue("{\"compileTimeout\":10,\"runTimeout\":10,\"memoryLimitMB\":512,\"maxConcurrent\":8,\"enableNetwork\":true}");

        SystemConfig scoringConfig = new SystemConfig();
        scoringConfig.setConfigKey("scoring");
        scoringConfig.setConfigValue("{\"correctnessWeight\":70,\"styleWeight\":15,\"efficiencyWeight\":15,\"deviationThreshold\":10,\"fallbackToRules\":false}");

        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(llmConfig)
                .thenReturn(sandboxConfig)
                .thenReturn(scoringConfig);

        systemConfigService.init();

        Map<String, Object> allConfig = systemConfigService.getAllConfig();
        assertEquals(3, allConfig.size());
        assertTrue(allConfig.containsKey("llm"));
        assertTrue(allConfig.containsKey("sandbox"));
        assertTrue(allConfig.containsKey("scoring"));

        @SuppressWarnings("unchecked")
        Map<String, Object> llm = (Map<String, Object>) allConfig.get("llm");
        assertEquals("openai", llm.get("provider"));
        assertEquals("gpt-4", llm.get("model"));
        assertEquals(0.5, llm.get("temperature"));
    }

    @Test
    void testInit_dbUnavailable_usesDefaults() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("DB connection failed"));

        systemConfigService.init();

        Map<String, Object> allConfig = systemConfigService.getAllConfig();
        assertEquals(3, allConfig.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> llm = (Map<String, Object>) allConfig.get("llm");
        assertEquals("deepseek", llm.get("provider"));
        assertEquals("deepseek-chat", llm.get("model"));
        assertEquals(0.3, llm.get("temperature"));
    }

    @Test
    void testGetAllConfig_returnsAllKeys() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);
        when(systemConfigMapper.insert(any(SystemConfig.class))).thenReturn(1);

        systemConfigService.init();

        Map<String, Object> allConfig = systemConfigService.getAllConfig();
        assertTrue(allConfig.containsKey("llm"));
        assertTrue(allConfig.containsKey("sandbox"));
        assertTrue(allConfig.containsKey("scoring"));
    }

    @Test
    void testUpdateConfig_persistsToDb() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);
        when(systemConfigMapper.insert(any(SystemConfig.class))).thenReturn(1);

        systemConfigService.init();

        SystemConfig existingScoring = new SystemConfig();
        existingScoring.setId(1L);
        existingScoring.setConfigKey("scoring");
        existingScoring.setConfigValue("{\"correctnessWeight\":60,\"styleWeight\":20,\"efficiencyWeight\":20,\"deviationThreshold\":15,\"fallbackToRules\":true}");

        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(existingScoring);
        when(systemConfigMapper.updateById(any(SystemConfig.class))).thenReturn(1);

        Map<String, Object> newScoring = Map.of(
                "correctnessWeight", 80,
                "styleWeight", 10,
                "efficiencyWeight", 10,
                "deviationThreshold", 5,
                "fallbackToRules", false
        );
        systemConfigService.updateConfig("scoring", newScoring);

        verify(systemConfigMapper).updateById(any(SystemConfig.class));

        Map<String, Object> allConfig = systemConfigService.getAllConfig();
        @SuppressWarnings("unchecked")
        Map<String, Object> scoring = (Map<String, Object>) allConfig.get("scoring");
        assertEquals(80, scoring.get("correctnessWeight"));
    }

    @Test
    void testUpdateConfig_unknownKey_throwsException() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);
        when(systemConfigMapper.insert(any(SystemConfig.class))).thenReturn(1);

        systemConfigService.init();

        assertThrows(IllegalArgumentException.class, () ->
                systemConfigService.updateConfig("unknown", Map.of()));
    }

    @Test
    void testUpdateConfig_dbUnavailable_onlyUpdatesCache() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("DB connection failed"));

        systemConfigService.init();

        Map<String, Object> newLlm = Map.of(
                "provider", "openai",
                "apiKey", "sk-new",
                "model", "gpt-4",
                "temperature", 0.7,
                "maxTokens", 8192,
                "timeout", 120
        );
        systemConfigService.updateConfig("llm", newLlm);

        verify(systemConfigMapper, never()).updateById(any(SystemConfig.class));

        Map<String, Object> allConfig = systemConfigService.getAllConfig();
        @SuppressWarnings("unchecked")
        Map<String, Object> llm = (Map<String, Object>) allConfig.get("llm");
        assertEquals("openai", llm.get("provider"));
        assertEquals("gpt-4", llm.get("model"));
    }
}
