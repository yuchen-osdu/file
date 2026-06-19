package org.opengroup.osdu.file.model.filemetadata.filedetails;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.stream.Stream;

import org.opengroup.osdu.file.exception.EnumValidationException;
import com.fasterxml.jackson.annotation.JsonCreator;

@Schema(description = "Indicates how a trace scalar value is applied. STANDARD uses the in-file scalar, NOSCALE ignores it, OVERRIDE replaces it.")
public enum ScalarIndicator {
    STANDARD("STANDARD"), NOSCALE("NOSCALE"), OVERRIDE("OVERRIDE");

    private final String value;

    ScalarIndicator(String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    String getType() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ScalarIndicator create(String value) throws EnumValidationException {
        if(value == null) {
            throw new EnumValidationException(value, "ScalarIndicator");
        }
        return Stream.of(values()).filter(e -> e.toString().equals(value)).findFirst()
                .orElseThrow(new EnumValidationException(value, "ScalarIndicator"));
    }
}
