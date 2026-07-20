/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.file.provider.aws.datamodel.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.core.common.model.file.DriverType;
import org.opengroup.osdu.core.common.model.file.FileLocation;
import org.opengroup.osdu.file.provider.aws.datamodel.coverter.DateToEpochTypeConverter;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
public class FileLocationDoc {

    private String fileId;
    private String dataPartitionId;
    private String driver;
    private String location;
    private Date createdAt;
    private String createdBy;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("fileId")
    public String getFileId() {
        return fileId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("dataPartitionId")
    public String getDataPartitionId() {
        return dataPartitionId;
    }

    @DynamoDbAttribute("driver")
    public String getDriver() {
        return driver;
    }

    @DynamoDbAttribute("location")
    public String getLocation() {
        return location;
    }

    @DynamoDbAttribute("createdAt")
    @DynamoDbConvertedBy(DateToEpochTypeConverter.class)
    public Date getCreatedAt() {
        return createdAt;
    }

    @DynamoDbAttribute("createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    public static FileLocationDoc createFileLocationDoc(FileLocation fileLocation, String dataPartitionId) {
        return FileLocationDoc.builder()
                              .fileId(fileLocation.getFileID())
                              .dataPartitionId(dataPartitionId)
                              .driver(fileLocation.getDriver().name())
                              .location(fileLocation.getLocation())
                              .createdAt(fileLocation.getCreatedAt())
                              .createdBy(fileLocation.getCreatedBy())
                              .build();
    }

    public FileLocation createFileLocationFromDoc() {
        return FileLocation.builder()
                           .fileID(fileId)
                           .driver(DriverType.valueOf(driver))
                           .location(location)
                           .createdAt(createdAt)
                           .createdBy(createdBy)
                           .build();
    }
}
