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

package org.opengroup.osdu.file.provider.aws.datamodel.coverter;

import java.util.Date;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class DateToEpochTypeConverter implements AttributeConverter<Date> {

    @Override
    public AttributeValue transformFrom(Date date) {
        if (date == null) {
            return AttributeValue.builder().nul(true).build();
        }
        return AttributeValue.builder().n(String.valueOf(date.getTime())).build();
    }

    @Override
    public Date transformTo(AttributeValue attributeValue) {
        if (attributeValue == null || Boolean.TRUE.equals(attributeValue.nul())) {
            return null;
        }
        return new Date(Long.parseLong(attributeValue.n()));
    }

    @Override
    public EnhancedType<Date> type() {
        return EnhancedType.of(Date.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.N;
    }
}
