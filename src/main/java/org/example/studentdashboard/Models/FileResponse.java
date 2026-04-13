package org.example.studentdashboard.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FileResponse {

    private String id;
    private String fileName;
    private double size;
    private String uploadDate;
}
