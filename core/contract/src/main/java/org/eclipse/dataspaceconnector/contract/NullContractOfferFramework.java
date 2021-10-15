package org.eclipse.dataspaceconnector.contract;

import java.util.stream.Stream;

import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFrameworkQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferTemplate;

/**
 * NullObject of the {@link ContractOfferFramework}
 */
public class NullContractOfferFramework implements ContractOfferFramework {

    @Override
    public Stream<ContractOfferTemplate> queryTemplates(ContractOfferFrameworkQuery query) {
        return Stream.empty();
    }
}
