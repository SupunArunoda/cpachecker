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
package org.sosy_lab.cpachecker.cpa.composite;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.ProofChecker;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableList;

public class CompositeStopOperator implements StopOperator, ForcedCoveringStopOperator {

  protected final ImmutableList<StopOperator> stopOperators;

  public CompositeStopOperator(ImmutableList<StopOperator> stopOperators)
  {
    this.stopOperators = stopOperators;
  }

  @Override
  public boolean stop(AbstractState element, Collection<AbstractState> reached, Precision precision) throws CPAException {
    CompositeElement compositeElement = (CompositeElement) element;
    CompositePrecision compositePrecision = (CompositePrecision) precision;

    for (AbstractState e : reached) {
      if (stop(compositeElement, (CompositeElement)e, compositePrecision)) {
        return true;
      }
    }
    return false;
  }

  private boolean stop(CompositeElement compositeElement, CompositeElement compositeReachedElement, CompositePrecision compositePrecision) throws CPAException {
    List<AbstractState> compositeElements = compositeElement.getWrappedElements();
    List<AbstractState> compositeReachedElements = compositeReachedElement.getWrappedElements();

    List<Precision> compositePrecisions = compositePrecision.getPrecisions();

    for (int idx = 0; idx < compositeElements.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);

      AbstractState absElem1 = compositeElements.get(idx);
      AbstractState absElem2 = compositeReachedElements.get(idx);
      Precision prec = compositePrecisions.get(idx);

      if (!stopOp.stop(absElem1, Collections.singleton(absElem2), prec)){
        return false;
      }
    }
    return true;
  }

  boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement, List<ConfigurableProgramAnalysis> cpas) throws CPAException {
    CompositeElement compositeElement = (CompositeElement)pElement;
    CompositeElement compositeOtherElement = (CompositeElement)pOtherElement;

    List<AbstractState> componentElements = compositeElement.getWrappedElements();
    List<AbstractState> componentOtherElements = compositeOtherElement.getWrappedElements();

    if(componentElements.size() != cpas.size()) {
      return false;
    }

    for (int idx = 0; idx < componentElements.size(); idx++) {
      ProofChecker componentProofChecker = (ProofChecker)cpas.get(idx);

      AbstractState absElem1 = componentElements.get(idx);
      AbstractState absElem2 = componentOtherElements.get(idx);

      if (!componentProofChecker.isCoveredBy(absElem1, absElem2)){
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean isForcedCoveringPossible(AbstractState pElement, AbstractState pReachedElement, Precision pPrecision) throws CPAException {

    CompositeElement compositeElement = (CompositeElement)pElement;
    CompositeElement compositeReachedElement = (CompositeElement)pReachedElement;
    CompositePrecision compositePrecision = (CompositePrecision)pPrecision;

    List<AbstractState> compositeElements = compositeElement.getWrappedElements();
    List<AbstractState> compositeReachedElements = compositeReachedElement.getWrappedElements();
    List<Precision> compositePrecisions = compositePrecision.getPrecisions();

    for (int idx = 0; idx < compositeElements.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);

      AbstractState wrappedElement = compositeElements.get(idx);
      AbstractState wrappedReachedElement = compositeReachedElements.get(idx);
      Precision prec = compositePrecisions.get(idx);

      boolean possible;
      if (stopOp instanceof ForcedCoveringStopOperator) {

        possible = ((ForcedCoveringStopOperator)stopOp).isForcedCoveringPossible(wrappedElement, wrappedReachedElement, prec);

      } else {
        possible = stopOp.stop(wrappedElement, Collections.singleton(wrappedReachedElement), prec);
      }

      if (!possible) {
        return false;
      }
    }

    return true;
  }
}
