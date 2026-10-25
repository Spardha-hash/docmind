package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.config.AppProperties;
import org.example.entity.DocumentMetadata;
import org.example.entity.DocumentStatus;
import org.example.exception.DocumentProcessingException;
import org.example.repository.DocumentMetaDataRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);
    private final VectorStore vectorStore;
    private final DocumentMetaDataRepo documentMetaDataRepo;
    private final AppProperties appProperties;
    public int ingest(DocumentMetadata documentMetadata, List<Document> parsedDocs) {
       log.info("Ingesting document [id={}, name={}, pages={}]", documentMetadata.getId(), documentMetadata.getFilename(), documentMetadata.getFileSize());

       try{
           documentMetadata.setStatus(DocumentStatus.PROCESSING);
           documentMetadata.setTotalPages(parsedDocs.size());

           //1. Text chunking using TokenTextSplitter
           TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                   .withChunkSize(appProperties.getRag().getChunkSize())
                   .withMinChunkSizeChars(appProperties.getRag().getMinChunksSizeChars())
                   .withMinChunkLengthToEmbed(appProperties.getRag().getMinChunkLengthToEmbed())
                   .withMaxNumChunks(appProperties.getRag().getMaxNumChunks())
                   .withKeepSeparator(true)
                   .build();

           List<Document> chunks = tokenTextSplitter.apply(parsedDocs);
           if(chunks.isEmpty()){
               documentMetadata.setStatus(DocumentStatus.FAILED);
               documentMetadata.setErrorMessage("Document appears to be empty or unscannable");
               documentMetaDataRepo.save(documentMetadata);
               return 0;
           }

           //2. Meta data enrichment on each chunk
           List<Document> enrichedChunks = new ArrayList<>();
           for(int i = 0; i< chunks.size(); i++){
               Document chunk = chunks.get(i);
               Map<String, Object> enrichedMetadata = new HashMap<>(chunk.getMetadata());
               enrichedMetadata.put("documentId", documentMetadata.getId().toString());
               enrichedMetadata.put("fileName", documentMetadata.getFilename());
               enrichedMetadata.put("contentType", documentMetadata.getContentType());
               enrichedMetadata.put("chunkIndex", i);

               //preserve or calculate page number if available
               Object pageNumber = chunk.getMetadata().get("page_number");
               if(pageNumber == null){
                   pageNumber = chunk.getMetadata().get("pageNumber");
               }
               if(pageNumber != null){
                  enrichedMetadata.put("pageNumber", pageNumber);
               }
               Document enrichedDoc = new Document(chunk.getText(), enrichedMetadata);
               enrichedChunks.add(enrichedDoc);
           }

           //3. write chunks and embedding to pg vector
           log.info("Writing {} vector chunks to PgVectorStore for document: {}", enrichedChunks.size(), documentMetadata.getFilename());
           vectorStore.add(enrichedChunks);

           //4. update document status to index
           documentMetadata.setStatus(DocumentStatus.INDEXED);
           documentMetadata.setTotalChunks(enrichedChunks.size());
           documentMetadata.setErrorMessage(null);
           documentMetaDataRepo.save(documentMetadata);
           log.info("Successfully indexed document [id={}, name={}, chunks={}]", documentMetadata.getId(), documentMetadata.getFilename(), enrichedChunks.size());
           return enrichedChunks.size();

       }
       catch (Exception ex){
           log.error("Failed to ingest document into vector store: {}", documentMetadata.getFilename(), ex);
           documentMetadata.setStatus(DocumentStatus.FAILED);
           documentMetadata.setErrorMessage(ex.getMessage());
           documentMetaDataRepo.save(documentMetadata);
           throw new DocumentProcessingException("Failed to index document: " + ex.getMessage(), ex);

       }
    }
}
