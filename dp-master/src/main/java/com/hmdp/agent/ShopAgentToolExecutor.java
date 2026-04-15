package com.hmdp.agent;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopService;
import com.hmdp.service.IShopTypeService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 将 LLM 的 function 调用映射到底层商户查询能力
 */
@Component
public class ShopAgentToolExecutor {

    @Resource
    private IShopService shopService;

    @Resource
    private IShopTypeService shopTypeService;

    public String execute(String name, String argumentsJson) {
        try {
            JSONObject args = StrUtil.isBlank(argumentsJson) ? new JSONObject() : JSONObject.parseObject(argumentsJson);
            switch (name) {
                case "list_shop_types":
                    return listShopTypes();
                case "search_shops":
                    return searchShops(args);
                case "search_shops_nearby":
                    return searchShopsNearby(args);
                case "get_shop_by_id":
                    return getShopById(args);
                default:
                    return errorJson("未知工具: " + name);
            }
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    private String listShopTypes() {
        List<ShopType> list = shopTypeService.query().orderByAsc("sort").list();
        JSONArray arr = new JSONArray();
        for (ShopType t : list) {
            JSONObject o = new JSONObject();
            o.put("id", t.getId());
            o.put("name", t.getName());
            arr.add(o);
        }
        JSONObject root = new JSONObject();
        root.put("shop_types", arr);
        return root.toJSONString();
    }

    private String searchShops(JSONObject args) {
        Integer typeId = args.getInteger("type_id");
        String nameKeyword = args.getString("name_keyword");
        String area = args.getString("area");
        Long minAvg = args.getLong("min_avg_price");
        Long maxAvg = args.getLong("max_avg_price");
        Double minStars = args.getDouble("min_score_stars");
        Integer minSold = args.getInteger("min_sold");
        Integer page = args.getInteger("page");
        Result r = shopService.searchShopsMulti(typeId, nameKeyword, area, minAvg, maxAvg, minStars, minSold, page);
        return resultToJson(r);
    }

    private String searchShopsNearby(JSONObject args) {
        Integer typeId = args.getInteger("type_id");
        Double x = args.getDouble("longitude");
        Double y = args.getDouble("latitude");
        Integer page = args.getInteger("page");
        if (page == null || page < 1) {
            page = 1;
        }
        Result r = shopService.queryShopByType(typeId, page, x, y);
        return shopsResultToJson(r);
    }

    private String getShopById(JSONObject args) {
        Long shopId = args.getLong("shop_id");
        if (shopId == null) {
            return errorJson("shop_id 不能为空");
        }
        Result r = shopService.queryById(shopId);
        return resultToJson(r);
    }

    private String resultToJson(Result r) {
        JSONObject o = new JSONObject();
        o.put("success", r.getSuccess());
        o.put("errorMsg", r.getErrorMsg());
        if (Boolean.TRUE.equals(r.getSuccess()) && r.getData() != null) {
            Object data = r.getData();
            if (data instanceof Shop) {
                o.put("shop", shopBrief((Shop) data, false));
            } else if (data instanceof List) {
                JSONArray arr = new JSONArray();
                for (Object item : (List<?>) data) {
                    if (item instanceof Shop) {
                        arr.add(shopBrief((Shop) item, false));
                    }
                }
                o.put("shops", arr);
            } else {
                o.put("data", data);
            }
        }
        if (r.getTotal() != null) {
            o.put("total", r.getTotal());
        }
        return o.toJSONString();
    }

    private String shopsResultToJson(Result r) {
        JSONObject o = new JSONObject();
        o.put("success", r.getSuccess());
        o.put("errorMsg", r.getErrorMsg());
        if (Boolean.TRUE.equals(r.getSuccess()) && r.getData() instanceof List) {
            @SuppressWarnings("unchecked")
            List<Shop> shops = (List<Shop>) r.getData();
            JSONArray arr = new JSONArray();
            for (Shop s : shops) {
                arr.add(shopBrief(s, true));
            }
            o.put("shops", arr);
        }
        return o.toJSONString();
    }

    private JSONObject shopBrief(Shop s, boolean includeDistance) {
        JSONObject o = new JSONObject();
        o.put("id", s.getId());
        o.put("name", s.getName());
        o.put("type_id", s.getTypeId());
        o.put("area", s.getArea());
        o.put("address", s.getAddress());
        o.put("avg_price", s.getAvgPrice());
        o.put("sold", s.getSold());
        o.put("comments", s.getComments());
        if (s.getScore() != null) {
            o.put("score_stars", s.getScore() / 10.0);
        }
        if (includeDistance && s.getDistance() != null) {
            o.put("distance_m", s.getDistance());
        }
        return o;
    }

    private String errorJson(String msg) {
        JSONObject o = new JSONObject();
        o.put("success", false);
        o.put("errorMsg", msg);
        return o.toJSONString();
    }
}
