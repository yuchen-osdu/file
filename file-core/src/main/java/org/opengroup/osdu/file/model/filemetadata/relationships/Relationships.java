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
@Schema(description = "Relationships of this file record to other entities in the data ecosystem")
public class Relationships {

    @Schema(description = "The single parent entity this record is derived from")
    private ParentEntity parentEntity;

    @Schema(description = "Related items associated with this file record")
    private RelatedItems relatedItems;
}
