package org.eclipse.dataspaceconnector.s4.fakes;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.util.UUID;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/mgmt")
public class MgmtController {

    private final AssetLoader assetLoader;
    private final ContractDefinitionLoader contractDefinitionLoader;

    public MgmtController(AssetLoader assetLoader, ContractDefinitionLoader contractDefinitionLoader) {
        this.assetLoader = assetLoader;
        this.contractDefinitionLoader = contractDefinitionLoader;
    }

    @Path("asset/{id}/{data}")
    @POST
    public Response addAsset(@PathParam("id") String id, @PathParam("data") String data) {

        Asset asset = Asset.Builder.newInstance()
                .id(id)
                .build();

        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .type("fake")
                .property("data", data)
                .build();

        assetLoader.accept(asset, dataAddress);

        return Response.ok().build();
    }

    @Path("contractdefinition/{asset-id}")
    @POST
    public Response addContractDefinition(@PathParam("asset-id") String assetId) {

        ContractDefinition contractDefinition = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .accessPolicy(fakePolicy())
                .contractPolicy(fakePolicy())
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, assetId).build())
                .build();

        contractDefinitionLoader.accept(contractDefinition);

        return Response.ok().build();
    }

    private Policy fakePolicy() {
        return Policy.Builder.newInstance()
                .permission(
                        Permission.Builder.newInstance()
                                .uid(UUID.randomUUID().toString())
                                .action(Action.Builder.newInstance().type("USE").build()).build())
                .build();
    }
}
