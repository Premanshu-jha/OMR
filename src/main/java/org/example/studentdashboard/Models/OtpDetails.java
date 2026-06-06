package org.example.studentdashboard.Models;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "OtpDetails",timeToLive = 900)
@Data
public class OtpDetails {

    @Id
    private String phoneNumber;

    private String otp;

    private String rollNo;
}
