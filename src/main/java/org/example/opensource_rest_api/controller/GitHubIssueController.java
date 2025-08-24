//package org.example.opensource_rest_api.controller;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.example.opensource_rest_api.dto.GitHubSearchResponse;
//import org.example.opensource_rest_api.dto.IssueSearchRequest;
//import org.example.opensource_rest_api.service.GitHubIssueService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/github")
//@CrossOrigin(origins = "*")
//@RequiredArgsConstructor
//public class GitHubIssueController {
//
//    private final GitHubIssueService githubIssueService;
//
//    /**
//     * 고급 이슈 검색 - POST /api/github/issues/search
//     */
//    @PostMapping("/issues/search")
//    public Mono<ResponseEntity<GitHubSearchResponse>> searchIssues(@RequestBody IssueSearchRequest request) {
//        log.info("고급 이슈 검색 요청: {}", request);
//
//        return githubIssueService.searchIssues(request)
//                .map(ResponseEntity::ok)
//                .onErrorReturn(ResponseEntity.badRequest().build());
//    }
//
//    /**
//     * 초보자용 이슈 조회 - GET /api/github/issues/beginner
//     */
//    @GetMapping("/issues/beginner")
//    public Mono<ResponseEntity<GitHubSearchResponse>> getBeginnerIssues(
//            @RequestParam(required = false) String language,
//            @RequestParam(required = false) Integer minStars,
//            @RequestParam(required = false) Integer maxStars,
//            @RequestParam(defaultValue = "1") Integer page,
//            @RequestParam(defaultValue = "30") Integer perPage) {
//
//        log.info("초보자용 이슈 검색 - language: {}, minStars: {}, maxStars: {}, page: {}, perPage: {}",
//                language, minStars, maxStars, page, perPage);
//
//        return githubIssueService.searchBeginnerIssues(language, minStars, maxStars, page, perPage)
//                .map(ResponseEntity::ok)
//                .onErrorReturn(ResponseEntity.badRequest().build());
//    }
//
//    /**
//     * 도움 요청 이슈 조회 - GET /api/github/issues/help-wanted
//     */
//    @GetMapping("/issues/help-wanted")
//    public Mono<ResponseEntity<GitHubSearchResponse>> getHelpWantedIssues(
//            @RequestParam(required = false) String language,
//            @RequestParam(defaultValue = "1") Integer page,
//            @RequestParam(defaultValue = "30") Integer perPage) {
//
//        log.info("도움 요청 이슈 검색 - language: {}, page: {}, perPage: {}", language, page, perPage);
//
//        return githubIssueService.searchHelpWantedIssues(language, page, perPage)
//                .map(ResponseEntity::ok)
//                .onErrorReturn(ResponseEntity.badRequest().build());
//    }
//
//    /**
//     * 특정 저장소 이슈 조회 - GET /api/github/issues/repository/{owner}/{repo}
//     */
//    @GetMapping("/issues/repository/{owner}/{repo}")
//    public Mono<ResponseEntity<GitHubSearchResponse>> getRepositoryIssues(
//            @PathVariable String owner,
//            @PathVariable String repo,
//            @RequestParam(required = false) List<String> labels,
//            @RequestParam(defaultValue = "1") Integer page,
//            @RequestParam(defaultValue = "30") Integer perPage) {
//
//        String repository = owner + "/" + repo;
//        log.info("저장소 이슈 검색 - repository: {}, labels: {}, page: {}, perPage: {}",
//                repository, labels, page, perPage);
//
//        return githubIssueService.searchRepositoryIssues(repository, labels, page, perPage)
//                .map(ResponseEntity::ok)
//                .onErrorReturn(ResponseEntity.badRequest().build());
//    }
//
//    /**
//     * 언어별 이슈 통계 - GET /api/github/issues/stats
//     */
//    @GetMapping("/issues/stats")
//    public Mono<ResponseEntity<Map<String, Object>>> getIssueStats(
//            @RequestParam(required = false) String language) {
//
//        log.info("이슈 통계 조회 - language: {}", language);
//
//        // 초보자용 이슈와 도움 요청 이슈를 각각 조회하여 통계 생성
//        Mono<GitHubSearchResponse> beginnerIssues = githubIssueService.searchBeginnerIssues(language, null, null, 1, 1);
//        Mono<GitHubSearchResponse> helpWantedIssues = githubIssueService.searchHelpWantedIssues(language, 1, 1);
//
//        return Mono.zip(beginnerIssues, helpWantedIssues)
//                .map(tuple -> {
//                    Map<String, Object> stats = Map.of(
//                        "language", language != null ? language : "all",
//                        "beginnerIssuesCount", tuple.getT1().getTotalCount(),
//                        "helpWantedIssuesCount", tuple.getT2().getTotalCount(),
//                        "totalOpportunities", tuple.getT1().getTotalCount() + tuple.getT2().getTotalCount()
//                    );
//                    return ResponseEntity.ok(stats);
//                })
//                .onErrorReturn(ResponseEntity.badRequest().build());
//    }
//
//    /**
//     * 추천 언어 목록 - GET /api/github/issues/languages
//     */
//    @GetMapping("/issues/languages")
//    public ResponseEntity<List<Map<String, String>>> getRecommendedLanguages() {
//        log.info("추천 언어 목록 조회");
//
//        List<Map<String, String>> languages = List.of(
//            Map.of("code", "javascript", "name", "JavaScript", "description", "웹 개발의 핵심 언어"),
//            Map.of("code", "python", "name", "Python", "description", "데이터 과학과 AI의 대표 언어"),
//            Map.of("code", "java", "name", "Java", "description", "기업용 애플리케이션 개발"),
//            Map.of("code", "typescript", "name", "TypeScript", "description", "타입 안전성을 제공하는 JavaScript"),
//            Map.of("code", "go", "name", "Go", "description", "클라우드 네이티브 개발"),
//            Map.of("code", "rust", "name", "Rust", "description", "메모리 안전성과 성능"),
//            Map.of("code", "kotlin", "name", "Kotlin", "description", "모던 안드로이드 개발"),
//            Map.of("code", "swift", "name", "Swift", "description", "iOS/macOS 개발"),
//            Map.of("code", "c++", "name", "C++", "description", "시스템 프로그래밍"),
//            Map.of("code", "php", "name", "PHP", "description", "서버사이드 웹 개발")
//        );
//
//        return ResponseEntity.ok(languages);
//    }
//
//    /**
//     * 추천 난이도 라벨 목록 - GET /api/github/issues/difficulty-labels
//     */
//    @GetMapping("/issues/difficulty-labels")
//    public ResponseEntity<List<Map<String, String>>> getDifficultyLabels() {
//        log.info("난이도 라벨 목록 조회");
//
//        List<Map<String, String>> labels = List.of(
//            Map.of("code", "good first issue", "name", "Good First Issue", "description", "GitHub에서 공식 인정하는 초보자용 라벨"),
//            Map.of("code", "beginner-friendly", "name", "Beginner Friendly", "description", "초보자도 쉽게 접근할 수 있는 이슈"),
//            Map.of("code", "easy", "name", "Easy", "description", "쉬운 난이도의 이슈"),
//            Map.of("code", "starter", "name", "Starter", "description", "시작하기 좋은 이슈"),
//            Map.of("code", "first-timers-only", "name", "First Timers Only", "description", "처음 기여하는 사람만을 위한 이슈"),
//            Map.of("code", "help wanted", "name", "Help Wanted", "description", "도움이 필요한 이슈"),
//            Map.of("code", "contributions welcome", "name", "Contributions Welcome", "description", "기여를 환영하는 이슈"),
//            Map.of("code", "up-for-grabs", "name", "Up For Grabs", "description", "누구나 가져가서 작업할 수 있는 이슈")
//        );
//
//        return ResponseEntity.ok(labels);
//    }
//
//    /**
//     * API 상태 확인 - GET /api/github/health
//     */
//    @GetMapping("/health")
//    public ResponseEntity<Map<String, Object>> healthCheck() {
//        Map<String, Object> health = Map.of(
//            "status", "UP",
//            "service", "GitHub Issue Explorer",
//            "version", "1.0.0",
//            "description", "GitHub API를 활용한 오픈소스 이슈 탐색 서비스"
//        );
//
//        return ResponseEntity.ok(health);
//    }
//}
