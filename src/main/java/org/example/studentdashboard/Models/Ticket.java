package org.example.studentdashboard.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "DownloadTicket",timeToLive = 30)
@Data
public class Ticket {

    @Id
    private String ticketId;
    private String entityId;
}
