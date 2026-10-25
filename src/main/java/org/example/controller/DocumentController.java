package org.example.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.dto.ApiResponse;
import org.example.dto.DocumentResponseDto;
import org.example.service.DocumentMetaDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name="Document Management", description = "Endpoints for uploading, listing and managing documents and their vector embeddings.")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentMetaDataService documentMetaDataService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and index a document(PDF, DOCX, TEXT, MD, CSV)",
    description = "This api is used to upload and index document files.")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadDocument(@RequestParam("file")MultipartFile file){


        DocumentResponseDto documentResponseDto = this.documentMetaDataService.uploadAndProcess(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<DocumentResponseDto>builder()
                        .success(true)
                        .data(documentResponseDto)
                        .timestamp(LocalDateTime.now())
                        .message("File indexed successfully")
                .build());
    }

    //api to upload multiple documents
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and index multiple documents simultaneously", description = "This api is used to upload and index multiple documents.")
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> uploadMultiple(
            @RequestParam("files") List<MultipartFile> files
    ){
        List<DocumentResponseDto> responseDtos = documentMetaDataService.uploadMultipleDocuments(files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<List<DocumentResponseDto>>builder().message("Documents uploaded successfully").success(true)
                        .timestamp(LocalDateTime.now())
                        .data(responseDtos)
                        .build());


    }



}
