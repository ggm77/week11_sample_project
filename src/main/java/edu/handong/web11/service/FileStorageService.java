package edu.handong.web11.service;

import edu.handong.web11.domain.Attachment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "pdf", "txt");
    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir}") String path) throws IOException {
        uploadDir = Path.of(path).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
    }

    // PostService에서 넘겨받은 멀티파트 파일을 검증·저장한 뒤 Attachment 엔티티로 반환
    public Attachment store(MultipartFile file) {
        // 파일이 빈 경우 예외 처리
        if (file.isEmpty()) throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");

        // 파일의 원본 이름을 변수로 저장
        String original = file.getOriginalFilename();

        // 파일명 특정 경우에 대한 예외처리
        if (
                original == null // 파일명이 null인 경우
                || original.isBlank() // 파일명이 공백인 경우
                || original.contains("/") // 파일명에 슬래쉬가 들어간 경우
                || original.contains("\\") // 파일명에 역슬래쉬가 들어간 경우
                || original.contains("..")) // 파일명에 ".."이 들어간 경우 (상위 폴더로 이동하는 경로 조작 방지)
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");

        // 파일명 길이 검사
        if (original.length() > 240) throw new IllegalArgumentException("파일 이름이 너무 깁니다.");

        // 확장자와 이름 구별하는데 쓰는 점의 인덱스 번호 찾기
        int dot = original.lastIndexOf('.');

        // 확장자 추출 (점이 없으면 빈 문자열, 있으면 lower case로 변환)
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);

        // 특정 확장자만 받도록 필터링
        if (!ALLOWED.contains(ext)) throw new IllegalArgumentException("허용 형식: jpg, jpeg, png, gif, pdf, txt");

        // 파일 사이즈 검증
        if (file.getSize() > 10L * 1024 * 1024) throw new IllegalArgumentException("파일은 10MB 이하만 가능합니다.");

        // 이름 충돌과 파일명 추측을 막기 위해 UUID + 확장자로 저장용 파일명 생성
        String stored = UUID.randomUUID() + "." + ext;

        // 저장 폴더와 새 파일명을 합쳐 경로를 만들고 정규화
        Path target = uploadDir.resolve(stored).normalize();

        // 정규화된 경로가 업로드 폴더 밖이면 예외 던지기 (Path Traversal 방어)
        if (!target.startsWith(uploadDir)) throw new IllegalArgumentException("잘못된 파일 경로입니다.");

        // 파일 저장하기
        try (InputStream input = file.getInputStream()) { // 멀티파트파일을 인풋스트림으로 열기
            // 멀티파트파일을 실제 파일로 저장
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);

            // 요청 헤더의 Content-Type은 조작될 수 있으므로 믿지 않고, 검증된 확장자로 content type 정하기
            // Teaching sample: MIME type is inferred by extension, not trusted from request headers.
            String contentType = switch (ext) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "pdf" -> "application/pdf";
                default -> "text/plain";
            };

            // 얻어낸 값들로 Attachment 생성해서 반환
            return new Attachment(original, stored, contentType, file.getSize());
        } catch (IOException e) { // IO 예외 터졌을 때
            try {
                // 저장 도중 실패해 남은 불완전한 파일 삭제
                Files.deleteIfExists(target);
            } catch (IOException ignored) {}
            // 500 에러로 응답하도록 예외 던지기
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 실패", e);
        }
    }

    public Resource load(Attachment attachment) {
        Path target = uploadDir.resolve(attachment.getStoredFilename()).normalize();
        if (!target.startsWith(uploadDir) || !Files.isRegularFile(target))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 없습니다.");
        return new PathResource(target);
    }

    public void deleteIfExists(Attachment attachment) {
        try {
            Files.deleteIfExists(uploadDir.resolve(attachment.getStoredFilename()).normalize());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제 실패", e);
        }
    }
}
