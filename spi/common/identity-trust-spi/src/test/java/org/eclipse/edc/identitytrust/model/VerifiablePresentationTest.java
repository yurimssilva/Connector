/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identitytrust.model;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identitytrust.TestFunctions.createCredential;

class VerifiablePresentationTest {

    @Test
    void buildMinimalVp() {
        assertThatNoException().isThrownBy(() -> VerifiablePresentation.Builder.newInstance()
                .credential(createCredential())
                .type("test-type")
                .build());

    }

    @Test
    void build_noType() {
        assertThatThrownBy(() -> VerifiablePresentation.Builder.newInstance()
                .types(new ArrayList<>())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one type");
    }

    @Test
    void build_noCredentials() {
        assertThatThrownBy(() -> VerifiablePresentation.Builder.newInstance()
                .types(List.of("test-type"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageEndingWith("must have at least one credential.");
    }
}
