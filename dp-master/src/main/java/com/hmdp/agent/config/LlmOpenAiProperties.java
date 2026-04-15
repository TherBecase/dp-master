package com.hmdp.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAI 兼容接口配置（支持 OpenAI、Azure OpenAI、国内兼容网关等）
 */
@Data
@ConfigurationProperties(prefix = "llm.openai")
public class LlmOpenAiProperties {

    /**
     * 基础地址，需包含 /v1，例如 https://api.openai.com/v1
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * API Key，建议使用环境变量注入，勿提交到仓库
     */
    private String apiKey = "";

    /**
     * 模型名称
     */
    private String model = "gpt-4o-mini";

    /**
     * 单次导购最大工具调用轮数，防止死循环
     */
    private int maxToolRounds = 5;
}
