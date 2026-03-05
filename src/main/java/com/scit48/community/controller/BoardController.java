package com.scit48.community.controller;

import com.scit48.common.dto.UserDTO;
import com.scit48.common.repository.UserRepository;
import com.scit48.community.domain.dto.BoardDTO;
import com.scit48.community.domain.entity.BoardEntity;
import com.scit48.community.domain.entity.CategoryEntity;
import com.scit48.community.repository.BoardRepository;
import com.scit48.community.repository.CategoryRepository;
import com.scit48.community.repository.LikeRepository;
import com.scit48.community.service.BoardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("board")
public class BoardController {
	
	private final BoardService bs;
	private final CategoryRepository cr;
	private final UserRepository ur;
	private final BoardRepository br;
	private final LikeRepository lr;
	
	// application.properties 파일의 설정값
	@Value("${board.pageSize}")
	int pageSize;
	
	@Value("${board.linkSize}")
	int linkSize;
	
	@Value("${board.uploadPath}")
	String uploadPath;
	
	@GetMapping("write")
	public String write(@AuthenticationPrincipal UserDetails user, Model model, BoardDTO boardDTO) {
		
		// 1. 서비스 호출 (비즈니스 로직 위임)
		UserDTO userDTO = bs.getUserInfo(user);
		
		// 2. 결과에 따른 분기 처리
		if (userDTO == null) {
			// 로그인이 안 된 경우
			return "redirect:/login"; // 임시이므로 제대로 된 로그인 화면 경로로 수정할 것
		}
		
		model.addAttribute("userDTO", userDTO);
		
		return "boardWrite";
	}
	
	@PostMapping("write")
	public String write(
			BoardDTO boardDTO
			, @AuthenticationPrincipal UserDetails user
			, @RequestParam(name = "categoryId") Long categoryId  // [추가] HTML에서 보낸 숫자를 직접 받음
			, @RequestParam(name = "upload", required = false)
			MultipartFile upload, Model model
	) {
		
		// 작성한 글 정보에 사용자 아이디 추가
		boardDTO.setMemberId(user.getUsername());
		boardDTO.setCategoryId(categoryId);
		log.debug("저장할 글 정보: {}", boardDTO);
		
		// 업로드된 첨부파일
		if (upload != null) {
			log.debug("Empty: {}", upload.isEmpty());
			log.debug("파라미터 이름: {}", upload.getName());
			log.debug("파일명: {}", upload.getOriginalFilename());
			log.debug("파일크기: {}", upload.getSize());
			log.debug("파일종류: {}", upload.getContentType());
		}
		
		try {
			bs.write(boardDTO, uploadPath, upload);
		} catch (Exception e) {
			log.debug("예외 발생: {}", e.getMessage());
			e.printStackTrace();
			model.addAttribute("error", "글 저장 중 오류가 발생했습니다.");
			model.addAttribute("userDTO", bs.getUserInfo(user));
			return "redirect:/board/write";
		}
		
		return "redirect:/board/read/" + boardDTO.getBoardId();
	}
	
	
	@GetMapping("feedWrite")
	public String feedWrite(@AuthenticationPrincipal UserDetails user,
							Model model) {
		
		// 1. 서비스 호출 (비즈니스 로직 위임)
		UserDTO userDTO = bs.getUserInfo(user);
		
		// 2. 결과에 따른 분기 처리
		if (userDTO == null) {
			// 로그인이 안 된 경우
			return "redirect:/login"; // 임시이므로 제대로 된 로그인 화면 경로로 수정할 것
		}
		
		// 3. 모델에 DTO 담기
		model.addAttribute("user", userDTO);
		
		// 4. 뷰 반환
		return "feedWrite";
	}
	
	
	@PostMapping("feedWrite")
	public String feedWrite(
			BoardDTO boardDTO
			, @AuthenticationPrincipal UserDetails user
			, @RequestParam(name = "upload", required = false)
			MultipartFile upload
	) {
		
		// 작성한 글 정보에 사용자 아이디 추가
		boardDTO.setMemberId(user.getUsername());
		log.debug("저장할 피드글 정보: {}", boardDTO);
		
		// 업로드된 첨부파일
		if (upload != null) {
			log.debug("Empty: {}", upload.isEmpty());
			log.debug("파라미터 이름: {}", upload.getName());
			log.debug("파일명: {}", upload.getOriginalFilename());
			log.debug("파일크기: {}", upload.getSize());
			log.debug("파일종류: {}", upload.getContentType());
		}
		try {
			bs.feedWrite(boardDTO, uploadPath, upload);
		} catch (Exception e) {
			log.debug("예외 발생: {}", e.getMessage());
			return "redirect:/board/feedWrite";
		}
		
		return "redirect:/board/feedView";
	}
	
