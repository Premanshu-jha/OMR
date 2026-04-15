package org.example.studentdashboard.Controller;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.example.studentdashboard.Models.DownloadStatus;
import org.example.studentdashboard.Models.FileResponse;
import org.example.studentdashboard.Models.StudentData;
import org.example.studentdashboard.Service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    FileService fileService;

    @GetMapping
    public List<FileResponse> getAllFiles(){
         return fileService.getAllFileLabels();
    }


    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
         return fileService.uploadOmrFile(file);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable String id){
         fileService.deleteFile(id);
         return ResponseEntity.ok().body("File deleted succesfully!");
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable String id) throws IOException{
      return fileService.downloadFile(id);
    }

    @GetMapping("download/status/{id}")
    public DownloadStatus getDownloadStatus(@PathVariable String id){
         return fileService.getDownloadStatus(id);
    }

    @GetMapping("/exam-results")
    public List<StudentData> getExamResults() throws IOException {
         return fileService.getExamResults();
    }

}
