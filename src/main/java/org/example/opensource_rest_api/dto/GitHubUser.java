package org.example.opensource_rest_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GitHubUser {
    private Long id;
    private String login;
    
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    private String type;
}
