package com.project.dykj.domain.news.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.project.dykj.domain.news.vo.NewsVO;

@Mapper
public interface NewsMapper {
    int insertNewsList(List<NewsVO> newsList);
    List<NewsVO> selectLatestNews();
}