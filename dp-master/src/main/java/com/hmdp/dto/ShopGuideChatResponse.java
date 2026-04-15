package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能导购对话响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopGuideChatResponse {

    /**
     * 模型最终自然语言回答
     */
    private String reply;

    /**
     * 是否调用了工具（便于排查与展示）
     */
    private boolean usedTools;
}
