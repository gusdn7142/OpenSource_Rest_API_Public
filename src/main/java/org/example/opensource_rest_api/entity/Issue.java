package org.example.opensource_rest_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"repository", "labels"})
public class Issue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_id")
    private Long issueId;

    @Column(name = "github_issue_id", unique = true, nullable = false)
    private Long githubIssueId;  // GitHub API의 원본 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false, foreignKey = @ForeignKey(name = "fk_issue_repository"))
    private Repository repository;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "estimated_time", length = 30)
    //백엔드 계산
    private String estimatedTime;  // "1시간 이내", "1-3시간", "3-8시간", "8시간 이상"

    @Column(name = "github_url", nullable = false, length = 500)
    private String githubUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;  // GitHub에서 이슈가 생성된 시간

    @Column(name = "difficulty_level", length = 20)
    //백엔드 계산
    private String difficultyLevel;  // "초급", "중급", "고급"

    @Column(name = "popularity_score")
    private Integer popularityScore;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Label> labels = new ArrayList<>();

    public void addLabel(Label label) {
        labels.add(label);
        label.setIssue(this);
    }
}