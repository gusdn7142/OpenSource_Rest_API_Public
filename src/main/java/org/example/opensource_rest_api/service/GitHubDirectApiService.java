package org.example.opensource_rest_api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.opensource_rest_api.dto.GitHubRepository;
import org.example.opensource_rest_api.dto.GitHubSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

/**
 * GitHub REST API 서비스
 * 
 * RestTemplate 기반의 동기식 GitHub API 호출을 제공합니다.
 * MVP 단계에서 안정성과 단순성을 우선시하며, 재시도 메커니즘과 Rate Limit 처리를 포함합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubDirectApiService {
    
    private final RestTemplate restTemplate;
    
    @Value("${github.token:}")
    private String githubToken;
    
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final int DEFAULT_PER_PAGE = 100; // GitHub API 최대값으로 증가
    private static final int DEFAULT_PAGE = 1;
    private static final int MAX_PAGES_PER_REPO = 10; // 저장소당 최대 1000개 이슈 (100 x 10)
    
    // 재시도 관련 상수
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_INITIAL_DELAY = 2000L; // 2초
    private static final double RETRY_MULTIPLIER = 2.0;
    
    /**
     * 저장소의 오픈 이슈를 검색합니다.
     * 
     * @param repository 검색 대상 저장소 (owner/repo)
     * @param labels 검색 라벨 목록 (현재 미사용)
     * @return 검색 결과 또는 빈 응답
     */
    public GitHubSearchResponse searchRepositoryIssues(String repository, List<String> labels) {
        return executeWithRetry(() -> searchRepositoryIssuesInternal(repository, labels), 
                               "Repository Issues Search", repository);
    }
    
    /**
     * 이슈 검색 내부 구현
     */
    private GitHubSearchResponse searchRepositoryIssuesInternal(String repository, List<String> labels) {
        try {
            String query = buildSearchQuery(repository, labels);
            URI uri = buildSearchUri(query, DEFAULT_PAGE, DEFAULT_PER_PAGE);
            
            log.info("GitHub API 호출 시작 - Repository: {}, Labels: {}", repository, labels);
            log.debug("검색 쿼리: {}", query);
            // URI에 민감한 정보가 포함될 수 있으므로 로그에서 제외
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<GitHubSearchResponse> response = restTemplate.exchange(
                uri, HttpMethod.GET, entity, GitHubSearchResponse.class
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
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 403 && e.getResponseBodyAsString().contains("rate limit")) {
                log.warn("GitHub API Rate Limit 도달 - Repository: {}, 메시지: {}", repository, e.getMessage());
                log.info("Rate Limit 복구를 위해 잠시 대기 후 빈 응답 반환");
                return new GitHubSearchResponse(); // 빈 응답 반환
            } else {
                log.error("GitHub API HTTP 에러 - Repository: {}, Status: {}, Error: {}", 
                         repository, e.getStatusCode(), e.getMessage());
                throw e;
            }
        } catch (RestClientException e) {
            log.error("GitHub API 호출 실패 - Repository: {}, Error: {}", repository, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * GitHub Search API 쿼리를 생성합니다.
     * 
     * @param repository 대상 저장소
     * @param labels 라벨 목록 (현재 미사용)
     * @return 검색 쿼리 문자열
     */
    private String buildSearchQuery(String repository, List<String> labels) {
        StringBuilder query = new StringBuilder();
        
        // 최소한의 조건 (Secondary Rate Limit 최대한 회피)
        query.append("repo:").append(repository);
        query.append(" state:open");
        query.append(" is:issue");
        query.append(" no:assignee");  // assignee가 없는 이슈만 검색
        
        // 라벨 조건은 일시적으로 제거 (테스트용)
        // 나중에 다시 추가할 예정
        
        return query.toString();
    }
    
    /**
     * Search API URI를 생성합니다.
     */
    private URI buildSearchUri(String query, int page, int perPage) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format("%s/search/issues?q=%s&page=%d&per_page=%d&sort=created&order=desc",
                    GITHUB_API_BASE, encodedQuery, page, perPage);
            return URI.create(url);
        } catch (Exception e) {
            log.error("URI 구성 실패 - Query: {}", query, e);
            throw new RuntimeException("URI 구성 실패", e);
        }
    }
    
    /**
     * GitHub API 요청 헤더를 생성합니다.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("User-Agent", "OpenSource-Rest-API");
        
        // GitHub Token이 설정된 경우에만 Authorization 헤더 추가
        if (githubToken != null && !githubToken.trim().isEmpty()) {
            headers.set("Authorization", "token " + githubToken);
            log.debug("GitHub Token 인증 활성화됨");  // 토큰 정보 완전 제거
        } else {
            log.warn("GitHub Token이 설정되지 않음 - Rate Limit (60/시간)에 주의 필요");
        }
        
        return headers;
    }
    
    /**
     * Rate Limit 상태를 조회합니다.
     */
    public String checkRateLimit() {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                GITHUB_API_BASE + "/rate_limit", HttpMethod.GET, entity, String.class
            );
            
            log.info("Rate Limit 확인 성공: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("Rate Limit 확인 실패: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 페이지네이션 지원 이슈 검색
     * 
     * @param repository 대상 저장소
     * @param labels 라벨 목록
     * @param page 페이지 번호
     * @param perPage 페이지당 결과 수
     * @return 검색 응답
     */
    public GitHubSearchResponse searchRepositoryIssuesWithPagination(
            String repository, List<String> labels, int page, int perPage) {
        
        // 페이지 파라미터 유효성 검사
        if (page < 1) page = 1;
        if (perPage < 1 || perPage > 100) perPage = DEFAULT_PER_PAGE;
        
        try {
            String query = buildSearchQuery(repository, labels);
            URI uri = buildSearchUri(query, page, perPage);
            
            log.info("페이지네이션 이슈 검색 - Repository: {}, Page: {}, PerPage: {}", 
                    repository, page, perPage);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<GitHubSearchResponse> response = restTemplate.exchange(
                uri, HttpMethod.GET, entity, GitHubSearchResponse.class
            );
            
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("페이지네이션 이슈 검색 실패 - Repository: {}, Page: {}, Error: {}", 
                    repository, page, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 저장소 상세 정보를 조회합니다.
     * 
     * @param fullName 저장소 이름 (owner/repo)
     * @return 저장소 정보
     */
    public GitHubRepository getRepositoryInfo(String fullName) {
        return executeWithRetry(() -> getRepositoryInfoInternal(fullName), 
                               "Repository Info", fullName);
    }
    
    /**
     * 저장소 정보 조회 내부 구현
     */
    private GitHubRepository getRepositoryInfoInternal(String fullName) {
        try {
            log.info("GitHub Repository API 호출: {}", fullName);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = GITHUB_API_BASE + "/repos/" + fullName;
            ResponseEntity<GitHubRepository> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, GitHubRepository.class);
            
            GitHubRepository repoInfo = response.getBody();
            if (repoInfo != null) {
                log.info("Repository 정보 조회 성공: {} (ID: {}, Stars: {}, Language: {})", 
                    fullName, repoInfo.getId(), repoInfo.getStargazersCount(), repoInfo.getLanguage());
            }
            
            return repoInfo;
            
        } catch (HttpClientErrorException.NotFound e) {
            log.error("저장소를 찾을 수 없음: {}", fullName);
            throw new RuntimeException("Repository not found: " + fullName, e);
        } catch (HttpClientErrorException e) {
            log.error("GitHub API 오류 - Repository: {}, Status: {}, Error: {}", 
                    fullName, e.getStatusCode(), e.getMessage());
            throw new RuntimeException("GitHub API error for repository: " + fullName, e);
        } catch (RestClientException e) {
            log.error("Repository API 호출 실패: {} - {}", fullName, e.getMessage());
            throw new RuntimeException("Failed to fetch repository info: " + fullName, e);
        }
    }
    
    /**
     * 재시도 로직이 포함된 API 호출 실행기
     * 
     * @param operation 실행할 작업
     * @param operationName 작업명
     * @param target 대상명
     * @return 실행 결과
     */
    private <T> T executeWithRetry(Supplier<T> operation, String operationName, String target) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < MAX_RETRY_ATTEMPTS) {
            attempts++;
            
            try {
                if (attempts > 1) {
                    long delay = (long) (RETRY_INITIAL_DELAY * Math.pow(RETRY_MULTIPLIER, attempts - 2));
                    log.info("{} 재시도 #{} - 대기 시간: {}ms, 대상: {}", 
                             operationName, attempts, delay, target);
                    Thread.sleep(delay);
                }
                
                return operation.get();
                
            } catch (HttpClientErrorException e) {
                lastException = e;
                
                // Rate Limit이나 Not Found 등은 재시도하지 않음
                if (e.getStatusCode().is4xxClientError()) {
                    log.warn("{} 클라이언트 오류 (재시도 안 함) - 대상: {}, 상태: {}", 
                             operationName, target, e.getStatusCode());
                    throw e;
                }
                
                log.warn("{} 시도 #{} 실패 - 대상: {}, 상태: {}, 메시지: {}", 
                         operationName, attempts, target, e.getStatusCode(), e.getMessage());
                         
            } catch (RestClientException e) {
                lastException = e;
                log.warn("{} 시도 #{} 실패 - 대상: {}, 오류: {}", 
                         operationName, attempts, target, e.getMessage());
                         
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("재시도 대기 중 인터럽트 발생", e);
            }
        }
        
        // 모든 재시도 실패
        log.error("{} 모든 재시도 실패 ({}/{}회) - 대상: {}", 
                 operationName, MAX_RETRY_ATTEMPTS, MAX_RETRY_ATTEMPTS, target);
        
        if (lastException instanceof RuntimeException) {
            throw (RuntimeException) lastException;
        } else {
            throw new RuntimeException(String.format("%s failed after %d attempts: %s", 
                                     operationName, MAX_RETRY_ATTEMPTS, target), lastException);
        }
    }
}