package org.example.opensource_rest_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GitHubSearchResponse {
    
    @JsonProperty("total_count")
    private Integer totalCount;
    
    @JsonProperty("incomplete_results")
    private Boolean incompleteResults;
    
    private List<GitHubIssue> items;
}
