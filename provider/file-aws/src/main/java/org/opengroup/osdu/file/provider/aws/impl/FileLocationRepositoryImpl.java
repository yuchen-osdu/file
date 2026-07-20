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

package org.opengroup.osdu.file.provider.aws.impl;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.aws.v2.dynamodb.interfaces.IDynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.v2.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.aws.v2.dynamodb.model.QueryPageResult;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import org.opengroup.osdu.core.common.model.file.FileListRequest;
import org.opengroup.osdu.core.common.model.file.FileListResponse;
import org.opengroup.osdu.core.common.model.file.FileLocation;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.file.exception.FileLocationNotFoundException;
import org.opengroup.osdu.file.exception.OsduException;
import org.opengroup.osdu.file.provider.aws.config.ProviderConfigurationBag;
import org.opengroup.osdu.file.provider.aws.datamodel.entity.FileLocationDoc;
import org.opengroup.osdu.file.provider.interfaces.IFileLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

@Slf4j
@Repository
public class FileLocationRepositoryImpl implements IFileLocationRepository {

    private static final String FIND_ALL_FILTER_EXPRESSION = "dataPartitionId = :partitionId AND createdAt BETWEEN :startDate and :endDate AND createdBy = :user";
    private final DpsHeaders headers;
    private final IDynamoDBQueryHelperFactory dynamoDBQueryHelperFactory;
    private final ProviderConfigurationBag providerConfigurationBag;

    @Autowired
    public FileLocationRepositoryImpl(DpsHeaders headers,
                                      IDynamoDBQueryHelperFactory dynamoDBQueryHelperFactory,
                                      ProviderConfigurationBag providerConfigurationBag) {
        this.headers = headers;
        this.dynamoDBQueryHelperFactory = dynamoDBQueryHelperFactory;
        this.providerConfigurationBag = providerConfigurationBag;
    }

    private DynamoDBQueryHelper<FileLocationDoc> getFileLocationQueryHelper() {
        return dynamoDBQueryHelperFactory.createQueryHelper(headers,
                                                           providerConfigurationBag.fileLocationTableParameterRelativePath,
                                                           FileLocationDoc.class);
    }

    @Override
    public FileLocation findByFileID(String fileID) {
        if (fileID == null) { //new file being generated
            return null;
        }

        try {
            DynamoDBQueryHelper<FileLocationDoc> queryHelper = getFileLocationQueryHelper();
            String dataPartitionId = headers.getPartitionIdWithFallbackToAccountId();

            Optional<FileLocationDoc> docOpt = queryHelper.getItem(fileID, dataPartitionId);

            return docOpt.map(FileLocationDoc::createFileLocationFromDoc).orElse(null);
        } catch (ResourceNotFoundException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                   HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), e.getMessage());
        }
    }

    @Override
    public FileLocation save(FileLocation fileLocation) {
        DynamoDBQueryHelper<FileLocationDoc> queryHelper = getFileLocationQueryHelper();
        String dataPartitionId = headers.getPartitionIdWithFallbackToAccountId();

        FileLocationDoc doc = FileLocationDoc.createFileLocationDoc(fileLocation, dataPartitionId);

        queryHelper.putItem(doc);

        return fileLocation;
    }

    @Override
    public FileListResponse findAll(FileListRequest request) {
        FileListResponse response = new FileListResponse();

        AttributeValue dataPartitionIdAV = AttributeValue.builder().s(headers.getPartitionIdWithFallbackToAccountId()).build();
        AttributeValue timeFromAV = AttributeValue.builder().n(dateToEpoch(request.getTimeFrom()).toString()).build();
        AttributeValue timeToAV = AttributeValue.builder().n(dateToEpoch(request.getTimeTo()).toString()).build();
        AttributeValue userAV = AttributeValue.builder().s(request.getUserID()).build();

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":partitionId", dataPartitionIdAV);
        eav.put(":startDate", timeFromAV);
        eav.put(":endDate", timeToAV);
        eav.put(":user", userAV);

        int pageSize = request.getItems();
        
        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
            .filterExpression(software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                .expression(FIND_ALL_FILTER_EXPRESSION)
                .expressionValues(eav)
                .build())
            .limit(pageSize)
            .build();
            
        QueryPageResult<FileLocationDoc> docs;
        try {
            DynamoDBQueryHelper<FileLocationDoc> queryHelper = getFileLocationQueryHelper();
            docs = queryHelper.scanPage(scanRequest);
        } catch (Exception e) {
            throw new OsduException(e.getMessage(), e);
        }

        if (docs != null) {
            log.debug("Found {} records", docs.getItems().size());

            if (docs.getItems().isEmpty()) {
                throw new FileLocationNotFoundException(
                    format("No file locations found for user %s and time range %s to %s", request.getUserID(), request.getTimeFrom(), request.getTimeTo()));
            }

            List<FileLocation> locations = new ArrayList<>();
            for (FileLocationDoc doc : docs.getItems()) {
                locations.add(doc.createFileLocationFromDoc());
            }

            response = FileListResponse.builder()
                .content(locations)
                .size(pageSize)
                .number(request.getPageNum())
                .numberOfElements(locations.size()).build();
        }

        return response;
    }

    private Long dateToEpoch(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
