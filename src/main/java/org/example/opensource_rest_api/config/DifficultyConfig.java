package org.example.opensource_rest_api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 난이도 계산을 위한 설정 클래스
 * 라벨별 가중치와 난이도 임계값을 동적으로 관리합니다.
 */
@Configuration
@ConfigurationProperties(prefix = "difficulty")
@Data
public class DifficultyConfig {
    
    /**
     * 라벨 패턴별 가중치 매핑
     * 음수: 난이도 감소 (쉬운 작업)
     * 양수: 난이도 증가 (어려운 작업)
     */
    private Map<String, Integer> labelWeights = new HashMap<>() {{
        // 초보자 친화적 라벨 (난이도 감소)
        put("good first issue", -30);
        put("beginner", -25);
        put("easy", -25);
        put("starter", -20);
        put("help wanted", -15);
        put("contribution welcome", -15);
        
        // 문서 작업 (비교적 쉬움)
        put("documentation", -20);
        put("docs", -20);
        put("type: documentation", -20);
        
        // 대기 상태 (보통 간단한 작업)
        put("waiting-for-triage", -15);
        put("needs-triage", -15);
        
        // 버그 수정 (중간 난이도)
        put("bug", 10);
        put("type: bug", 10);
        put("bugfix", 10);
        
        // 기능 개선 (중간-높은 난이도)
        put("enhancement", 15);
        put("feature", 20);
        put("new feature", 20);
        
        // 리팩토링 (높은 난이도)
        put("refactor", 20);
        put("refactoring", 20);
        
        // 성능 최적화 (높은 난이도)
        put("performance", 25);
        put("optimization", 25);
        
        // 보안 관련 (높은 난이도)
        put("security", 30);
        put("vulnerability", 30);
        
        // 컴포넌트 작업 (중간 난이도)
        put("component:", 10);
        put("module:", 10);
    }};
    
    /**
     * 난이도 레벨별 임계값
     */
    private DifficultyThresholds thresholds = new DifficultyThresholds();
    
    @Data
    public static class DifficultyThresholds {
        private int beginner = -15;    // 이 값 미만: 초급
        private int intermediate = 15;  // beginner 이상 ~ intermediate 미만: 중급
                                        // intermediate 이상: 고급
    }
    
    /**
     * 저장소별 커스텀 가중치 (선택적)
     * key: repository fullName
     * value: 해당 저장소의 특별 라벨 가중치
     */
    private Map<String, Map<String, Integer>> repositoryCustomWeights = new HashMap<>();
}