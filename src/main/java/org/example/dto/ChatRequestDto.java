package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatRequestDto {


    @NotBlank(message = "Question cannot be empty")
    private String question;

    private UUID documentId;

    private Integer topK;

    private Double minSimilarity;

    private String conversationId;

}
