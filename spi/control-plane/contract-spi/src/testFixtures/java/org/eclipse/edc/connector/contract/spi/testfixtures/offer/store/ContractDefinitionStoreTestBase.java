/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Tests
 *       Microsoft Corporation - Method signature change
 *       Microsoft Corporation - refactoring
 *       Microsoft Corporation - added tests
 *       Fraunhofer Institute for Software and Systems Engineering - added tests
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.contract.spi.testfixtures.offer.store;

import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.contract.spi.testfixtures.offer.store.TestFunctions.createContractDefinition;
import static org.eclipse.edc.connector.contract.spi.testfixtures.offer.store.TestFunctions.createContractDefinitions;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

public abstract class ContractDefinitionStoreTestBase {

    @Nested
    class Save {

        @Test
        @DisplayName("Save a single Contract Definition that doesn't already exist")
        void doesNotExist() {
            var definition = createContractDefinition("id");
            getContractDefinitionStore().save(definition);

            var definitions = getContractDefinitionStore().findAll(QuerySpec.max())
                    .collect(Collectors.toList());

            assertThat(definitions).hasSize(1);
            assertThat(definitions.get(0)).usingRecursiveComparison().isEqualTo(definition);
        }

        @Test
        @DisplayName("Shouldn't save a single Contract Definition that already exists")
        void alreadyExist_shouldNotUpdate() {
            getContractDefinitionStore().save(createContractDefinition("id", "policy", "contract"));
            var saveResult = getContractDefinitionStore().save(createContractDefinition("id", "updatedAccess", "updatedContract"));

            assertThat(saveResult.failed()).isTrue();
            assertThat(saveResult.reason()).isEqualTo(ALREADY_EXISTS);

            var result = getContractDefinitionStore().findAll(QuerySpec.max());

            assertThat(result).hasSize(1).containsExactly(createContractDefinition("id", "policy", "contract"));
        }

        @Test
        @DisplayName("Save a single Contract Definition that is identical to an existing contract definition except for the id")
        void sameParametersDifferentId() {
            var definition1 = createContractDefinition("id1", "policy", "contract");
            var definition2 = createContractDefinition("id2", "policy", "contract");
            getContractDefinitionStore().save(definition1);
            getContractDefinitionStore().save(definition2);

            var definitions = getContractDefinitionStore().findAll(QuerySpec.max());

            assertThat(definitions).isNotNull().hasSize(2);
        }

        @Test
        @DisplayName("Save multiple Contract Definitions with no preexisting Definitions")
        void noneExist() {
            var definitionsCreated = createContractDefinitions(10);
            saveContractDefinitions(definitionsCreated);

            var definitionsRetrieved = getContractDefinitionStore().findAll(QuerySpec.max());

            assertThat(definitionsRetrieved).hasSize(definitionsCreated.size());
        }

        @Test
        @DisplayName("Save multiple Contract Definitions with some preexisting Definitions")
        void someExist() {
            saveContractDefinitions(createContractDefinitions(3));
            saveContractDefinitions(createContractDefinitions(10));

            var definitionsRetrieved = getContractDefinitionStore().findAll(QuerySpec.max());

            assertThat(definitionsRetrieved).hasSize(10);
        }

        @Test
        @DisplayName("Save multiple Contract Definitions with all preexisting Definitions")
        void allExist() {
            var definitionsCreated = createContractDefinitions(10);
            saveContractDefinitions(definitionsCreated);
            saveContractDefinitions(definitionsCreated);

            var definitionsRetrieved = getContractDefinitionStore().findAll(QuerySpec.max());

            assertThat(definitionsRetrieved).isNotNull().hasSize(definitionsCreated.size());
        }
    }

    @Nested
    class Update {
        @Test
        @DisplayName("Update a non-existing Contract Definition")
        void doesNotExist_shouldNotCreate() {
            var definition = createContractDefinition("id", "policy1", "contract1");

            var result = getContractDefinitionStore().update(definition);

            assertThat(result.failed()).isTrue();
            assertThat(result.reason()).isEqualTo(NOT_FOUND);

            var existing = getContractDefinitionStore().findAll(QuerySpec.max());

            assertThat(existing).hasSize(0);
        }

