package org.example.studentdashboard.Service;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.example.studentdashboard.Models.DownloadStatus;
import org.example.studentdashboard.Models.FileResponse;
import org.example.studentdashboard.Models.OmrFile;
import org.example.studentdashboard.Repositories.DownloadStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {
    @Autowired
    GridFsTemplate gridFsTemplate;

    @Autowired
    DownloadStatusRepository statusRepository;


    public GridFSFile getFileLabel(String fileId){
        GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
        if(gridFSFile == null) throw new RuntimeException("File not found for the given id");
        return gridFSFile;
    }

    public List<FileResponse> getAllFileLabels(){
        List<FileResponse> list = new ArrayList<>();
        Query query = new Query().with(Sort.by(Sort.Direction.DESC,"uploadDate"));
        gridFsTemplate.find(query).forEach(file -> {
            double bytes = file.getLength();
            double mb = bytes/(1024 * 1024);
            mb = Math.round(mb*100.0)/100.0;
            list.add(new FileResponse(file.getObjectId().toHexString(),file.getFilename(),mb,file.getUploadDate().toString()));
        });
        return list;
    }


    public void deleteFile(String fileId) {
        ObjectId objId = new ObjectId(fileId);
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(objId)));
        Query chunkQuery = new Query(Criteria.where("files_id").is(objId));
        gridFsTemplate.delete(chunkQuery);
    }
    public String uploadOmrFile(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        GridFSFile existingFile = gridFsTemplate.findOne(
                new Query(Criteria.where("filename").is(originalFileName)));
        if(existingFile != null) {
            deleteFile(existingFile.getObjectId().toString());
        }
        OmrFile metaData = new OmrFile(originalFileName,file.getContentType(),file.getSize(),Instant.now());

        ObjectId fileId = gridFsTemplate.store(file.getInputStream(),
                file.getOriginalFilename(),file.getContentType(),metaData);

        return fileId.toString();
    }



    public DownloadStatus getDownloadStatus(String fileId){
         return statusRepository.findById(fileId)
                 .orElse(new DownloadStatus(fileId,"NOT STARTED"));
    }

    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable String fileId) throws IOException{
        GridFsResource gridFsResource = gridFsTemplate.getResource(getFileLabel(fileId));
        statusRepository.save(new DownloadStatus(fileId,"PENDING"));
        StreamingResponseBody responseBody = outputStream -> {
            try(InputStream inputStream = gridFsResource.getInputStream()){
                byte[] buffer = new byte[8192];
                int bytesRead;
                while((bytesRead = inputStream.read(buffer)) != -1){
                    outputStream.write(buffer,0,bytesRead);
                }
                outputStream.flush();
                statusRepository.save(new DownloadStatus(fileId,"COMPLETED"));
            } catch (IOException e) {
                statusRepository.save(new DownloadStatus(fileId,"FAILED"));
                throw new RuntimeException("Download Failed!");
            }
        };
        Long contentLength = gridFsResource.contentLength();
        String contentType = gridFsResource.getContentType();
        String disposition = String.format("attachment; filename=\"%s\"",gridFsResource.getFilename());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(contentLength)
                .header(HttpHeaders.CONTENT_DISPOSITION,disposition)
                .body(responseBody);

    }

}
