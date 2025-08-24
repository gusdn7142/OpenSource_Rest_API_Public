package org.example.opensource_rest_api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.opensource_rest_api.config.MVPRepositoryConfig;
import org.example.opensource_rest_api.dto.GitHubIssue;
import org.example.opensource_rest_api.dto.GitHubLabel;
import org.example.opensource_rest_api.dto.GitHubSearchResponse;
import org.example.opensource_rest_api.dto.RepositoryTarget;
import org.example.opensource_rest_api.entity.Issue;
import org.example.opensource_rest_api.entity.Label;
import org.example.opensource_rest_api.entity.Repository;
import org.example.opensource_rest_api.repository.IssueRepository;
import org.example.opensource_rest_api.repository.RepositoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * GitHub 이슈 동기화 서비스 (MVP 버전)
 *
 * Phase 1: 코어 5개 저장소에서 초보자용 이슈를 수집하는 서비스입니다.
 * RestTemplate 기반의 직접 API 호출 방식을 사용하여 안정적인 데이터 수집을 제공합니다.
 *
 * 주요 기능:
 * - 2시간마다 자동 실행되는 스케줄링
 * - 레포지토리별 맞춤 라벨 기반 이슈 수집
 * - 중복 이슈 방지 및 데이터 정합성 보장
 * - 트랜잭션 기반 안전한 데이터 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubSyncService {

    // 기존 서비스는 레거시 지원용으로 유지
//    private final GitHubIssueService githubIssueService;

    // MVP용 새로운 서비스들
    private final GitHubDirectApiService githubDirectApiService;
    private final MVPRepositoryConfig mvpRepositoryConfig;

    // 데이터 저장소
    private final RepositoryRepository repositoryRepository;
    private final IssueRepository issueRepository;

    /**
     * MVP GitHub 이슈 동기화 - 2시간마다 실행
     *
     * Phase 1 코어 5개 저장소에서 초보자 친화적 이슈를 수집합니다.
     * RestTemplate 기반 동기 방식으로 안정적인 데이터 처리를 보장합니다.
     *
     * 수집 대상:
     * - spring-projects/spring-boot (Java 백엔드)
     * - elastic/elasticsearch (Java 백엔드)
     * - facebook/react (JavaScript 프론트엔드)
     * - vuejs/vue (JavaScript 프론트엔드)
     * - vercel/next.js (JavaScript 프론트엔드)
     *
     * 각 저장소당 최대 20개 이슈, 실제 사용 라벨만 적용
     */
    @Scheduled(fixedDelay = 7200000) // 2시간마다 실행 (MVP 최적화)
    public void syncMVPGitHubIssues() {
        log.info("=== MVP GitHub 이슈 동기화 시작 ===");

        int totalProcessed = 0;
        int totalSkipped = 0;
        int failedRepositories = 0;

        List<RepositoryTarget> mvpRepositories = mvpRepositoryConfig.getMVPRepositories();
        log.info("수집 대상 저장소 수: {}개", mvpRepositories.size());

        // 각 MVP 저장소에서 이슈 수집
        for (RepositoryTarget target : mvpRepositories) {
            try {
                log.info("저장소 수집 시작: {} (언어: {}, 라벨: {})",
                        target.getFullName(), target.getLanguage(), target.getLabels());

                GitHubSearchResponse response = githubDirectApiService.searchRepositoryIssues(
                    target.getFullName(),
                    target.getLabels()
                );

                if (response != null && response.getItems() != null) {
                    int[] results = processMVPGitHubResponse(response, target);
                    totalProcessed += results[0];
                    totalSkipped += results[1];

                    log.info("저장소 수집 완료: {} - 신규: {}개, 중복: {}개",
                            target.getFullName(), results[0], results[1]);
                } else {
                    log.warn("저장소 응답 데이터 없음: {}", target.getFullName());
                }

            } catch (Exception e) {
                failedRepositories++;
                log.error("저장소 수집 실패: {} - {}", target.getFullName(), e.getMessage(), e);
            }
        }

        log.info("=== MVP 동기화 완료: 총 신규 {}개, 중복 {}개, 실패 {}개 저장소 ===",
                totalProcessed, totalSkipped, failedRepositories);
    }

    /**
     * 레거시 GitHub 이슈 동기화 (기존 방식 유지)
     *
     * 기존 시스템과의 호환성을 위해 유지되는 메서드입니다.
     * MVP 이후에는 제거될 예정입니다.
     */
//    @Scheduled(fixedDelay = 3600000) // 1시간마다 실행
//    public void syncLegacyGitHubIssues() {
//        log.info("=== 레거시 GitHub 이슈 동기화 시작 ===");
//
//        try {
//            // 동기 방식으로 처리 (block() 사용)
//            GitHubSearchResponse response = githubIssueService
//                    .searchBeginnerIssues("java", 100, null, 1, 50)
//                    .block(); // 동기 처리
//
//            if (response != null) {
//                log.info("레거시 API 응답 수신: {}개 이슈", response.getItems().size());
//                processGitHubResponse(response);
//            }
//        } catch (Exception e) {
//            log.error("레거시 GitHub API 호출 실패", e);
//        }
//    }

    /**
     * MVP GitHub 응답 처리
     *
     * RestTemplate 기반으로 수집된 이슈 데이터를 처리합니다.
     * 레포지토리별 맞춤 라벨과 메타데이터를 활용하여 더 정확한 데이터를 저장합니다.
     *
     * @param response GitHub API 검색 응답
     * @param target 수집 대상 저장소 정보 (라벨, 언어 등 메타데이터 포함)
     * @return int[] {처리된 이슈 수, 중복 이슈 수}
     */
    @Transactional
    protected int[] processMVPGitHubResponse(GitHubSearchResponse response, RepositoryTarget target) {
        int processedCount = 0;
        int skippedCount = 0;

        if (response.getItems() == null || response.getItems().isEmpty()) {
            log.info("수집된 이슈가 없습니다 - Repository: {}", target.getFullName());
            return new int[]{0, 0};
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

        log.info("MVP 이슈 처리 완료 - Repository: {}, 신규: {}개, 중복: {}개",
                target.getFullName(), processedCount, skippedCount);
        return new int[]{processedCount, skippedCount};
    }

    /**
     * 레거시 GitHub 응답 처리 (기존 방식)
     *
     * 기존 WebClient 기반으로 수집된 데이터를 처리하는 메서드입니다.
     * MVP 이후 단계적으로 제거될 예정입니다.
     */
    @Transactional
    protected void processGitHubResponse(GitHubSearchResponse response) {
        int processedCount = 0;
        int skippedCount = 0;

        for (GitHubIssue gitHubIssue : response.getItems()) {
            try {
                if (processIssue(gitHubIssue)) {
                    processedCount++;
                    log.debug("처리 완료: {} - {}",
                            gitHubIssue.getRepository().getFullName(),
                            gitHubIssue.getTitle());
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                log.error("이슈 처리 실패 - ID: {}, Repo: {}",
                        gitHubIssue.getId(),
                        gitHubIssue.getRepository().getFullName(), e);
            }
        }

        log.info("=== 동기화 완료: 신규 {}개, 중복 {}개 ===", processedCount, skippedCount);
    }

    /**
     * MVP 개별 이슈 처리
     *
     * RepositoryTarget의 메타데이터를 활용하여 더 정확한 이슈 분류를 수행합니다.
     * 레포지토리별 맞춤 라벨 정보를 기반으로 난이도와 예상 시간을 계산합니다.
     *
     * @param dto GitHub 이슈 데이터
     * @param target 수집 대상 저장소 정보 (언어, 라벨 등)
     * @return 처리 성공 여부 (true: 신규 저장, false: 중복 또는 실패)
     */
    private boolean processMVPIssue(GitHubIssue dto, RepositoryTarget target) {
        // 필수 데이터 검증
        if (dto.getRepository() == null || dto.getId() == null) {
            log.warn("MVP 이슈 필수 데이터 누락: Issue ID={}, Repository={}",
                    dto.getId(), target.getFullName());
            return false;
        }

        // 1. Repository 처리 (UPSERT) - MVP 정보 활용
        Repository repository = repositoryRepository
                .findByGithubRepoId(dto.getRepository().getId())
                .orElseGet(() -> createMVPRepository(dto.getRepository(), target));

        // 스타 수 업데이트
        if (dto.getRepository().getStargazersCount() != null) {
            repository.setStarsCount(dto.getRepository().getStargazersCount());
            repository = repositoryRepository.save(repository);
        }

        // 2. 중복 체크
        if (issueRepository.existsByGithubIssueId(dto.getId())) {
            log.trace("MVP 이미 존재하는 이슈: {} - {}", target.getFullName(), dto.getId());
            return false;
        }

        // 3. MVP Issue 생성 - 타겟 정보 활용
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

        // 4. MVP Labels 추가 - 필터링된 라벨만
        if (dto.getLabels() != null && !dto.getLabels().isEmpty()) {
            for (GitHubLabel labelDto : dto.getLabels()) {
                // MVP 타겟 라벨에 포함된 것만 저장
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
        log.debug("MVP 이슈 저장 완료: {} - {}", target.getFullName(), dto.getTitle());
        return true;
    }

    /**
     * 레거시 개별 이슈 처리 (기존 방식)
     *
     * 기존 로직을 유지하여 호환성을 보장합니다.
     * MVP 이후 단계적으로 제거될 예정입니다.
     */
    private boolean processIssue(GitHubIssue dto) {
        // 필수 데이터 검증
        if (dto.getRepository() == null || dto.getId() == null) {
            log.warn("필수 데이터 누락: Issue ID={}", dto.getId());
            return false;
        }

        // 1. Repository 처리 (UPSERT)
        Repository repository = repositoryRepository
                .findByGithubRepoId(dto.getRepository().getId())
                .orElseGet(() -> createRepository(dto.getRepository()));

        // 스타 수 업데이트
        if (dto.getRepository().getStargazersCount() != null) {
            repository.setStarsCount(dto.getRepository().getStargazersCount());
            repository = repositoryRepository.save(repository);
        }

        // 2. 중복 체크
        if (issueRepository.existsByGithubIssueId(dto.getId())) {
            log.trace("이미 존재하는 이슈: {}", dto.getId());
            return false;
        }

        // 3. Issue 생성 (githubIssueId 필드 추가!)
        Issue issue = Issue.builder()
                .githubIssueId(dto.getId())  // ★ 중요: GitHub Issue ID 설정
                .repository(repository)
                .title(dto.getTitle() != null ? dto.getTitle() : "제목 없음")
                .githubUrl(dto.getHtmlUrl())
                .createdAt(dto.getCreatedAt())
                .difficultyLevel(calculateDifficulty(dto.getLabels()))
                .estimatedTime(estimateTime(dto.getLabels()))
                .popularityScore(calculatePopularity(dto.getComments()))
                .build();

        // 4. Labels 추가
        if (dto.getLabels() != null && !dto.getLabels().isEmpty()) {
            for (GitHubLabel labelDto : dto.getLabels()) {
                Label label = Label.builder()
                        .labelName(labelDto.getName())
                        .labelColor("#" + labelDto.getColor())
                        .build();
                issue.addLabel(label);
            }
        }

        issueRepository.save(issue);
        return true;
    }

    /**
     * Repository 엔티티 생성
     */
    private Repository createRepository(org.example.opensource_rest_api.dto.GitHubRepository dto) {
        String fullName = dto.getFullName() != null ? dto.getFullName() : "";
        String[] parts = fullName.split("/");
        String owner = parts.length > 0 ? parts[0] : "unknown";
        String name = parts.length > 1 ? parts[1] : dto.getName();

        Repository repository = Repository.builder()
                .githubRepoId(dto.getId())
                .owner(owner)  // ★ 중요: owner 필드 추가
                .name(name != null ? name : "unknown")
                .githubUrl(dto.getHtmlUrl() != null ? dto.getHtmlUrl() : "")
                .primaryLanguage(dto.getLanguage())
                .starsCount(dto.getStargazersCount() != null ? dto.getStargazersCount() : 0)
                .build();

        repository = repositoryRepository.save(repository);
        log.info("새 저장소 등록: {} (스타: {})", fullName, repository.getStarsCount());
        return repository;
    }

    /**
     * 난이도 계산 로직
     */
    private String calculateDifficulty(List<GitHubLabel> labels) {
        if (labels == null || labels.isEmpty()) {
            return "중급";
        }

        int score = 0;

        for (GitHubLabel label : labels) {
            String labelName = label.getName().toLowerCase();

            // 난이도 점수 계산
            if (labelName.contains("good first issue") ||
                    labelName.contains("beginner") ||
                    labelName.contains("easy")) {
                score -= 50;
            } else if (labelName.contains("documentation") ||
                    labelName.contains("docs")) {
                score -= 30;
            } else if (labelName.contains("bug")) {
                score += 20;
            } else if (labelName.contains("performance") ||
                    labelName.contains("optimization")) {
                score += 40;
            } else if (labelName.contains("refactor") ||
                    labelName.contains("enhancement")) {
                score += 30;
            }
        }

        // 점수를 난이도로 변환
        if (score < 0) return "초급";
        else if (score < 40) return "중급";
        else return "고급";
    }

    /**
     * 예상 시간 계산 로직
     */
    private String estimateTime(List<GitHubLabel> labels) {
        if (labels == null || labels.isEmpty()) {
            return "1-3시간";
        }

        for (GitHubLabel label : labels) {
            String labelName = label.getName().toLowerCase();

            // size 라벨 기반 시간 추정
            if (labelName.contains("size/xs") || labelName.contains("tiny")) {
                return "1시간 이내";
            }
            if (labelName.contains("size/s") || labelName.contains("small")) {
                return "1-3시간";
            }
            if (labelName.contains("size/m") || labelName.contains("medium")) {
                return "3-8시간";
            }
            if (labelName.contains("size/l") || labelName.contains("large")) {
                return "8시간 이상";
            }
        }

        // good first issue는 보통 짧은 시간
        for (GitHubLabel label : labels) {
            if (label.getName().toLowerCase().contains("good first issue")) {
                return "1-3시간";
            }
        }

        return "3-8시간"; // 기본값
    }

    /**
     * 인기도 점수 계산
     */
    private Integer calculatePopularity(Integer comments) {
        // 댓글 수에 가중치 적용
        return comments != null ? comments * 2 : 0;
    }

    /**
     * MVP용 Repository 엔티티 생성
     *
     * RepositoryTarget의 메타데이터를 활용하여 더 정확한 저장소 정보를 생성합니다.
     * 기본 언어 정보가 누락된 경우 타겟 설정의 언어를 사용합니다.
     *
     * @param dto GitHub 저장소 데이터
     * @param target MVP 저장소 타겟 정보
     * @return 생성된 Repository 엔티티
     */
    private Repository createMVPRepository(org.example.opensource_rest_api.dto.GitHubRepository dto,
                                         RepositoryTarget target) {
        String fullName = dto.getFullName() != null ? dto.getFullName() : target.getFullName();
        String[] parts = fullName.split("/");
        String owner = parts.length > 0 ? parts[0] : "unknown";
        String name = parts.length > 1 ? parts[1] : dto.getName();

        // MVP: 타겟 언어 정보를 우선 사용
        String primaryLanguage = dto.getLanguage() != null ? dto.getLanguage() : target.getLanguage();

        Repository repository = Repository.builder()
                .githubRepoId(dto.getId())
                .owner(owner)
                .name(name != null ? name : "unknown")
                .githubUrl(dto.getHtmlUrl() != null ? dto.getHtmlUrl() : "")
                .primaryLanguage(primaryLanguage)  // MVP 개선: 타겟 정보 활용
                .starsCount(dto.getStargazersCount() != null ? dto.getStargazersCount() : 0)
                .build();

        repository = repositoryRepository.save(repository);
        log.info("MVP 새 저장소 등록: {} (언어: {}, 스타: {})",
                fullName, primaryLanguage, repository.getStarsCount());
        return repository;
    }

    /**
     * MVP용 난이도 계산 로직
     *
     * RepositoryTarget의 실제 라벨 정보를 기반으로 더 정확한 난이도를 계산합니다.
     * 저장소별 고유 라벨 체계를 반영하여 개선된 분류 정확도를 제공합니다.
     *
     * @param labels 이슈의 라벨 목록
     * @param target 저장소 타겟 정보 (맞춤 라벨 포함)
     * @return 계산된 난이도 ("초급", "중급", "고급")
     */
    private String calculateMVPDifficulty(List<GitHubLabel> labels, RepositoryTarget target) {
        if (labels == null || labels.isEmpty()) {
            return "중급";  // 기본값
        }

        int score = 0;

        for (GitHubLabel label : labels) {
            String labelName = label.getName().toLowerCase();

            // 기본 초보자 라벨
            if (labelName.contains("good first issue") ||
                labelName.contains("beginner") ||
                labelName.contains("easy")) {
                score -= 50;
            }

            // MVP: 저장소별 맞춤 초보자 라벨 (Spring Boot 예시)
            else if (labelName.contains("waiting-for-triage")) {
                score -= 30;  // 검토 대기 중인 이슈는 비교적 쉬움
            }

            // MVP: 저장소별 맞춤 라벨 (Vue, Next.js 등)
            else if (labelName.contains("contribution welcome") ||
                     labelName.contains("documentation")) {
                score -= 30;  // 기여 환영, 문서 작업은 접근하기 쉬움
            }

            // 난이도 증가 라벨들
            else if (labelName.contains("bug")) {
                score += 20;
            }
            else if (labelName.contains("performance") ||
                     labelName.contains("optimization")) {
                score += 40;
            }
            else if (labelName.contains("refactor") ||
                     labelName.contains("enhancement")) {
                score += 30;
            }

            // MVP: 컴포넌트 관련 (React 등)
            else if (labelName.contains("component:")) {
                score += 10;  // 컴포넌트 작업은 약간 복잡
            }
        }

        // 점수를 난이도로 변환
        if (score < -20) return "초급";      // MVP: 임계값 조정
        else if (score < 20) return "중급";
        else return "고급";
    }

    /**
     * MVP용 예상 시간 계산 로직
     *
     * 저장소별 라벨 특성을 반영하여 더 정확한 시간 추정을 제공합니다.
     *
     * @param labels 이슈의 라벨 목록
     * @param target 저장소 타겟 정보
     * @return 계산된 예상 시간
     */
    private String calculateMVPEstimatedTime(List<GitHubLabel> labels, RepositoryTarget target) {
        if (labels == null || labels.isEmpty()) {
            return "1-3시간";  // 기본값
        }

        for (GitHubLabel label : labels) {
            String labelName = label.getName().toLowerCase();

            // 표준 size 라벨
            if (labelName.contains("size/xs") || labelName.contains("tiny")) {
                return "1시간 이내";
            }
            if (labelName.contains("size/s") || labelName.contains("small")) {
                return "1-3시간";
            }
            if (labelName.contains("size/m") || labelName.contains("medium")) {
                return "3-8시간";
            }
            if (labelName.contains("size/l") || labelName.contains("large")) {
                return "8시간 이상";
            }

            // MVP: 저장소별 특성 반영
            if (labelName.contains("documentation") || labelName.contains("type: documentation")) {
                return "1-3시간";  // 문서 작업은 비교적 빠름
            }
            if (labelName.contains("waiting-for-triage")) {
                return "1-3시간";  // 트리아지 대기는 보통 간단함
            }
        }

        // good first issue는 보통 짧은 시간
        for (GitHubLabel label : labels) {
            if (label.getName().toLowerCase().contains("good first issue")) {
                return "1-3시간";
            }
        }

        return "3-8시간"; // 기본값
    }

    /**
     * 타겟 라벨 포함 여부 확인
     *
     * 이슈의 라벨이 MVP 수집 대상 라벨에 포함되는지 확인합니다.
     * 대소문자를 구분하지 않고 부분 매칭을 지원합니다.
     *
     * @param labelName 확인할 라벨 이름
     * @param targetLabels 타겟 저장소의 수집 대상 라벨 목록
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

    /**
     * MVP 수동 동기화 트리거 (테스트용)
     *
     * 개발 및 테스트 목적으로 MVP 동기화를 수동 실행합니다.
     */
    public void syncMVPNow() {
        log.info("MVP 수동 동기화 시작");
        syncMVPGitHubIssues();
    }

}
