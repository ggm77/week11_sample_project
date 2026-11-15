package edu.handong.web11.controller;

import edu.handong.web11.domain.Attachment;
import edu.handong.web11.repository.AttachmentRepository;
import edu.handong.web11.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/attachments")
public class AttachmentController {
    private final AttachmentRepository attachments;
    private final FileStorageService storage;
    public AttachmentController(AttachmentRepository attachments, FileStorageService storage) {
        this.attachments = attachments; this.storage = storage;
    }

    // /attachments/{id}/download에 GET 요청을 날리는 경우
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download( // 반환 값은 ResponseEntity에 파일을 담아서 줌
            @PathVariable Long id // PathVariable의 타입을 Long으로 지정
    ) {

        // id로 DB에서 첨부파일 정보 조회, 없으면 404
        Attachment file = attachments.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일이 없습니다."));

        // 엔티티에 들어있는 저장 파일명으로 실제 파일을 Resource로 가져오기
        Resource resource = storage.load(file);

        // Content-Disposition을 attachment로 설정하고 UTF-8로 인코딩한 원본 파일명 지정 (UUID 이름이 아닌 원래 이름으로 다운로드되게 함)
        String disposition = ContentDisposition.attachment()
            .filename(file.getOriginalFilename(), StandardCharsets.UTF_8)
            .build().toString();

        // ResponseEntity로 조립해서 리턴
        return ResponseEntity.ok() // 200 OK
            .contentType(MediaType.APPLICATION_OCTET_STREAM) // 파일 종류와 상관없이 일반 바이너리로 보내 브라우저가 다운로드하게 함
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition) // 위에서 만든 Content-Disposition 헤더 추가
            .body(resource); // 응답 본문에 파일 담기
    }
    // STUDENT TODO: GET /attachments/{id}/view (image preview).
    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> imagePreview(
            @PathVariable final Long id
    ) {

        Attachment file = attachments.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일이 없습니다."));

        Resource resource = storage.load(file);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getOriginalFilename() + "\"")
                .body(resource);
    }
}
