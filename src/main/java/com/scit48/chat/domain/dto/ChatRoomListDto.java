package com.scit48.chat.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomListDto {
	private Long roomId;
	private String roomName;
	private boolean hasUnread;
	
	// ✅ [추가] 리스트에서도 상대방 프로필과 이름을 보여주기 위한 필드
	private String opponentNickname;
	private String opponentProfileImg;
	private String opponentProfileImgName;
}