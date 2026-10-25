package org.example.dto;

import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultDto {

    private String query;
    private int totalMatches;
    private List<CitationDto> matches;
}
