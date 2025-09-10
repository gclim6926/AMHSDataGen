package demo.amhsdatagen.controller;

import demo.amhsdatagen.model.UserSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class IndexController {

    @GetMapping({"/main"})
    public String main(HttpSession session, Model model) {
        UserSession userSession = (UserSession) session.getAttribute("userSession");
        if (userSession == null || !userSession.isLoggedIn()) {
            return "redirect:/login";
        }
        model.addAttribute("userId", userSession.getUserId());
        model.addAttribute("userSession", userSession);
        return "index";
    }

    @GetMapping({"/index"})
    public String indexAlias(HttpSession session) {
        UserSession userSession = (UserSession) session.getAttribute("userSession");
        if (userSession == null || !userSession.isLoggedIn()) {
            return "redirect:/login";
        }
        return "redirect:/main";
    }

    @GetMapping({"/viewer2d"})
    public String viewer2d(HttpSession session) {
        UserSession userSession = (UserSession) session.getAttribute("userSession");
        if (userSession == null || !userSession.isLoggedIn()) {
            return "redirect:/login";
        }
        return "viewer2d";
    }

    @GetMapping({"/viewer3d"})
    public String viewer3d(HttpSession session) {
        UserSession userSession = (UserSession) session.getAttribute("userSession");
        if (userSession == null || !userSession.isLoggedIn()) {
            return "redirect:/login";
        }
        return "viewer3d";
    }
}


