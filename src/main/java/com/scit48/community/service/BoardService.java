package com.scit48.community.service;

import com.scit48.common.domain.entity.UserEntity;
import com.scit48.common.dto.UserDTO;
import com.scit48.common.repository.UserRepository;
import com.scit48.community.domain.dto.BoardDTO;
import com.scit48.community.domain.dto.CommentDTO;
import com.scit48.community.domain.entity.*;
import com.scit48.community.repository.BoardRepository;
import com.scit48.community.repository.CategoryRepository;
import com.scit48.community.repository.CommentRepository;
import com.scit48.community.repository.LikeRepository;
import com.scit48.community.util.FileManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class BoardService {

	private final BoardRepository br;
	private final CategoryRepository ctr;
	private final UserRepository ur;
	private final FileManager fm;
	private final LikeRepository lr;
	private final CommentRepository cr;

	@Value("${board.uploadPath}")
	private String uploadPath;

	public void write(BoardDTO boardDTO, String uploadPath, MultipartFile upload) throws IOException {
		// 1. 작성자(User) 조회
		UserEntity userEntity = ur.findByMemberId(boardDTO.getMemberId())
				.orElseThrow(() -> new EntityNotFoundException("사용자가 존재하지 않습니다. id=" + boardDTO.getMemberId()));

		// 2. [수정] 카테고리 조회 (이름이 아닌 ID로 조회)
		CategoryEntity categoryEntity = ctr.findById(boardDTO.getCategoryId()) // findByName -> findById
				.orElseThrow(() -> new EntityNotFoundException("해당 카테고리가 없습니다. id=" + boardDTO.getCategoryId()));

		// 4. Entity 변환 및 저장
		BoardEntity boardEntity = BoardEntity.builder()
				.title(boardDTO.getTitle())
				.content(boardDTO.getContent())
				.viewCount(0)
				.category(categoryEntity)
				.user(userEntity)
				.build();

		// 첨부파일이 있는 경우
		if (upload != null && !upload.isEmpty()) {
			String fileName = fm.saveFile(uploadPath, upload);
			boardEntity.setFileName(fileName);
			boardEntity.setFilePath("/files/" + fileName);
			boardEntity.setFileOriginalName(upload.getOriginalFilename());

		}

		BoardEntity savedEntity = br.save(boardEntity);
		boardDTO.setBoardId(savedEntity.getBoardId());

	}

	public void feedWrite(BoardDTO boardDTO, String uploadPath,
			MultipartFile upload) throws IOException {

		// 1. 작성자(User) 조회
		UserEntity userEntity = ur.findByMemberId(boardDTO.getMemberId())
				.orElseThrow(() -> new EntityNotFoundException("사용자가 존재하지 않습니다. id=" + boardDTO.getId()));

		// '일상' 카테고리 자동 지정 (DB에 'DAILY' 또는 '일상'이라는 이름의 카테고리가 있다고 가정)
		CategoryEntity dailyCategory = ctr.findByName("일상") // 혹은 "DAILY"
				.orElseThrow(() -> new EntityNotFoundException("일상 카테고리가 DB에 없습니다."));

		boardDTO.setCategoryId(dailyCategory.getCategoryId());

		// 4. Entity 변환 및 저장 (Builder 패턴 사용)
		BoardEntity boardEntity = BoardEntity.builder()
				.title(boardDTO.getTitle())
				.content(boardDTO.getContent())
				.viewCount(0)
				.category(dailyCategory) // 연관관계 설정
				.user(userEntity) // 연관관계 설정
				.build();

		// 첨부파일이 있는 경우
		if (upload != null && !upload.isEmpty()) {
			String fileName = fm.saveFile(uploadPath, upload);
			boardEntity.setFileName(fileName);
			boardEntity.setFilePath("/files/" + fileName);
			boardEntity.setFileOriginalName(upload.getOriginalFilename());

		}

		br.save(boardEntity);
	}

	/**
	 * 로그인한 사용자의 정보를 가져와 DTO로 반환하는 메소드
	 * 
	 * @param user 시큐리티 인증 객체
	 * @return UserDTO (로그인 안 된 경우 null 반환)
	 */
	public UserDTO getUserInfo(UserDetails user) {

		// 1. 로그인 여부 확인
		if (user == null) {
			return null; // 컨트롤러에서 null이면 리다이렉트 처리
		}

		// 2. ID 추출
		String memberId = user.getUsername();

		// 3. DB 조회 (없으면 에러 발생)
		UserEntity userEntity = ur.findByMemberId(memberId)
				.orElseThrow(() -> new EntityNotFoundException("사용자 정보를 찾을 수 없습니다. ID: " + memberId));

		// 4. Entity -> DTO 변환 후 반환
		UserDTO userDTO = UserDTO.fromEntity(userEntity);

		return userDTO;
	}

	/**
	 * 페이징된 게시글 목록 조회
	 */
	public Page<BoardDTO> getBoardList(Pageable pageable) {
		// 실제 페이지 번호 보정 (사용자는 1페이지를 요청하지만 DB는 0페이지부터 시작)
		int requestPage = pageable.getPageNumber();
		int page = (requestPage <= 0) ? 0 : requestPage - 1;

		PageRequest pageRequest = PageRequest.of(page, pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));

		Page<BoardEntity> boardEntityList = br.findAll(pageRequest);

		// Entity -> DTO 변환 (Java 8 Stream 활용)
		// 목록에는 '내용' 전체가 필요 없으므로 필요한 필드만 빌더로 넣습니다.
		Page<BoardDTO> boardDTOS = boardEntityList.map(board -> BoardDTO.builder()
				.id(board.getUser().getId())
				.title(board.getTitle())
				.viewCount(board.getViewCount())
				.boardId(board.getBoardId())
				.createdDate(board.getCreatedAt()) // BaseTimeEntity 사용 시
				.categoryId(board.getCategory().getCategoryId())
				.categoryName(board.getCategory().getName()) // 카테고리 명
				.writerNickname(board.getUser().getNickname()) // 작성자 닉네임
				.build());

		return boardDTOS;
	}

	public Page<BoardDTO> searchPosts(Pageable pageable, String cateName, String searchType, String keyword) {

		int requestPage = pageable.getPageNumber();
		int page = (requestPage <= 0) ? 0 : requestPage - 1;
		PageRequest pageRequest = PageRequest.of(page, pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "boardId"));

		String excludeName = "일상"; // 제외할 카테고리명
		Page<BoardEntity> entities;

		// 1. 카테고리 필터가 있는 경우 (예: '질문'만 보기)
		if (cateName != null) {
			CategoryEntity category = ctr.findByName(cateName).orElse(null);
			if (category != null) {
				// 특정 카테고리 내에서만 조회 (이때도 혹시 몰라 '일상' 제외 조건을 걸 수 있지만, ID로 조회하므로 안전)
				// 하지만 검색어까지 있다면 복잡해지므로 여기서는 단순 필터링만 구현하거나
				// QueryDSL 없이 완벽한 동적 쿼리는 복잡하므로, '카테고리 필터'와 '검색'을 분리하거나 조합해야 합니다.
				// 여기서는 [카테고리 선택]이 우선순위가 높다고 가정하고 해당 카테고리 글만 가져옵니다.
				entities = br.findByCategoryNameAndCategoryNameNot(category.getName(), excludeName, pageRequest);
			} else {
				entities = br.findByCategoryNameNot(excludeName, pageRequest);
			}
		}
		// 2. 검색어가 있는 경우 (전체 카테고리 중 검색)
		else if (keyword != null && !keyword.isBlank()) {
			switch (searchType) {
				case "title":
					entities = br.findByTitleContainingAndCategoryNameNot(keyword, excludeName, pageRequest);
					break;
				case "content":
					entities = br.findByContentContainingAndCategoryNameNot(keyword, excludeName, pageRequest);
					break;
				case "writer":
					entities = br.findByUserNicknameContainingAndCategoryNameNot(keyword, excludeName, pageRequest);
					break;
				default:
					entities = br.findByCategoryNameNot(excludeName, pageRequest);
			}
		}
		// 3. 아무 조건 없음 (기본 목록)
		else {
			entities = br.findByCategoryNameNot(excludeName, pageRequest);
		}

		// Entity -> DTO 변환
		return entities.map(board -> BoardDTO.builder()
				.id(board.getUser().getId())
				.title(board.getTitle())
				.boardId(board.getBoardId())
				.viewCount(board.getViewCount())
				.createdDate(board.getCreatedAt())
				.categoryName(board.getCategory().getName())
				.writerNickname(board.getUser().getNickname())
				.profileImageName(board.getUser().getProfileImageName())
				.profileImagePath(board.getUser().getProfileImagePath())
				.memberId(board.getUser().getMemberId())
				.build());
	}

	// 마이페이지 - 내 게시글 목록 조회 로직
	// =========================================================
	public Page<BoardDTO> getMyBoardList(String memberId, int page, String cateName, String searchType,
			String keyword) {

		// 페이지 요청 설정 (10개씩, 최신순 정렬)
		int requestPage = (page <= 0) ? 0 : page - 1;
		PageRequest pageRequest = PageRequest.of(requestPage, 10, Sort.by(Sort.Direction.DESC, "boardId"));

		Page<BoardEntity> entities;

		boolean hasCategory = (cateName != null && !cateName.isBlank());
		boolean hasKeyword = (keyword != null && !keyword.isBlank());

		// 카테고리 여부와 검색어 여부에 따라 알맞은 Repository 메서드 호출
		if (hasCategory && hasKeyword) {
			// [카테고리 + 검색어]
			switch (searchType) {
				case "title":
					entities = br.findMyBoardByCategoryAndTitle(memberId, cateName, keyword, pageRequest);
					break;
				case "content":
					entities = br.findMyBoardByCategoryAndContent(memberId, cateName, keyword, pageRequest);
					break;
				case "both":
					entities = br.findMyBoardByCategoryAndBoth(memberId, cateName, keyword, pageRequest);
					break;
				default:
					entities = br.findMyBoardByCategory(memberId, cateName, pageRequest);
			}
		} else if (hasCategory && !hasKeyword) {
			// [카테고리만 적용]
			entities = br.findMyBoardByCategory(memberId, cateName, pageRequest);
		} else if (!hasCategory && hasKeyword) {
			// [검색어만 적용 (일상 제외)]
			switch (searchType) {
				case "title":
					entities = br.findMyBoardByTitle(memberId, keyword, pageRequest);
					break;
				case "content":
					entities = br.findMyBoardByContent(memberId, keyword, pageRequest);
					break;
				case "both":
					entities = br.findMyBoardByBoth(memberId, keyword, pageRequest);
					break;
				default:
					entities = br.findMyBoardAll(memberId, pageRequest);
			}
		} else {
			// [조건 없음 (기본 목록)]
			entities = br.findMyBoardAll(memberId, pageRequest);
		}

		// Entity -> DTO 변환 후 반환
		return entities.map(board -> BoardDTO.builder()
				.id(board.getUser().getId())
				.title(board.getTitle())
				.boardId(board.getBoardId())
				.viewCount(board.getViewCount())
				.createdDate(board.getCreatedAt())
				.categoryName(board.getCategory().getName())
				.writerNickname(board.getUser().getNickname())
				.profileImageName(board.getUser().getProfileImageName())
				.memberId(board.getUser().getMemberId())
				.build());
	}

	@Transactional
	public BoardDTO read(Long boardId) {
		// 1. 조회수 증가
		br.updateHits(boardId);

		// 2. 게시글 조회
		BoardEntity board = br.findById(boardId)
				.orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다"));

		// 3. 프로필 이미지 경로 처리 (앞선 답변 참고)
		String profileName = board.getUser().getProfileImageName();
		String profilePath = (profileName != null) ? "/files/" + profileName : "/images/profile/default.png";

		// 4. Entity -> DTO 변환
		return BoardDTO.builder()
				.id(board.getUser().getId())
				.boardId(board.getBoardId())
				.title(board.getTitle())
				.content(board.getContent())
				.profileImageName(board.getUser().getProfileImageName())
				.viewCount(board.getViewCount())
				.createdDate(board.getCreatedAt())
				.categoryId(board.getCategory().getCategoryId())
				.categoryName(board.getCategory().getName())
				.writerNickname(board.getUser().getNickname())
				.memberId(board.getUser().getMemberId())
				.nation(board.getUser().getNation())
				.profileImagePath(board.getUser().getProfileImagePath()) // 프로필 경로
				.manner(board.getUser().getManner()) // 매너온도 (있다면)
				.filePath(board.getFilePath()) // 첨부파일(이미지)
				.fileName(board.getFileName())
				// 댓글 리스트 변환 (CommentEntity -> CommentDTO)
				.comments(board.getComments().stream().map(c -> CommentDTO.builder()
						.commentId(c.getCommentId())
						.content(c.getContent())
						.writerNickname(c.getUser().getNickname())
						.writerProfileImage(c.getUser().getProfileImageName())
						.createdDate(c.getCreatedAt())
						.memberId(c.getUser().getMemberId())
						.build()).collect(Collectors.toList()))
				.likeCnt(board.getLikeCnt())
				.build();
	}

	@Transactional
	public BoardDTO findById(Long boardId) {
		// 1. 조회수 증가 기능은 제거

		// 2. 게시글 조회
		BoardEntity board = br.findById(boardId)
				.orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다"));

		// 3. 프로필 이미지 경로 처리 (앞선 답변 참고)
		String profileName = board.getUser().getProfileImageName();
		String profilePath = (profileName != null) ? "/files/" + profileName : "/images/default_profile.png";

		// 4. Entity -> DTO 변환
		return BoardDTO.builder()
				.id(board.getUser().getId())
				.boardId(board.getBoardId())
				.title(board.getTitle())
				.content(board.getContent())
				.profileImageName(board.getUser().getProfileImageName())
				.viewCount(board.getViewCount())
				.createdDate(board.getCreatedAt())
				.categoryId(board.getCategory().getCategoryId())
				.categoryName(board.getCategory().getName())
				.writerNickname(board.getUser().getNickname())
				.memberId(board.getUser().getMemberId())
				.nation(board.getUser().getNation())
				.profileImagePath(board.getUser().getProfileImagePath()) // 프로필 경로
				.manner(board.getUser().getManner()) // 매너온도 (있다면)
				.filePath(board.getFilePath()) // 첨부파일(이미지)
				.fileName(board.getFileName())
				// 댓글 리스트 변환 (CommentEntity -> CommentDTO)
				.comments(board.getComments().stream().map(c -> CommentDTO.builder()
						.commentId(c.getCommentId())
						.content(c.getContent())
						.writerNickname(c.getUser().getNickname())
						.writerProfileImage(c.getUser().getProfileImageName())
						.createdDate(c.getCreatedAt())
						.build()).collect(Collectors.toList()))
				.likeCnt(board.getLikeCnt())
				.build();
	}

	@Transactional
	public BoardDTO findById2(Long boardId, String loginMemberId) {
		// 1. 조회수 증가 기능은 제거

		// 2. 게시글 조회
		BoardEntity board = br.findById(boardId)
				.orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다"));

		// 3. 프로필 이미지 경로 처리 (앞선 답변 참고)
		String profileName = board.getUser().getProfileImageName();
		String profilePath = (profileName != null) ? "/files/" + profileName : "/images/default_profile.png";

		boolean isLiked = false;
		if (loginMemberId != null) {
			isLiked = isLiked(boardId, loginMemberId); // 위에 만든 isLiked 메서드 활용
		}

		// 4. Entity -> DTO 변환
		return BoardDTO.builder()
				.id(board.getUser().getId())
				.boardId(board.getBoardId())
				.title(board.getTitle())
				.content(board.getContent())
				.profileImageName(board.getUser().getProfileImageName())
				.viewCount(board.getViewCount())
				.createdDate(board.getCreatedAt())
				.categoryId(board.getCategory().getCategoryId())
				.categoryName(board.getCategory().getName())
				.writerNickname(board.getUser().getNickname())
				.memberId(board.getUser().getMemberId())
				.nation(board.getUser().getNation())
				.profileImagePath(profilePath) // 프로필 경로
				.manner(board.getUser().getManner()) // 매너온도 (있다면)
				.filePath(board.getFilePath()) // 첨부파일(이미지)
				.fileName(board.getFileName())
				// 댓글 리스트 변환 (CommentEntity -> CommentDTO)
				.comments(board.getComments().stream().map(c -> CommentDTO.builder()
						.commentId(c.getCommentId())
						.content(c.getContent())
						.writerNickname(c.getUser().getNickname())
						.writerProfileImage(c.getUser().getProfileImageName())
						.createdDate(c.getCreatedAt())
						.build()).collect(Collectors.toList()))
				.likeCnt(board.getLikeCnt())
				.liked(isLiked)
				.build();
	}

	public void update(BoardDTO boardDTO, String uploadPath, MultipartFile file) throws IOException {
		// 1. 기존 게시글 엔티티 조회
		BoardEntity boardEntity = br.findById(boardDTO.getBoardId())
				.orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

		// 2. 내용 수정 (제목, 카테고리는 수정하지 않음)
		boardEntity.setContent(boardDTO.getContent());

		// 3. 파일 수정 (새 파일이 올라왔을 때만 처리)
		if (file != null && !file.isEmpty()) {
			// 기존 파일 삭제 로직이 필요하다면 여기에 추가 (선택사항)

			// 새 파일 저장
			String fileName = fm.saveFile(uploadPath, file); // 기존에 사용하시던 FileManager 활용
			String filePath = "/files/" + fileName;

			// 엔티티 정보 갱신
			boardEntity.setFileName(fileName);
			boardEntity.setFilePath(filePath);
			boardEntity.setFileOriginalName(file.getOriginalFilename());
		}

		// @Transactional이 걸려있으므로 메서드 종료 시 자동 update 됩니다.
	}

	@Transactional
	public void delete(Long boardId) throws Exception {

		// 1. 게시글 조회 (파일 삭제를 위해 필요)
		BoardEntity board = br.findById(boardId)
				.orElseThrow(() -> new EntityNotFoundException("게시글이 없습니다."));

		// 2. 첨부파일이 있다면 로컬(디스크)에서도 삭제
		if (board.getFileName() != null) {
			fm.deleteFile(board.getFilePath(), board.getFileName());
		}

		// 3. DB에서 삭제
		br.delete(board);

	}

	/**
	 * 좋아요 토글 기능
	 * 
	 * @param boardId  게시글 ID
	 * @param memberId 로그인한 유저의 ID (String)
	 * @param clientIp 클라이언트 IP 주소 (LikeKey 생성을 위해 필요)
	 * @return 좋아요가 추가되었으면 true, 취소되었으면 false
	 */
	public boolean toggleLike(Long boardId, String memberId, String clientIp) {
		// 1. 유저와 게시글 조회
		UserEntity user = ur.findByMemberId(memberId)
				.orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다."));

		BoardEntity board = br.findById(boardId)
				.orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));

		// 2. 좋아요 존재 여부 확인 (IP 무관, 유저와 게시글 기준)
		if (lr.existsByUser_IdAndBoard_BoardId(user.getId(), boardId)) {
			// [취소] 이미 좋아요가 있다면 삭제
			lr.deleteByUser_IdAndBoard_BoardId(user.getId(), boardId);
			board.setLikeCnt(board.getLikeCnt() - 1); // 게시글 내 카운트 감소
			return false;

		} else {
			// [추가] 좋아요가 없다면 생성

			// LikeKey 생성 (boardId와 IP)
			LikeKey key = new LikeKey(boardId, clientIp);

			LikeEntity like = LikeEntity.builder()
					.likeId(key)
					.user(user)
					.board(board)
					// inputDate는 @CreatedDate로 자동 처리되거나 필요시 LocalDateTime.now()
					.build();

			lr.save(like);
			board.setLikeCnt(board.getLikeCnt() + 1); // 게시글 내 카운트 증가
			return true;
		}
	}

	// 게시글 상세 조회 시 현재 유저의 좋아요 상태 확인용
	@Transactional
	public boolean isLiked(Long boardId, String memberId) {
		UserEntity user = ur.findByMemberId(memberId).orElse(null);
		if (user == null)
			return false;
		return lr.existsByUser_IdAndBoard_BoardId(user.getId(), boardId);
	}

	// 댓글 작성
	public CommentDTO commentWrite(Long boardId, String memberId, String content) {
		// 1. 게시글 및 유저 조회
		BoardEntity board = br.findById(boardId)
				.orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

		UserEntity user = ur.findByMemberId(memberId)
				.orElseThrow(() -> new EntityNotFoundException("사용자 정보가 없습니다."));

		// 2. 엔티티 생성 및 저장
		CommentEntity comment = CommentEntity.builder()
				.content(content)
				.board(board)
				.user(user)
				// createdAt은 @PrePersist로 자동 설정됨
				.build();

		cr.save(comment);

		// 3. DTO 변환 및 반환 (화면 갱신용)
		return CommentDTO.builder()
				.commentId(comment.getCommentId())
				.boardId(board.getBoardId())
				.content(comment.getContent())
				.writerNickname(user.getNickname())
				.writerProfileImage(user.getProfileImageName()) // UserEntity 필드명 확인 필요
				.createdDate(comment.getCreatedAt())
				.memberId(comment.getUser().getMemberId())
				.build();
	}

	// 댓글 수정
	@Transactional
	public CommentDTO updateComment(Long commentId, String memberId, String newContent) {
		CommentEntity comment = cr.findById(commentId)
				.orElseThrow(() -> new EntityNotFoundException("댓글이 존재하지 않습니다."));

		// 작성자 본인 확인 (보안)
		if (!comment.getUser().getMemberId().equals(memberId)) {
			throw new IllegalArgumentException("수정 권한이 없습니다.");
		}

		// 내용 변경 (Dirty Checking으로 자동 update 쿼리 실행)
		comment.setContent(newContent);

		// 변경된 DTO 반환
		return CommentDTO.builder()
				.commentId(comment.getCommentId())
				.content(comment.getContent())
				.writerNickname(comment.getUser().getNickname())
				// 필요한 필드들...
				.build();
	}

	// 댓글 삭제
	@Transactional
	public void deleteComment(Long commentId, String memberId) {
		CommentEntity comment = cr.findById(commentId)
				.orElseThrow(() -> new EntityNotFoundException("댓글이 존재하지 않습니다."));

		if (!comment.getUser().getMemberId().equals(memberId)) {
			throw new IllegalArgumentException("삭제 권한이 없습니다.");
		}

		cr.delete(comment);
	}

	// 피드 댓글 목록 조회
	@Transactional
	public List<CommentDTO> getCommentsByBoardId(Long boardId) {
		BoardEntity board = br.findById(boardId)
				.orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

		// Entity 리스트를 DTO 리스트로 변환하여 반환
		return board.getComments().stream().map(comment -> CommentDTO.builder()
				.commentId(comment.getCommentId())
				.content(comment.getContent())
				.writerNickname(comment.getUser().getNickname())
				.writerProfileImage(comment.getUser().getProfileImageName()) // 파일명만 전달
				.memberId(comment.getUser().getMemberId())
				.createdDate(comment.getCreatedAt())
				.build()).collect(Collectors.toList());
	}

	public long getMyPostCount(String memberId) {
		return br.countByUser_MemberId(memberId);
	}
}
