package org.example.opensource_rest_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MVP용 타겟 레포지토리 정보를 담는 DTO 클래스
 * Phase 1에서 수집 대상이 되는 5개 레포지토리의 메타데이터를 관리합니다.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepositoryTarget {
    
    /**
     * GitHub 레포지토리 전체 이름 (owner/repo 형식)
     * 예: "spring-projects/spring-boot", "facebook/react"
     */
    private String fullName;
    
    /**
     * 주요 프로그래밍 언어
     * 백엔드: "java", 프론트엔드: "javascript"
     */
    private String language;
    
    /**
     * 수집 대상 라벨 리스트
     * 각 레포지토리별로 실제 사용하는 라벨만 포함
     * 예: ["good first issue", "status: waiting-for-triage"]
     */
    private List<String> labels;
}