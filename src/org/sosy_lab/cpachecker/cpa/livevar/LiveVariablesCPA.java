/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.livevar;

import java.util.Collections;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

@Options
public class LiveVariablesCPA implements ConfigurableProgramAnalysis {

  @Option(secure=true, name = "merge", toUppercase = true, values = { "SEP", "JOIN" },
      description = "which merge operator to use for LiveVariablesCPA")
  private String mergeType = "JOIN";

  @Option(secure=true, name = "stop", toUppercase = true, values = { "SEP", "JOIN", "NEVER" },
      description = "which stop operator to use for LiveVariablesCPA")
  private String stopType = "SEP";

  private final AbstractDomain domain;
  private final LiveVariablesTransferRelation transfer;
  private final MergeOperator merge;
  private final StopOperator stop;
  private final LogManager logger;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(LiveVariablesCPA.class);
  }

  private LiveVariablesCPA(final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    pConfig.inject(this, LiveVariablesCPA.class);
    logger = pLogger;
    domain = DelegateAbstractDomain.<LiveVariablesState>getInstance();
    transfer = new LiveVariablesTransferRelation();

    if (mergeType.equals("SEP")) {
      merge = MergeSepOperator.getInstance();
    } else {
      merge = new MergeJoinOperator(domain);
    }

    if (stopType.equals("JOIN")) {
      stop = new StopJoinOperator(domain);
    } else {
      stop = new StopSepOperator(domain);
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    if (pNode instanceof FunctionExitNode) {
      FunctionExitNode eNode = (FunctionExitNode) pNode;
      Type type = eNode.getEntryNode().getFunctionDefinition().getType().getReturnType();

      // p.e. a function void foo();
      if (type instanceof CVoidType) {
        return new LiveVariablesState();

      // all other function types
      } else {
        String functionReturnVariable = VariableClassification.createFunctionReturnVariable(eNode.getFunctionName());
        transfer.putInitialLiveVariables(pNode, Collections.singleton(functionReturnVariable));
        return new LiveVariablesState(ImmutableSet.of(functionReturnVariable));
      }

    } else {
      logger.log(Level.WARNING, "No FunctionExitNode given, thus creating initial state without having the return variable.");
      return new LiveVariablesState();
    }
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
  }

  /**
   * Returns the liveVariables that are currently computed. Calling this method
   * makes only sense if the analysis was completed
   * @return a Multimap containing the variables that are live at each location
   */
  public Multimap<CFANode, String> getLiveVariables() {
    return transfer.getLiveVariables();
  }

}
