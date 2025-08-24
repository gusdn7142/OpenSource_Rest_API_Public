package org.example.opensource_rest_api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.opensource_rest_api.dto.GitHubSearchResponse;
import org.example.opensource_rest_api.dto.IssueSearchRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubIssueService {

    private final WebClient githubWebClient;

    /**
     * 이슈 검색
     */
    public Mono<GitHubSearchResponse> searchIssues(IssueSearchRequest request) {
        String query = buildSearchQuery(request);
        log.info("GitHub API 검색 쿼리: {}", query);

        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/issues")
                        .queryParam("q", query)
                        .queryParam("sort", request.getSort())
                        .queryParam("order", request.getOrder())
                        .queryParam("per_page", request.getPerPage())
                        .queryParam("page", request.getPage())
                        .build())
                .retrieve()
                .bodyToMono(GitHubSearchResponse.class)
                .doOnSuccess(response -> 
                    log.info("GitHub API 응답 성공: 총 {}개의 이슈 발견", response.getTotalCount()))
                .doOnError(WebClientResponseException.class, ex -> 
                    log.error("GitHub API 호출 실패: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString()));
    }

    /**
     * 초보자용 이슈 검색
     */
    public Mono<GitHubSearchResponse> searchBeginnerIssues(String language, Integer minStars, Integer maxStars, Integer page, Integer perPage) {
        IssueSearchRequest request = new IssueSearchRequest();
        request.setLanguage(language);
        request.setDifficultyLabels(List.of("good first issue", "beginner-friendly", "easy", "starter"));
        request.setMinStars(minStars);
        request.setMaxStars(maxStars);
        request.setAssignee("none"); // 아직 할당되지 않은 이슈
        request.setPage(page != null ? page : 1);
        request.setPerPage(perPage != null ? perPage : 30);
        
        return searchIssues(request);
    }

    /**
     * 도움이 필요한 이슈 검색
     */
    public Mono<GitHubSearchResponse> searchHelpWantedIssues(String language, Integer page, Integer perPage) {
        IssueSearchRequest request = new IssueSearchRequest();
        request.setLanguage(language);
        request.setAdditionalLabels(List.of("help wanted", "contributions welcome", "up-for-grabs"));
        request.setPage(page != null ? page : 1);
        request.setPerPage(perPage != null ? perPage : 30);
        
        return searchIssues(request);
    }

    /**
     * 특정 저장소의 이슈 검색
     */
    public Mono<GitHubSearchResponse> searchRepositoryIssues(String repository, List<String> labels, Integer page, Integer perPage) {
        IssueSearchRequest request = new IssueSearchRequest();
        request.setRepository(repository);
        request.setAdditionalLabels(labels);
        request.setPage(page != null ? page : 1);
        request.setPerPage(perPage != null ? perPage : 30);
        
        return searchIssues(request);
    }

    /**
     * 검색 쿼리 구성
     */
    private String buildSearchQuery(IssueSearchRequest request) {
        List<String> queryParts = new ArrayList<>();

        // 기본 조건
        queryParts.add("state:" + request.getState());
        queryParts.add("type:" + request.getType());

        // 키워드 검색
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            queryParts.add("\"" + request.getKeyword() + "\"");
        }

        // 언어 조건
        if (request.getLanguage() != null && !request.getLanguage().isEmpty()) {
            queryParts.add("language:" + request.getLanguage());
        }

        // 저장소 조건
        if (request.getRepository() != null && !request.getRepository().isEmpty()) {
            queryParts.add("repo:" + request.getRepository());
        }

        // 난이도 라벨 조건 (OR 조건으로 연결)
        if (request.getDifficultyLabels() != null && !request.getDifficultyLabels().isEmpty()) {
            String labelQuery = request.getDifficultyLabels().stream()
                    .map(label -> "label:\"" + label + "\"")
                    .collect(Collectors.joining(" OR "));
            queryParts.add("(" + labelQuery + ")");
        }

        // 추가 라벨 조건
        if (request.getAdditionalLabels() != null && !request.getAdditionalLabels().isEmpty()) {
            for (String label : request.getAdditionalLabels()) {
                queryParts.add("label:\"" + label + "\"");
            }
        }

        // 스타 수 조건
        if (request.getMinStars() != null && request.getMaxStars() != null) {
            queryParts.add("stars:" + request.getMinStars() + ".." + request.getMaxStars());
        } else if (request.getMinStars() != null) {
            queryParts.add("stars:>" + (request.getMinStars() - 1));
        } else if (request.getMaxStars() != null) {
            queryParts.add("stars:<" + (request.getMaxStars() + 1));
        }

        // 할당자 조건
        if (request.getAssignee() != null && !request.getAssignee().isEmpty()) {
            if ("none".equals(request.getAssignee())) {
                queryParts.add("no:assignee");
            } else {
                queryParts.add("assignee:" + request.getAssignee());
            }
        }

        // 댓글 수 조건
        if (request.getMaxComments() != null) {
            queryParts.add("comments:<" + (request.getMaxComments() + 1));
        }

        // 날짜 조건
        if (request.getCreatedAfter() != null && !request.getCreatedAfter().isEmpty()) {
            queryParts.add("created:>" + request.getCreatedAfter());
        }

        if (request.getUpdatedAfter() != null && !request.getUpdatedAfter().isEmpty()) {
            queryParts.add("updated:>" + request.getUpdatedAfter());
        }

        return String.join(" ", queryParts);
    }
}