	// ✅ 수정된 list 컨트롤러 메서드
	@GetMapping("list")
	public String list(
			Model model,
			@PageableDefault(page = 1, size = 10, sort = "boardId", direction = Sort.Direction.DESC)
			Pageable pageable,
			@RequestParam(required = false) String cateName,       // 카테고리 필터
			@RequestParam(required = false) String searchType, // title, content, writer
			@RequestParam(required = false) String keyword) {  // 검색어
		
		// ⭐ [핵심 추가 코드] 넘어온 파라미터가 빈 문자열("")이면 null로 강제 변환
		if (cateName != null && cateName.trim().isEmpty()) cateName = null;
		if (searchType != null && searchType.trim().isEmpty()) searchType = null;
		if (keyword != null && keyword.trim().isEmpty()) keyword = null;
		
		// 1. 서비스 호출 (이제 "" 대신 완벽한 null이 넘어갑니다)
		Page<BoardDTO> boardList = bs.searchPosts(pageable, cateName, searchType, keyword);
		
		// 2. 페이징 계산 및 방어 코드
		int currentPage = Math.max(1, pageable.getPageNumber());
		
		int blockLimit = 5;
		int startPage = (((int) (Math.ceil((double) currentPage / blockLimit))) - 1) * blockLimit + 1;
		
		int totalPages = boardList.getTotalPages() == 0 ? 1 : boardList.getTotalPages();
		int endPage = Math.min(startPage + blockLimit - 1, totalPages);
		
		model.addAttribute("boardList", boardList);
		model.addAttribute("startPage", startPage);
		model.addAttribute("endPage", endPage);
		
		// 검색 상태 유지를 위해 모델에 추가
		model.addAttribute("cateName", cateName);
		model.addAttribute("searchType", searchType);
		model.addAttribute("keyword", keyword);
		
		return "boardList";
	}
	
	@GetMapping("/feedView")
	public String feedView(
			Model model,
			@AuthenticationPrincipal UserDetails userDetails,
			@PageableDefault(page = 0, size = 5, sort = "boardId", direction = Sort.Direction.DESC) Pageable pageable
	) {
		Slice<BoardEntity> feeds = br.findByCategoryName("일상", pageable);
		String loginId = (userDetails != null) ? userDetails.getUsername() : null;
		
		List<Long> likedBoardIds = new ArrayList<>();
		if (loginId != null && !feeds.isEmpty()) {
			// 현재 조회된 게시글들의 ID 목록 추출
			List<Long> boardIds = feeds.getContent().stream()
					.map(BoardEntity::getBoardId)
					.collect(Collectors.toList());
			// DB에서 내가 좋아요 누른 ID들만 가져옴 (예: [10, 15])
			likedBoardIds = lr.findLikedBoardIds(loginId, boardIds);
		}
		
		// final 키워드로 람다 내부에서 사용 가능하게 함
		final List<Long> myLikes = likedBoardIds;
		
		// DTO 변환
		Slice<BoardDTO> feedList = feeds.map(board -> {
			// 메모리에서 바로 확인 (DB 조회 X -> 엄청 빠름)
			boolean isLiked = myLikes.contains(board.getBoardId());
			
			return BoardDTO.builder()
					.boardId(board.getBoardId())
					.content(board.getContent())
					.writerNickname(board.getUser().getNickname())
					.profileImageName(board.getUser().getProfileImageName())
					.filePath(board.getFilePath())
					.likeCnt(board.getLikeCnt() != null ? board.getLikeCnt() : 0)
					.liked(isLiked) // 최적화된 결과
					.createdDate(board.getCreatedAt())
					.profileImagePath(board.getUser().getProfileImagePath())
					.nation(board.getUser().getNation())
					.memberId(board.getUser().getMemberId())
					.build();
		});
		
		model.addAttribute("feedList", feedList);
		
		return "feedView";
	}
	
	//  더보기(AJAX) 요청용 (JSON 반환)
	@GetMapping("/api/feedList")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getFeedList(
			@AuthenticationPrincipal UserDetails userDetails,
			@PageableDefault(size = 5, sort = "boardId", direction = Sort.Direction.DESC) Pageable pageable
	) {
		Slice<BoardEntity> feeds = br.findByCategoryName("일상", pageable);
		String loginId = (userDetails != null) ? userDetails.getUsername() : null;
		
		List<Long> likedBoardIds = new ArrayList<>();
		if (loginId != null && !feeds.isEmpty()) {
			List<Long> boardIds = feeds.getContent().stream()
					.map(BoardEntity::getBoardId)
					.collect(Collectors.toList());
			likedBoardIds = lr.findLikedBoardIds(loginId, boardIds);
		}
		
		final List<Long> myLikes = likedBoardIds;
		
		List<BoardDTO> dtoList = feeds.stream().map(board -> {
			boolean isLiked = myLikes.contains(board.getBoardId());
			
			return BoardDTO.builder()
					.boardId(board.getBoardId())
					.content(board.getContent())
					.writerNickname(board.getUser().getNickname())
					.profileImageName(board.getUser().getProfileImageName())
					.filePath(board.getFilePath())
					.likeCnt(board.getLikeCnt() != null ? board.getLikeCnt() : 0)
					.liked(isLiked)
					.createdDate(board.getCreatedAt())
					.nation(board.getUser().getNation())
					.memberId(board.getUser().getMemberId())
					.build();
		}).collect(Collectors.toList());
		
		Map<String, Object> response = new HashMap<>();
		response.put("content", dtoList);
		response.put("last", feeds.isLast()); // Slice도 isLast() 지원함
		
		return ResponseEntity.ok(response);
	}
	
