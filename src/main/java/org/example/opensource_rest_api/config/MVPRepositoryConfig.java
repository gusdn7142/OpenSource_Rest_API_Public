package org.example.opensource_rest_api.config;

import org.example.opensource_rest_api.dto.RepositoryTarget;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MVP Phase 1용 대상 레포지토리 설정 클래스
 * 
 * 백엔드/프론트엔드 개발자를 위한 코어 5개 저장소를 관리합니다.
 * 각 저장소는 실제 사용하는 라벨을 기반으로 맞춤 설정되어 있습니다.
 */
@Component
public class MVPRepositoryConfig {
    
    /**
     * MVP Phase 1 대상 레포지토리 목록
     * 
     * 선정 기준:
     * - 높은 스타 수 (20k+ stars)
     * - 활발한 커뮤니티와 정기적인 이슈 생성
     * - 초보자도 접근 가능한 다양한 라벨 포함
     * - 백엔드(2개) + 프론트엔드(3개) 기술 다양성
     * 
     * 실제 저장소 라벨 조사 결과 반영:
     * - Spring Boot: waiting-for-triage, type: documentation, type: enhancement 주로 사용
     * - Elasticsearch: Team 라벨과 >test-failure 주로 사용 -> 더 포괄적 접근 필요
     * - React: Component, Status, Type 라벨 체계 사용
     * - Vue.js: 라벨 사용 빈도가 낮음 -> 라벨 없이도 수집
     * - Next.js: 기능별 라벨 (TypeScript, Turbopack 등) 사용
     */
    public static final List<RepositoryTarget> MVP_REPOSITORIES = List.of(
        
        // 백엔드 레포지토리 - 더 포괄적인 라벨 사용
        new RepositoryTarget(
            "spring-projects/spring-boot", 
            "java",
            List.of("type: documentation", "type: enhancement", "status: waiting-for-triage")
        ),
        
        new RepositoryTarget(
            "elastic/elasticsearch", 
            "java",
            List.of(">test-failure", "Team:Core", ":Delivery")
        ),
        
        // 프론트엔드 레포지토리 - 실제 사용 라벨 반영
        new RepositoryTarget(
            "facebook/react", 
            "javascript",
            List.of("Component: Developer Tools", "Status: Unconfirmed", "Type: Bug")
        ),
        
        new RepositoryTarget(
            "vuejs/vue", 
            "javascript",
            List.of() // 라벨 사용 빈도가 낮으므로 라벨 조건 없이 수집
        ),
        
        new RepositoryTarget(
            "vercel/next.js", 
            "javascript",
            List.of("TypeScript", "Documentation", "Turbopack")
        )
    );
    
    /**
     * MVP 레포지토리 목록을 반환합니다.
     * 
     * @return 설정된 5개 MVP 레포지토리 리스트
     */
    public List<RepositoryTarget> getMVPRepositories() {
        return MVP_REPOSITORIES;
    }
    
    /**
     * 특정 언어의 레포지토리 목록을 필터링하여 반환합니다.
     * 
     * @param language 필터링할 언어 ("java" 또는 "javascript")
     * @return 해당 언어의 레포지토리 리스트
     */
    public List<RepositoryTarget> getRepositoriesByLanguage(String language) {
        return MVP_REPOSITORIES.stream()
                .filter(repo -> language.equals(repo.getLanguage()))
                .toList();
    }
    
    /**
     * 백엔드 레포지토리(Java) 목록을 반환합니다.
     * 
     * @return Java 언어 기반 레포지토리 리스트
     */
    public List<RepositoryTarget> getBackendRepositories() {
        return getRepositoriesByLanguage("java");
    }
    
    /**
     * 프론트엔드 레포지토리(JavaScript) 목록을 반환합니다.
     * 
     * @return JavaScript 언어 기반 레포지토리 리스트
     */
    public List<RepositoryTarget> getFrontendRepositories() {
        return getRepositoriesByLanguage("javascript");
    }
}