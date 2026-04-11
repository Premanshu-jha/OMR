package org.example.studentdashboard.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "DownloadStatus",timeToLive = 600)
public class DownloadStatus {
    @Id
    private String fileId;
    private String status;
}
