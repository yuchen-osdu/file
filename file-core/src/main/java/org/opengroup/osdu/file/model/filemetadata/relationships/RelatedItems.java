package org.opengroup.osdu.file.model.filemetadata.relationships;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A collection of related entities associated with this file record")
public class RelatedItems {

    @ArraySchema(arraySchema = @Schema(description = "Confidence levels for each related item (0–100)"))
    private List<Integer> confidences = new ArrayList<>();

    @ArraySchema(arraySchema = @Schema(description = "Record IDs of the related entities"))
    private List<String> ids = new ArrayList<>();

    @ArraySchema(arraySchema = @Schema(description = "Names of the related entities"))
    private List<String> names = new ArrayList<>();

    @ArraySchema(arraySchema = @Schema(description = "Versions of the related entities"))
    private List<Integer> versions = new ArrayList<>();
}
