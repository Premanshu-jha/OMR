package org.example.studentdashboard.Repositories;

import org.example.studentdashboard.Models.OtpDetails;
import org.springframework.data.repository.CrudRepository;

public interface OtpRepository extends CrudRepository<OtpDetails,String> {
}
