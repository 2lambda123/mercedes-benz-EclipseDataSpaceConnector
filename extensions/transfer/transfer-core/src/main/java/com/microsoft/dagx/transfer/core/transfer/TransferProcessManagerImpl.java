/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.core.transfer;

import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.TransferInitiateResponse;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.transfer.TransferWaitStrategy;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.transfer.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.microsoft.dagx.spi.types.domain.transfer.TransferProcess.Type.CLIENT;
import static com.microsoft.dagx.spi.types.domain.transfer.TransferProcess.Type.PROVIDER;
import static com.microsoft.dagx.spi.types.domain.transfer.TransferProcessStates.INITIAL;
import static com.microsoft.dagx.spi.types.domain.transfer.TransferProcessStates.PROVISIONED;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;

/**
 *
 */
public class TransferProcessManagerImpl implements TransferProcessManager {
    private final AtomicBoolean active = new AtomicBoolean();
    private int batchSize = 5;
    private TransferWaitStrategy waitStrategy = () -> 5000L;  // default wait five seconds
    private ResourceManifestGenerator manifestGenerator;
    private ProvisionManager provisionManager;
    private TransferProcessStore transferProcessStore;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private DataFlowManager dataFlowManager;
    private Monitor monitor;
    private ExecutorService executor;
    private StatusCheckerRegistry statusCheckerRegistry;

    private TransferProcessManagerImpl() {
    }

