# Week 11 — Attachment Board (Student Sample)


## Project Information

## Installation
1. Java 17+ 및 MariaDB 준비, `CREATE DATABASE web11 CHARACTER SET utf8mb4;`
2. 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 자신의 MariaDB에 맞게 설정. 기본값은 `jdbc:mariadb://localhost:3306/web11`, `webuser`, `webpass`.
3. Gradle이 설치된 환경: `gradle bootRun` / IntelliJ: `AttachmentBoardApplication.main()` 실행.
4. 접속: http://localhost:8080/posts

## Source Code Exploration

### 1. Project Structure & Spring MVC

**Q1. PostController, PostService, PostRepository는 각각 어떤 역할을 하며, 서로 어떻게 연결되는가?**

PostController는 HTTP 요청과 응답을 처리하고, PostService와 연결된다. PostService는 대부분의 비즈니스 로직을 처리하며 PostRepository와 연결된다. PostRepository는 DB와 연결되며 DB에서 정보를 가져오거나 수정하는 역할을 한다.

**Q2. 게시글 작성 시 HTML Form의 입력값과 첨부파일은 Controller에 어떻게 전달되는가?**

form.html의 form은 `method="post"`, `enctype="multipart/form-data"`로 `/posts`에 전송된다. 제목과 내용은 `@ModelAttribute("postForm")`로 `PostForm` 객체에 바인딩되고, `@Valid`로 검증된 뒤 결과가 `BindingResult`에 담긴다. 첨부파일은 `name="files"`인 `multiple` input으로 전송되어 `@RequestParam(name = "files", required = false) List<MultipartFile>`로 전달된다. 그 후 Controller는 검증 오류가 있으면 다시 form을 보여주고, 없으면 `PostService.create(form, files)`를 호출한다.

### 2. JPA & Entity Relationship

**Q1. Post와 Attachment Entity는 어떤 관계이며, @ManyToOne과 @OneToMany는 각각 어떤 역할을 하는가?**

Post와 Attachment는 1:N 관계이다. 하나의 게시글에 여러 첨부파일이 달릴 수 있다. Attachment의 `@ManyToOne` + `@JoinColumn(name = "post_id")`은 attachments 테이블에 FK `post_id`를 만들며, 이 쪽이 연관관계의 주인이다. Post의 `@OneToMany(mappedBy = "post")`는 외래키를 직접 관리하지 않고, Post에서 자신의 첨부파일 목록을 조회할 수 있게 해주는 역방향 매핑이다. 그래서 `addAttachment()`에서 리스트에 추가하는 것과 `attachment.setPost(this)`를 함께 호출해 양쪽을 맞춰준다.

**Q2. cascade = CascadeType.ALL과 orphanRemoval = true는 어떤 역할을 하며, 실제 디스크에 저장된 파일에도 영향을 주는가?**

CascadeType.ALL은 모든 종류의 cascade를 적용한다는 뜻이므로 부모에 변경사항이 있으면 자식에게도 자동으로 변경사항이 반영된다. orphanRemoval은 부모와의 연관관계가 끊겨 고아 상태가 된 자식 엔티티를 삭제한다는 것이다. 이 모든 것들은 DB 내에만 적용이 된다. 그래서 디스크의 실제 파일은 `PostService.delete()`에서 `storage.deleteIfExists()`를 따로 호출해 직접 지워준다.

### 3. File Upload

**Q1. MultipartFile은 무엇이며, 업로드된 파일은 어떤 과정을 거쳐 서버에 저장되는가?**

MultipartFile은 multipart/form-data 요청으로 업로드된 파일을 Spring이 표현하는 인터페이스이다. 원본 파일명, 크기, InputStream 등을 제공한다. 저장 과정은 먼저 빈 파일인지, 파일명에 `/`, `\`, `..`가 있는지, 파일명이 240자를 넘는지 검사한다. 확장자가 허용 목록(jpg, jpeg, png, gif, pdf, txt)에 있는지, 크기가 10MB 이하인지 확인한다. 그 다음 `UUID + 확장자`로 저장 파일명을 만들고 경로를 검증한 뒤 `Files.copy()`로 업로드 폴더에 저장한다. 마지막으로 원본명, 저장명, 확장자로 정한 contentType, 크기를 담은 Attachment를 만든다. PostService는 이를 Post에 추가하고 DB에 저장하며, 중간에 실패하면 이미 저장한 파일을 삭제한다.

**Q2. 파일을 원본 이름 대신 UUID 기반 파일명으로 저장하는 이유는 무엇인가?**

사용자들이 같은 이름의 파일을 올려도 서로 덮어쓰지 않도록 이름 충돌을 막기 위해서이다. 또한 사용자가 정한 파일명을 그대로 경로에 쓰지 않으므로 경로 조작이나 특수문자, 한글 인코딩 문제를 피할 수 있고, 파일명을 추측해서 접근하기도 어렵다. 원본 이름은 DB의 `originalFilename`에 따로 저장해 다운로드할 때 사용한다.

### 4. File Download & Security

**Q1. 파일 다운로드 시 Resource, ResponseEntity, Content-Disposition은 각각 어떤 역할을 하는가?**

Resource는 디스크에 저장된 파일을 추상화한 객체로, `FileStorageService.load()`가 `PathResource`로 만들어 응답 본문으로 사용한다. ResponseEntity는 상태 코드(200 OK), 헤더, 본문을 함께 담아 HTTP 응답 전체를 구성한다. Content-Disposition 헤더는 `attachment`로 설정되어 브라우저가 파일을 화면에 열지 않고 다운로드하게 한다. 이때 filename을 UTF-8로 인코딩한 원본 파일명으로 지정하므로 사용자는 UUID 이름이 아닌 원래 이름으로 파일을 받게 된다.

**Q2. 파일 저장 과정에서 normalize()와 startsWith()를 사용하는 이유는 무엇인가?**

Path Traversal 공격을 막기 위해서이다. `normalize()`는 경로 안의 `.`, `..` 같은 요소를 정리해 실제로 가리키는 경로로 바꾼다. `startsWith(uploadDir)`는 그렇게 정리된 경로가 업로드 폴더 안에 있는지 확인한다. 예를 들어 `../../etc/passwd` 같은 이름이 들어오더라도 업로드 폴더 밖의 파일을 쓰거나 읽을 수 없게 한다. 이 검사는 저장과 다운로드 모두에서 사용된다.


## Customizing
## New Feature
## Testing & Deployment
## Weekly Review:
- 새롭게 배운 내용 3가지
- 어려웠던 점과 해결 방법
- AI 활용 내용 및 느낀 점