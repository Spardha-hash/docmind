package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.DocumentResponseDto;
import org.example.entity.DocumentMetadata;
import org.example.entity.DocumentStatus;
import org.example.repository.DocumentMetaDataRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentMetaDataService {

    private static final Logger log = LoggerFactory.getLogger(DocumentMetaDataService.class);
    private final DocumentMetaDataRepo documentMetaDataRepo;
    private final DocumentParserService documentParserService;
    private final DocumentIngestionService documentIngestionService;
    private final JdbcTemplate jdbcTemplate;

    //method to upload and parse document
    public DocumentResponseDto uploadAndProcess(MultipartFile file){
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "application/octat-stream";

        //document meta data create
        DocumentMetadata documentMetadata = DocumentMetadata.builder().filename(fileName).contentType(contentType).status(DocumentStatus.UPLOADING).fileSize(file.getSize()).createdAt(LocalDateTime.now()).build();

        //save the document meta data
        documentMetadata = documentMetaDataRepo.save(documentMetadata);

        //parse the file
        List<Document> parsedDocs = documentParserService.parse(file);

        //ingest service
        int chunksCreated= documentIngestionService.ingest(documentMetadata, parsedDocs);

        return DocumentResponseDto.builder()
                .id(documentMetadata.getId())
                .fileName(documentMetadata.getFilename())
                .fileSize(documentMetadata.getFileSize())
                .chunksCreated(chunksCreated)
                .status(documentMetadata.getStatus())
                .message("Document successfully processed and indexed")
                .build();

    }

    public List<DocumentResponseDto> uploadMultipleDocuments(List<MultipartFile> files) {

        List<DocumentResponseDto> responseDtos = new ArrayList<>();
        for(MultipartFile file : files){
            DocumentResponseDto result = this.uploadAndProcess(file);
            responseDtos.add(result);
        }
        return responseDtos;
    }
}
