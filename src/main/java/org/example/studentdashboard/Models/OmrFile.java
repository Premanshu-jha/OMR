package org.example.studentdashboard.Models;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
public class OmrFile {

    private String fileName;
    private String fileType;
    private Long fileSize;
    private ZonedDateTime uploadedAt;

}
