package com.lee.mapper;

import com.lee.pojo.SearchRecords;
import com.lee.utils.MyMapper;

import java.util.List;

public interface SearchRecordsMapper extends MyMapper<SearchRecords> {

    /**
     * (聚合)查询最热搜索词列表
     */
    List<String> getHotwords();

}