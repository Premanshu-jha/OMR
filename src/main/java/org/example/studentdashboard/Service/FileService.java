package org.example.studentdashboard.Service;

import org.bson.types.ObjectId;
import org.example.studentdashboard.Models.OmrFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.ZonedDateTime;

@Service
public class FileService {
    @Autowired
    GridFsTemplate gridFsTemplate;

    public String saveOmrFile(MultipartFile file) throws IOException {
        OmrFile metaData = new OmrFile();
        metaData.setFileName(file.getOriginalFilename());
        metaData.setFileType(file.getContentType());
        metaData.setFileSize(file.getSize());
        metaData.setUploadedAt(ZonedDateTime.now());

        ObjectId fileId = gridFsTemplate.store(file.getInputStream(),
                file.getOriginalFilename(),file.getContentType(),metaData);

        return fileId.toString();
    }
}
