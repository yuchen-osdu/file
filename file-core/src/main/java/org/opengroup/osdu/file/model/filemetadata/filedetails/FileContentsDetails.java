package org.opengroup.osdu.file.model.filemetadata.filedetails;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.opengroup.osdu.file.model.filemetadata.MetaItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details describing the contents and format of a file")
public class FileContentsDetails {

    @Schema(description = "Kind identifier for the file contents")
    private String kind;

    @JsonProperty("TargetKind")
    @Schema(description = "Target kind for data conversion or mapping")
    private String targetKind;

    @JsonProperty("FileType")
    @Schema(description = "Type of the file (e.g. SEG-Y, LAS)")
    private String fileType;

    @Valid
    @JsonProperty("FrameOfReference")
    @ArraySchema(arraySchema = @Schema(description = "Frame of reference items defining unit and CRS context"))
    private List<MetaItem> frameOfReference = new ArrayList<>();

    @JsonProperty("ExtensionProperties")
    @Schema(description = "Additional extension properties for the file contents")
    private Object extensionProperties;

    @JsonProperty("ParentReference")
    @Schema(description = "Reference to the parent record this file is derived from")
    private String parentReference;
}
