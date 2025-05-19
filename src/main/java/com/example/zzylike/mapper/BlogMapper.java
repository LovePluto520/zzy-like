package com.example.zzylike.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.zzylike.model.entity.Blog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * @author
 */
@Mapper
public interface BlogMapper extends BaseMapper<Blog> {
    void batchUpdateThumbCount(@Param("countMap")Map<Long,Long> countMap);

}




