/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.web.jersey.mapper;

import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.BadGatewayException;
import org.eclipse.edc.web.spi.exception.EdcApiException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.eclipse.edc.validator.spi.Violation.violation;

class EdcApiExceptionMapperTest {

    private final EdcApiExceptionMapper mapper = new EdcApiExceptionMapper();

    @ParameterizedTest
    @ArgumentsSource(EdcApiExceptions.class)
    void toResponse_edcApiExceptions(EdcApiException throwable, int expectedCode) {
        try (var response = mapper.toResponse(throwable)) {
            assertThat(response.getStatus()).isEqualTo(expectedCode);
            assertThat(response.getStatusInfo().getReasonPhrase()).isNotBlank();
            assertThat(response.getEntity()).asInstanceOf(LIST).first().asInstanceOf(type(ApiErrorDetail.class))
                    .satisfies(detail -> {
                        assertThat(detail.getMessage()).isNotBlank();
                        assertThat(detail.getType()).isNotBlank();
                    });
        }
    }

    @Test
    void validationFailureException_shouldMapViolationsToApiErrorDetail() {
        var violations = List.of(
                violation("violation one", "path one"),
                violation("violation two", "path two", "invalid value")
        );
        var throwable = new ValidationFailureException(violations);

        try (var response = mapper.toResponse(throwable)) {
            assertThat(response.getEntity()).asInstanceOf(LIST).hasSize(2)
                    .map(ApiErrorDetail.class::cast)
                    .anySatisfy(one -> {
                        assertThat(one.getMessage()).isEqualTo("violation one");
                        assertThat(one.getPath()).isEqualTo("path one");
                    })
                    .anySatisfy(two -> {
                        assertThat(two.getMessage()).isEqualTo("violation two");
                        assertThat(two.getPath()).isEqualTo("path two");
                        assertThat(two.getInvalidValue()).isEqualTo("invalid value");
                    });
        }
    }

    private static class EdcApiExceptions implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(new AuthenticationFailedException(), 401),
                    Arguments.of(new ObjectConflictException(List.of("conflict")), 409),
                    Arguments.of(new ObjectNotFoundException(Object.class, "test-object-id"), 404),
                    Arguments.of(new NotAuthorizedException(), 403),
                    Arguments.of(new InvalidRequestException(List.of("detail")), 400),
                    Arguments.of(new ValidationFailureException(List.of(violation("error", "path"))), 400),
                    Arguments.of(new BadGatewayException("something happened"), 502)
            );
        }
    }

}
