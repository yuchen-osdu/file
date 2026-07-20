package org.opengroup.osdu.file.model.filemetadata.filedetails;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Schema(description = "A numeric value paired with a unit of measure")
public class ValueWithUnit {

    @NotNull(message = "unitKey can not be null")
    @NotEmpty
    @Schema(description = "Unit of measure key (e.g. 'm', 'ft', 'degC')", requiredMode = Schema.RequiredMode.REQUIRED)
    private String unitKey;

    @NotNull(message = "value can not be null")
    @Schema(description = "The numeric value", requiredMode = Schema.RequiredMode.REQUIRED)
    private double value;
}
