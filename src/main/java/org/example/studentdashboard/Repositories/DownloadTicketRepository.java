package org.example.studentdashboard.Repositories;
import org.example.studentdashboard.Models.Ticket;
import org.springframework.data.repository.CrudRepository;

public interface DownloadTicketRepository extends CrudRepository<Ticket,String> {
}
