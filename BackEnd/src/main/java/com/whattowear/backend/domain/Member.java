package com.whattowear.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "member_table")
@Getter @Setter
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(unique = true, nullable = false)
    private String loginId;
    private String password;
    private String nickname;
    private String location;

    private Double baseConstitutionWeight = 0.0;

    private Double constitutionWeight = 0.0;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Clothes> clothesList = new ArrayList<>();

    public void updateConstitutionWeight(Double feedbackScore) {
        double learningRate = 0.5;

        double currentWeight = this.constitutionWeight != null ? this.constitutionWeight : 0.0;
        double newWeight = currentWeight + (feedbackScore * learningRate);

        if (newWeight > 2.0) {
            newWeight = 2.0;
        } else if (newWeight < -2.0) {
            newWeight = -2.0;
        }

        this.constitutionWeight = Math.round(newWeight * 10) / 10.0;
    }

    public interface MemberRepository extends JpaRepository<Member, Long> {
        Member findByLoginId(String loginId);
    }
}