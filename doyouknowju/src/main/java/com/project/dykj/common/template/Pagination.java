package com.project.dykj.common.template;

import com.project.dykj.common.model.vo.PageInfo;


public class Pagination {
	
	//페이징 처리용 PageInfo 객체에 필드를 담아주는 메소드 (계산처리까지)
	
	public static PageInfo getPageInfo(int listCount,int currentPage
									  ,int boardLimit,int pageLimit) {
		
		int maxPage = (int)Math.ceil((double)listCount/boardLimit);
		int startPage = (currentPage-1)/pageLimit*pageLimit+1;
		int endPage = startPage+pageLimit-1;
		
		//endPage가 maxPage보다 클때
		if(maxPage<endPage) {
			endPage = maxPage;
		}
		
		//PageInfo pi = new PageInfo(currentPage,listCount,boardLimit,pageLimit,maxPage,startPage,endPage);
		
		PageInfo pi2 =	PageInfo.builder()
								.listCount(listCount)
							    .currentPage(currentPage)
							    .boardLimit(boardLimit)
							    .pageLimit(pageLimit)
							    .maxPage(maxPage)
							    .startPage(startPage)
							    .endPage(endPage)
							    .build(); 
		//lombok의 @Builder 어노테이션을 이용하여 생성할 각 필드를 메소드처럼 사용하여 값을 채워줄 수 있다.
		//필요한 필드만 채워넣을 수 있는 장점이 있음,값을 넣는 순서 중요하지 않음
							    
		return pi2;
	}
	
	
	
}
