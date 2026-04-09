package org.example.studentdashboard.Models;
import lombok.Data;

import java.time.Instant;

@Data
public class OmrFile {

    private String fileName;
    private String fileType;
    private Long fileSize;
    private Instant uploadedAt;

}
