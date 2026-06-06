package org.example.studentdashboard.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class SMSService {

    // Updated to use the correct control domain from your cURL
    public final WebClient webClient = WebClient.builder().baseUrl("https://control.msg91.com").build();

    @Value("${msg91.auth-key}")
    private String authKey;

    @Value("${msg91.template-id}")
    private String templateId;

    public void sendOtp(String phoneNumber, String otp){
        if(phoneNumber.length() != 10)
            throw new RuntimeException("Invalid phone number!");

        Map<String, String> recipient = Map.of(
                "mobiles", "91" + phoneNumber,
                "OTP", otp
        );


        Map<String, Object> requestBody = Map.of(
                "template_id", templateId,
                "short_url", "0",
                "recipients", List.of(recipient)
        );

        String response = webClient.post()
                .uri("/api/v5/flow")
                .header("authkey", authKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        System.out.println("MSG91 Flow Response: " + response);
    }
}