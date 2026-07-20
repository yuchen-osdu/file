package org.opengroup.osdu.file.model.filemetadata;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.opengroup.osdu.file.model.filemetadata.filedetails.ForKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A frame of reference meta item describing unit of measure or coordinate reference system context")
public class MetaItem {

    @NotNull(message = "frameOfReference.kind must not be null")
    @Schema(description = "The kind of frame of reference", requiredMode = Schema.RequiredMode.REQUIRED)
    private ForKind kind;

    @Schema(description = "Human-readable name of the frame of reference")
    private String name;

    @NotNull(message = "persistableReference must not be null")
    @Schema(description = "Persistable reference string for the frame of reference", requiredMode = Schema.RequiredMode.REQUIRED)
    private String persistableReference;

    @ArraySchema(arraySchema = @Schema(description = "Names of the properties this frame of reference applies to"))
    private List<String> propertyNames = new ArrayList<>();

    @ArraySchema(arraySchema = @Schema(description = "Values associated with the property names"))
    private List<String> propertyValues = new ArrayList<>();

    @Schema(description = "Uncertainty value associated with this frame of reference")
    private int uncertainty;
}
