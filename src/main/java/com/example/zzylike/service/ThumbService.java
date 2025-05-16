package com.example.zzylike.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.zzylike.model.dto.thumb.DoThumbRequest;
import com.example.zzylike.model.entity.Thumb;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author
 */
public interface ThumbService extends IService<Thumb> {

    Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);

    Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);

    Boolean hasThumb(Long blogId, Long userId);
}
