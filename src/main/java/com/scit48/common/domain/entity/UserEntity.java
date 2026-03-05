package com.scit48.common.domain.entity;

import com.scit48.common.enums.Gender;
import com.scit48.common.enums.LanguageLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users", uniqueConstraints = {
		@UniqueConstraint(columnNames = "member_id"),
		@UniqueConstraint(columnNames = "nickname")
})
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	/*
	 * =========================
	 * 인증 정보
	 * =========================
	 */
	@Column(name = "member_id", nullable = false, length = 50)
	private String memberId;

	@Column(name = "password", nullable = false)
	private String password;

	@Builder.Default
	@Column(nullable = false, length = 20)
	private String role = "ROLE_MEMBER";

	@Column(name = "nickname", nullable = false, length = 20)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender", nullable = false, length = 10)
	private Gender gender;

	@Column(name = "intro", columnDefinition = "TEXT")
	private String intro;

	@Column(name = "age", nullable = false)
	private Integer age;

	@Column(name = "nation", nullable = false, length = 10)
	private String nation;

	// 초기값 36.5 설정
	@Builder.Default
	@Column(name = "manner", nullable = false)
	private double manner = 36.5;
	
	@Builder.Default
	@Column(name = "like_count", nullable = false)
	private int likeCount = 0;

	@Column(name = "native_language", nullable = false, length = 10)
	private String nativeLanguage;

	@Enumerated(EnumType.STRING)
	@Column(name = "level_language", nullable = false, length = 20)
	private LanguageLevel levelLanguage;

	@Column(name = "study_language", nullable = false, length = 20)
	private String studyLanguage;

	@Column(name = "profile_image_name")
	private String profileImageName;

	@Column(name = "profile_image_path")
	private String profileImagePath;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@Builder.Default
	@OneToMany(mappedBy = "user",
			cascade = CascadeType.ALL,
			orphanRemoval = true)
	private List<UserInterestEntity> interests = new ArrayList<>();
	
	// --- Builder 생성자 ---
	@Builder
	public UserEntity(String memberId, String password, String nickname, Gender gender,
			String intro, Integer age, String nation, Double manner,
			String nativeLanguage, LanguageLevel levelLanguage,
			String profileImageName, String profileImagePath) {
		this.memberId = memberId;
		this.password = password;
		this.nickname = nickname;
		this.gender = gender;
		this.intro = intro;
		this.age = age;
		this.nation = nation;
		this.nativeLanguage = nativeLanguage;
		this.levelLanguage = levelLanguage;

		// manner 값이 입력되지 않았다면(null) 기본값 36.5를 할당
		this.manner = (manner != null) ? manner : 36.5;

		this.profileImageName = profileImageName;
		this.profileImagePath = profileImagePath;
	}

	// 프로필 사진 변경을 위한 비즈니스 메서드
	public void updateProfileImage(String name, String path) {
		this.profileImageName = name;
		this.profileImagePath = path;
	}

	// 프로필 정보 수정
	public void updateProfile(String intro, LanguageLevel levelLanguage) {

		if (intro != null) {
			this.intro = intro;
		}

		if (levelLanguage != null) {
			this.levelLanguage = levelLanguage;
		}
	}

	// ==========================================================
	// [추가할 부분] 매너 온도 감점 메서드
	// ==========================================================
	public void decreaseManner(double amount) {
		// 현재 점수에서 amount만큼 뺍니다.
		// 단, 0점 미만으로 내려가지 않도록 Math.max 사용 (안전 장치)
		this.manner = Math.max(0.0, this.manner - amount);
	}

	// 2. [추가] 가산점 (최대 99.9도까지만 상승 제한)
	public void increaseManner(double amount) {
		// 현재 점수 + amount가 99.9를 넘지 않도록 설정
		this.manner = Math.min(99.9, this.manner + amount);
	}
}
