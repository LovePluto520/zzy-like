package com.example.zzylike.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zzylike.mapper.BlogMapper;
import com.example.zzylike.model.entity.Thumb;
import com.example.zzylike.model.enums.ThumbTypeEnum;
import com.example.zzylike.service.ThumbService;
import com.example.zzylike.util.RedisKeyUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 定时将Redis中的临时点赞数据同步到数据库
 */
@Component
@Slf4j
public class SyncThumb2DBJob {
    @Resource
    private ThumbService thumbService;

    @Resource
    private BlogMapper blogMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置十秒执行一次，遇到错误就回滚
     */
    @Scheduled(fixedRate = 10000)
    @Transactional(rollbackFor = Exception.class)
    public void run() {
        log.info("开始执行");
        DateTime nowDate = DateUtil.date();
        //如果秒数为0~9则回到上一分钟的50秒
        int second = (DateUtil.second(nowDate) / 10 - 1) * 10;
        if (second == -10) {
            second = 50;
            //回到上一分钟
            nowDate = DateUtil.offsetMillisecond(nowDate, -1);
        }
        String date = DateUtil.format(nowDate, "HH:mm:") + second;
        syncThumb2DBByDate(date);
        log.info("临时数据同步完成");
    }

    public void syncThumb2DBByDate(String date) {
        // 获取Redis中存储的临时点赞数据键（格式如：temp_thumb:20230519）
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(date);
        // 获取该键下的所有哈希数据（userId:blogId → 点赞类型）
        Map<Object, Object> allTempThumbMap = redisTemplate.opsForHash().entries(tempThumbKey);
        boolean thumbMapEmpty = CollUtil.isEmpty(allTempThumbMap);

        /**
         * 同步点赞数据到数据库
         * 1. 构建待插入的点赞记录列表
         * 2. 收集需要更新点赞数的博客ID
         */
        Map<Long, Long> blogThumbCountMap = new HashMap<>();
        if (thumbMapEmpty) {
            return; // 没有数据，直接返回
        }

        // 待插入的点赞记录列表
        ArrayList<Thumb> thumbList = new ArrayList<>();
        // 批量删除条件构造器
        LambdaQueryWrapper<Thumb> wrapper = new LambdaQueryWrapper<>();
        Boolean needRemove = false;

        // 遍历Redis中的所有临时点赞数据
        for (Object userIdBlogIdObj : allTempThumbMap.keySet()) {
            String userIdBlogId = (String) userIdBlogIdObj;
            // 解析userId和blogId（格式：userId:blogId）
            String[] userIdAndBlogId = userIdBlogId.split(StrPool.COLON);
            Long userId = Long.valueOf(userIdAndBlogId[0]);
            Long blogId = Long.valueOf(userIdAndBlogId[1]);

            // 获取点赞类型：1=点赞，-1=取消点赞
            Integer thumbType = Integer.valueOf(allTempThumbMap.get(userIdBlogId).toString());

            if (thumbType == ThumbTypeEnum.INCR.getValue()) {
                // 点赞操作：创建点赞记录并添加到待插入列表
                Thumb thumb = new Thumb();
                thumb.setUserId(userId);
                thumb.setBlogId(blogId);
                thumbList.add(thumb);
            } else if (thumbType == ThumbTypeEnum.DECR.getValue()) {
                // 取消点赞操作：构造批量删除条件
                needRemove = true;
                wrapper.or().eq(Thumb::getUserId, userId).eq(Thumb::getBlogId, blogId);
            } else {
                // 异常数据处理（既不是点赞也不是取消点赞）
                if (thumbType != ThumbTypeEnum.NON.getValue()) {
                    log.warn("数据异常:{}", userId + "," + blogId + "," + thumbType);
                }
                continue;
            }

            // 计算每个博客的点赞增量（点赞+1，取消点赞-1）
            blogThumbCountMap.put(blogId, blogThumbCountMap.getOrDefault(blogId, 0L) + thumbType);
        }

        // 批量插入新的点赞记录
        thumbService.saveBatch(thumbList);

        // 批量删除取消点赞的记录
        if (needRemove) {
            thumbService.remove(wrapper);
        }

        // 批量更新博客的点赞数量
        if (!blogThumbCountMap.isEmpty()) {
            blogMapper.batchUpdateThumbCount(blogThumbCountMap);
        }

        // 异步删除Redis中的临时数据（使用虚拟线程）
        Thread.startVirtualThread(() -> {
            redisTemplate.delete(tempThumbKey);
        });
    }
}
