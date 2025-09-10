package demo.amhsdatagen.controller;

import demo.amhsdatagen.model.User;
import demo.amhsdatagen.model.UserSession;
import demo.amhsdatagen.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    private final UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        UserSession userSession = (UserSession) session.getAttribute("userSession");
        if (userSession == null || !userSession.isLoggedIn()) {
            return "login";
        }
        return "redirect:/main";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String userId, 
                       @RequestParam String password,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        
        if (userId == null || userId.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "UserID를 입력해주세요.");
            return "redirect:/login";
        }
        
        if (password == null || password.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "비밀번호를 입력해주세요.");
            return "redirect:/login";
        }

        userId = userId.trim();
        
        try {
            // 사용자 존재 여부 확인
            if (!userService.userExists(userId)) {
                // 새로운 UserID인 경우 자동 회원가입
                if (password.length() < 4) {
                    redirectAttributes.addFlashAttribute("error", "새 계정 생성 시 비밀번호는 4자 이상이어야 합니다.");
                    return "redirect:/login";
                }
                
                // 자동 회원가입
                User user = userService.registerUser(userId, password);
                
                // 자동 로그인 처리
                UserSession userSession = new UserSession(userId);
                session.setAttribute("userSession", userSession);
                
                redirectAttributes.addFlashAttribute("success", "새 계정 생성 및 자동 로그인 성공! UserID: " + userId);
                return "redirect:/main";
            } else {
                // 기존 사용자 로그인
                User user = userService.loginUser(userId, password);
                
                // 로그인 성공
                UserSession userSession = new UserSession(userId);
                session.setAttribute("userSession", userSession);
                
                redirectAttributes.addFlashAttribute("success", "로그인 성공! UserID: " + userId);
                return "redirect:/main";
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "로그인 실패: " + e.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/register")
    public String register(@RequestParam String userId,
                          @RequestParam String password,
                          @RequestParam String passwordConfirm,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        
        if (userId == null || userId.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "UserID를 입력해주세요.");
            return "redirect:/login";
        }
        
        if (password == null || password.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "비밀번호를 입력해주세요.");
            return "redirect:/login";
        }
        
        if (!password.equals(passwordConfirm)) {
            redirectAttributes.addFlashAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "redirect:/login";
        }
        
        if (password.length() < 4) {
            redirectAttributes.addFlashAttribute("error", "비밀번호는 4자 이상이어야 합니다.");
            return "redirect:/login";
        }

        userId = userId.trim();
        
        try {
            // 사용자 등록
            User user = userService.registerUser(userId, password);
            
            // 자동 로그인 처리
            UserSession userSession = new UserSession(userId);
            session.setAttribute("userSession", userSession);
            
            redirectAttributes.addFlashAttribute("success", "회원가입 및 자동 로그인 성공! UserID: " + userId);
            return "redirect:/main";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "회원가입 실패: " + e.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("userSession");
        return "redirect:/login";
    }
}
