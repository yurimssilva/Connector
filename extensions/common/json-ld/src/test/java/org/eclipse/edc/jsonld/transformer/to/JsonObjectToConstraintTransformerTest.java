/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToConstraintTransformerTest {
    
    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private TransformerContext context = mock(TransformerContext.class);
    
    private JsonObjectToConstraintTransformer transformer;
    
    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToConstraintTransformer();
    }
    
    @Test
    void transform_shouldCallContext_whenAtomicConstraint() {
        var atomicConstraintJson = jsonFactory.createObjectBuilder()
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, jsonFactory.createObjectBuilder()
                        .add(VALUE, "left"))
                .add(ODRL_OPERATOR_ATTRIBUTE, jsonFactory.createObjectBuilder()
                        .add(VALUE, "EQ"))
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, jsonFactory.createObjectBuilder()
                        .add(VALUE, "right"))
                .build();
        var atomicConstraint = AtomicConstraint.Builder.newInstance().build();
        when(context.transform(atomicConstraintJson, AtomicConstraint.class)).thenReturn(atomicConstraint);
        
        var result = transformer.transform(atomicConstraintJson, context);
        
        assertThat(result).isEqualTo(atomicConstraint);
        verify(context, times(1)).transform(atomicConstraintJson, AtomicConstraint.class);
    }
    
    @Test
    void transform_shouldCallContext_whenLogicalConstraint() {
        var logicalConstraintJson = jsonFactory.createObjectBuilder()
                .add(ODRL_OPERAND_ATTRIBUTE, jsonFactory.createObjectBuilder()
                        .add(ODRL_AND_CONSTRAINT_ATTRIBUTE, jsonFactory.createArrayBuilder().build()))
                .build();
        var andConstraint = AndConstraint.Builder.newInstance().build();
        when(context.transform(logicalConstraintJson, MultiplicityConstraint.class)).thenReturn(andConstraint);
    
        var result = transformer.transform(logicalConstraintJson, context);
    
        assertThat(result).isEqualTo(andConstraint);
        verify(context, times(1)).transform(logicalConstraintJson, MultiplicityConstraint.class);
    }
    
    @Test
    void transform_shouldReportProblem_whenUnknownConstraint() {
        var invalidConstraintJson = jsonFactory.createObjectBuilder().build();
        
        var result = transformer.transform(invalidConstraintJson, context);
        
        assertThat(result).isNull();
        verify(context, times(1)).reportProblem(anyString());
    }
    
}
