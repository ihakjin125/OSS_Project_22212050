package com.whattowear.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Getter @Setter
public class Clothes {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clothes_id")
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Enumerated(EnumType.STRING)
    private Thickness thickness;

    @Enumerated(EnumType.STRING)
    private Tpo tpo;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public interface ClothesRepository extends JpaRepository<Clothes, Long> {
        List<Clothes> findByMember(Member member);
    }

    public enum Category {
        TOP, BOTTOM, OUTER
    }

    public enum Thickness {
        THIN, NORMAL, THICK
    }

    public enum Tpo {
        WORKOUT, DAILY, FORMAL
    }
}