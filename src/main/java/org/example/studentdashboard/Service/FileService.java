package org.example.studentdashboard.Service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.example.studentdashboard.Models.OmrFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

@Service
public class FileService {
    @Autowired
    GridFsTemplate gridFsTemplate;

    public GridFSFile getFileLabel(String fileId){
        GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
        if(gridFSFile == null) throw new RuntimeException("File not found for the given id");
        return gridFSFile;
    }

    public void deleteFile(String fileId) {
        ObjectId objId = new ObjectId(fileId);
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(objId)));
        Query chunkQuery = new Query(Criteria.where("files_id").is(objId));
        gridFsTemplate.delete(chunkQuery);
    }
    public String uploadOmrFile(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        Query query = new Query(Criteria.where("filename").is(originalFileName));
        gridFsTemplate.delete(query);
        OmrFile metaData = new OmrFile();
        metaData.setFileName(originalFileName);
        metaData.setFileType(file.getContentType());
        metaData.setFileSize(file.getSize());
        metaData.setUploadedAt(Instant.now());

        ObjectId fileId = gridFsTemplate.store(file.getInputStream(),
                file.getOriginalFilename(),file.getContentType(),metaData);

        return fileId.toString();
    }

    public GridFsResource getOmrFileResource(String fileId){
        GridFSFile gridFSFile = getFileLabel(fileId);
        return gridFsTemplate.getResource(gridFSFile);
    }

}
