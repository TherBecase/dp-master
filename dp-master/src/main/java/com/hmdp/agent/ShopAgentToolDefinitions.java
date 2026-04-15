package com.hmdp.agent;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * OpenAI Function Calling 工具定义（与 /v1/chat/completions 的 tools 字段一致）
 */
public final class ShopAgentToolDefinitions {

    private ShopAgentToolDefinitions() {
    }

    public static JSONArray buildTools() {
        JSONArray tools = new JSONArray();

        tools.add(tool(
                "list_shop_types",
                "列出系统中所有商铺类型及其数字 ID。当用户提到「美食、奶茶、KTV」等类型名称时，先用本工具拿到 type_id，再调用 search_shops 或 search_shops_nearby。",
                emptyParams()));

        tools.add(tool(
                "search_shops",
                "按多维度条件在数据库中检索商铺：类型、名称关键字、商圈、人均消费区间、最低评分、最低销量等。不需要经纬度时使用本工具。",
                JSONObject.parseObject("{\n"
                        + "  \"type\": \"object\",\n"
                        + "  \"properties\": {\n"
                        + "    \"type_id\": { \"type\": [\"integer\", \"null\"], \"description\": \"商铺类型 ID，来自 list_shop_types\" },\n"
                        + "    \"name_keyword\": { \"type\": [\"string\", \"null\"], \"description\": \"名称模糊关键字，如 星巴克\" },\n"
                        + "    \"area\": { \"type\": [\"string\", \"null\"], \"description\": \"商圈关键字，如 陆家嘴\" },\n"
                        + "    \"min_avg_price\": { \"type\": [\"integer\", \"null\"], \"description\": \"最低人均（元）\" },\n"
                        + "    \"max_avg_price\": { \"type\": [\"integer\", \"null\"], \"description\": \"最高人均（元）\" },\n"
                        + "    \"min_score_stars\": { \"type\": [\"number\", \"null\"], \"description\": \"最低评分 1～5 星（小数）\" },\n"
                        + "    \"min_sold\": { \"type\": [\"integer\", \"null\"], \"description\": \"最低销量\" },\n"
                        + "    \"page\": { \"type\": [\"integer\", \"null\"], \"description\": \"页码，从 1 开始，默认 1\" }\n"
                        + "  }\n"
                        + "}")));

        tools.add(tool(
                "search_shops_nearby",
                "按商铺类型 + 用户经纬度，在 Redis GEO 中按距离排序检索附近商铺（5km 内）。用户明确给出或系统已知坐标时使用。",
                JSONObject.parseObject("{\n"
                        + "  \"type\": \"object\",\n"
                        + "  \"properties\": {\n"
                        + "    \"type_id\": { \"type\": \"integer\", \"description\": \"商铺类型 ID\" },\n"
                        + "    \"longitude\": { \"type\": \"number\", \"description\": \"经度 x\" },\n"
                        + "    \"latitude\": { \"type\": \"number\", \"description\": \"纬度 y\" },\n"
                        + "    \"page\": { \"type\": [\"integer\", \"null\"], \"description\": \"页码，默认 1\" }\n"
                        + "  },\n"
                        + "  \"required\": [\"type_id\", \"longitude\", \"latitude\"]\n"
                        + "}")));

        tools.add(tool(
                "get_shop_by_id",
                "根据商铺主键 ID 查询详情（缓存穿透防护后的完整信息）。",
                JSONObject.parseObject("{\n"
                        + "  \"type\": \"object\",\n"
                        + "  \"properties\": {\n"
                        + "    \"shop_id\": { \"type\": \"integer\", \"description\": \"商铺 id\" }\n"
                        + "  },\n"
                        + "  \"required\": [\"shop_id\"]\n"
                        + "}")));

        return tools;
    }

    private static JSONObject emptyParams() {
        JSONObject p = new JSONObject();
        p.put("type", "object");
        p.put("properties", new JSONObject());
        return p;
    }

    private static JSONObject tool(String name, String description, JSONObject parameters) {
        JSONObject fn = new JSONObject();
        fn.put("name", name);
        fn.put("description", description);
        fn.put("parameters", parameters);
        JSONObject wrap = new JSONObject();
        wrap.put("type", "function");
        wrap.put("function", fn);
        return wrap;
    }
}