	@GetMapping("/read/{boardId}")
	public String read(@PathVariable Long boardId, Model model, HttpSession session) {
		// 서비스에서 데이터 가져오기
		BoardDTO boardDTO = bs.read(boardId);
		
		// 현재 로그인한 사용자 ID (수정/삭제 버튼 표시 여부 확인용)
		Long loginUserId = (Long) session.getAttribute("loginUserId");
		
		// (선택 사항) 좋아요 여부 확인 로직이 있다면 여기서 boolean isLiked 등을 모델에 담음
		
		model.addAttribute("board", boardDTO);
		model.addAttribute("loginUserId", loginUserId); // 뷰에서 본인 확인용
		
		return "boardRead";
	}
	
	
	@GetMapping("/update/{boardId}")
	public String update(@PathVariable Long boardId, Model model) {
		// 기존 데이터를 조회해서 폼에 채워넣기 위해 DTO를 가져옵니다.
		BoardDTO boardDTO = bs.findById(boardId);
		model.addAttribute("board", boardDTO);
		return "boardUpdate";
	}
	
	@PostMapping("/update")
	public String update(@ModelAttribute BoardDTO boardDTO,
						 @RequestParam(name = "file", required = false) MultipartFile file) throws IOException {
		
		// 서비스의 수정 메서드 호출
		bs.update(boardDTO, uploadPath, file);
		
		// 수정 완료 후 상세 페이지로 리다이렉트
		return "redirect:/board/read/" + boardDTO.getBoardId();
	}
	
	
	@PostMapping("/delete")
	public String delete(@RequestParam Long boardId) {
		
		try {
			bs.delete(boardId);
		} catch (Exception e) {
			throw new RuntimeException("삭제에 실패했습니다.");
		}
		
		return "redirect:/board/list";
	}
	
	// 1. 피드 수정 페이지 이동 (GET)
	@GetMapping("/feedUpdate/{boardId}")
	public String feedUpdateForm(@PathVariable Long boardId, Model model) {
		// 기존 조회 메서드 재사용 (조회수 증가 없는 findById 사용)
		BoardDTO boardDTO = bs.findById(boardId);
		model.addAttribute("board", boardDTO);
		
		return "feedUpdate"; // feedUpdate.html 로 이동
	}
	
	@PostMapping("/feedUpdate")
	public String feedUpdate(@ModelAttribute BoardDTO boardDTO,
							 @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
		
		// 기존 서비스의 update 메서드 재사용
		bs.update(boardDTO, uploadPath, file);
		
		// 수정 후 '피드 목록'으로 리다이렉트
		return "redirect:/board/feedView";
	}
	
	@PostMapping("/feedDelete")
	public String feedDelete(@RequestParam Long boardId) {
		// 기존에 만들어둔 서비스의 삭제 로직을 그대로 재사용합니다.
		
		try {
			bs.delete(boardId);
		} catch (Exception e) {
			throw new RuntimeException("삭제에 실패했습니다.");
		}
		
		
		// 삭제 후 피드 목록으로 돌아갑니다.
		return "redirect:/board/feedView";
	}
	
	// 좋아요 토글 (AJAX 요청 처리)
	@PostMapping("/like/{boardId}")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> toggleLike(
			@PathVariable Long boardId,
			@AuthenticationPrincipal UserDetails userDetails,
			HttpServletRequest request) {
		
		if (userDetails == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		String memberId = userDetails.getUsername();
		String clientIp = getClientIp(request); // IP 추출 메서드 호출
		
		// 서비스 호출
		boolean liked = bs.toggleLike(boardId, memberId, clientIp);
		
		// 갱신된 좋아요 개수 조회 (화면 업데이트용)
		// (BoardService의 findById 등이 DTO를 반환한다면 거기서 getLikeCnt()를 하세요)
		BoardDTO updatedBoard = bs.findById2(boardId, memberId);
		int currentLikeCount = updatedBoard.getLikeCnt();
		
		
		Map<String, Object> response = new HashMap<>();
		response.put("liked", liked);
		response.put("likeCount", currentLikeCount);
		
		return ResponseEntity.ok(response);
	}
	
	// IP 추출 유틸 메서드
	private String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}
	
	
	
}