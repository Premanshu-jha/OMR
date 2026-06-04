package org.example.studentdashboard.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.studentdashboard.Models.Student;
import org.example.studentdashboard.Repositories.StudentRepository;
import org.example.studentdashboard.Service.JWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JWTFilter extends OncePerRequestFilter {

    @Autowired
    JWTService jwtService;

    @Autowired
    StudentRepository studentRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if(authHeader != null && authHeader.startsWith("Bearer ")){
             String token = authHeader.substring(7);
             String rollNum = jwtService.getRollNumber(token);

             if(rollNum != null &&
                     SecurityContextHolder.getContext().getAuthentication() == null){

                 Optional<Student> student = studentRepository.findByRollNo(rollNum);
                  if(student.isPresent() && !jwtService.isTokenExpired(token)){
                      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(student.get(),null, List.of(new SimpleGrantedAuthority("ROLE_"+student.get().getRole().name()))));
                  }
             }
        }
        filterChain.doFilter(request, response);
    }
}
