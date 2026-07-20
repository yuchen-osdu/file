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

package org.opengroup.osdu.file.helper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import software.amazon.awssdk.services.ssm.model.Parameter;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.aws.partition.PartitionInfoAws;
import org.opengroup.osdu.core.aws.partition.PartitionServiceClientWithCache;
import org.opengroup.osdu.core.aws.v2.ssm.SSMManagerUtil;
import org.opengroup.osdu.core.common.exception.NotFoundException;
import org.opengroup.osdu.file.provider.aws.cache.StsIamRoleCache;
import org.opengroup.osdu.file.provider.aws.helper.StsRoleHelper;

@ExtendWith(MockitoExtension.class)
class StsRoleHelperTest {
    private final String empty = null;
    private final String dataPartition = "dataPartition";
    private final String iamParameterRelativePath = "iamParameterRelativePath";
    private final String tenantSSMPrefix = "tenantSSMPrefix";


    @Test
    void testGetRoleArnForPartition_nullPartition() {
        StsRoleHelper helper = new StsRoleHelper(null, null, null);
        assertThrows(IllegalArgumentException.class, () -> {
            helper.getRoleArnForPartition(empty, iamParameterRelativePath);
        });
    }
    
    @Test
    void testGetRoleArnForPartition_nullIamParameterRelativePath() {
        StsRoleHelper helper = new StsRoleHelper(null, null, null);
        assertThrows(IllegalArgumentException.class, () -> {
            helper.getRoleArnForPartition(dataPartition, empty);
        });
    }

    @Test
    void testGetRoleArnForPartition_nullPartitionInfo() {

        PartitionServiceClientWithCache partitionServiceClient = mock(PartitionServiceClientWithCache.class);
        StsIamRoleCache stsIamRoleCache = mock(StsIamRoleCache.class);
        SSMManagerUtil ssmManagerUtil = mock(SSMManagerUtil.class);

        when(stsIamRoleCache.get(anyString())).thenReturn(empty);
        when(partitionServiceClient.getPartition(anyString())).thenReturn(null);

        StsRoleHelper helper = new StsRoleHelper(partitionServiceClient, stsIamRoleCache, ssmManagerUtil);
        assertThrows(NullPointerException.class, () -> {
            helper.getRoleArnForPartition(dataPartition, iamParameterRelativePath);
        });
    }

    @Test
    void testGetRoleArnForPartition_notFound() {

        PartitionServiceClientWithCache partitionServiceClient = mock(PartitionServiceClientWithCache.class);
        StsIamRoleCache stsIamRoleCache = mock(StsIamRoleCache.class);
        SSMManagerUtil ssmManagerUtil = mock(SSMManagerUtil.class);
        PartitionInfoAws partitionInfo = mock(PartitionInfoAws.class);

        when(stsIamRoleCache.get(anyString())).thenReturn(empty);
        when(partitionServiceClient.getPartition(anyString())).thenReturn(partitionInfo);
        when(partitionInfo.getTenantSSMPrefix()).thenReturn(tenantSSMPrefix);

        StsRoleHelper helper = new StsRoleHelper(partitionServiceClient, stsIamRoleCache, ssmManagerUtil);
        assertThrows(NotFoundException.class, () -> {
            helper.getRoleArnForPartition(dataPartition, iamParameterRelativePath);
        });
    }

    @Test
    void testGetRoleArnForPartition() {

        PartitionServiceClientWithCache partitionServiceClient = mock(PartitionServiceClientWithCache.class);
        StsIamRoleCache stsIamRoleCache = mock(StsIamRoleCache.class);
        SSMManagerUtil ssmManagerUtil = mock(SSMManagerUtil.class);
        PartitionInfoAws partitionInfo = mock(PartitionInfoAws.class);

        List<Parameter> parameters = new ArrayList<Parameter>();

        Parameter parameter = Parameter.builder()
                .name("/arn")
                .value("value")
                .build();
        parameters.add(parameter);

        when(stsIamRoleCache.get(anyString())).thenReturn(empty);
        when(partitionServiceClient.getPartition(anyString())).thenReturn(partitionInfo);
        when(partitionInfo.getTenantSSMPrefix()).thenReturn(tenantSSMPrefix);
        when(ssmManagerUtil.getSsmParamsUnderPath(anyString())).thenReturn(parameters);

        StsRoleHelper helper = new StsRoleHelper(partitionServiceClient, stsIamRoleCache, ssmManagerUtil);
        helper.getRoleArnForPartition(dataPartition, iamParameterRelativePath);

        verify(partitionServiceClient, Mockito.times(1)).getPartition(anyString());
        verify(stsIamRoleCache, Mockito.times(1)).get(anyString());
        verify(stsIamRoleCache, Mockito.times(1)).put(anyString(), anyString());
        verify(ssmManagerUtil, Mockito.times(1)).getSsmParamsUnderPath(anyString());
        verify(partitionInfo, Mockito.times(1)).getTenantSSMPrefix();
    }
}

