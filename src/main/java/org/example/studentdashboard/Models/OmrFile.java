package org.example.studentdashboard.Models;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OmrFile {

    private String fileName;
    private String fileType;
    private Long fileSize;
    private Instant uploadedAt;
    private String examType;
    private String examIdentifier;
}
