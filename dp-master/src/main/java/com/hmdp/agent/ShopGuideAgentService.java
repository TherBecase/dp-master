package com.hmdp.agent;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hmdp.agent.config.LlmOpenAiProperties;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShopGuideChatResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

/**
 * 基于 LLM Function Calling 的智能导购编排：驱动模型按需调用商户查询工具。
 */
@Service
public class ShopGuideAgentService {

    private static final String SYSTEM_PROMPT = "你是「点评」类应用中的智能导购助手。\n"
            + "用户会用自然语言描述找店需求（类型、商圈、价格、评分、距离等）。\n"
            + "你必须通过提供的工具查询真实数据后再回答，不得编造店铺名称、评分或地址。\n"
            + "流程建议：若涉及类型名称，可先 list_shop_types 获取 type_id；\n"
            + "无经纬度时用 search_shops 做多条件检索；有经纬度且要按距离排序时用 search_shops_nearby。\n"
            + "若缺少必要参数（例如附近搜索缺坐标），向用户说明并给出可填的信息示例。\n"
            + "回答简洁、分点列出推荐，并说明依据的筛选条件。";

    @Resource
    private LlmOpenAiProperties llmProps;

    @Resource(name = "llmRestTemplate")
    private RestTemplate restTemplate;

    @Resource
    private ShopAgentToolExecutor toolExecutor;

    public Result chat(String userMessage) {
        if (StrUtil.isBlank(userMessage)) {
            return Result.fail("message 不能为空");
        }
        if (StrUtil.isBlank(llmProps.getApiKey())) {
            return Result.fail("未配置 LLM：请在 application.yaml 设置 llm.openai.api-key，或环境变量 LLM_OPENAI_API_KEY");
        }

        JSONArray messages = new JSONArray();
        messages.add(msg("system", SYSTEM_PROMPT, null));
        messages.add(msg("user", userMessage, null));

        boolean usedTools = false;
        for (int round = 0; round < llmProps.getMaxToolRounds(); round++) {
            JSONObject body = new JSONObject();
            body.put("model", llmProps.getModel());
            body.put("messages", messages);
            body.put("tools", ShopAgentToolDefinitions.buildTools());
            body.put("tool_choice", "auto");

            JSONObject resp = postChatCompletions(body);
            if (resp.containsKey("error")) {
                JSONObject err = resp.getJSONObject("error");
                return Result.fail("LLM 错误: " + err.getString("message"));
            }
            JSONArray choices = resp.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return Result.fail("LLM 返回空 choices");
            }
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String finishReason = choice.getString("finish_reason");

            JSONArray toolCalls = message.getJSONArray("tool_calls");
            if (toolCalls != null && !toolCalls.isEmpty()) {
                usedTools = true;
                messages.add(message);
                for (int i = 0; i < toolCalls.size(); i++) {
                    JSONObject tc = toolCalls.getJSONObject(i);
                    String id = tc.getString("id");
                    JSONObject fn = tc.getJSONObject("function");
                    String name = fn.getString("name");
                    String args = fn.getString("arguments");
                    String content = toolExecutor.execute(name, args);
                    messages.add(msg("tool", content, id));
                }
                continue;
            }

            String content = message.getString("content");
            if (StrUtil.isNotBlank(content)) {
                return Result.ok(new ShopGuideChatResponse(content, usedTools));
            }
            if ("length".equals(finishReason)) {
                return Result.fail("模型输出过长被截断，请缩小问题范围后重试");
            }
            return Result.fail("模型未返回有效内容，finish_reason=" + finishReason);
        }
        return Result.fail("工具调用轮数超过上限(" + llmProps.getMaxToolRounds() + ")，请简化需求");
    }

    private JSONObject msg(String role, String content, String toolCallId) {
        JSONObject m = new JSONObject();
        m.put("role", role);
        if (content != null) {
            m.put("content", content);
        }
        if (toolCallId != null) {
            m.put("tool_call_id", toolCallId);
        }
        return m;
    }

    private JSONObject postChatCompletions(JSONObject body) {
        String base = llmProps.getBaseUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(llmProps.getApiKey());

        HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return JSONObject.parseObject(response.getBody());
    }
}
