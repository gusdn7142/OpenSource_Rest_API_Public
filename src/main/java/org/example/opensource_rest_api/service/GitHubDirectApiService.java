package org.example.opensource_rest_api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.opensource_rest_api.dto.GitHubSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * GitHub API 직접 호출 서비스
 * 
 * RestTemplate을 사용하여 GitHub Search API를 직접 호출합니다.
 * WebClient 대신 동기 방식의 단순한 API 호출로 MVP에 최적화되어 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubDirectApiService {
    
    private final RestTemplate restTemplate;
    
    @Value("${github.token:}")
    private String githubToken;
    
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final int DEFAULT_PER_PAGE = 20;
    private static final int DEFAULT_PAGE = 1;
    
    /**
     * 특정 레포지토리에서 라벨 기반으로 이슈를 검색합니다.
     * 
     * MVP 전용: 지정된 레포지토리에서 초보자용 이슈만 수집
     * - open 상태의 이슈만 대상
     * - 아직 할당되지 않은 이슈만 대상 (no:assignee)
     * - 레포지토리별 맞춤 라벨 적용
     * 
     * @param repository 검색 대상 레포지토리 (예: "spring-projects/spring-boot")
     * @param labels 검색할 라벨 리스트 (예: ["good first issue", "help wanted"])
     * @return GitHub API 검색 응답 객체
     * @throws RestClientException GitHub API 호출 실패 시
     */
    public GitHubSearchResponse searchRepositoryIssues(String repository, List<String> labels) {
        try {
            String query = buildSearchQuery(repository, labels);
            String url = buildSearchUrl(query, DEFAULT_PAGE, DEFAULT_PER_PAGE);
            
            log.info("GitHub API 호출 시작 - Repository: {}, Labels: {}", repository, labels);
            log.debug("검색 쿼리: {}", query);
            log.debug("요청 URL: {}", url);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<GitHubSearchResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, GitHubSearchResponse.class
            );
            
            GitHubSearchResponse searchResponse = response.getBody();
            if (searchResponse != null) {
                log.info("GitHub API 호출 성공 - Repository: {}, 발견된 이슈 수: {}", 
                        repository, searchResponse.getTotalCount());
                return searchResponse;
            } else {
                log.warn("GitHub API 응답이 비어있음 - Repository: {}", repository);
                return new GitHubSearchResponse();
            }
            
        } catch (RestClientException e) {
            log.error("GitHub API 호출 실패 - Repository: {}, Error: {}", repository, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * GitHub Search API용 검색 쿼리를 구성합니다.
     * 
     * 구성 요소:
     * - repo: 특정 레포지토리 지정
     * - state:open: 열린 이슈만
     * - no:assignee: 할당되지 않은 이슈만
     * - label: 각 라벨을 OR 조건으로 연결
     * 
     * @param repository 대상 레포지토리
     * @param labels 검색할 라벨 목록
     * @return 완성된 검색 쿼리 문자열
     */
    private String buildSearchQuery(String repository, List<String> labels) {
        StringBuilder query = new StringBuilder();
        
        // 기본 조건
        query.append("repo:").append(repository);
        query.append(" state:open");
        query.append(" no:assignee");
        query.append(" is:issue");
        
        // 라벨 조건 (OR 조건으로 연결)
        if (labels != null && !labels.isEmpty()) {
            query.append(" (");
            for (int i = 0; i < labels.size(); i++) {
                if (i > 0) {
                    query.append(" OR ");
                }
                query.append("label:\"").append(labels.get(i)).append("\"");
            }
            query.append(")");
        }
        
        return query.toString();
    }
    
    /**
     * GitHub Search API URL을 구성합니다.
     * 
     * @param query 검색 쿼리
     * @param page 페이지 번호
     * @param perPage 페이지당 결과 수
     * @return 완성된 API URL
     */
    private String buildSearchUrl(String query, int page, int perPage) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            return String.format("%s/search/issues?q=%s&page=%d&per_page=%d&sort=created&order=desc",
                    GITHUB_API_BASE, encodedQuery, page, perPage);
        } catch (Exception e) {
            log.error("URL 인코딩 실패 - Query: {}", query, e);
            throw new RuntimeException("URL 구성 실패", e);
        }
    }
    
    /**
     * GitHub API 호출용 HTTP 헤더를 생성합니다.
     * 
     * - GitHub Token이 설정된 경우 Authorization 헤더 추가
     * - API 응답 형식을 JSON으로 지정
     * - User-Agent 설정 (GitHub API 요구사항)
     * 
     * @return 설정된 HTTP 헤더
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("User-Agent", "OpenSource-Rest-API");
        
        // GitHub Token이 설정된 경우에만 Authorization 헤더 추가
        if (githubToken != null && !githubToken.trim().isEmpty()) {
            headers.set("Authorization", "token " + githubToken);
            log.debug("GitHub Token 인증 헤더 추가됨");
        } else {
            log.warn("GitHub Token이 설정되지 않음 - Rate Limit에 주의 필요");
        }
        
        return headers;
    }
    
    /**
     * 페이지네이션을 지원하는 이슈 검색
     * 
     * 대량의 이슈를 수집해야 할 때 사용합니다.
     * MVP에서는 주로 첫 페이지만 사용하지만, 향후 확장을 위해 제공됩니다.
     * 
     * @param repository 대상 레포지토리
     * @param labels 검색할 라벨 목록
     * @param page 페이지 번호 (1부터 시작)
     * @param perPage 페이지당 결과 수 (최대 100)
     * @return GitHub API 검색 응답
     */
    public GitHubSearchResponse searchRepositoryIssuesWithPagination(
            String repository, List<String> labels, int page, int perPage) {
        
        // 페이지 파라미터 유효성 검사
        if (page < 1) page = 1;
        if (perPage < 1 || perPage > 100) perPage = DEFAULT_PER_PAGE;
        
        try {
            String query = buildSearchQuery(repository, labels);
            String url = buildSearchUrl(query, page, perPage);
            
            log.info("페이지네이션 이슈 검색 - Repository: {}, Page: {}, PerPage: {}", 
                    repository, page, perPage);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<GitHubSearchResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, GitHubSearchResponse.class
            );
            
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("페이지네이션 이슈 검색 실패 - Repository: {}, Page: {}, Error: {}", 
                    repository, page, e.getMessage(), e);
            throw e;
        }
    }
}