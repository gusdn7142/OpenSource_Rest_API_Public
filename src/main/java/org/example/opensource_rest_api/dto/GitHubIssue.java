package org.example.opensource_rest_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class GitHubIssue {
    private Long id;
    private String title;
    private String body;
    private String state;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("closed_at")
    private LocalDateTime closedAt;
    
    private List<GitHubLabel> labels;
    private GitHubUser assignee;
    private GitHubRepository repository;
    
    @JsonProperty("comments_url")
    private String commentsUrl;
    
    private Integer comments;
    
    @JsonProperty("pull_request")
    private Object pullRequest;  // PR인 경우에만 존재
}
