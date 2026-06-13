package com.whattowear.backend.service;

import com.whattowear.backend.domain.Clothes;
import com.whattowear.backend.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

        // 3. TPO 변환
        Clothes.Tpo tpo;
        try {
            tpo = Clothes.Tpo.valueOf(tpoString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "잘못된 상황(TPO) 정보입니다.";
        }

        // 체질 가중치 적용
        // 가중치 1단계당 2.0도씩 체감 온도를 보정
        double weight = member.getConstitutionWeight() != null ? member.getConstitutionWeight() : 0.0;
        double perceivedTemp = temperature + (weight * 2.0);

        // 체감 온도에 따른 목표 두께
        // 고도화된 온도 분기점 적용 (22도, 12도 기준)
        Clothes.Thickness targetThickness;
        if (perceivedTemp >= 22.0) {
            targetThickness = Clothes.Thickness.THIN;     // 22도 이상: 얇은 옷 (여름)
        } else if (perceivedTemp >= 12.0) {
            targetThickness = Clothes.Thickness.NORMAL;   // 12도 ~ 21.9도: 보통 두께 (봄/가을)
        } else {
            targetThickness = Clothes.Thickness.THICK;    // 12도 미만: 두꺼운 옷 (겨울)
        }

        // 조건에 맞는 옷 1차 필터링
        List<Clothes> matchedClothes = myClothes.stream()
                .filter(c -> c.getTpo() == tpo)
                .filter(c -> c.getThickness() == targetThickness)
                .collect(Collectors.toList());

        boolean isFallback = false;

        // 2차 시도: 만약 딱 맞는 두께의 옷이 없다면? -> 같은 TPO(상황)의 다른 옷이라도 일단 가져오기
        if (matchedClothes.isEmpty()) {
            matchedClothes = myClothes.stream()
                    .filter(c -> c.getTpo() == tpo) // 두께 조건은 빼고 TPO만 일치하는 옷 검색
                    .collect(Collectors.toList());
            isFallback = true; // 대안으로 찾았다는 표시
        }

        // 카테고리별로 있는 것만 최대 1벌씩 뽑기 (유연한 조합)
        List<Clothes> finalOutfit = new ArrayList<>();

        // 1. 상의 딱 1개 찾아서 넣기 (없으면 통과)
        matchedClothes.stream()
                .filter(c -> "상의".equals(c.getCategory()))
                .findFirst()
                .ifPresent(finalOutfit::add);

        // 2. 하의 딱 1개 찾아서 넣기 (없으면 통과)
        matchedClothes.stream()
                .filter(c -> "하의".equals(c.getCategory()))
                .findFirst()
                .ifPresent(finalOutfit::add);

        // 3. 아우터 딱 1개 찾아서 넣기 (없으면 통과)
        matchedClothes.stream()
                .filter(c -> "아우터".equals(c.getCategory()))
                .findFirst()
                .ifPresent(finalOutfit::add);

        // 만약 상의, 하의, 아우터가 진짜 단 하나도 매칭 안 됐다면?
        if (finalOutfit.isEmpty()) {
            return "옷장에 해당 상황(" + tpo.name() + ")에 입을 옷이 없네요! 새 옷을 등록해보세요. 👕";
        }

        // 4. 결과 포장해서 보내기
        String recommendedNames = finalOutfit.stream()
                .map(Clothes::getName)
                .collect(Collectors.joining("] / ["));

        String finalResult = "[" + recommendedNames + "]";

        // 대안으로 찾은 경우 안내 멘트 추가
        if (isFallback) {
            finalResult += "<br><span style='font-size: 15px; font-weight: normal; color: #ff9800; margin-top: 5px; display: inline-block;'>(💡 딱 맞는 두께가 없어 같은 상황의 다른 옷을 추천했어요!)</span>";
        }

        return finalResult;
    }
}