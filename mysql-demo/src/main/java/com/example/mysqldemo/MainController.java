package com.example.mysqldemo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(path="/demo")
public class MainController {
    @Autowired
    private GreetingRepository greetingRepository;

    @PostMapping(path="/add")
    public @ResponseBody String addNewUser (@RequestParam("greeting") String input) {
        Greeting greeting = new Greeting();
        greeting.setGreeting(input);
        greetingRepository.save(greeting);
        return "Saved";
    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<Greeting> getAllUsers() {
        return greetingRepository.findAll();
    }
}