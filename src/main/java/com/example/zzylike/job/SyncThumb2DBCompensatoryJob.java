package com.example.zzylike.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.example.zzylike.constant.ThumbConstant;
import com.example.zzylike.util.RedisKeyUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class SyncThumb2DBCompensatoryJob {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private SyncThumb2DBJob syncThumb2DBJob;

    @Scheduled(cron = "0 0 2 * * *")
    public void run(){
        log.info("开始补偿数据");
        Set<String> thumbKeys=redisTemplate.keys(RedisKeyUtil.getTempThumbKey("")+"*");
        Set<String> needHandleDataSet=new HashSet<>();
        thumbKeys.stream()                  // 将键列表转换为流
                .filter(ObjUtil::isNotNull)     // 过滤掉null值
                .forEach(thumbKey ->            // 遍历每个非空键
                        needHandleDataSet.add(      // 将处理后的键添加到结果集
                                thumbKey.replace(       // 替换键前缀，提取真正的数据ID
                                        ThumbConstant.TEMP_THUMB_KEY_PREFIX.formatted(""),
                                        ""
                                )
                        )
                );
        if (CollUtil.isEmpty(needHandleDataSet)){
            log.info("没有需要补偿的临时数据");
            return;
        }
        //补偿数据
        for (String date : needHandleDataSet) {
            syncThumb2DBJob.syncThumb2DBByDate(date);
        }
        log.info("临时数据补偿完成");
    }
}
