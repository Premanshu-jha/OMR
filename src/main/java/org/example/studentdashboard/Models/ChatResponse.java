package org.example.studentdashboard.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {

    private String title;
    private String content;
    private String createdAt;

}
