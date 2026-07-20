package org.opengroup.osdu.file.model.filemetadata.filedetails;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Kind of frame of reference. Enumeration: CRS, Unit, Measurement, AzimuthReference, DateTime.")
public enum ForKind {

    CRS("CRS"), Unit("Unit"), Measurement("Measurement"), AzimuthReference("AzimuthReference"), DateTime("DateTime");

    private String kind;

    ForKind(String kind)
    {
        this.kind = kind;
    }

    public String getValue()
    {
        return kind;
    }
}