    public void start(TransferProcessStore processStore) {
        transferProcessStore = processStore;
        active.set(true);
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::run);
    }

    public void stop() {
        active.set(false);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public TransferInitiateResponse initiateClientRequest(DataRequest dataRequest) {
        return initiateRequest(CLIENT, dataRequest);
    }

    @Override
    public TransferInitiateResponse initiateProviderRequest(DataRequest dataRequest) {
        return initiateRequest(PROVIDER, dataRequest);
    }

    private TransferInitiateResponse initiateRequest(TransferProcess.Type type, DataRequest dataRequest) {
        // make the request idempotent: if the process exists, return
        var processId = transferProcessStore.processIdForTransferId(dataRequest.getId());
        if (processId != null) {
            return TransferInitiateResponse.Builder.newInstance().id(processId).status(ResponseStatus.OK).build();
        }
        String id = randomUUID().toString();
        var process = TransferProcess.Builder.newInstance().id(id).dataRequest(dataRequest).type(type).build();
        transferProcessStore.create(process);
        return TransferInitiateResponse.Builder.newInstance().id(process.getId()).status(ResponseStatus.OK).build();
    }

    private void run() {
        while (active.get()) {
            try {
                int provisioning = provisionInitialProcesses();

                // TODO check processes in provisioning state and timestamps for failed processes

                int sent = sendOrProcessProvisionedRequests();

                int provisioned = checkProvisioned();

                int finished = checkCompleted();

                if (provisioning + provisioned + sent + finished == 0) {
                    Thread.sleep(waitStrategy.waitForMillis());
                }
                waitStrategy.success();
            } catch (Error e) {
                throw e; // let the thread die and don't reschedule as the error is unrecoverable
            } catch (InterruptedException e) {
                Thread.interrupted();
                active.set(false);
                break;
            } catch (Throwable e) {
                monitor.severe("Error caught in transfer process manager", e);
                try {
                    Thread.sleep(waitStrategy.retryInMillis());
                } catch (InterruptedException e2) {
                    Thread.interrupted();
                    active.set(false);
                    break;
                }
            }
        }
    }

    /**
     * Transition all processes, who have provisioned resources, into the IN_PROCRESS or STREAMING status, depending on
     * whether they're finite or not.
     * If a process does not have provisioned resources, it will remain in REQUESTED_ACK.
     */
    private int checkProvisioned() {
        List<TransferProcess> requestAcked = transferProcessStore.nextForState(TransferProcessStates.REQUESTED_ACK.code(), batchSize);

        for (var process : requestAcked) {
            if (!process.getProvisionedResourceSet().empty()) {
                monitor.debug("Process " + process.getId() + " is now ongoing");
                if (process.getDataRequest().getTransferType().isFinite()) {
                    process.transitionInProgress();
                } else {
                    process.transitionStreaming();
                }
            } else {
                monitor.debug("Process " + process.getId() + " does not yet have provisioned resources, will stay in " + TransferProcessStates.REQUESTED_ACK);
            }
            transferProcessStore.update(process);
        }

        return requestAcked.size();

    }

    /**
     * Checks all provisioned resources that are assigned to a transfer process for completion. If no StatusChecker exists
     * for a particular ProvisionedResource, it is automatically assumed to be complete.
     */
    private int checkCompleted() {

        //deal with all the client processes
        List<TransferProcess> processesInProgress = transferProcessStore.nextForState(TransferProcessStates.IN_PROGRESS.code(), batchSize);

        for (var process : processesInProgress.stream().filter(p -> p.getType() == CLIENT).collect(Collectors.toList())) {

            //only check resources for which a checker was registered.
            // todo: maybe error out processes with uncheckable resources??
            List<ProvisionedResource> resources = process.getProvisionedResourceSet().getResources().stream().filter(this::hasChecker).collect(Collectors.toList());

            //todo: comment this in if we want to error out uncheckable resources
//            var resourcesWithNoChecker = resources.stream().filter(resource -> statusCheckerRegistry.resolve(resource) == null).collect(Collectors.toList());
//            if (!resourcesWithNoChecker.isEmpty()) {
//                final String violatingResourceClasses = resourcesWithNoChecker.stream().map(r -> r.getClass().getName()).collect(Collectors.joining(", "));
//                monitor.severe("There is no StatusChecker for resource type " + violatingResourceClasses);
//                process.transitionError("No StatusChecker found for a provisioned resource of type(s) " + violatingResourceClasses + ". The violating resource is part of transfer process " + process.getId());
//                transferProcessStore.update(process);
//                continue;
//            }

            // update the process once ALL resources are completed
            if (resources.stream().allMatch(this::isComplete)) {
                process.transitionCompleted();
                monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.COMPLETED);
            }
            transferProcessStore.update(process);
        }
        return processesInProgress.size();
    }

    private boolean hasChecker(ProvisionedResource provisionedResource) {
        return provisionedResource instanceof ProvisionedDataDestinationResource && statusCheckerRegistry.resolve((ProvisionedDataDestinationResource) provisionedResource) != null;
    }

    private boolean isComplete(ProvisionedResource resource) {
        if (!(resource instanceof ProvisionedDataDestinationResource)) {
            return false;
        }
        ProvisionedDataDestinationResource dataResource = (ProvisionedDataDestinationResource) resource;
        var checker = statusCheckerRegistry.resolve(dataResource);
        if (checker == null) {
            return true;
        }
        return checker.isComplete(dataResource);
    }

    /**
     * Performs client-side or provider side provisioning for a service.
     * <p>
     * On a client, provisioning may entail setting up a data destination and supporting infrastructure. On a provider, provisioning is initiated when a request is received and
     * map involve preprocessing data or other operations.
     */
    private int provisionInitialProcesses() {
        List<TransferProcess> processes = transferProcessStore.nextForState(INITIAL.code(), batchSize);
        for (TransferProcess process : processes) {
            DataRequest dataRequest = process.getDataRequest();
            ResourceManifest manifest;
            if (process.getType() == CLIENT) {
                // if resources are managed by this connector, generate the manifest; otherwise create an empty one
                manifest = dataRequest.isManagedResources() ? manifestGenerator.generateClientManifest(process) : ResourceManifest.Builder.newInstance().build();
            } else {
                manifest = manifestGenerator.generateProviderManifest(process);
            }
            process.transitionProvisioning(manifest);
            transferProcessStore.update(process);
            provisionManager.provision(process);
        }
        return processes.size();
    }

    /**
     * On a client, sends provisioned requests to the provider connector. On the provider, sends provisioned requests to the data flow manager.
     *
     * @return the number of requests processed
     */
    private int sendOrProcessProvisionedRequests() {
        List<TransferProcess> processes = transferProcessStore.nextForState(PROVISIONED.code(), batchSize);
        for (TransferProcess process : processes) {
            DataRequest dataRequest = process.getDataRequest();
            if (CLIENT == process.getType()) {
                process.transitionRequested();
                transferProcessStore.update(process);   // update before sending to accommodate synchronous transports; reliability will be managed by retry and idempotency
                dispatcherRegistry.send(Void.class, dataRequest, process::getId);
            } else {
                var response = dataFlowManager.initiate(dataRequest);
                if (ResponseStatus.ERROR_RETRY == response.getStatus()) {
                    monitor.severe("Error processing transfer request. Setting to retry: " + process.getId());
                    process.transitionProvisioned();
                } else if (ResponseStatus.FATAL_ERROR == response.getStatus()) {
                    monitor.severe(format("Fatal error processing transfer request: %s. Error details: %s", process.getId(), response.getError()));
                    process.transitionError(response.getError());
                } else {
                    if (process.getDataRequest().getTransferType().isFinite()) {
                        process.transitionInProgress();
                    } else {
                        process.transitionStreaming();
                    }
                }
            }
            transferProcessStore.update(process);
        }
        return processes.size();
    }

    public static class Builder {
        private final TransferProcessManagerImpl manager;

        private Builder() {
            manager = new TransferProcessManagerImpl();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder batchSize(int size) {
            manager.batchSize = size;
            return this;
        }

        public Builder waitStrategy(TransferWaitStrategy waitStrategy) {
            manager.waitStrategy = waitStrategy;
            return this;
        }

        public Builder manifestGenerator(ResourceManifestGenerator manifestGenerator) {
            manager.manifestGenerator = manifestGenerator;
            return this;
        }

        public Builder provisionManager(ProvisionManager provisionManager) {
            manager.provisionManager = provisionManager;
            return this;
        }

        public Builder dataFlowManager(DataFlowManager dataFlowManager) {
            manager.dataFlowManager = dataFlowManager;
            return this;
        }

        public Builder dispatcherRegistry(RemoteMessageDispatcherRegistry registry) {
            manager.dispatcherRegistry = registry;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            manager.monitor = monitor;
            return this;
        }

        public Builder statusCheckerRegistry(StatusCheckerRegistry statusCheckerRegistry) {
            manager.statusCheckerRegistry = statusCheckerRegistry;
            return this;
        }

        public TransferProcessManagerImpl build() {
            Objects.requireNonNull(manager.manifestGenerator, "manifestGenerator");
            Objects.requireNonNull(manager.provisionManager, "provisionManager");
            Objects.requireNonNull(manager.dataFlowManager, "dataFlowManager");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry");
            Objects.requireNonNull(manager.monitor, "monitor");
            Objects.requireNonNull(manager.statusCheckerRegistry, "StatusCheckerRegistry cannot be null!");
            return manager;
        }
    }
}