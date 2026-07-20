package org.opengroup.osdu.file.model.filerecord;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.core.common.model.storage.Record;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response returned after creating a file record")
public class CreateFileRecordResponse {
    @JsonProperty("FileRecord")
    @Schema(description = "The created storage record for the file")
    Record fileRecord;
}
