package com.hmdp.dto;

import lombok.Data;

/**
 * 智能导购对话请求
 */
@Data
public class ShopGuideChatRequest {

    /**
     * 用户自然语言需求
     */
    private String message;
}
