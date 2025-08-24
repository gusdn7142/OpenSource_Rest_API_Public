package org.example.opensource_rest_api.repository;

import org.example.opensource_rest_api.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    Optional<Issue> findByGithubIssueId(Long githubIssueId);
    boolean existsByGithubIssueId(Long githubIssueId);
}
