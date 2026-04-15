package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);

    /**
     * 多维度条件组合检索（数据库侧过滤，用于智能导购等场景）
     */
    Result searchShopsMulti(Integer typeId, String nameKeyword, String area,
                            Long minAvgPrice, Long maxAvgPrice, Double minScoreStars,
                            Integer minSold, Integer current);
}
