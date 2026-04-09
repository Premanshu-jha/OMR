package org.example.studentdashboard.Controller;

import org.example.studentdashboard.Service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    FileService fileService;

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
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String id) throws IOException{
        GridFsResource gridFsResource = fileService.getOmrFileResource(id);
        String disposition = String.format("attachment; filename=\"%s\"",gridFsResource.getFilename());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(gridFsResource.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,disposition)
                .body(new InputStreamResource(gridFsResource.getInputStream()));
    }



}
