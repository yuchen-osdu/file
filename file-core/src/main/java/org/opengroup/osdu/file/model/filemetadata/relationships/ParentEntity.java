package org.opengroup.osdu.file.model.filemetadata.relationships;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A parent entity from which this file record is derived")
public class ParentEntity {

    @Schema(description = "Confidence level of the parent relationship (0–100)")
    private int confidence;

    @Schema(description = "Record ID of the parent entity")
    private String id;

    @Schema(description = "Name of the parent entity")
    private String name;

    @Schema(description = "Version of the parent entity")
    private int version;
}
