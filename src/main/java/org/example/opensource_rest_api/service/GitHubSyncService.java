package org.example.opensource_rest_api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.opensource_rest_api.config.DifficultyConfig;
import org.example.opensource_rest_api.config.MVPRepositoryConfig;
import org.example.opensource_rest_api.dto.*;
import org.example.opensource_rest_api.entity.Issue;
import org.example.opensource_rest_api.entity.Label;
import org.example.opensource_rest_api.entity.Repository;
import org.example.opensource_rest_api.repository.IssueRepository;
import org.example.opensource_rest_api.repository.RepositoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * GitHub 이슈 수집 서비스
 * 
 * 여러 오픈소스 저장소에서 오픈 이슈를 주기적으로 수집합니다.
 * 전체 이슈 데이터를 수집하여 개발자들이 기여할 수 있는 프로젝트를 찾을 수 있도록 도움니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubSyncService {

    // 동기화 관련 상수
    private static final Duration SYNC_INTERVAL = Duration.ofHours(4);
    private static final Duration RATE_LIMIT_DELAY = Duration.ofSeconds(30); // Secondary Rate Limit 대응으로 30초로 증가
    private static final Duration RETRY_DELAY_AFTER_RATE_LIMIT = Duration.ofMinutes(2); // Rate Limit 후 2분 대기
    private static final int MAX_PAGES_PER_REPO = 10; // 저장소당 최대 10페이지 (1000개 이슈)
    private static final int ISSUES_PER_PAGE = 100; // 페이지당 이슈 수 (GitHub API 최대값)
    
    // 난이도 관련 상수
    private static final String DIFFICULTY_BEGINNER = "초급";
    private static final String DIFFICULTY_INTERMEDIATE = "중급"; 
    private static final String DIFFICULTY_ADVANCED = "고급";
    
    // 인기도 계산 상수
    private static final int MAX_POPULARITY_SCORE = 100;
    private static final int MIN_POPULARITY_SCORE = 0;

    // MVP용 새로운 서비스들
    private final GitHubDirectApiService githubDirectApiService;
    private final MVPRepositoryConfig mvpRepositoryConfig;
    private final DifficultyConfig difficultyConfig;

    // 데이터 저장소
    private final RepositoryRepository repositoryRepository;
    private final IssueRepository issueRepository;

    /**
     * GitHub 이슈 동기화 - 4시간마다 실행
     * 
     * 대상 저장소에서 오픈 이슈를 수집하여 데이터베이스에 저장합니다.
     * 각 저장소에서 최대 1000개의 이슈를 수집합니다.
     */
    @Scheduled(fixedDelayString = "#{T(java.time.Duration).ofHours(4).toMillis()}") // 4시간마다 실행 (Rate Limit 고려하여 조정)
    public void syncMVPGitHubIssues() {
        log.info("=== MVP GitHub 이슈 동기화 시작 ===");

        // Rate Limit 상태 확인
        String rateLimitInfo = githubDirectApiService.checkRateLimit();
        if (rateLimitInfo != null) {
            log.info("현재 GitHub API Rate Limit 상태: {}", rateLimitInfo);
        }

        int totalProcessed = 0;
        int totalSkipped = 0;
        int failedRepositories = 0;

        // MVP 저장소 목록 가져오기
        List<RepositoryTarget> mvpRepositories = mvpRepositoryConfig.getMVPRepositories();
        log.info("수집 대상 저장소 수: {}개", mvpRepositories.size());

        // 각 MVP 저장소에서 이슈 수집
        for (RepositoryTarget target : mvpRepositories) {
            try {
                log.info("저장소 수집 시작: {} (언어: {}, 라벨: {})",
                        target.getFullName(), target.getLanguage(), target.getLabels());

                // 다중 페이지 처리로 더 많은 이슈 수집
                ProcessingResult repositoryResult = collectAllIssuesFromRepository(target);
                totalProcessed += repositoryResult.getProcessedCount();
                totalSkipped += repositoryResult.getSkippedCount();

                log.info("저장소 수집 완료: {} - {}", target.getFullName(), repositoryResult.getSummary());

            } catch (Exception e) {
                failedRepositories++;
                log.error("저장소 수집 실패: {} - {}", target.getFullName(), e.getMessage(), e);
            }
        }

        log.info("=== MVP 동기화 완료: 총 신규 {}개, 중복 {}개, 실패 {}개 저장소 ===",
                totalProcessed, totalSkipped, failedRepositories);
    }
    
    /**
     * 저장소에서 모든 오픈 이슈를 수집합니다.
     * 
     * @param target 대상 저장소
     * @return 처리 결과
     */
    private ProcessingResult collectAllIssuesFromRepository(RepositoryTarget target) {
        int totalProcessed = 0;
        int totalSkipped = 0;
        int currentPage = 1;
        boolean hasMorePages = true;
        
        log.info("다중 페이지 이슈 수집 시작: {} (최대 {}P 처리)", 
                target.getFullName(), MAX_PAGES_PER_REPO);
        
        while (hasMorePages && currentPage <= MAX_PAGES_PER_REPO) {
            try {
                log.debug("페이지 {} 처리 시작: {}", currentPage, target.getFullName());
                
                // API 호출
                GitHubSearchResponse response = githubDirectApiService.searchRepositoryIssuesWithPagination(
                        target.getFullName(),
                        target.getLabels(),
                        currentPage,
                        ISSUES_PER_PAGE
                );
                
                // Rate Limit 방지 대기
                try {
                    Thread.sleep(RATE_LIMIT_DELAY.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("요청 간격 대기 중 인터럽트 발생");
                    break;
                }
                
                if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
                    ProcessingResult pageResult = processMVPGitHubResponse(response, target);
                    totalProcessed += pageResult.getProcessedCount();
                    totalSkipped += pageResult.getSkippedCount();
                    
                    log.info("페이지 {} 처리 완료: {} - 수집 {}+{}개, {}", 
                            currentPage, target.getFullName(), 
                            pageResult.getProcessedCount(), pageResult.getSkippedCount(),
                            response.getItems().size() < ISSUES_PER_PAGE ? "마지막 페이지" : "계속");
                    
                    // 마지막 페이지 확인
                    if (response.getItems().size() < ISSUES_PER_PAGE) {
                        hasMorePages = false;
                        log.debug("마지막 페이지 도달: {} ({}< {})", 
                                target.getFullName(), response.getItems().size(), ISSUES_PER_PAGE);
                    }
                } else {
                    log.warn("페이지 {} 데이터 없음: {} - Rate Limit 또는 API 오류", 
                            currentPage, target.getFullName());
                    
                    // Rate Limit 발생 시 더 긴 대기
                    try {
                        log.info("Rate Limit 복구를 위해 {}초 추가 대기", RETRY_DELAY_AFTER_RATE_LIMIT.getSeconds());
                        Thread.sleep(RETRY_DELAY_AFTER_RATE_LIMIT.toMillis());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Rate Limit 대기 중 인터럽트 발생");
                    }
                    hasMorePages = false;
                }
                
                currentPage++;
                
            } catch (Exception e) {
                log.error("페이지 {} 처리 실패: {} - {}", 
                        currentPage, target.getFullName(), e.getMessage(), e);
                hasMorePages = false;
            }
        }
        
        ProcessingResult finalResult = new ProcessingResult(totalProcessed, totalSkipped);
        log.info("다중 페이지 수집 완료: {} - 총 {}P 처리, {}", 
                target.getFullName(), currentPage - 1, finalResult.getSummary());
        
        return finalResult;
    }


    /**
     * GitHub API 응답을 처리하여 이슈를 데이터베이스에 저장합니다.
     * 
     * @param response 검색 응답
     * @param target 대상 저장소
     * @return 처리 결과
     */
    @Transactional
    protected ProcessingResult processMVPGitHubResponse(GitHubSearchResponse response, RepositoryTarget target) {
        int processedCount = 0;
        int skippedCount = 0;

        if (response.getItems() == null || response.getItems().isEmpty()) {
            log.info("수집된 이슈가 없습니다 - Repository: {}", target.getFullName());
            return new ProcessingResult(0, 0);
        }

        log.info("MVP 이슈 처리 시작 - Repository: {}, 이슈 수: {}개",
                target.getFullName(), response.getItems().size());

        for (GitHubIssue gitHubIssue : response.getItems()) {
            try {
                if (processMVPIssue(gitHubIssue, target)) {
                    processedCount++;
                    log.debug("MVP 이슈 처리 완료: {} - {}",
                            target.getFullName(), gitHubIssue.getTitle());
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                log.error("MVP 이슈 처리 실패 - ID: {}, Repo: {}, Error: {}",
                        gitHubIssue.getId(), target.getFullName(), e.getMessage(), e);
            }
        }

        ProcessingResult result = new ProcessingResult(processedCount, skippedCount);
        log.info("MVP 이슈 처리 완료 - Repository: {}, {}", target.getFullName(), result.getSummary());
        return result;
    }

    /**
     * GitHub 이슈를 처리하여 데이터베이스에 저장합니다.
     * 
     * @param dto 이슈 데이터
     * @param target 대상 저장소
     * @return 처리 성공 여부
     */
    private boolean processMVPIssue(GitHubIssue dto, RepositoryTarget target) {
        // 필수 데이터 검증
        if (dto.getId() == null || dto.getTitle() == null) {
            log.warn("MVP 이슈 필수 데이터 누락: Issue ID={}, Title={}, Target={}",
                    dto.getId(), dto.getTitle(), target.getFullName());
            return false;
        }

        // 1. Repository 처리 (UPSERT) - GitHub Search API에서는 repository 객체가 없으므로 target 정보 사용
        Repository repository = repositoryRepository
                .findByOwnerAndName(target.getOwner(), target.getName())
                .orElseGet(() -> createMVPRepositoryFromTarget(target));

        // 2. 중복 체크
        if (issueRepository.existsByGithubIssueId(dto.getId())) {
            log.trace("MVP 이미 존재하는 이슈: {} - {}", target.getFullName(), dto.getId());
            return false;
        }

        // 3. Issue 생성 - 타겟 정보 활용
        Issue issue = Issue.builder()
                .githubIssueId(dto.getId())
                .repository(repository)
                .title(dto.getTitle() != null ? dto.getTitle() : "제목 없음")
                .githubUrl(dto.getHtmlUrl())
                .createdAt(dto.getCreatedAt())
                .difficultyLevel(calculateMVPDifficulty(dto.getLabels(), target))
                .estimatedTime(calculateMVPEstimatedTime(dto.getLabels(), target))
                .popularityScore(calculatePopularity(dto.getComments()))
                .build();

        // 4. Labels 추가 - 필터링된 라벨만
        if (dto.getLabels() != null && !dto.getLabels().isEmpty()) {
            for (GitHubLabel labelDto : dto.getLabels()) {
                //  타겟 라벨에 포함된 것만 저장
                if (isTargetLabel(labelDto.getName(), target.getLabels())) {
                    Label label = Label.builder()
                            .labelName(labelDto.getName())
                            .labelColor("#" + labelDto.getColor())
                            .build();
                    issue.addLabel(label);
                }
            }
        }

        issueRepository.save(issue);
        log.debug(" 이슈 저장 완료: {} - {}", target.getFullName(), dto.getTitle());
        return true;
    }

    /**
     * 이슈의 인기도 점수를 계산합니다.
     * 
     * @param comments 댓글 수
     * @return 0-100 사이의 인기도 점수
     */
    private Integer calculatePopularity(Integer comments) {
        if (comments == null || comments <= 0) {
            return 0;
        }
        
        // 1. 댓글 수 기반 점수 (로그 스케일 적용) - 참고용으로 보관
        // 실제 계산은 구간별 보정 사용
        
        // 2. 댓글 수별 구간 보정
        int baseScore;
        if (comments <= 5) {
            baseScore = comments * 8;  // 1-5개: 8~40점
        } else if (comments <= 15) {
            baseScore = 40 + (comments - 5) * 4;  // 6-15개: 44~80점
        } else if (comments <= 30) {
            baseScore = 80 + (comments - 15) * 2;  // 16-30개: 82~110점
        } else {
            // 30개 이상은 급격한 증가 억제
            baseScore = 110 + (int) Math.log(comments - 29) * 10;
        }
        
        // 3. 최종 점수 계산 및 상한선 적용
        int finalScore = Math.min(baseScore, MAX_POPULARITY_SCORE);  // 최대 점수 제한
        
        return Math.max(finalScore, MIN_POPULARITY_SCORE);  // 최소 점수 제한
    }

    /**
     * Repository 엔티티를 생성합니다.
     * 
     * @param target 대상 저장소 정보
     * @return 생성된 Repository
     */
    private Repository createMVPRepositoryFromTarget(RepositoryTarget target) {
        try {
            // GitHub Repository API 호출하여 실제 정보 가져오기
            GitHubRepository repoInfo = githubDirectApiService.getRepositoryInfo(target.getFullName());
            
            if (repoInfo == null || repoInfo.getId() == null) {
                throw new IllegalStateException("GitHub API가 유효하지 않은 저장소 정보를 반환했습니다: " + target.getFullName());
            }

            Repository repository = Repository.builder()
                    .githubRepoId(repoInfo.getId())
                    .owner(target.getOwner())
                    .name(target.getName())
                    .githubUrl(repoInfo.getHtmlUrl() != null ? repoInfo.getHtmlUrl() : "https://github.com/" + target.getFullName())
                    .primaryLanguage(repoInfo.getLanguage() != null ? repoInfo.getLanguage() : target.getLanguage())
                    .starsCount(repoInfo.getStargazersCount() != null ? repoInfo.getStargazersCount() : 0)
                    .build();

            repository = repositoryRepository.save(repository);
            log.info("MVP 새 저장소 등록 (GitHub API 기반): {} (ID: {}, 언어: {}, 스타: {})",
                    target.getFullName(), repoInfo.getId(), repository.getPrimaryLanguage(), repository.getStarsCount());
            return repository;

        } catch (Exception e) {
            // API 실패는 심각한 문제 - 데이터 무결성을 위해 예외를 전파
            log.error("GitHub Repository API 호출 실패 - 저장소 생성 중단: {} - 원인: {}", 
                     target.getFullName(), e.getMessage(), e);
            
            // 임시 ID 사용 대신 예외를 던져 상위에서 처리하도록 함
            throw new RuntimeException(
                String.format("저장소 정보를 가져올 수 없습니다: %s (원인: %s)", 
                             target.getFullName(), e.getMessage()), e);
        }
    }

    /**
     * 이슈의 난이도를 계산합니다.
     * 
     * @param labels 이슈 라벨 목록
     * @param target 대상 저장소
     * @return 난이도 ("초급", "중급", "고급")
     */
    private String calculateMVPDifficulty(List<GitHubLabel> labels, RepositoryTarget target) {
        if (labels == null || labels.isEmpty()) {
            return DIFFICULTY_INTERMEDIATE;  // 기본값
        }

        int score = calculateDifficultyScore(labels, target);
        return convertScoreToDifficulty(score);
    }
    
    // 라벨 기반 난이도 점수 계산
    private int calculateDifficultyScore(List<GitHubLabel> labels, RepositoryTarget target) {
        int score = 0;
        Map<String, Integer> repoCustomWeights = getRepositoryCustomWeights(target);
        
        for (GitHubLabel label : labels) {
            String labelName = label.getName().toLowerCase();
            
            // 타겟 라벨 확인
            if (!isValidTargetLabel(labelName, target)) {
                continue;
            }
            
            // 가중치 적용
            int weight = calculateLabelWeight(labelName, repoCustomWeights);
            score += weight;
        }
        
        return score;
    }
    
    // 저장소별 커스텀 가중치 조회
    private Map<String, Integer> getRepositoryCustomWeights(RepositoryTarget target) {
        if (target == null || target.getFullName() == null) {
            return Map.of();  // 빈 맵 반환 (null 대신)
        }
        
        Map<String, Map<String, Integer>> customWeights = difficultyConfig.getRepositoryCustomWeights();
        Map<String, Integer> result = customWeights.get(target.getFullName());
        return result != null ? result : Map.of();  // null 대신 빈 맵 반환
    }
    
    // 라벨 유효성 검사
    private boolean isValidTargetLabel(String labelName, RepositoryTarget target) {
        if (target == null || target.getLabels() == null) {
            return true;  // 타겟 라벨 제한이 없으면 모든 라벨 허용
        }
        
        boolean isValid = target.getLabels().stream()
                .anyMatch(targetLabel -> targetLabel.toLowerCase().equals(labelName));
        
        if (!isValid) {
            log.trace("라벨 '{}'는 {} 저장소의 타겟 라벨이 아님, 건너뜀",
                    labelName, target.getFullName());
        }
        
        return isValid;
    }
    
    // 라벨별 가중치 계산
    private int calculateLabelWeight(String labelName, Map<String, Integer> repoCustomWeights) {
        // 1. 저장소별 커스텀 가중치 우선 적용
        if (repoCustomWeights != null) {
            for (Map.Entry<String, Integer> entry : repoCustomWeights.entrySet()) {
                if (labelName.contains(entry.getKey().toLowerCase())) {
                    log.trace("저장소 커스텀 가중치 적용: {} -> {}", labelName, entry.getValue());
                    return entry.getValue();
                }
            }
        }
        
        // 2. 전역 가중치 적용
        Map<String, Integer> labelWeights = difficultyConfig.getLabelWeights();
        for (Map.Entry<String, Integer> entry : labelWeights.entrySet()) {
            if (labelName.contains(entry.getKey().toLowerCase())) {
                log.trace("전역 가중치 적용: {} -> {}", labelName, entry.getValue());
                return entry.getValue();
            }
        }
        
        return 0;  // 매칭되는 가중치가 없으면 0점
    }
    
    // 점수를 난이도로 변환
    private String convertScoreToDifficulty(int score) {
        DifficultyConfig.DifficultyThresholds thresholds = difficultyConfig.getThresholds();
        String difficulty;
        
        if (score < thresholds.getBeginner()) {
            difficulty = DIFFICULTY_BEGINNER;
        } else if (score < thresholds.getIntermediate()) {
            difficulty = DIFFICULTY_INTERMEDIATE;
        } else {
            difficulty = DIFFICULTY_ADVANCED;
        }
        
        log.debug("난이도 계산 완료: score={} -> {}", score, difficulty);
        return difficulty;
    }

    // 시간 추정 상수 정의
    private static final String TIME_UNDER_1H = "1시간 이내";
    private static final String TIME_1_TO_3H = "1-3시간";
    private static final String TIME_3_TO_8H = "3-8시간";
    private static final String TIME_OVER_8H = "8시간 이상";
    
    /**
     * 이슈의 예상 작업 시간을 계산합니다.
     * 
     * @param labels 이슈 라벨 목록
     * @param target 대상 저장소
     * @return 예상 시간
     */
    private String calculateMVPEstimatedTime(List<GitHubLabel> labels, RepositoryTarget target) {
        if (labels == null || labels.isEmpty()) {
            return TIME_1_TO_3H;  // 기본값
        }

        // 1순위: 명시적 size 라벨 확인 (즉시 반환)
        String explicitSize = checkForExplicitSizeLabels(labels);
        if (explicitSize != null) {
            return explicitSize;
        }
        
        // 2순위: 작업 유형과 특성을 종합적으로 고려
        int timeScore = calculateTimeScore(labels, target);
        return convertScoreToTime(timeScore, hasGoodFirstIssue(labels));
    }
    
    // size 라벨 확인
    private String checkForExplicitSizeLabels(List<GitHubLabel> labels) {
        for (GitHubLabel label : labels) {
            String labelName = label.getName().toLowerCase();
            
            if (labelName.contains("size/xs") || labelName.contains("tiny")) {
                return TIME_UNDER_1H;
            }
            if (labelName.contains("size/s") || labelName.contains("small")) {
                return TIME_1_TO_3H;
            }
            if (labelName.contains("size/m") || labelName.contains("medium")) {
                return TIME_3_TO_8H;
            }
            if (labelName.contains("size/l") || labelName.contains("large") || 
                labelName.contains("size/xl") || labelName.contains("huge")) {
                return TIME_OVER_8H;
            }
        }
        return null;  // 명시적 크기 라벨 없음
    }
    
    // 작업 유형별 시간 점수 계산
    private int calculateTimeScore(List<GitHubLabel> labels, RepositoryTarget target) {
        int timeScore = 0;
        
        // 작업 유형별 점수 누적
        for (GitHubLabel label : labels) {
            String labelName = label.getName().toLowerCase();
            
            if (labelName.contains("documentation") || labelName.contains("docs")) {
                timeScore -= 20;  // 문서 작업은 보통 빠름
            }
            else if (labelName.contains("bug") || labelName.contains("fix")) {
                timeScore += 10;  // 버그 수정은 중간
            }
            else if (labelName.contains("feature") || labelName.contains("enhancement")) {
                timeScore += 20;  // 기능 추가는 시간 소요
            }
            else if (labelName.contains("refactor") || labelName.contains("performance")) {
                timeScore += 30;  // 리팩토링/성능 개선은 오래 걸림
            }
            else if (labelName.contains("test") || labelName.contains("testing")) {
                timeScore += 15;  // 테스트 작성
            }
            
            // good first issue 보정
            if (labelName.contains("good first issue") || labelName.contains("beginner")) {
                timeScore -= 15;
            }
        }
        
        // 저장소별 특성 반영
        timeScore += calculateRepositoryComplexityBonus(target);
        
        return timeScore;
    }
    
    // good first issue 라벨 확인
    private boolean hasGoodFirstIssue(List<GitHubLabel> labels) {
        return labels.stream()
                .anyMatch(label -> {
                    String labelName = label.getName().toLowerCase();
                    return labelName.contains("good first issue") || labelName.contains("beginner");
                });
    }
    
    // 저장소 복잡도 보너스 계산
    private int calculateRepositoryComplexityBonus(RepositoryTarget target) {
        if (target == null) {
            return 0;
        }
        
        int complexityBonus = 0;
        
        // 언어별 특성
        if (target.getLanguage() != null && 
            (target.getLanguage().equalsIgnoreCase("javascript") || 
             target.getLanguage().equalsIgnoreCase("typescript"))) {
            complexityBonus += 5;  // 프론트엔드는 UI 작업이 많음
        }
        
        // 프로젝트 규모별 특성
        if (target.getFullName() != null && 
            (target.getFullName().contains("spring") || target.getFullName().contains("elastic"))) {
            complexityBonus += 10;  // 대형 프로젝트는 진입 장벽 높음
        }
        
        return complexityBonus;
    }
    
    // 점수를 예상 시간으로 변환
    private String convertScoreToTime(int timeScore, boolean hasGoodFirstIssue) {
        // 초보자 이슈는 보수적으로 추정
        if (hasGoodFirstIssue && timeScore < 10) {
            return TIME_1_TO_3H;
        }
        
        if (timeScore < -10) return TIME_UNDER_1H;
        else if (timeScore < 15) return TIME_1_TO_3H;
        else if (timeScore < 30) return TIME_3_TO_8H;
        else return TIME_OVER_8H;
    }

    /**
     * 라벨이 수집 대상에 포함되는지 확인합니다.
     * 
     * @param labelName 라벨명
     * @param targetLabels 대상 라벨 목록
     * @return 포함 여부
     */
    private boolean isTargetLabel(String labelName, List<String> targetLabels) {
        if (labelName == null || targetLabels == null) {
            return false;
        }

        String lowerLabelName = labelName.toLowerCase();
        return targetLabels.stream()
                .anyMatch(target -> lowerLabelName.contains(target.toLowerCase()) ||
                        target.toLowerCase().contains(lowerLabelName));
    }
}
