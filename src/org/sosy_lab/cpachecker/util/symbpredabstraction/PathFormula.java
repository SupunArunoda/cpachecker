/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.symbpredabstraction;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;

public class PathFormula {

  private final SymbolicFormula formula;
  private final SSAMap ssa;
  private final int length;
  
  // this formula contains information about the branches we took so we can
  // single out feasible paths
  private final SymbolicFormula reachingPathsFormula;
  
  // number of branching locations we saw on the paths (used for reachingPathsFormula)
  private final int branchingCounter; 
  
  protected PathFormula(SymbolicFormula pf, SSAMap ssa, int pLength,
        SymbolicFormula pReachingPathsFormula, int pBranchingCounter) {
    this.formula = pf;
    this.ssa = ssa;    
    this.length = pLength;
    this.reachingPathsFormula = pReachingPathsFormula;
    this.branchingCounter = pBranchingCounter;
  }

  public SymbolicFormula getSymbolicFormula() {
    return formula;
  }

  public SSAMap getSsa() {
    return ssa;
  }
  
  public int getLength() {
    return length;
  }

  public SymbolicFormula getReachingPathsFormula() {
    return reachingPathsFormula;
  }
  
  public int getBranchingCounter() {
    return branchingCounter;
  }
  
  @Override
  public String toString(){
    return getSymbolicFormula().toString();
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof PathFormula)
      && formula.equals(((PathFormula)other).formula)
      && ssa.equals(((PathFormula)other).ssa)
      && reachingPathsFormula.equals(((PathFormula)other).reachingPathsFormula)
      && branchingCounter == ((PathFormula)other).branchingCounter;
  }
  
  @Override
  public int hashCode() {
    return (formula.hashCode() * 17 + ssa.hashCode()) * 17 + branchingCounter;
  }
}