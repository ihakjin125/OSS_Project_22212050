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

    // MemberService.java 안에 이 코드가 있는지 확인하고, 없으면 추가해 주세요!
    public Member findByLoginId(String loginId) {
        // (memberRepository.findByLoginId() 등 실제 DB 조회 로직)
        return memberRepository.findByLoginId(loginId);
    }
}
