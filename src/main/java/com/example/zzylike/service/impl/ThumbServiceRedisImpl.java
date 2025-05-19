package com.example.zzylike.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.zzylike.constant.RedisLuaScriptConstant;
import com.example.zzylike.constant.ThumbConstant;
import com.example.zzylike.mapper.ThumbMapper;
import com.example.zzylike.model.dto.thumb.DoThumbRequest;
import com.example.zzylike.model.entity.Blog;
import com.example.zzylike.model.entity.Thumb;
import com.example.zzylike.model.entity.User;
import com.example.zzylike.model.enums.LuaStatusEnum;
import com.example.zzylike.service.BlogService;
import com.example.zzylike.service.ThumbService;
import com.example.zzylike.service.UserService;
import com.example.zzylike.util.RedisKeyUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;

/**
 * @author
 */
@Service("thumbService")
@Slf4j
@RequiredArgsConstructor
public class ThumbServiceRedisImpl extends ServiceImpl<ThumbMapper, Thumb>
        implements ThumbService {

    private final UserService userService;


    private final RedisTemplate<String, Object> redisTemplate;


    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        Long blogId = doThumbRequest.getBlogId();

        String timeSlice = getTimeSlice();
        //Redis Key
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(timeSlice);
        String userThumbKey = RedisKeyUtil.getUserThumbKey(loginUser.getId());

        //执行Lua脚本
        long result = redisTemplate.execute(
                RedisLuaScriptConstant.THUMB_SCRIPT,
                Arrays.asList(tempThumbKey, userThumbKey),
                loginUser.getId(),
                blogId
        );

        if (LuaStatusEnum.FAIL.getValue() ==result){
            throw new RuntimeException("用户已点赞");
        };

        //更新成功才执行
        return LuaStatusEnum.SUCCESS.getValue() == result;
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        User loginUser = userService.getLoginUser(request);

        Long blogId=doThumbRequest.getBlogId();
        //计算时间片
        String timeSlice=getTimeSlice();
        //Redis Key
        String tempThumbKey=RedisKeyUtil.getTempThumbKey(timeSlice);
        String userThumbKey=RedisKeyUtil.getUserThumbKey(loginUser.getId());

        //执行Lua脚本
        long result = redisTemplate.execute(
                RedisLuaScriptConstant.UNTHUMB_SCRIPT,
                Arrays.asList(tempThumbKey,userThumbKey),
                loginUser.getId(),
                blogId
        );
        //根据返回值处理结果
        if (result==LuaStatusEnum.FAIL.getValue()){
            throw new RuntimeException("用户未点赞");
        }
        return LuaStatusEnum.SUCCESS.getValue() ==result;
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        return redisTemplate.opsForHash().hasKey(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString());
    }

    private String getTimeSlice() {
        DateTime nowDate = DateUtil.date();
        // 获取到当前时间前最近的整数秒，比如当前 11:20:23 ，获取到 11:20:20
        return DateUtil.format(nowDate, "HH:mm:") + (DateUtil.second(nowDate) / 10) * 10;
    }

}




