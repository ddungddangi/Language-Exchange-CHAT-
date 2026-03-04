package com.scit48.recommend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scit48.chat.domain.ChatRoom;
import com.scit48.chat.domain.ChatRoomMemberEntity;
import com.scit48.chat.repository.ChatRoomMemberRepository;
import com.scit48.chat.repository.ChatRoomRepository;
import com.scit48.common.domain.entity.UserEntity;
import com.scit48.common.enums.InterestType;
import com.scit48.common.enums.LanguageLevel;
import com.scit48.common.repository.UserRepository;
import com.scit48.recommend.criteria.Criteria;
import com.scit48.recommend.domain.dto.MatchResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.scit48.recommend.criteria.Criteria;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MatchService {
	
	private final UserRepository userRepository;
	
	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	
	private final RedisMatchQueueService redisMatchQueueService;
	
	private final RedisTemplate<String, Object> redisObjectTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	private static final long RESULT_TTL_SEC = 10;
	
	// pop 후보를 너무 오래 뒤지지 않도록 제한(실무에서는 대기열 크기/트래픽에 맞춰 조절)
	private static final int TRY_POP_LIMIT = 30;
	
	
	
	// ===== public API =====
	
	public MatchResponseDTO start(Long myId, String rawCriteriaKey) {
		
		// 0) 이미 MATCHED 결과가 있으면 그대로 반환
		MatchResponseDTO cached = getResult(myId);
		if (cached != null && "MATCHED".equals(cached.getStatus())) {
			return cached;
		}
		
		// 1) 내 정보 로드
		UserEntity me = userRepository.findById(myId)
				.orElseThrow(() -> new IllegalArgumentException("유저 없음: " + myId));
		
		// 2) criteriaKey 정규화/기본값 적용
		String myCriteriaKey = normalizeCriteriaKey(rawCriteriaKey, me);
		Criteria myCriteria = Criteria.parse(myCriteriaKey);
		
		// 3) 내 criteria 저장 (상대가 나를 검사할 때 필요)
		redisMatchQueueService.saveCriteria(myId, myCriteriaKey);
		
		// 4) 이미 waiting 중이면(중복 클릭) 그냥 WAITING 반환
		if (redisMatchQueueService.isWaiting(myId)) {
			return MatchResponseDTO.waiting();
		}
		
		// 5) partner를 pop 하면서 “상호 조건 만족” 찾기
		List<Long> poppedButNotMatched = new ArrayList<>();
		Long partnerId = null;
		
		for (int i = 0; i < TRY_POP_LIMIT; i++) {
			Long candId = redisMatchQueueService.popPartner();
			if (candId == null) break;
			if (candId.equals(myId)) continue;
			
			String candCriteriaKey = redisMatchQueueService.getCriteria(candId);
			if (candCriteriaKey == null) {
				// 기준키가 없으면 매칭 불가 → 다시 큐로
				poppedButNotMatched.add(candId);
				continue;
			}
			
			UserEntity partner = userRepository.findById(candId).orElse(null);
			if (partner == null) {
				poppedButNotMatched.add(candId);
				continue;
			}
			
			Criteria partnerCriteria = Criteria.parse(candCriteriaKey);
			
			boolean mutual =
					acceptsUserFilters(myCriteria, partner) &&
							acceptsUserFilters(partnerCriteria, me) &&
							interestCompatibleSoft(myCriteria, partnerCriteria); // ✅ 관심사는 criteria끼리(완화 모드)
			
			if (mutual) {
				partnerId = candId;
				break;
			} else {
				poppedButNotMatched.add(candId);
			}
		}
		
		// 6) 매칭 실패한 후보들은 다시 대기열로 복귀
		for (Long uid : poppedButNotMatched) {
			redisMatchQueueService.enqueueIfAbsent(uid);
		}
		
		// 7) 매칭 실패 → 나는 대기열로 들어가고 WAITING
		if (partnerId == null) {
			redisMatchQueueService.enqueueIfAbsent(myId);
			return MatchResponseDTO.waiting();
		}
		
		// 8) 매칭 성공 → 방 생성 + 멤버 insert
		UserEntity partner = userRepository.findById(partnerId)
				.orElseThrow(() -> new IllegalArgumentException("파트너 유저 없음 "));
		
		//방이름 제작하 때 수정 필요
		String myNickname = me.getNickname();
		String partnerNickname = partner.getNickname();

		// 순서 고정 (사전순)
			String roomName = " "
				+ (myNickname.compareTo(partnerNickname) <= 0
				? myNickname + "&" + partnerNickname
				: partnerNickname + "&" + myNickname);
		ChatRoom room = chatRoomRepository.save(new ChatRoom(roomName));
		
//		String roomName = "direct:" + Math.min(myId, partnerId) + ":" + Math.max(myId, partnerId);
//		ChatRoom room = chatRoomRepository.save(new ChatRoom(roomName));
		
		chatRoomMemberRepository.save(ChatRoomMemberEntity.builder()
				.room(room).user(me).roomName(null).build());
		
		chatRoomMemberRepository.save(ChatRoomMemberEntity.builder()
				.room(room).user(partner).roomName(null).build());
		
		// 9) 결과 저장(양쪽)
		MatchResponseDTO myRes = MatchResponseDTO.matched(room.getRoomId(), room.getRoomUuid(), partnerId);
		MatchResponseDTO partnerRes = MatchResponseDTO.matched(room.getRoomId(), room.getRoomUuid(), myId);
		
		setResult(myId, myRes);
		setResult(partnerId, partnerRes);
		
		// (선택) criteria 삭제
		redisMatchQueueService.clearCriteria(myId);
		redisMatchQueueService.clearCriteria(partnerId);
		
		return myRes;
	}
	
	public MatchResponseDTO getOrWaiting(Long myId) {
		MatchResponseDTO res = getResult(myId);
		return (res != null) ? res : MatchResponseDTO.waiting();
	}
	
	public void cancel(Long myId) {
		redisMatchQueueService.cancelWaiting(myId);
	}
	
	// ===== criteria / matching helpers =====
	
	private String normalizeCriteriaKey(String raw, UserEntity me) {
		// 프론트에서 안 보내면: “조건 완화(ANY)” 기본
		if (raw == null || raw.isBlank()) {
			return "g=ANY|age=18-80|n=ANY|lang=ANY|lv=ANY|interest=ANY";
		}
		return raw.trim();
	}
	/**
	 * criteria가 target(상대)에게 허용되는지 (관심사 제외: 관심사는 criteria끼리 비교)
	 */
	private boolean acceptsUserFilters(Criteria c, UserEntity target) {
		
		// gender
		if (!"ANY".equals(c.getGender())) {
			if (target.getGender() == null) return false;
			if (!target.getGender().name().equals(c.getGender())) return false;
		}
		
		// age range
		if (target.getAge() == null) return false;
		if (target.getAge() < c.getAgeMin() || target.getAge() > c.getAgeMax()) return false;
		
		// nation
		if (!"ANY".equals(c.getNation())) {
			if (target.getNation() == null) return false;
			if (!c.getNation().equals(target.getNation())) return false;
		}
		
		// study language
		if (!"ANY".equals(c.getStudyLang())) {
			if (target.getStudyLanguage() == null) return false;
			if (!c.getStudyLang().equals(target.getStudyLanguage())) return false;
		}
		
		// level (1~4 or ANY)
		if (!c.isLevelsAny()) {
			int targetLv = levelToInt(target.getLevelLanguage());
			if (!c.getLevels().contains(targetLv)) return false;
		}
		
		return true;
	}
	
	/**
	 * ✅ 완화(Soft) 모드 관심사 매칭
	 * - 둘 다 ANY: 통과
	 * - 한쪽만 ANY: 통과 (관심사로는 제한하지 않음)
	 * - 둘 다 선택: 교집합이 있으면 통과, 없으면 실패
	 */
	private boolean interestCompatibleSoft(Criteria a, Criteria b) {
		if (a == null || b == null) return true;
		
		// 둘 다 ANY면 통과
		if (a.isInterestsAny() && b.isInterestsAny()) return true;
		
		// 한쪽만 ANY면 통과(완화 모드)
		if (a.isInterestsAny() || b.isInterestsAny()) return true;
		
		// 둘 다 선택했는데 파싱 결과가 비면(이상 케이스) 완화 정책상 통과 처리
		if (a.getInterests() == null || a.getInterests().isEmpty()) return true;
		if (b.getInterests() == null || b.getInterests().isEmpty()) return true;
		
		// 교집합 검사
		for (InterestType t : a.getInterests()) {
			if (b.getInterests().contains(t)) return true;
		}
		return false;
	}
	
	private int levelToInt(LanguageLevel lv) {
		if (lv == null) return 1;
		return switch (lv) {
			case BEGINNER -> 1;
			case INTERMEDIATE -> 2;
			case ADVANCED -> 3;
			case NATIVE -> 4;
		};
	}
	
	// ===== redis result helpers =====
	
	private void setResult(Long userId, MatchResponseDTO res) {
		String key = "match:result:" + userId;
		redisObjectTemplate.opsForValue().set(key, res, RESULT_TTL_SEC, TimeUnit.SECONDS);
	}
	
	private MatchResponseDTO getResult(Long userId) {
		String key = "match:result:" + userId;
		Object obj = redisObjectTemplate.opsForValue().get(key);
		if (obj == null) return null;
		
		if (obj instanceof MatchResponseDTO mr) return mr;
		
		// Redis serializer에 따라 Map으로 올 수 있음 → 안전 변환
		try {
			return objectMapper.convertValue(obj, MatchResponseDTO.class);
		} catch (Exception e) {
			return null;
		}
	}
	
	
	// ===== Criteria class =====
	
//	private static class Criteria {
//		String gender = "ANY";     // MALE/FEMALE/ANY
//		int ageMin = 18;
//		int ageMax = 80;
//		String nation = "ANY";     // KOREA/JAPAN/ANY
//		String studyLang = "ANY";  // KOREAN/JAPANESE/ANY
//
//		boolean levelsAny = true;
//		Set<Integer> levels = new HashSet<>(); // 1~4
//
//		boolean interestsAny = true;
//		Set<InterestType> interests = new HashSet<>();
//
//		static Criteria parse(String key) {
//			Criteria c = new Criteria();
//			if (key == null || key.isBlank()) return c;
//
//			String[] parts = key.split("\\|");
//			Map<String, String> map = new HashMap<>();
//			for (String p : parts) {
//				String[] kv = p.split("=", 2);
//				if (kv.length == 2) map.put(kv[0].trim(), kv[1].trim());
//			}
//
//			c.gender = map.getOrDefault("g", "ANY").toUpperCase();
//
//			// age=20-30
//			String age = map.get("age");
//			if (age != null && age.contains("-")) {
//				try {
//					String[] rr = age.split("-", 2);
//					c.ageMin = Integer.parseInt(rr[0].trim());
//					c.ageMax = Integer.parseInt(rr[1].trim());
//					if (c.ageMin > c.ageMax) {
//						int tmp = c.ageMin;
//						c.ageMin = c.ageMax;
//						c.ageMax = tmp;
//					}
//				} catch (Exception ignored) { }
//			}
//
//			c.nation = map.getOrDefault("n", "ANY").toUpperCase();
//			c.studyLang = map.getOrDefault("lang", "ANY").toUpperCase();
//
//			// lv=ANY or lv=1,2,3
//			String lv = map.getOrDefault("lv", "ANY").toUpperCase();
//			if (!"ANY".equals(lv)) {
//				c.levelsAny = false;
//				for (String s : lv.split(",")) {
//					try {
//						int v = Integer.parseInt(s.trim());
//						if (v >= 1 && v <= 4) c.levels.add(v);
//					} catch (Exception ignored) { }
//				}
//				if (c.levels.isEmpty()) c.levelsAny = true;
//			}
//
//			// interest=ANY or interest=CULTURE,IT
//			String it = map.getOrDefault("interest", "ANY").toUpperCase();
//			if (!"ANY".equals(it)) {
//				c.interestsAny = false;
//				for (String s : it.split(",")) {
//					String name = s.trim();
//					if (name.isEmpty()) continue;
//					try {
//						c.interests.add(InterestType.valueOf(name));
//					} catch (Exception ignored) {
//						log.debug("Invalid interest token: {}", name);
//					}
//				}
//				if (c.interests.isEmpty()) c.interestsAny = true;
//			}
//
//			return c;
//		}
//	}
}
