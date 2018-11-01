package com.lee.service;

import com.lee.pojo.Bgm;
import java.util.List;

public interface BgmService {
	
	/**
	 * 查询背景音乐列表
	 */
	List<Bgm> queryBgmList();
	
	/**
	 * 根据id查询bgm信息
	 */
	Bgm queryBgmById(String bgmId);
}
