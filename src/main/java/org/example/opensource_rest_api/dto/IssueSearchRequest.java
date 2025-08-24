package org.example.opensource_rest_api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class IssueSearchRequest {
    
    // 검색할 프로그래밍 언어
    private String language;
    
    // 난이도 라벨들 (good first issue, beginner-friendly 등)
    private List<String> difficultyLabels;
    
    // 추가 라벨들 (help wanted, bug 등)
    private List<String> additionalLabels;
    
    // 저장소 최소 스타 수
    private Integer minStars;
    
    // 저장소 최대 스타 수  
    private Integer maxStars;
    
    // 이슈 상태 (open, closed)
    private String state = "open";
    
    // 검색 타입 (issue, pr)
    private String type = "issue";
    
    // 할당자 조건 (none, username)
    private String assignee;
    
    // 댓글 수 조건
    private Integer maxComments;
    
    // 생성일 조건 (YYYY-MM-DD 형식)
    private String createdAfter;
    
    // 업데이트일 조건 (YYYY-MM-DD 형식)
    private String updatedAfter;
    
    // 정렬 기준 (created, updated, comments, reactions)
    private String sort = "updated";
    
    // 정렬 순서 (asc, desc)
    private String order = "desc";
    
    // 페이지 번호
    private Integer page = 1;
    
    // 페이지당 결과 수
    private Integer perPage = 30;
    
    // 키워드 검색 (제목, 본문에서 검색)
    private String keyword;
    
    // 특정 저장소 검색 (owner/repo 형식)
    private String repository;
}
