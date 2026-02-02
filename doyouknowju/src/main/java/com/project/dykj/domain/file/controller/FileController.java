package com.project.dykj.domain.file.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.project.dykj.domain.file.service.FileStorageService;

@RestController
@RequestMapping("/api/files")
public class FileController {

	private final FileStorageService fileStorageService;

	public FileController(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	/**
	 * Summernote 이미지 업로드용 API.
	 *
	 * 요청: multipart/form-data, part name = file
	 * 응답: { "url": "http://localhost:8080/dykj/uploads/..." }
	 */
	@PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String, String> uploadImage(@RequestPart("file") MultipartFile file) {
		String relativePath = fileStorageService.storeBoardImage(file);
		String url = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path(relativePath)
				.toUriString();
		return Map.of("url", url);
	}
}

