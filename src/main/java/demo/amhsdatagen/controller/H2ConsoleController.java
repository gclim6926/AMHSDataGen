package demo.amhsdatagen.controller;

import demo.amhsdatagen.model.UserSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/h2console")
public class H2ConsoleController {

    @GetMapping
    public void h2console(HttpSession session, HttpServletResponse response) throws IOException {
        UserSession userSession = (UserSession) session.getAttribute("userSession");
        
        // 로그인 확인
        if (userSession == null || !userSession.isLoggedIn()) {
            response.sendRedirect("/login");
            return;
        }
        
        // 수퍼유저 권한 확인
        if (!userSession.isSuperuser()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "H2 Console에 접근할 권한이 없습니다. 수퍼유저만 접근 가능합니다.");
            return;
        }
        
        // H2 Console로 직접 리다이렉트
        response.sendRedirect("/h2-console");
    }
}
