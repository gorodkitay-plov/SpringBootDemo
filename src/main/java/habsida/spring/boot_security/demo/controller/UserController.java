package habsida.spring.boot_security.demo.controller;

import habsida.spring.boot_security.demo.model.Role;
import habsida.spring.boot_security.demo.model.User;
import habsida.spring.boot_security.demo.repository.RoleRepository;
import habsida.spring.boot_security.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import habsida.spring.boot_security.demo.model.UserEditDto;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    public UserController(UserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public String users(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("rolesList", roleRepository.findAll());
        return "users";
    }

    @GetMapping("/new")
    public String newUser(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("rolesList", roleRepository.findAll());
        return "newUser";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("user") User user,
                         BindingResult bindingResult,
                         @RequestParam(value = "rolesSelected", required = false) Set<String> rolesSelected,
                         Model model) {

        if (userService.isUsernameTaken(user.getUsername())) {
            bindingResult.rejectValue("username","","Username уже существует");
        }

        if (rolesSelected == null || rolesSelected.isEmpty()) {
            bindingResult.rejectValue("roles", "", "Выберите хотя бы одну роль");
        }

        if (bindingResult.hasErrors()){
            model.addAttribute("rolesList", roleRepository.findAll());
            return "newUser";
        }

        Set<Role> roles = roleRepository.findAll().stream()
                .filter(r -> rolesSelected.contains(r.getName()))
                .collect(java.util.stream.Collectors.toSet());

        user.setRoles(roles);

        userService.saveUser(user);
        return "redirect:/admin";
    }

    @GetMapping("/{id}/edit")
    public String editUser(Model model, @PathVariable Long id) {
        model.addAttribute("user", userService.getUser(id));
        model.addAttribute("rolesList", roleRepository.findAll());
        return "editUser";
    }

    @PatchMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("user") User user,
                         @RequestParam(value = "rolesSelected", required = false)
                         Set<String> rolesSelected) {

        // username check
        if (userService.isUsernameTakenForUpdate(user.getUsername(), id)) {
            return "redirect:/admin";
        }

        // roles check
        if (rolesSelected == null || rolesSelected.isEmpty()) {
            return "redirect:/admin";
        }

        Set<Role> roles = roleRepository.findAll().stream()
                .filter(r -> rolesSelected.contains(r.getName()))
                .collect(java.util.stream.Collectors.toSet());

        user.setRoles(roles);

        userService.updateUser(id, user);

        return "redirect:/admin";
    }

    @PatchMapping("/{id}/ajax")
    @ResponseBody
    public Map<String, Object> updateUserAjax(@PathVariable Long id,
                                              @Valid @RequestBody UserEditDto dto,
                                              BindingResult bindingResult) {

        Map<String, Object> response = new HashMap<>();

        // Проверка на уникальность username
        if(userService.isUsernameTakenForUpdate(dto.getUsername(), id)) {
            bindingResult.rejectValue("username","","Username уже существует");
        }

        if(bindingResult.hasErrors()) {
            Map<String,String> errors = bindingResult.getFieldErrors()
                    .stream()
                    .collect(Collectors.toMap(f -> f.getField(), f -> f.getDefaultMessage()));
            response.put("status","error");
            response.put("errors", errors);
            return response;
        }

        // Обновление пользователя
        User user = userService.getUser(id);
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setAge(dto.getAge());

        Set<Role> roles = roleRepository.findAll()
                .stream()
                .filter(r -> dto.getRoles().contains(r.getName()))
                .collect(Collectors.toSet());
        user.setRoles(roles);

        userService.updateUser(id, user);

        response.put("status","success");
        return response;
    }
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin";
    }
}