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

    private Double constitutionWeight = 0.0;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Clothes> clothesList = new ArrayList<>();

    // 체질 가중치 업데이트 비즈니스 로직
    public void updateConstitutionWeight(Double feedbackScore) {
        // 학습률(Learning Rate): 한 번의 피드백으로 체질이 너무 확 바뀌지 않도록 조절 (0.5도씩)
        double learningRate = 0.5;

        double currentWeight = this.constitutionWeight != null ? this.constitutionWeight : 0.0;
        double newWeight = currentWeight + (feedbackScore * learningRate);

        // 프론트엔드 UI(-2.0 ~ +2.0)가 깨지지 않도록 최소/최대값 제한 (Clamping)
        if (newWeight > 2.0) {
            newWeight = 2.0;
        } else if (newWeight < -2.0) {
            newWeight = -2.0;
        }

        // 소수점 첫째 자리까지만 깔끔하게 유지 (예: 1.50000002 -> 1.5)
        this.constitutionWeight = Math.round(newWeight * 10) / 10.0;
    }

    public interface MemberRepository extends JpaRepository<Member, Long> {
        Member findByLoginId(String loginId);
    }
}