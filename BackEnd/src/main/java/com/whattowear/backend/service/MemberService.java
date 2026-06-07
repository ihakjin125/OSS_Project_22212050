package com.whattowear.backend.service;

import com.whattowear.backend.domain.Member;
import com.whattowear.backend.domain.Member.MemberRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    private final EntityManager em;

    public Long join(Member member){
        em.persist(member);
        return member.getId();
    }

    public Member login(String loginId, String password){
        List<Member> members = em.createQuery("select m from Member m where m.loginId = :loginId", Member.class)
                .setParameter("loginId", loginId)
                .getResultList();

        if(members.isEmpty()){
            return null;
        }

        Member member = members.get(0);
        if(member.getPassword().equals(password)){
            return member;
        }

        return null;
    }

    public Member findByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId);
    }

    public void save(Member member) {
        memberRepository.save(member);
    }

    // 🌟 [추가됨] 사용자의 체감 온도 피드백을 받아 체질 가중치를 갱신하는 로직 🌟
    public void applyFeedback(String loginId, Double feedbackScore) {
        // 1. DB에서 사용자 조회
        Member member = memberRepository.findByLoginId(loginId);
        if (member == null) {
            throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다.");
        }

        // 2. Member 엔티티 내부의 가중치 계산 및 방어 로직 호출
        member.updateConstitutionWeight(feedbackScore);

        // 💡 클래스 상단의 @Transactional 덕분에 여기서 메서드가 정상 종료되면
        // JPA가 변경된 가중치 값을 감지하고 DB에 자동으로 UPDATE 쿼리를 날려줍니다!
    }
}