package com.scit48.admin.controller;


import com.scit48.Inquiry.repository.InquiryRepository;
import com.scit48.common.repository.UserRepository;
import com.scit48.community.repository.BoardRepository;
import com.scit48.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {
	
	private final UserRepository userRepository;
	private final NoticeRepository noticeRepository;
	private final InquiryRepository inquiryRepository;
	private final BoardRepository boardRepository;
	
	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		
		model.addAttribute("totalUsers", userRepository.count());
		model.addAttribute("todayUsers",
				userRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay()));
		
		model.addAttribute("noticeCount", noticeRepository.count() + boardRepository.count());
		model.addAttribute("inquiryCount", inquiryRepository.count());
		
		return "admin/dashboard";
	}
	
	@GetMapping("/dashboard/stats")
	@ResponseBody
	public Map<String, Object> getStats(
			@RequestParam String type,
			@RequestParam String range
	) {
		List<Object[]> rows;
		
		if ("users".equals(type)) {
			rows = switch (range) {
				case "weekly" -> userRepository.countWeeklyStats();
				case "monthly" -> userRepository.countMonthlyStats();
				default -> userRepository.countDailyStats();
			};
			
		} else if ("posts".equals(type)) {
			rows = switch (range) {
				case "weekly" -> mergeStats(
						noticeRepository.countWeeklyStats(),
						boardRepository.countWeeklyStats()
				);
				case "monthly" -> mergeStats(
						noticeRepository.countMonthlyStats(),
						boardRepository.countMonthlyStats()
				);
				default -> mergeStats(
						noticeRepository.countDailyStats(),
						boardRepository.countDailyStats()
				);
			};
			
		} else {
			rows = switch (range) {
				case "weekly" -> inquiryRepository.countWeeklyStats();
				case "monthly" -> inquiryRepository.countMonthlyStats();
				default -> inquiryRepository.countDailyStats();
			};
		}
		
		List<String> labels = new ArrayList<>();
		List<Long> values = new ArrayList<>();
		
		for (Object[] row : rows) {
			labels.add(row[0].toString());
			values.add(((Number) row[1]).longValue());
		}
		
		Map<String, Object> result = new HashMap<>();
		result.put("labels", labels);
		result.put("values", values);
		
		return result;
	}
	
	private List<Object[]> mergeStats(
			List<Object[]> noticeRows,
			List<Object[]> boardRows
	) {
		Map<String, Long> map = new HashMap<>();
		
		// 공지 데이터
		for (Object[] row : noticeRows) {
			String date = row[0].toString();
			Long count = ((Number) row[1]).longValue();
			map.put(date, count);
		}
		
		// 커뮤니티 데이터 (같은 날짜면 합산)
		for (Object[] row : boardRows) {
			String date = row[0].toString();
			Long count = ((Number) row[1]).longValue();
			map.merge(date, count, Long::sum);
		}
		
		// 날짜 오름차순 정렬
		List<Object[]> result = new ArrayList<>();
		map.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(e -> result.add(new Object[]{e.getKey(), e.getValue()}));
		
		return result;
	}
	
	
}
