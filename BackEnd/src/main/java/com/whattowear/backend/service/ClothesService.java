package com.whattowear.backend.service;

import com.whattowear.backend.domain.Clothes;
import com.whattowear.backend.domain.Member;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ClothesService {
    @Autowired
    private Clothes.ClothesRepository clothesRepository;

    private final EntityManager em;

    public Long addClothes(Clothes clothes){
        em.persist(clothes);
        return clothes.getId();
    }

    public List<Clothes> getClothesByMember(Member member){
        return em.createQuery("select c from Clothes c where c.member = :member", Clothes.class)
                .setParameter("member", member)
                .getResultList();
    }

    // 1. 옷 저장하기 메서드 추가
    public void save(Clothes clothes) {
        clothesRepository.save(clothes);
    }

    // 2. 옷 삭제하기 메서드 추가
    public void deleteClothes(Long id) {
        clothesRepository.deleteById(id);
    }
}
