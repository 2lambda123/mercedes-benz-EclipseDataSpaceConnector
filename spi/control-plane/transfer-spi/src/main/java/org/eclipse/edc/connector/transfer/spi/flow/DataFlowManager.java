/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.spi.flow;

import java.util.Set;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

/**
 * Manages data flows and dispatches to {@link DataFlowController}s.
 * Priority is used to decide which controller should be chosen first, higher
 * priority values will make the controller being chosen first.
 */
@ExtensionPoint
public interface DataFlowManager {

  /**
   * Register the controller. The priority is set to 0.
   */
  void register(DataFlowController controller);

  /**
   * Register the controller with a specific priority.
   *
   * @param priority the priority.
   * @param controller the controller.
   */
  void register(int priority, DataFlowController controller);

  /**
   * Initiates a data flow.
   *
   * @param transferProcess the transfer process
   * @param policy          the contract agreement usage policy for the asset
   *     being transferred
   * @return succeeded StatusResult if flow has been initiated correctly, failed
   *     one otherwise.
   */
  @NotNull
  StatusResult<DataFlowResponse> initiate(TransferProcess transferProcess,
                                          Policy policy);

  /**
   * Terminates a data flow.
   *
   * @param transferProcess the transfer process.
   * @return success if the flow has been stopped correctly, failed otherwise.
   */
  @NotNull StatusResult<Void> terminate(TransferProcess transferProcess);

  /**
   * Returns the transfer types available for a specific asset.
   *
   * @param asset the asset.
   * @return tranfer types list.
   */
  Set<String> transferTypesFor(Asset asset);
}
