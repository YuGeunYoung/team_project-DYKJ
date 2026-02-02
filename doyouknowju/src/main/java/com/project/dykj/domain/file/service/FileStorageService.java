package com.project.dykj.domain.file.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

	// application.properties의 app.upload.dir 값을 사용한다.
	// 예) ${user.home}/dykj-uploads  -> Windows: C:\Users\계정\dykj-uploads
	@Value("${app.upload.dir}")
	private String uploadDir;

	/**
	 * 게시판 에디터(Summernote)에서 올라온 이미지를 서버 디스크에 저장하고,
	 * 브라우저에서 접근 가능한 "/uploads/**" 형태의 상대 URL을 반환한다.
	 *
	 * - 실제 저장 위치: {app.upload.dir}/board/yyyyMMdd/uuid.ext
	 * - 반환 URL 예시: /uploads/board/20260130/uuid.png
	 *
	 * DB에는 파일을 저장하지 않는다.
	 * (게시글 내용(boardContent)에 <img src="..."> URL만 저장되는 구조)
	 */
	public String storeBoardImage(MultipartFile file) {
		// 1) 이미지 파일인지 최소 검증
		validateImage(file);

		// 2) 확장자 결정 + 날짜 폴더 + UUID 파일명 생성(충돌 방지)
		String ext = guessExtension(file);
		String dateDir = LocalDate.now().format(DAY);
		String filename = UUID.randomUUID() + ext;

		// 3) 업로드 루트(app.upload.dir)/board/yyyyMMdd 경로에 저장
		Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
		Path dir = root.resolve("board").resolve(dateDir);

		try {
			Files.createDirectories(dir);
			Path target = dir.resolve(filename);
			try (InputStream in = file.getInputStream()) {
				Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			}
			// 4) 정적 리소스 서빙(UploadStaticResourceConfig)과 매칭되는 URL 경로 반환
			return "/uploads/board/" + dateDir + "/" + filename;
		} catch (IOException e) {
			throw new IllegalStateException("file upload failed", e);
		}
	}

	// Content-Type 기준으로 이미지인지 확인 (image/*)
	private static void validateImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("file is required");
		}
		String contentType = file.getContentType();
		if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
			throw new IllegalArgumentException("only image files are allowed");
		}
	}

	// 파일 확장자 결정 규칙
	// 1) 원본 파일명 확장자가 화이트리스트(png/jpg/jpeg/gif/webp/bmp)이면 그걸 사용
	// 2) 아니면 Content-Type을 보고 확장자 추정
	// 3) 그래도 모르겠으면 png로 저장
	private static String guessExtension(MultipartFile file) {
		String original = file.getOriginalFilename();
		if (original != null) {
			int dot = original.lastIndexOf('.');
			if (dot >= 0 && dot < original.length() - 1) {
				String ext = original.substring(dot).toLowerCase(Locale.ROOT);
				// 최소한의 화이트리스트
				if (ext.matches("^\\.(png|jpg|jpeg|gif|webp|bmp)$")) {
					return ext;
				}
			}
		}

		String ct = file.getContentType();
		if (ct == null) {
			return ".png";
		}
		ct = ct.toLowerCase(Locale.ROOT);
		if (ct.contains("png")) return ".png";
		if (ct.contains("jpeg") || ct.contains("jpg")) return ".jpg";
		if (ct.contains("gif")) return ".gif";
		if (ct.contains("webp")) return ".webp";
		if (ct.contains("bmp")) return ".bmp";
		return ".png";
	}
}