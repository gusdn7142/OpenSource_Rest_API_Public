package org.example.opensource_rest_api.repository;

import org.example.opensource_rest_api.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RepositoryRepository extends JpaRepository<Repository, Long> {
    Optional<Repository> findByGithubRepoId(Long githubRepoId);
    boolean existsByGithubRepoId(Long githubRepoId);
}