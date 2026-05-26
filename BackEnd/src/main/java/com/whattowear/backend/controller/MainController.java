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

    // ---------------------------------------------------------
    // [기존 코드 유지]
    @GetMapping("/weather")
    public WeatherData getWeather(@RequestParam String location) {
        return weatherService.getCurrentWeather(location);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<String> getRecommendation(
            @RequestParam String loginId,
            @RequestParam Double temperature,
            @RequestParam String tpo) {

        // 서비스 클래스(두뇌)에 데이터 넘기고 추천 결과 문자열로 받아오기
        String result = recommendationService.recommendClothes(loginId, temperature, tpo);
        return ResponseEntity.ok(result);
    }

    // ▼ 여기서부터 프론트엔드와 통신할 [옷장 CRUD] API들 입니다! ▼

    @Getter @Setter
    public static class ClothesRequest {
        private String loginId;
        private String name;
        private Category category;
        private Thickness thickness;
        private Tpo tpo;
    }

    // 1. [옷 등록]
    @PostMapping("/clothes")
    public ResponseEntity<String> addClothes(@RequestBody ClothesRequest request) {
        System.out.println("🔎 프론트에서 넘어온 아이디: [" + request.getLoginId() + "]");

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

    // 2. [내 옷장 조회]
    @GetMapping("/clothes")
    public ResponseEntity<List<Clothes>> getMyWardrobe(@RequestParam String loginId) {
        Member owner = memberService.findByLoginId(loginId);
        List<Clothes> myClothes = clothesService.getClothesByMember(owner);
        return ResponseEntity.ok(myClothes);
    }

    // 3. [옷 삭제]
    @DeleteMapping("/clothes/{id}")
    public ResponseEntity<String> deleteClothes(@PathVariable Long id) {
        clothesService.deleteClothes(id);
        return ResponseEntity.ok("옷 삭제 성공!");
    }

    // 🌟 4. [닉네임 조회] 마이페이지에서 진짜 닉네임을 가져가기 위한 추가 API 🌟
    @GetMapping("/member/nickname")
    public ResponseEntity<String> getNickname(@RequestParam String loginId) {
        Member owner = memberService.findByLoginId(loginId);
        if (owner == null) {
            return ResponseEntity.badRequest().body("회원을 찾을 수 없습니다.");
        }
        return ResponseEntity.ok(owner.getNickname()); // 진짜 닉네임만 쏙 뽑아서 전달!
    }
}