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
        if (member.getConstitutionWeight() != null) {
            member.setBaseConstitutionWeight(member.getConstitutionWeight());
        } else {
            member.setBaseConstitutionWeight(0.0);
            member.setConstitutionWeight(0.0);
        }

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

    public void applyFeedback(String loginId, Double feedbackScore) {
        Member member = memberRepository.findByLoginId(loginId);
        if (member == null) {
            throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다.");
        }

        member.updateConstitutionWeight(feedbackScore);
    }
}