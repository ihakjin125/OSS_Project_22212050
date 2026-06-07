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

    // 🌟 [수정됨] 서비스 계층의 applyFeedback 로직을 호출하여 엔티티의 보호 로직을 활용 🌟
    @PostMapping("/feedback")
    public ResponseEntity<String> submitFeedback(@RequestBody FeedbackRequest request) {
        try {
            // MemberService에서 가중치 업데이트 + 예외 방어 로직 수행
            memberService.applyFeedback(request.getLoginId(), request.getScore().doubleValue());
            return ResponseEntity.ok("피드백이 반영되어 체질 가중치가 업데이트되었습니다! 📈");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("회원을 찾을 수 없습니다.");
        }
    }
}