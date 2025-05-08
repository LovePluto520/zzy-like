package com.example.zzylike.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.zzylike.model.entity.Blog;
import com.example.zzylike.model.entity.User;
import com.example.zzylike.model.vo.BlogVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * @author pine
 */
public interface BlogService extends IService<Blog> {

    BlogVO getBlogVOById(long blogId, HttpServletRequest request);

    BlogVO getBlogVO(Blog blog, User loginUser);

    List<BlogVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request);
}
