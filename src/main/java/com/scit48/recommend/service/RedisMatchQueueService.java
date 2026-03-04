package com.scit48.recommend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@RequiredArgsConstructor
public class RedisMatchQueueService {
	
	private final RedisTemplate<String, String> redisTemplate;
	
	private static final String QUEUE_KEY = "match:queue:global";
	private static final String WAITING_SET_KEY = "match:waiting:global";
	private static final String CRITERIA_PREFIX = "match:criteria:";
	// (MatchService에 있는 result 키와 동일 규칙이면 여기서도 지울 수 있음)
	private static final String RESULT_PREFIX = "match:result:";
	
	private static final long CRITERIA_TTL_SEC = 10;
	
	// 큐에서 1명 꺼내고(waiting에서도 제거) partnerId 반환, 없으면 0
	private static final String LUA_POP_ONE = """
        local queueKey = KEYS[1]
        local waitingKey = KEYS[2]
        local partner = redis.call('LPOP', queueKey)
        if not partner then
          return 0
        end
        redis.call('SREM', waitingKey, partner)
        return tonumber(partner)
    """;
	
	// ✅ 취소: waiting set 제거 + queue list에서 제거 + criteria/result 삭제(선택)
	private static final String LUA_CANCEL = """
        local queueKey = KEYS[1]
        local waitingKey = KEYS[2]
        local criteriaKey = KEYS[3]
        local resultKey = KEYS[4]
        local uid = ARGV[1]

        -- waiting set에서 제거
        redis.call('SREM', waitingKey, uid)

        -- queue(list)에서 제거 (모든 항목 제거)
        redis.call('LREM', queueKey, 0, uid)

        -- criteria 삭제
        redis.call('DEL', criteriaKey)

        -- result 삭제
        redis.call('DEL', resultKey)

        return 1
    """;
	
	public void saveCriteria(Long userId, String criteriaKey) {
		if (criteriaKey == null) return;
		redisTemplate.opsForValue().set(CRITERIA_PREFIX + userId, criteriaKey, CRITERIA_TTL_SEC, TimeUnit.SECONDS);
	}
	
	public String getCriteria(Long userId) {
		return redisTemplate.opsForValue().get(CRITERIA_PREFIX + userId);
	}
	
	public void clearCriteria(Long userId) {
		redisTemplate.delete(CRITERIA_PREFIX + userId);
	}
	
	public boolean isWaiting(Long userId) {
		Boolean member = redisTemplate.opsForSet().isMember(WAITING_SET_KEY, String.valueOf(userId));
		return member != null && member;
	}
	
	// 대기 등록(중복 방지)
	public void enqueueIfAbsent(Long userId) {
		String uid = String.valueOf(userId);
		Long added = redisTemplate.opsForSet().add(WAITING_SET_KEY, uid);
		if (added != null && added == 1L) {
			redisTemplate.opsForList().rightPush(QUEUE_KEY, uid);
		}
	}
	
	// partner 1명 pop (없으면 null)
	public Long popPartner() {
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setScriptText(LUA_POP_ONE);
		script.setResultType(Long.class);
		
		//LUA 스크립트 실행  > execute
		Long result = redisTemplate.execute(script, List.of(QUEUE_KEY, WAITING_SET_KEY));
		if (result == null || result == 0L) return null;
		return result;
	}
	
	// ✅ 핵심: 취소(대기열에서 제거)
	public void cancelWaiting(Long userId) {
		String uid = String.valueOf(userId);
		
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setScriptText(LUA_CANCEL);
		script.setResultType(Long.class);
		
		String criteriaKey = CRITERIA_PREFIX + userId;
		String resultKey = RESULT_PREFIX + userId;
		
		redisTemplate.execute(
				script,
				List.of(QUEUE_KEY, WAITING_SET_KEY, criteriaKey, resultKey),
				uid
		);
	}
}

