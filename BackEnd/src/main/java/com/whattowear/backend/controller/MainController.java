package com.whattowear.backend.controller;

import com.whattowear.backend.domain.*;
import com.whattowear.backend.service.ClothesService;
import com.whattowear.backend.service.MemberService;
import com.whattowear.backend.service.RecommendationService;
import com.whattowear.backend.service.WeatherService;
import com.whattowear.backend.domain.Clothes.Category;
import com.whattowear.backend.domain.Clothes.Thickness;
import com.whattowear.backend.domain.Clothes.Tpo;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/main")
@RequiredArgsConstructor
public class MainController {

    private final RecommendationService recommendationService;
    private final WeatherService weatherService;
    private final ClothesService clothesService;
    private final MemberService memberService;

    @GetMapping("/weather")
    public WeatherData getWeather(@RequestParam String location) {
        return weatherService.getCurrentWeather(location);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<String> getRecommendation(
            @RequestParam String loginId,
            @RequestParam Double temperature,
            @RequestParam String tpo) {

        String result = recommendationService.recommendClothes(loginId, temperature, tpo);
        return ResponseEntity.ok(result);
    }

    @Getter @Setter
    public static class ClothesRequest {
        private String loginId;
        private String name;
        private Category category;
        private Thickness thickness;
        private Tpo tpo;
    }

    @PostMapping("/clothes")
    public ResponseEntity<String> addClothes(@RequestBody ClothesRequest request) {
        Member owner = memberService.findByLoginId(request.getLoginId());
        if (owner == null) return ResponseEntity.badRequest().body("회원을 찾을 수 없습니다.");

        Clothes newClothes = new Clothes();
        newClothes.setName(request.getName());
        newClothes.setCategory(request.getCategory());
        newClothes.setThickness(request.getThickness());
        newClothes.setTpo(request.getTpo());
        newClothes.setMember(owner);

        clothesService.save(newClothes);
        return ResponseEntity.ok("옷 등록 성공!");
    }

    @GetMapping("/clothes")
    public ResponseEntity<List<Clothes>> getMyWardrobe(@RequestParam String loginId) {
        Member owner = memberService.findByLoginId(loginId);
        List<Clothes> myClothes = clothesService.getClothesByMember(owner);
        return ResponseEntity.ok(myClothes);
    }

    @DeleteMapping("/clothes/{id}")
    public ResponseEntity<String> deleteClothes(@PathVariable Long id) {
        clothesService.deleteClothes(id);
        return ResponseEntity.ok("옷 삭제 성공!");
    }

    @GetMapping("/member/nickname")
    public ResponseEntity<String> getNickname(@RequestParam String loginId) {
        Member owner = memberService.findByLoginId(loginId);
        if (owner == null) {
            return ResponseEntity.badRequest().body("회원을 찾을 수 없습니다.");
        }
        return ResponseEntity.ok(owner.getNickname());
    }

    @Getter @Setter
    public static class FeedbackRequest {
        private String loginId;
        private Double tempAtTime;
        private Integer score;
    }

    @PostMapping("/feedback")
    public ResponseEntity<String> submitFeedback(@RequestBody FeedbackRequest request) {
        try {
            memberService.applyFeedback(request.getLoginId(), request.getScore().doubleValue());
            return ResponseEntity.ok("피드백이 반영되어 체질 가중치가 업데이트되었습니다! 📈");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("회원을 찾을 수 없습니다.");
        }
    }

    // 🌟 [추가됨] 마이페이지 UI에 기본 체질과 현재 체질을 한 번에 뿌려주기 위한 API 🌟
    @GetMapping("/member/constitution")
    public ResponseEntity<Map<String, Double>> getMemberConstitution(@RequestParam String loginId) {
        Member owner = memberService.findByLoginId(loginId);
        if (owner == null) {
            return ResponseEntity.badRequest().build(); // 400 Bad Request 반환
        }

        // 프론트엔드로 두 개의 데이터를 Map 형태로 포장해서 보냅니다.
        // 결과 예시: { "baseWeight": 0.0, "currentWeight": 1.5 }
        Map<String, Double> response = new HashMap<>();
        response.put("baseWeight", owner.getBaseConstitutionWeight());
        response.put("currentWeight", owner.getConstitutionWeight());

        return ResponseEntity.ok(response);
    }
}