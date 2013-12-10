
/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit.refiner.utils;

import static com.google.common.collect.Iterables.skip;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class ExplicitInterpolator {

  /**
   * the configuration of the interpolator
   */
  private Configuration config = null;

  private final ShutdownNotifier shutdownNotifier;

  /**
   * the transfer relation in use
   */
  private ExplicitTransferRelation transfer = null;

  /**
   * the precision in use
   */
  private ExplicitPrecision precision = null;

  /**
   * the first path element without any successors
   */
  public Integer conflictingOffset = null;

  public Integer currentOffset = null;

  /**
   * boolean flag telling whether the current path is feasible
   */
  private boolean isFeasible = false;

  /**
   * the number of interpolations
   */
  private int numberOfInterpolations = 0;

  /**
   * This method acts as the constructor of the class.
   */
  public ExplicitInterpolator(final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa) throws CPAException {
    shutdownNotifier = pShutdownNotifier;
    try {
      config      = Configuration.builder().build();
      transfer    = new ExplicitTransferRelation(config, pLogger, pCfa);
      precision   = new ExplicitPrecision("", config, Optional.<VariableClassification>absent());
    }
    catch (InvalidConfigurationException e) {
      throw new CounterexampleAnalysisFailed("Invalid configuration for checking path: " + e.getMessage(), e);
    }
  }

  /**
   * This method derives an interpolant for the given error path and interpolation state.
   *
   * @param errorPath the path to check
   * @param offset offset of the state at where to start the current interpolation
   * @param inputInterpolant the input interpolant
   * @throws CPAException
   * @throws InterruptedException
   */
  public Set<Pair<MemoryLocation, Long>> deriveInterpolant(
      List<CFAEdge> errorPath,
      int offset,
      Map<MemoryLocation, Long> inputInterpolant) throws CPAException, InterruptedException {
    numberOfInterpolations = 0;

    currentOffset = offset;

    // cancel the interpolation if we are interpolating at the conflicting element
    if (conflictingOffset != null && currentOffset >= conflictingOffset) {
      return null;
    }

    // create initial state, based on input interpolant, and create initial successor by consuming the next edge
    ExplicitState initialState      = new ExplicitState(PathCopyingPersistentTreeMap.copyOf(inputInterpolant));
    ExplicitState initialSuccessor  = getInitialSuccessor(initialState, errorPath.get(offset));
    if (initialSuccessor == null) {
      return null;
    }

    // if the remaining path is infeasible by itself, i.e., contradicting by itself, skip interpolation
    if (initialSuccessor.getSize() > 1 && !isRemainingPathFeasible(skip(errorPath, offset + 1), new ExplicitState())) {
      return Collections.emptySet();
    }

    Set<Pair<MemoryLocation, Long>> interpolant = new HashSet<>();

    List<MemoryLocation> memoryLocations = Lists.newArrayList(initialSuccessor.getTrackedMemoryLocations());
    for (MemoryLocation currentMemoryLocation : memoryLocations) {
      shutdownNotifier.shutdownIfNecessary();
      ExplicitState successor = initialSuccessor.clone();

      // remove the value of the current and all already-found-to-be-irrelevant variables from the successor
      successor.forget(currentMemoryLocation);
      for (Pair<MemoryLocation, Long> interpolantVariable : interpolant) {
        if (interpolantVariable.getSecond() == null) {
          successor.forget(interpolantVariable.getFirst());
        }
      }

      // check if the remaining path now becomes feasible
      isFeasible = isRemainingPathFeasible(skip(errorPath, offset + 1), successor);

      if (isFeasible) {
        interpolant.add(Pair.of(currentMemoryLocation, initialSuccessor.getValueFor(currentMemoryLocation)));
      } else {
        interpolant.add(Pair.<MemoryLocation, Long>of(currentMemoryLocation, null));
      }
    }

    return interpolant;
  }

  /**
   * This method returns whether or not the last error path was feasible.
   *
   * @return whether or not the last error path was feasible
   */
  public boolean isFeasible() {
    return isFeasible;
  }

  /**
   * This method returns the number of performed interpolations.
   *
   * @return the number of performed interpolations
   */
  public int getNumberOfInterpolations() {
    return numberOfInterpolations;
  }

  /**
   * This method gets the initial successor, i.e. the state following the initial state.
   *
   * @param initialState the initial state, i.e. the state represented by the input interpolant.
   * @param initialEdge the initial edge of the error path
   * @return the initial successor
   * @throws CPATransferException
   */
  private ExplicitState getInitialSuccessor(ExplicitState initialState, CFAEdge initialEdge)
      throws CPATransferException {
    Collection<ExplicitState> successors = transfer.getAbstractSuccessors(
        initialState,
        precision,
        initialEdge);
    ExplicitState initialSuccessor = extractSuccessorState(successors);

    return initialSuccessor;
  }

  /**
   * This method checks, whether or not the (remaining) error path is feasible when starting with the given (pseudo) initial state.
   *
   * @param errorPath the error path to check feasibility on
   * @param initialState the (pseudo) initial state
   * @return true, it the path is feasible, else false
   * @throws CPATransferException
   */
  private boolean isRemainingPathFeasible(Iterable<CFAEdge> errorPath, ExplicitState initialState)
      throws CPATransferException {
    numberOfInterpolations++;

    List<CFAEdge> path = Lists.newArrayList(errorPath);
    for (int i = 0; i < path.size(); i++) {
      CFAEdge currentEdge = path.get(i);
      Collection<ExplicitState> successors = transfer.getAbstractSuccessors(
        initialState,
        precision,
        currentEdge);

      initialState = extractSuccessorState(successors);

      // there is no successor and the end of the path is not reached => error path is spurious
      if (initialState == null && currentEdge != Iterables.getLast(path)) {
        /* needed for sequences like ...
          ...
          status = 259;
          [status == 0] <- first conflictingElement
          ...
          [!(status >= 0)]
          ... as this would otherwise stop interpolation after first conflicting element,
          as the path to first conflicting element always is infeasible here
        */
        //if ((conflictingOffset == null) || (conflictingOffset <= i + currentOffset))

        if ((conflictingOffset == null) || (conflictingOffset <= i + currentOffset)) {
          conflictingOffset = i + currentOffset + 1;
        }
        return false;
      }
    }
    return true;
  }

  /**
   * This method extracts the single successor out of the (hopefully singleton) successor collection.
   *
   * @param successors the collection of successors
   * @return the successor, or null if none exists
   */
  private ExplicitState extractSuccessorState(Collection<ExplicitState> successors) {
    if (successors.isEmpty()) {
      return null;
    } else {
      assert (successors.size() == 1);
      return Lists.newArrayList(successors).get(0);
    }
  }
}