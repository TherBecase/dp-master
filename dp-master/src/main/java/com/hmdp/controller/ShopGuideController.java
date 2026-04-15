package com.hmdp.controller;

import com.hmdp.agent.ShopGuideAgentService;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShopGuideChatRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 智能导购：自然语言 + LLM Function Calling + 商户查询工具
 */
@RestController
@RequestMapping("/shop/guide")
public class ShopGuideController {

    @Resource
    private ShopGuideAgentService shopGuideAgentService;

    /**
     * 发送用户自然语言，返回导购回答（内部可多轮调用商户查询工具）
     */
    @PostMapping("/chat")
    public Result chat(@RequestBody ShopGuideChatRequest request) {
        return shopGuideAgentService.chat(request.getMessage());
    }
}
