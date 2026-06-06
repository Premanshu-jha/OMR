package org.example.studentdashboard.Controller;
import org.example.studentdashboard.Models.OtpDetails;
import org.example.studentdashboard.Service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @PostMapping("/generate-otp")
    public void sendOtp(@RequestBody OtpDetails otpDetails){
        authService.sendOtp(otpDetails);
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestBody OtpDetails otpDetails){
        return authService.verifyOtp(otpDetails);
    }
}