        @Test
        @DisplayName("Update an existing Contract Definition")
        void exists() {
            var definition1 = createContractDefinition("id", "policy1", "contract1");
            var definition2 = createContractDefinition("id", "policy2", "contract2");

            getContractDefinitionStore().save(definition1);
            getContractDefinitionStore().update(definition2);

            var definitions = getContractDefinitionStore().findAll(QuerySpec.none()).collect(Collectors.toList());

            assertThat(definitions).isNotNull().hasSize(1).first().satisfies(definition -> {
                assertThat(definition.getAccessPolicyId()).isEqualTo(definition2.getAccessPolicyId());
                assertThat(definition.getContractPolicyId()).isEqualTo(definition2.getContractPolicyId());
            });
        }
    }

    @Nested
    class FindAll {
        @Test
        void shouldReturnAll_whenNoFiltersApplied() {
            var definitionsExpected = createContractDefinitions(10);
            saveContractDefinitions(definitionsExpected);

            var definitionsRetrieved = getContractDefinitionStore().findAll(QuerySpec.max());

            assertThat(definitionsRetrieved).isNotNull().hasSize(definitionsExpected.size());
        }

        @ParameterizedTest
        @ValueSource(ints = { 49, 50, 51, 100 })
        void verifyQueryDefaults(int size) {
            var all = IntStream.range(0, size).mapToObj(i -> createContractDefinition("id" + i, "policyId" + i, "contractId" + i))
                    .peek(cd -> getContractDefinitionStore().save(cd))
                    .collect(Collectors.toList());

            assertThat(getContractDefinitionStore().findAll(QuerySpec.max())).hasSize(size)
                    .usingRecursiveFieldByFieldElementComparator()
                    .isSubsetOf(all);
        }

        @Test
        @DisplayName("Find all contract definitions with limit and offset")
        void withSpec() {
            var limit = 20;

            var definitionsExpected = createContractDefinitions(50);
            saveContractDefinitions(definitionsExpected);

            var spec = QuerySpec.Builder.newInstance()
                    .limit(limit)
                    .offset(20)
                    .build();

            var definitionsRetrieved = getContractDefinitionStore().findAll(spec);

            assertThat(definitionsRetrieved).isNotNull().hasSize(limit);
        }

        @Test
        @DisplayName("Find all contract definitions that exactly match a particular access policy ID")
        void queryByAccessPolicyId_withEquals() {

            var definitionsExpected = createContractDefinitions(20);
            saveContractDefinitions(definitionsExpected);

            var spec = QuerySpec.Builder.newInstance()
                    .filter(criterion("accessPolicyId", "=", "policy4"))
                    .build();

            var definitionsRetrieved = getContractDefinitionStore().findAll(spec);

            assertThat(definitionsRetrieved).hasSize(1)
                    .usingRecursiveFieldByFieldElementComparator()
                    .allSatisfy(cd -> assertThat(cd.getId()).isEqualTo("id4"));
        }

        @Test
        @DisplayName("Find all contract definitions that match a range of access policy IDs")
        void queryByAccessPolicyId_withIn() {

            var definitionsExpected = createContractDefinitions(20);
            saveContractDefinitions(definitionsExpected);

            var spec = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("accessPolicyId", "in", List.of("policy4", "policy5", "policy6"))))
                    .build();

            var definitionsRetrieved = getContractDefinitionStore().findAll(spec);

