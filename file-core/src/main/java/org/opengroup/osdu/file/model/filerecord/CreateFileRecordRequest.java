package org.opengroup.osdu.file.model.filerecord;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for creating a file record from an unsigned URL")
public class CreateFileRecordRequest {
  // Will we need a legal tag and acl from the user? Or will these be inferred somehow
  // could infer acl to be data.file
  // could infer legal tag to a default US legal tag
    @JsonProperty("UnsignedUrl")
    @Schema(description = "Unsigned URL pointing to the file to be registered")
    String unsignedUrl;

    @JsonProperty("FileName")
    @Schema(description = "Name of the file")
    String fileName;

    @JsonProperty("FileDescription")
    @Schema(description = "Description of the file")
    String fileDescription;
}
