package org.example.opensource_rest_api.entity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "issues")
public class Repository extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repository_id")
    private Long repositoryId;

    @Column(name = "github_repo_id", unique = true, nullable = false)
    private Long githubRepoId;  // GitHub API의 원본 ID

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "owner", nullable = false)
    private String owner;  // 저장소 소유자 (조직 또는 개인)

    @Column(name = "github_url", nullable = false, length = 500)
    private String githubUrl;

    @Column(name = "primary_language", length = 50)
    private String primaryLanguage;

    @Column(name = "stars_count")
    private Integer starsCount;

    @OneToMany(mappedBy = "repository", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Issue> issues = new ArrayList<>();

}
