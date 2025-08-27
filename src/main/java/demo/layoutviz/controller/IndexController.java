package demo.layoutviz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping({"/index"})
    public String indexAlias() {
        return "index";
    }

    @GetMapping({"/", ""})
    public String rootAlias() {
        return "index";
    }

    @GetMapping({"/viewer2d"})
    public String viewer2d() {
        return "viewer2d";
    }

    @GetMapping({"/viewer3d"})
    public String viewer3d() {
        return "viewer3d";
    }
}


