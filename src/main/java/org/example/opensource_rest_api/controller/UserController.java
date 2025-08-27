//package org.example.opensource_rest_api.controller;
//
//import lombok.extern.slf4j.Slf4j;
//import org.example.opensource_rest_api.model.User;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/users")
//@CrossOrigin(origins = "*")
//public class UserController {
//
//    // 메모리 내 데이터 저장소 (실제로는 데이터베이스 사용)
//    private final Map<Long, User> userStorage = new ConcurrentHashMap<>();
//    private Long nextId = 1L;
//
//    // 생성자에서 샘플 데이터 초기화
//    public UserController() {
//        userStorage.put(1L, new User(1L, "김철수", "kim@example.com", 25, "개발팀"));
//        userStorage.put(2L, new User(2L, "이영희", "lee@example.com", 30, "디자인팀"));
//        userStorage.put(3L, new User(3L, "박민수", "park@example.com", 28, "마케팅팀"));
//        nextId = 4L;
//    }
//
//    /**
//     * 모든 사용자 조회 - GET /api/users
//     */
//    @GetMapping
//    public ResponseEntity<List<User>> getAllUsers() {
//        log.info("전체 사용자 조회 요청");
//        List<User> users = new ArrayList<>(userStorage.values());
//        return ResponseEntity.ok(users);
//    }
//
//    /**
//     * 특정 사용자 조회 - GET /api/users/{id}
//     */
//    @GetMapping("/{id}")
//    public ResponseEntity<User> getUserById(@PathVariable Long id) {
//        log.info("사용자 조회 요청 - ID: {}", id);
//        User user = userStorage.get(id);
//        if (user != null) {
//            return ResponseEntity.ok(user);
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    /**
//     * 새 사용자 생성 - POST /api/users
//     */
//    @PostMapping
//    public ResponseEntity<User> createUser(@RequestBody User user) {
//        log.info("새 사용자 생성 요청: {}", user);
//        user.setId(nextId++);
//        userStorage.put(user.getId(), user);
//        return ResponseEntity.status(HttpStatus.CREATED).body(user);
//    }
//
//    /**
//     * 사용자 정보 업데이트 - PUT /api/users/{id}
//     */
//    @PutMapping("/{id}")
//    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
//        log.info("사용자 업데이트 요청 - ID: {}, 데이터: {}", id, updatedUser);
//        if (userStorage.containsKey(id)) {
//            updatedUser.setId(id);
//            userStorage.put(id, updatedUser);
//            return ResponseEntity.ok(updatedUser);
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    /**
//     * 사용자 부분 업데이트 - PATCH /api/users/{id}
//     */
//    @PatchMapping("/{id}")
//    public ResponseEntity<User> patchUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
//        log.info("사용자 부분 업데이트 요청 - ID: {}, 업데이트 데이터: {}", id, updates);
//        User user = userStorage.get(id);
//        if (user == null) {
//            return ResponseEntity.notFound().build();
//        }
//
//        // 부분 업데이트 처리
//        updates.forEach((key, value) -> {
//            switch (key) {
//                case "name" -> user.setName((String) value);
//                case "email" -> user.setEmail((String) value);
//                case "age" -> user.setAge(((Number) value).intValue());
//                case "department" -> user.setDepartment((String) value);
//            }
//        });
//
//        userStorage.put(id, user);
//        return ResponseEntity.ok(user);
//    }
//
//    /**
//     * 사용자 삭제 - DELETE /api/users/{id}
//     */
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
//        log.info("사용자 삭제 요청 - ID: {}", id);
//        if (userStorage.containsKey(id)) {
//            userStorage.remove(id);
//            return ResponseEntity.noContent().build();
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    /**
//     * 사용자 검색 - GET /api/users/search?name=xxx&department=xxx
//     */
//    @GetMapping("/search")
//    public ResponseEntity<List<User>> searchUsers(
//            @RequestParam(required = false) String name,
//            @RequestParam(required = false) String department,
//            @RequestParam(required = false) Integer minAge,
//            @RequestParam(required = false) Integer maxAge) {
//
//        log.info("사용자 검색 요청 - name: {}, department: {}, minAge: {}, maxAge: {}",
//                name, department, minAge, maxAge);
//
//        List<User> filteredUsers = userStorage.values().stream()
//                .filter(user -> name == null || user.getName().toLowerCase().contains(name.toLowerCase()))
//                .filter(user -> department == null || user.getDepartment().toLowerCase().contains(department.toLowerCase()))
//                .filter(user -> minAge == null || user.getAge() >= minAge)
//                .filter(user -> maxAge == null || user.getAge() <= maxAge)
//                .toList();
//
//        return ResponseEntity.ok(filteredUsers);
//    }
//
//    /**
//     * API 상태 확인 - GET /api/users/health
//     */
//    @GetMapping("/health")
//    public ResponseEntity<Map<String, Object>> healthCheck() {
//        Map<String, Object> health = new HashMap<>();
//        health.put("status", "UP");
//        health.put("timestamp", new Date());
//        health.put("userCount", userStorage.size());
//        health.put("message", "User API is running successfully!");
//
//        return ResponseEntity.ok(health);
//    }
//}
