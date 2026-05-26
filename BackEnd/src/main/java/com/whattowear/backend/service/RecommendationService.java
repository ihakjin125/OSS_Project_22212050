package com.whattowear.backend.service;

import com.whattowear.backend.domain.Clothes;
import com.whattowear.backend.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final MemberService memberService;
    private final ClothesService clothesService;

    public String recommendClothes(String loginId, Double temperature, String tpoString) {
        // 1. 유저 정보 찾기
        Member member = memberService.findByLoginId(loginId);
        if (member == null) return "회원 정보를 찾지 못했습니다.";

        // 2. 내 옷장에서 등록된 옷 모두 가져오기
        List<Clothes> myClothes = clothesService.getClothesByMember(member);
        if (myClothes.isEmpty()) return "옷장이 텅 비었습니다! 옷을 먼저 등록해주세요. 👕";

        // 3. TPO 변환 (프론트에서 온 'daily'를 대문자 Enum 'DAILY'로 변환)
        Clothes.Tpo tpo;
        try {
            tpo = Clothes.Tpo.valueOf(tpoString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "잘못된 상황(TPO) 정보입니다.";
        }

        // 🌟 [핵심 1] 체질 가중치 적용 🌟
        // DB의 constitution_weight 값이 +1(더위 탐)이면 체감 온도를 +2도 올려서 더 얇은 옷이 나오게 유도!
        double weight = member.getConstitutionWeight() != null ? member.getConstitutionWeight() : 0.0;
        double perceivedTemp = temperature + (weight * 2.0);

        // 🌟 [핵심 2] 체감 온도에 따른 목표 두께(Thickness) 설정 🌟
        Clothes.Thickness targetThickness;
        if (perceivedTemp >= 23) {
            targetThickness = Clothes.Thickness.THIN;     // 23도 이상: 얇은 옷
        } else if (perceivedTemp >= 15) {
            targetThickness = Clothes.Thickness.NORMAL;   // 15~22도: 보통 두께
        } else {
            targetThickness = Clothes.Thickness.THICK;    // 14도 이하: 두꺼운 옷
        }

        // 🌟 [핵심 3] 조건에 맞는 옷 필터링 🌟
        List<Clothes> matchedClothes = myClothes.stream()
                .filter(c -> c.getTpo() == tpo)
                .filter(c -> c.getThickness() == targetThickness)
                .collect(Collectors.toList());

        boolean isFallback = false;

        // [플랜 B] 2차 시도: 만약 딱 맞는 두께의 옷이 없다면? -> 같은 TPO(상황)의 다른 옷이라도 일단 가져오기!
        if (matchedClothes.isEmpty()) {
            matchedClothes = myClothes.stream()
                    .filter(c -> c.getTpo() == tpo) // 두께 조건은 빼고 TPO만 일치하는 옷 검색
                    .collect(Collectors.toList());
            isFallback = true; // 대안으로 찾았다는 표시
        }

        // 그래도 없다면? (해당 TPO 옷이 아예 없는 경우)
        if (matchedClothes.isEmpty()) {
            return "옷장에 " + tpo.name() + " 상황에 입을 옷이 없네요! 새 옷을 등록해보세요.👕";
        }

        // 4. 결과 포장해서 보내기
        String recommendedNames = matchedClothes.stream()
                .map(Clothes::getName)
                .limit(3)
                .collect(Collectors.joining("] / ["));

        String finalResult = "[" + recommendedNames + "]";

        // 플랜 B로 찾은 경우 친절한 안내 멘트 추가
        if (isFallback) {
            finalResult += "<br><span style='font-size: 15px; font-weight: normal; color: #ff9800; margin-top: 5px; display: inline-block;'>(💡 딱 맞는 두께가 없어 같은 카테고리의 다른 옷을 추천했어요!)</span>";
        }

        return finalResult;
    }
}