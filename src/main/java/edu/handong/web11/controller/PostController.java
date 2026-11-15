package edu.handong.web11.controller;

import edu.handong.web11.domain.Post;
import edu.handong.web11.dto.PostForm;
import edu.handong.web11.service.PostService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 타임리프 뷰를 반환하기 위해 @Controller 어노테이션 작성
@Controller
public class PostController {

    // DI한 PostService를 가져올 변수 선언
    private final PostService posts;

    // 생성자를 통한 DI
    public PostController(PostService posts) {
        this.posts = posts;
    }

    // 메인 페이지에 GET 요청 날렸을 때
    @GetMapping("/")
    public String home() {
        // /posts로 리다이렉트
        return "redirect:/posts";
    }

    // /posts에 GET 요청 날렸을 때
    @GetMapping("/posts")
    public String list(Model model) { // Spring이 주입해주는 Model (뷰에 데이터를 전달할 때 사용)
        // 모델에 실제 데이터 넣기
        model.addAttribute("posts", posts.all());

        // list.html 반환
        return "posts/list";
    }

    // /posts/new에 GET 요청 날렸을 때
    @GetMapping("/posts/new")
    public String form(Model model) { // Spring이 주입해주는 Model (뷰에 데이터를 전달할 때 사용)
        // 모델에 실제 데이터 넣기
        model.addAttribute("postForm", new PostForm());

        // form.html 반환
        return "posts/form";
    }

    // /posts에 POST 요청 날렸을 때
    @PostMapping("/posts")
    public String create(
            @Valid @ModelAttribute("postForm") PostForm form, // form 태그로 보낸 정보 PostForm으로 받고 Validation 적용
            BindingResult errors, // validation 결과 들어있는 객체
            // MultipartResolver가 파싱한 파일 중 name="files"인 것들을 files 변수에 바인딩한다. required=false이기 때문에 필수는 아니다.
            @RequestParam(name = "files", required = false) List<MultipartFile> files
    ) {
        // validation에 오류가 있을 경우에 form.html로 되돌려보낸다.
        if (errors.hasErrors()) return "posts/form";

        // 파일이 있는 경우에는 파일을, 없으면 빈 리스트를 서비스 쪽으로 보내고, 그 결과인 id를 변수에 저장한다.
        Long id = posts.create(form, files == null ? List.of() : files);

        // /posts/{id}로 리다이렉트 시킨다.
        return "redirect:/posts/" + id;
    }

    // /posts/{id}에 GET 요청 날리는 경우
    @GetMapping("/posts/{id}")
    public String detail(
            // PathVariable의 타입을 Long으로 지정
            @PathVariable Long id,

            // Spring이 주입해주는 Model (뷰에 데이터를 전달할 때 사용)
            Model model
    ) {
        // 모델에 실제 데이터 넣기
        model.addAttribute("post", posts.get(id));

        // detail.html 반환
        return "posts/detail";
    }

    // /posts/{id}/edit에 GET 요청 날린 경우
    @GetMapping("/posts/{id}/edit")
    public String edit(
            @PathVariable Long id, // PathVariable의 타입을 Long으로 지정
            Model model // Spring이 주입해주는 Model (뷰에 데이터를 전달할 때 사용)
    ) {
        // 서비스 레이어에서 정보 조회
        Post post = posts.get(id);

        // 새로운 dto 생성
        PostForm form = new PostForm();

        // dto에 정보 넣기
        form.setTitle(post.getTitle());
        form.setContent(post.getContent());

        // 모델에 정보 넣기
        model.addAttribute("postForm", form);
        model.addAttribute("postId", id);

        // form.html 반환
        return "posts/form";
    }

    // /posts/{id}/edit에 POST 요청 날린 경우
    @PostMapping("/posts/{id}/edit")
    public String update(
            @PathVariable Long id, // PathVariable의 타입을 Long으로 지정
            @Valid @ModelAttribute("postForm") PostForm form, // form 태그로 보낸 정보 PostForm으로 받고 Validation 적용
            BindingResult errors, // validation 결과 들어있는 객체
            Model model // Spring이 주입해주는 Model (뷰에 데이터를 전달할 때 사용)
    ) {
        // validation에서 오류 발견시
        if (errors.hasErrors()) {
            // postForm과 검증 오류는 Spring이 Model에 자동으로 넣어주지만 postId는 아니므로 직접 다시 넣어서 (form.html이 수정 모드와 action 주소를 정할 때 사용)
            model.addAttribute("postId", id);
            // form.html 반환
            return "posts/form";
        }
        // 서비스에서 적절한 동작하도록 요청
        posts.update(id, form);

        // /posts/{id}로 리다이렉트
        return "redirect:/posts/" + id;
    }

    // /posts/{id}/delete에 POST 요청 날리는 경우
    @PostMapping("/posts/{id}/delete")
    public String delete(@PathVariable Long id) { // PathVariable의 타입을 Long으로 지정
        posts.delete(id); // 서비스에 적절한 기능 요청
        return "redirect:/posts"; // /posts로 리다이렉트
    }
}
