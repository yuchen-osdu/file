package org.opengroup.osdu.file.service;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.storage.MultiRecordInfo;

final class DatasetMetadataRecords {

  private DatasetMetadataRecords() {
  }

  static MultiRecordInfo requirePresent(MultiRecordInfo records) {
    if (records == null) {
      throw new AppException(HttpStatus.SC_NOT_FOUND, "Not Found.", "Dataset metadata records not found.");
    }
    return records;
  }
}
