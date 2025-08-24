package org.example.opensource_rest_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class GitHubRepository {
    private Long id;
    private String name;
    
    @JsonProperty("full_name")
    private String fullName;
    
    private String description;
    private String language;
    private List<String> topics;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    @JsonProperty("clone_url")
    private String cloneUrl;
    
    @JsonProperty("stargazers_count")
    private Integer stargazersCount;
    
    @JsonProperty("forks_count")
    private Integer forksCount;
    
    @JsonProperty("watchers_count")
    private Integer watchersCount;
    
    @JsonProperty("open_issues_count")
    private Integer openIssuesCount;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("pushed_at")
    private LocalDateTime pushedAt;
    
    private GitHubUser owner;
    private boolean fork;
    private boolean archived;
    private boolean disabled;
}
