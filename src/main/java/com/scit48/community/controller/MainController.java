package com.scit48.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
	
	@GetMapping("community")
	public String community() {
		
		return "community";
	}
}
