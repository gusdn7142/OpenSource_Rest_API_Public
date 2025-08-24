package org.example.opensource_rest_api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
//import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

//    @Value("${github.token:}")
//    private String githubToken;
//
//    @GetMapping("/github/direct")
//
//    public ResponseEntity<Map<String, Object>> testGitHub() {
//
//        //log.info("githubToken : " + githubToken);
//
//        try {
//            if (githubToken.equals("YOUR_GITHUB_PERSONAL_ACCESS_TOKEN")) {
//                return ResponseEntity.ok(Map.of("status", "error", "message", "토큰 미설정"));
//            }
//
//            String response = WebClient.builder()
//                //.baseUrl("https://20.200.245.245")  //https://api.github.com
//                //.defaultHeader("Host", "api.github.com")  // ← Host 헤더 필수!
//                 .baseUrl("https://api.github.com")
//                 .defaultHeader("Authorization", "Bearer " + githubToken)
//                .defaultHeader("Accept", "application/vnd.github+json")
//                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")  // 추가!
//                .build()
//                .get()
//                .uri("/search/issues?q=bug")    //
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
//
//            return ResponseEntity.ok(Map.of("status", "success", "data", response));
//
//        } catch (Exception e) {
//            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
//        }
//    }
//
//    @GetMapping("/github/rest-template")
//    public ResponseEntity<Map<String, Object>> testGitHubWithRestTemplate() {
//        try {
////            if (githubToken.equals("YOUR_GITHUB_PERSONAL_ACCESS_TOKEN")) {
////                return ResponseEntity.ok(Map.of("status", "error", "message", "토큰 미설정"));
////            }
//
//            // RestTemplate 생성
//            RestTemplate restTemplate = new RestTemplate();
//
//            // 헤더 설정
//            HttpHeaders headers = new HttpHeaders();
//            headers.set("Authorization", "Bearer " + githubToken);
//            headers.set("Accept", "application/vnd.github+json");
//            headers.set("X-GitHub-Api-Version", "2022-11-28");
//
//            HttpEntity<String> entity = new HttpEntity<>(headers);
//
//            // API 호출
//            String response = restTemplate.exchange(
//                "https://api.github.com/search/issues?q=label:\"good first issue\" state:open type:issue&per_page=1",
//                HttpMethod.GET,
//                entity,
//                String.class
//            ).getBody();
//
//            // JSON 파싱 및 로그 출력을 별도 메서드로 호출
//            parseAndLogGitHubResponse(response);
//
//            return ResponseEntity.ok(Map.of("status", "success", "data", response));
//
//        } catch (Exception e) {
//            log.error("GitHub API 호출 실패: {}", e.getMessage());
//            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
//        }
//    }
//
//    // JSON 파싱 및 로그 출력 전용 메서드
//    private void parseAndLogGitHubResponse(String response) {
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode jsonNode = mapper.readTree(response);
//
//            // 필요한 정보 추출 및 로그 출력
//            int totalCount = jsonNode.get("total_count").asInt();
//            JsonNode items = jsonNode.get("items");
//
//            log.info("=== GitHub API 응답 분석 ===");
//            log.info("총 매칭 이슈 수: {}", totalCount);
//            log.info("반환된 이슈 수: {}", items.size());
//
//            if (items.isArray() && items.size() > 0) {
//                JsonNode firstItem = items.get(0);
//                String title = firstItem.get("title").asText();
//                String htmlUrl = firstItem.get("html_url").asText();
//                String state = firstItem.get("state").asText();
//
//                log.info("--- 첫 번째 이슈 정보 ---");
//                log.info("제목: {}", title);
//                log.info("URL: {}", htmlUrl);
//                log.info("상태: {}", state);
//
//                // 라벨 정보 추출
//                JsonNode labels = firstItem.get("labels");
//                if (labels.isArray() && labels.size() > 0) {
//                    StringBuilder labelNames = new StringBuilder();
//                    for (JsonNode label : labels) {
//                        if (labelNames.length() > 0) labelNames.append(", ");
//                        labelNames.append(label.get("name").asText());
//                    }
//                    log.info("라벨: [{}]", labelNames.toString());
//                }
//
//                // 작성자 정보
//                JsonNode user = firstItem.get("user");
//                if (user != null) {
//                    log.info("작성자: {}", user.get("login").asText());
//                }
//            }
//
//        } catch (Exception e) {
//            log.error("JSON 파싱 실패: {}", e.getMessage());
//        }
//    }
}
