package org.example.studentdashboard.Models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FileResponse {

    private String id;
    private String fileName;
    private double size;
    private String uploadDate;
    private String examType;
    private String examIdentifier;
    private String rollNumber;
}
