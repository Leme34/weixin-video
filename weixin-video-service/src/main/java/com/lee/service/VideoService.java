package com.lee.service;

import com.lee.VO.PagedResult;
import com.lee.pojo.Comments;
import com.lee.pojo.Videos;

import java.util.List;

public interface VideoService {

    /**
     * 保存视频,返回video的id用于上传视频成功后上传封面图片(微信小程序原因不能多文件上传)
     */
    String saveVideo(Videos video);

    /**
     * 分页查询小视频
     * 返回自定义的PagedResult到前端处理
     * video : 需要查询的video对象
     * isSaveRecord：1 - 需要保存热搜词,只有在搜索视频的时候才需要
     * 				 0 - 不需要保存热搜词 ，或者为空的时候
     */
    PagedResult getAllVideo(Videos video, Integer isSaveRecord, int page, int pageSize);

//	/**
//	 * 修改视频的封面
//	 */
//	void updateVideo(String videoId, String coverPath);

//	/**
//	 * 分页查询视频列表
//	 */
//	PagedResult getAllVideos(Videos video, Integer isSaveRecord,
//							 Integer page, Integer pageSize);

//	/**
//	 * 查询我喜欢的视频列表
//	 */
//	PagedResult queryMyLikeVideos(String userId, Integer page, Integer pageSize);
//
//	/**
//	 * 查询我关注的人的视频列表
//	 */
//	PagedResult queryMyFollowVideos(String userId, Integer page, Integer pageSize);

	/**
	 * 获取热搜词列表
	 */
	List<String> getHotwords();

//	/**
//	 * 用户喜欢/点赞视频
//	 */
//	void userLikeVideo(String userId, String videoId, String videoCreaterId);
//
//	/**
//	 * 用户不喜欢/取消点赞视频
//	 */
//	void userUnLikeVideo(String userId, String videoId, String videoCreaterId);

	/**
	 * 用户留言
	 */
	void saveComment(Comments comment);

	/**
	 * 分页查询此视频的留言
	 */
	PagedResult getAllComments(String videoId, Integer page, Integer pageSize);

}


