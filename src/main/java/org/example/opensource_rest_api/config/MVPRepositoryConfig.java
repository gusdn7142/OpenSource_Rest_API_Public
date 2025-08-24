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
     * - "good first issue" 라벨 활용
     * - 백엔드(2개) + 프론트엔드(3개) 기술 다양성
     * 
     * 실제 저장소 검증 완료:
     * - Spring Boot: "status: ideal-for-contribution" (실제 존재)
     * - React: "Component: Developer Tools" (실제 존재)
     * - Vue: "contribution welcome" (실제 존재)
     * - Next.js: "Documentation" (실제 존재)
     * - Elasticsearch: "help wanted" (실제 존재)
     */
    public static final List<RepositoryTarget> MVP_REPOSITORIES = List.of(
        
        // 백엔드 레포지토리
        new RepositoryTarget(
            "spring-projects/spring-boot", 
            "java",
            List.of("good first issue", "status: ideal-for-contribution")
        ),
        
        new RepositoryTarget(
            "elastic/elasticsearch", 
            "java",
            List.of("good first issue", "help wanted")
        ),
        
        // 프론트엔드 레포지토리
        new RepositoryTarget(
            "facebook/react", 
            "javascript",
            List.of("good first issue", "Component: Developer Tools")
        ),
        
        new RepositoryTarget(
            "vuejs/vue", 
            "javascript",
            List.of("good first issue", "contribution welcome")
        ),
        
        new RepositoryTarget(
            "vercel/next.js", 
            "javascript",
            List.of("good first issue", "Documentation")
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