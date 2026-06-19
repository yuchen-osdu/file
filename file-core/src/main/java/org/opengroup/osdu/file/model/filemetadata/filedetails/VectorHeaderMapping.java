package org.opengroup.osdu.file.model.filemetadata.filedetails;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Mapping definition for a vector header field in a binary file")
public class VectorHeaderMapping {

    @JsonProperty("KeyName")
    @Schema(description = "Name of the header key")
    private String keyName;

    @JsonProperty("WordFormat")
    @Schema(description = "Data format of the word (e.g. IBM_Float, INT32)")
    private String wordFormat;

    @JsonProperty("WordWidth")
    @Schema(description = "Width of the word in bytes")
    private Integer wordWidth;

    @JsonProperty("Position")
    @Schema(description = "Byte position of this field in the header")
    private Integer position;

    @JsonProperty("UoM")
    @Schema(description = "Unit of measure for the header value")
    private String uom;

    @JsonProperty("ScalarIndicator")
    @Valid
    @Schema(description = "Indicates how scaling is applied to the value")
    private ScalarIndicator scalarIndicator;

    @JsonProperty("ScalarOverride")
    @Schema(description = "Override value for the scalar when ScalarIndicator is OVERRIDE")
    private Integer scalarOverride;

}
