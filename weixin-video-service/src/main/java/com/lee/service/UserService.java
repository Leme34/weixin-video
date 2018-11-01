package com.lee.service;

import com.lee.pojo.Bgm;
import com.lee.pojo.Users;
import com.lee.pojo.UsersReport;

import java.util.List;

public interface UserService {
	
	/**
	 * 判断用户名是否存在
	 */
	boolean queryUsernameIsExist(String username);
	
	/**
	 * 保存用户(用户注册)
	 */
	void saveUser(Users user);
	
	/**
	 * 用户登录，根据用户名和密码查询用户
	 */
	Users queryUserForLogin(String username, String password);

	/**
	 * 用户修改信息
	 */
	void updateUserInfo(Users user);

	/**
	 * 查询用户信息
	 */
	Users queryUserInfo(String userId);

//	/**
//	 * 查询用户是否喜欢点赞视频
//	 */
//	boolean isUserLikeVideo(String userId, String videoId);
//
//	/**
//	 * 增加用户和粉丝的关系
//	 */
//	void saveUserFanRelation(String userId, String fanId);
//
//	/**
//	 * 删除用户和粉丝的关系
//	 */
//	void deleteUserFanRelation(String userId, String fanId);

//	/**
//	 * 查询用户是否关注
//	 */
//	boolean queryIfFollow(String userId, String fanId);

//	/**
//	 * 举报用户
//	 */
//	void reportUser(UsersReport userReport);
}