            assertThat(definitionsRetrieved).hasSize(3)
                    .usingRecursiveFieldByFieldElementComparator()
                    .allMatch(cd -> cd.getId().matches("(id)[4-6]"));
        }

        @Test
        @DisplayName("Verify empty result when query contains a nonexistent value")
        void queryByNonexistentValue() {

            var definitionsExpected = createContractDefinitions(20);
            saveContractDefinitions(definitionsExpected);

            var spec = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("contractPolicyId", "=", "somevalue")))
                    .build();

            assertThat(getContractDefinitionStore().findAll(spec)).isEmpty();
        }

        @Test
        void invalidOperator() {

            var definitionsExpected = createContractDefinitions(20);
            saveContractDefinitions(definitionsExpected);

            var spec = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("accessPolicyId", "sqrt", "foobar"))) //sqrt is invalid
                    .build();

            assertThatThrownBy(() -> getContractDefinitionStore().findAll(spec)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void defaultQuerySpec() {
            var all = IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).peek(getContractDefinitionStore()::save).toList();
            assertThat(getContractDefinitionStore().findAll(QuerySpec.none())).containsExactlyInAnyOrder(all.toArray(new ContractDefinition[]{}));
        }

        @Test
        void verifyPaging() {
            IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(getContractDefinitionStore()::save);

            // page size fits
            assertThat(getContractDefinitionStore().findAll(QuerySpec.Builder.newInstance().offset(4).limit(2).build())).hasSize(2);

            // page size larger than collection
            assertThat(getContractDefinitionStore().findAll(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);
        }

        @Test
        void verifyFiltering() {
            IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(getContractDefinitionStore()::save);
            var criterion = criterion("id", "=", "id3");
            var querySpec = QuerySpec.Builder.newInstance().filter(criterion).build();

            var result = getContractDefinitionStore().findAll(querySpec);

            assertThat(result).extracting(ContractDefinition::getId).containsOnly("id3");
        }

        @Test
        void shouldReturnEmpty_whenQueryByInvalidKey() {
            var definitionsExpected = TestFunctions.createContractDefinitions(5);
            saveContractDefinitions(definitionsExpected);

            var spec = QuerySpec.Builder.newInstance()
                    .filter(criterion("not-exist", "=", "some-value"))
                    .build();

            assertThat(getContractDefinitionStore().findAll(spec)).isEmpty();
        }

        @Test
        void verifySorting() {
            IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(getContractDefinitionStore()::save);

            assertThat(getContractDefinitionStore().findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build())).hasSize(10).isSortedAccordingTo(Comparator.comparing(ContractDefinition::getId));
            assertThat(getContractDefinitionStore().findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build())).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
        }

        @Test
        void verifySorting_invalidProperty() {
            IntStream.range(0, 10).mapToObj(i -> createContractDefinition("id" + i)).forEach(getContractDefinitionStore()::save);
            var query = QuerySpec.Builder.newInstance().sortField("not-exist").sortOrder(SortOrder.DESC).build();

            // must actually collect, otherwise the stream is not materialized
            assertThatThrownBy(() -> getContractDefinitionStore().findAll(query).toList()).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void queryByAssetsSelector_left() {
            var definitionsExpected = createContractDefinitions(20);
            // add a selector expression to the last 5 elements
            definitionsExpected.get(0).getAssetsSelector().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset"));
            definitionsExpected.get(5).getAssetsSelector().add(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));
            saveContractDefinitions(definitionsExpected);

            var spec = QuerySpec.Builder.newInstance()
                    .filter(criterion("assetsSelector.operandLeft", "=", Asset.PROPERTY_ID))
                    .build();

            var definitionsRetrieved = getContractDefinitionStore().findAll(spec);

            assertThat(definitionsRetrieved).hasSize(2)
                    .usingRecursiveFieldByFieldElementComparator()
                    .allSatisfy(cd -> assertThat(cd.getId()).matches("id[0,5]"));
        }

        @Test
        void queryByAssetsSelector_right() {
            var definitionsExpected = createContractDefinitions(20);
            definitionsExpected.get(0).getAssetsSelector().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset"));
            definitionsExpected.get(5).getAssetsSelector().add(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));
            saveContractDefinitions(definitionsExpected);

            var spec = QuerySpec.Builder.newInstance()
                    .filter(criterion("assetsSelector.operandRight", "=", "foobar-asset"))
                    .build();

            var definitionsRetrieved = getContractDefinitionStore().findAll(spec);

            assertThat(definitionsRetrieved).hasSize(1)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsOnly(definitionsExpected.get(5));
        }

        @Test
        void queryByAssetsSelector_rightAndLeft() {
            var definitionsExpected = createContractDefinitions(10);
            definitionsExpected.get(0).getAssetsSelector().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset"));
            definitionsExpected.get(5).getAssetsSelector().add(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));
            saveContractDefinitions(definitionsExpected);

            var spec = QuerySpec.Builder.newInstance()
                    .filter(List.of(
                            new Criterion("assetsSelector.operandLeft", "=", Asset.PROPERTY_ID),
                            new Criterion("assetsSelector.operandRight", "=", "foobar-asset")))
                    .build();

            var definitionsRetrieved = getContractDefinitionStore().findAll(spec);

            assertThat(definitionsRetrieved).hasSize(1)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsOnly(definitionsExpected.get(5));
        }

        @Test
        void queryMultiple() {
            var definitionsExpected = createContractDefinitions(20);
            definitionsExpected.forEach(d -> d.getAssetsSelector().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset")));
            definitionsExpected.forEach(d -> d.getAssetsSelector().add(new Criterion(Asset.PROPERTY_DESCRIPTION, "=", "other")));
            saveContractDefinitions(definitionsExpected);

            var spec = QuerySpec.Builder.newInstance()
                    .filter(new Criterion("assetsSelector.operandRight", "=", "test-asset"))
                    .filter(new Criterion("contractPolicyId", "=", "contract4"))
                    .build();

            var definitionsRetrieved = getContractDefinitionStore().findAll(spec);

            assertThat(definitionsRetrieved).hasSize(1)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsOnly(definitionsExpected.get(4));
        }
    }

    @Nested
    class FindById {
        @Test
        void findById() {
            var id = "definitionId";
            var definition = createContractDefinition(id, "policyId", "contractId");
            getContractDefinitionStore().save(definition);

            var result = getContractDefinitionStore().findById(id);

            assertThat(result).isNotNull().isEqualTo(definition);
        }

        @Test
        void findById_invalidId() {
            assertThat(getContractDefinitionStore().findById("invalid-id")).isNull();
        }

    }

    @Nested
    class Delete {
        @Test
        void shouldDelete() {
            var definitionExpected = createContractDefinition("test-id1", "policy1", "contract1");
            getContractDefinitionStore().save(definitionExpected);
            assertThat(getContractDefinitionStore().findAll(QuerySpec.max())).hasSize(1);

            var deleted = getContractDefinitionStore().deleteById("test-id1");

            assertThat(deleted.succeeded()).isTrue();
            assertThat(deleted.getContent()).isNotNull().usingRecursiveComparison().isEqualTo(definitionExpected);
            assertThat(getContractDefinitionStore().findAll(QuerySpec.max())).isEmpty();
        }

        @Test
        void shouldNotDelete_whenEntityDoesNotExist() {
            var deleted = getContractDefinitionStore().deleteById("test-id1");

            assertThat(deleted).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
        }

        @Test
        void verifyStore() {
            var definition1 = createContractDefinition("1");
            var definition2 = createContractDefinition("2");

            getContractDefinitionStore().save(definition1);
            assertThat(getContractDefinitionStore().findAll(QuerySpec.max())).contains(definition1);

            getContractDefinitionStore().save(definition2);
            assertThat(getContractDefinitionStore().findAll(QuerySpec.max())).contains(definition1);

            var deletedDefinition = getContractDefinitionStore().deleteById(definition1.getId());
            assertThat(deletedDefinition.succeeded()).isTrue();
            assertThat(deletedDefinition.getContent()).isEqualTo(definition1);
            assertThat(getContractDefinitionStore().findAll(QuerySpec.max())).doesNotContain(definition1);
        }
    }

    protected abstract ContractDefinitionStore getContractDefinitionStore();

    protected void saveContractDefinitions(List<ContractDefinition> definitions) {
        definitions.forEach(it -> getContractDefinitionStore().save(it));
    }
}
