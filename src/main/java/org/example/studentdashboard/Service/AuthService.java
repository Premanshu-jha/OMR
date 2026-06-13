package org.example.studentdashboard.Service;

import org.example.studentdashboard.Models.OtpDetails;
import org.example.studentdashboard.Models.Student;
import org.example.studentdashboard.Repositories.OtpRepository;
import org.example.studentdashboard.Repositories.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    SMSService smsService;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    OtpRepository otpRepository;

    @Autowired
    JWTService jwtService;

    public void sendOtp(OtpDetails otpDetails){
        String rollNumber = otpDetails.getRollNo();
        if(rollNumber == null) throw new RuntimeException("Please enter your roll number!");
        Student student = studentRepository.findByRollNo(rollNumber).orElseThrow(()->new RuntimeException("Invalid User!"));
        if(!Boolean.TRUE.equals(student.getSmsOtpByPass())){
            String phoneNumber = student.getPhone();
            String otp = String.format("%04d",new SecureRandom().nextInt(10000));
            otpDetails.setOtp(otp);
            otpDetails.setPhoneNumber(phoneNumber);
            otpRepository.save(otpDetails);
            smsService.sendOtp(phoneNumber,otp);
        }
    }

    public String verifyOtp(OtpDetails reqOtp) {
        Student student = studentRepository.findByRollNo(reqOtp.getRollNo()).orElseThrow(() -> new RuntimeException("Invalid User!"));
        if (Boolean.TRUE.equals(student.getSmsOtpByPass())) {
            return jwtService.createToken(student);
        } else {
            String phoneNumber = student.getPhone();
            String otp = reqOtp.getOtp();
            if (otp == null) throw new RuntimeException("Please enter the otp!");
            if (phoneNumber == null) throw new RuntimeException("PLease enter the phone number!");

            OtpDetails savedOtp = otpRepository.findById(student.getPhone())
                    .orElseThrow(() -> new RuntimeException("Pease generate the otp first!"));

            if (savedOtp.getOtp().equals(otp)) {
                otpRepository.deleteById(phoneNumber);
                return jwtService.createToken(student);
            }

            throw new RuntimeException("Invalid otp!");
        }
    }

}
