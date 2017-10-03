/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.scopebounded;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class ScopeBoundedTransferRelation implements TransferRelation {

  private final ScopeBoundedCPA cpa;
  private final LogManager logger;
  private final TransferRelation wrappedTransfer;
  private final ShutdownNotifier shutdownNotifier;

  ScopeBoundedTransferRelation(
      final ScopeBoundedCPA pCPA, final ShutdownNotifier pShutdownNotifier) {
    cpa = pCPA;
    logger = pCPA.getLogger();
    wrappedTransfer = pCPA.getWrappedCpa().getTransferRelation();
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      final AbstractState pState, final Precision pPrecision)
      throws InterruptedException, CPATransferException {

    shutdownNotifier.shutdownIfNecessary();
    final CFANode node = extractLocation(pState);
    final boolean shouldProceed;
    if (node instanceof FunctionEntryNode) {
      final String name = ((FunctionEntryNode) node).getFunctionName();
      final boolean isStub = cpa.isStub(name);
      final boolean hasStub = cpa.hasStub(name);
      shouldProceed =
          (!isStub && (!hasStub || ScopeBoundedPrecision.shouldUnroll(name)))
              || (isStub && !ScopeBoundedPrecision.shouldUnroll(cpa.originalName(name)));
      if (hasStub) {
        logger.log(
            Level.FINER, "Will " + (shouldProceed ? "unroll" : "skip") + " function " + name);
      }
    } else {
      shouldProceed = true;
    }
    if (shouldProceed) {
      return wrappedTransfer.getAbstractSuccessors(pState, pPrecision);
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      final AbstractState pState,
      final List<AbstractState> pOtherStates,
      final CFAEdge pCfaEdge,
      final Precision pPrecision)
      throws CPATransferException, InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    return wrappedTransfer.strengthen(pState, pOtherStates, pCfaEdge, pPrecision);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      final AbstractState pState, final Precision pPrecision, final CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException(
        "ScopeBoundedCPA needs to be used as the outermost CPA,"
            + " thus it does not support returning successors for a single edge.");
  }
}