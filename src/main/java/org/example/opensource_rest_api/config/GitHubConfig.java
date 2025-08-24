package org.example.opensource_rest_api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class GitHubConfig {

    @Value("${github.token:}")
    private String githubToken;

//    @Bean
//    public WebClient githubWebClient() {
//        WebClient.Builder builder = WebClient.builder()
//                .baseUrl("https://api.github.com")
//                .defaultHeader("Accept", "application/vnd.github+json")
//                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
//                .defaultHeader("User-Agent", "OpenSource-Rest-API/1.0");
//
//        // 토큰이 설정되어 있으면 Authorization 헤더 추가
//        if (githubToken != null && !githubToken.isEmpty()) {
//            builder.defaultHeader("Authorization", "Bearer " + githubToken);
//        }
//
//        //log.info("githubToken : " + githubToken);
//
//        return builder.build();
//    }

    /**
     * RestTemplate Bean 설정
     * <p>
     * MVP에서 GitHub API 직접 호출을 위한 RestTemplate을 구성합니다.
     * 동기 방식의 HTTP 클라이언트로 스케줄링 작업에 적합합니다.
     *
     * @return 설정된 RestTemplate 인스턴스
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
