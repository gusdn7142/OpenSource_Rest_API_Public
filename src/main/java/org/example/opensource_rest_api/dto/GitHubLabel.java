package org.example.opensource_rest_api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GitHubLabel {
    private Long id;
    private String name;
    private String description;
    private String color;
    private boolean isDefault;
}
