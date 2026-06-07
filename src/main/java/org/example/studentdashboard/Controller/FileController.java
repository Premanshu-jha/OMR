package org.example.studentdashboard.Controller;
import org.example.studentdashboard.Models.DownloadStatus;
import org.example.studentdashboard.CSVModels.ExamResults;
import org.example.studentdashboard.Models.FileResponse;
import org.example.studentdashboard.Service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    FileService fileService;

    @GetMapping
    public List<FileResponse> getAllFiles(){
         return fileService.getAllFileLabels();
    }


    @PostMapping("/upload")
    public FileResponse uploadFile(@RequestParam("file") MultipartFile file,@RequestParam(required = false) String examType,@RequestParam(required = false) String rollNumber) throws IOException {
         return fileService.uploadFile(file,examType,rollNumber);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable String id){
         fileService.deleteFile(id);
         return ResponseEntity.ok().body("File deleted succesfully!");
    }

    @GetMapping("/generate-ticket/{fileId}")
    public String generateTicket(@PathVariable String fileId) {
        return fileService.generateTicket(fileId);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable String id,@RequestParam("ticket") String ticketId) throws IOException{
      return fileService.downloadFile(id,ticketId);
    }

    @GetMapping("/download/status/{id}")
    public DownloadStatus getDownloadStatus(@PathVariable String id){
         return fileService.getDownloadStatus(id);
    }

    @GetMapping("/{examType}/{examIdentifier}/exam-results")
    public ExamResults getExamResults(@PathVariable String examType,@PathVariable String examIdentifier) throws IOException {
         return fileService.getExamResults(examType,examIdentifier);
    }

    @PostMapping("/{examType}/{examIdentifier}/bulk-update")
    public ResponseEntity<String> bulkUpdate(@PathVariable String examType,@PathVariable String examIdentifier) throws IOException {
        fileService.bulkPushFileData(examType,examIdentifier);
        return ResponseEntity.ok("File records pushed succesfully!");
    }

}
