package org.example.opensource_rest_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이슈 처리 결과를 담는 DTO 클래스
 * int[] 반환 대신 의미있는 객체로 결과를 전달합니다.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessingResult {
    
    /**
     * 처리된 이슈 수 (신규 저장된 이슈)
     */
    private int processedCount;
    
    /**
     * 건너뛴 이슈 수 (중복 등의 이유로 처리되지 않은 이슈)
     */
    private int skippedCount;
    
    /**
     * 전체 이슈 수 (processed + skipped)
     */
    public int getTotalCount() {
        return processedCount + skippedCount;
    }
    
    /**
     * 처리 성공률 (0.0 ~ 1.0)
     */
    public double getSuccessRate() {
        int total = getTotalCount();
        return total > 0 ? (double) processedCount / total : 0.0;
    }
    
    /**
     * 새로운 이슈가 있었는지 여부
     */
    public boolean hasNewIssues() {
        return processedCount > 0;
    }
    
    /**
     * 결과 요약 문자열
     */
    public String getSummary() {
        return String.format("처리완료: %d개, 중복: %d개, 성공률: %.1f%%", 
                           processedCount, skippedCount, getSuccessRate() * 100);
    }
}