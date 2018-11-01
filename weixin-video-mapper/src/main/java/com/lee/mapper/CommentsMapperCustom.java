package com.lee.mapper;

import com.lee.pojo.Comments;
import com.lee.pojo.vo.CommentsVO;
import com.lee.utils.MyMapper;

import java.util.List;

public interface CommentsMapperCustom extends MyMapper<Comments> {
    /**
     * 关联users表查询此视频的评论列表
     */
    List<CommentsVO> queryComments(String videoId);
}