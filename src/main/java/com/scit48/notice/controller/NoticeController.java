package com.scit48.notice.controller;

import com.scit48.auth.member.service.CustomUserDetails;
import com.scit48.notice.domain.dto.NoticeDTO;
import com.scit48.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class NoticeController {
	private final NoticeService noticeService;
	
	
	
	@GetMapping("/customer/notice")
	public String noticePage(
			@RequestParam(defaultValue = "0") int page,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			Model model
	) {
		Page<NoticeDTO> result = noticeService.getNoticePage(page);
		
		model.addAttribute("list", result.getContent());
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", result.getTotalPages());
		model.addAttribute("baseUrl", "/customer/notice");
		
		model.addAttribute("activeTab", "notice");
		model.addAttribute("title", "공지사항");
		
		// 🔥 관리자 여부
		boolean isAdmin = userDetails != null &&
				"ADMIN".equals(userDetails.getUser().getRole());
		model.addAttribute("isAdmin", isAdmin);
		
		return "notice/notice";
	}
	
	@GetMapping("/customer/faq")
	public String faqPage(
			@RequestParam(defaultValue = "0") int page,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			Model model
	) {
		Page<NoticeDTO> result = noticeService.getFaqPage(page);
		
		model.addAttribute("list", result.getContent());
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", result.getTotalPages());
		model.addAttribute("baseUrl", "/customer/faq");
		
		model.addAttribute("activeTab", "faq");
		model.addAttribute("title", "FAQ");
		
		boolean isAdmin = userDetails != null &&
				"ADMIN".equals(userDetails.getUser().getRole());
		model.addAttribute("isAdmin", isAdmin);
		System.out.println("userDetails = " + userDetails);
		
		return "notice/notice";
	}
	
	@GetMapping("/customer/search")
	@ResponseBody
	public Page<NoticeDTO> search(
			@RequestParam String type,
			@RequestParam String keyword,
			@RequestParam(defaultValue = "0") int page
	) {
		return noticeService.search(type, keyword, page);
	}
	
	@GetMapping("/customer/notice/{id}")
	public String noticeDetail(
			@PathVariable Long id,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			Model model
	) {
		NoticeDTO notice = noticeService.getNotice(id);
		
		model.addAttribute("notice", notice);
		model.addAttribute("title", notice.getTitle());
		model.addAttribute("activeTab",
				"FAQ".equals(notice.getType()) ? "faq" : "notice");
		
		boolean isAdmin = userDetails != null &&
				"ADMIN".equals(userDetails.getUser().getRole());
		model.addAttribute("isAdmin", isAdmin);
		
		return "notice/notice-detail";
	}
	
	// 수정용 데이터 조회
	@GetMapping("/customer/notice/{id}/edit")
	@ResponseBody
	public NoticeDTO getNoticeForEdit(@PathVariable Long id) {
		return noticeService.getNotice(id);
	}
	
	// 수정 저장
	@PostMapping("/customer/notice/{id}/edit")
	@ResponseBody
	public void updateNotice(
			@PathVariable Long id,
			@RequestParam String title,
			@RequestParam String content
	) {
		noticeService.updateNotice(id, title, content);
	}
	
	// 삭제
	@PostMapping("/customer/notice/{id}/delete")
	public String deleteNotice(@PathVariable Long id) {
		noticeService.deleteNotice(id);
		return "redirect:/customer/notice";
	}
	
	// 글작성 페이지
	@GetMapping("/customer/notice/write")
	public String noticeWritePage(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			Model model
	) {
		if (userDetails == null || !"ADMIN".equals(userDetails.getUser().getRole())) {
			throw new IllegalStateException("권한 없음");
		}
		
		model.addAttribute("title", "공지 작성");
		model.addAttribute("activeTab", "notice");
		return "notice/notice-write";
	}
	
	// 글 저장
	@PostMapping("/customer/notice/write")
	public String noticeWrite(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam String type,
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam String category,
			@RequestParam(defaultValue = "false") boolean isPinned
	) {
		noticeService.createNotice(
				type,
				title,
				content,
				category,
				isPinned,
				userDetails.getUser().getId()
		);
		
		return "redirect:/customer/notice";
	}
	
}
