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
package org.sosy_lab.cpachecker.core.algorithm.parallel_bam;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.parallel_bam.ParallelBAMAlgorithm.ParallelBAMStatistics;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPAWithoutReachedSetCreation;
import org.sosy_lab.cpachecker.cpa.bam.BlockSummaryMissingException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * A wrapper for a single reached-set and the corresponding data-structures. We assume that each
 * reachedset is contained in only one ReachedSetExecutor and that each instance of
 * ReachedSetExecutor is only executed by a single thread, because this guarantees us
 * single-threaded access to the reached-sets.
 */
class ReachedSetExecutor {

  private static final Level level = Level.ALL;
  private static final Runnable NOOP = () -> {};

  /** the working reached-set, single-threaded access. */
  private final ReachedSet rs;

  /** the working algorithm for the reached-set, single-threaded access. */
  private final CPAAlgorithm algorithm;

  /** flag that causes termination if enabled. */
  private boolean targetStateFound = false;

  /** main reached-set is used for checking termination of the algorithm. */
  private final ReachedSet mainReachedSet;

  /**
   * important central data structure, shared over all threads, need to be synchronized directly.
   */
  private final Map<ReachedSet, Pair<ReachedSetExecutor, CompletableFuture<Void>>>
      reachedSetMapping;

  private final ExecutorService pool;

  private final BAMCPAWithoutReachedSetCreation bamcpa;
  private final CPAAlgorithmFactory algorithmFactory;
  private final ShutdownNotifier shutdownNotifier;
  private final ParallelBAMStatistics stats;
  private final AtomicReference<Throwable> error;
  private final LogManager logger;

  int execCounter = 0; // statistics

  /**
   * Sub-reached-sets have to be finished before the current one. The state is unique. Synchronized
   * access needed!
   */
  private final Set<AbstractState> dependsOn = new LinkedHashSet<>();

  /**
   * The current reached-set has to be finished before parent reached-set. The state is unique, RSE
   * is not. Synchronized access needed!
   */
  private final Multimap<ReachedSetExecutor, AbstractState> dependingFrom =
      LinkedHashMultimap.create();

  public ReachedSetExecutor(
      BAMCPAWithoutReachedSetCreation pBamCpa,
      ReachedSet pRs,
      ReachedSet pMainReachedSet,
      Map<ReachedSet, Pair<ReachedSetExecutor, CompletableFuture<Void>>> pReachedSetMapping,
      ExecutorService pPool,
      CPAAlgorithmFactory pAlgorithmFactory,
      ShutdownNotifier pShutdownNotifier,
      ParallelBAMStatistics pStats,
      AtomicReference<Throwable> pError,
      LogManager pLogger) {
    bamcpa = pBamCpa;
    rs = pRs;
    mainReachedSet = pMainReachedSet;
    reachedSetMapping = pReachedSetMapping;
    pool = pPool;
    algorithmFactory = pAlgorithmFactory;
    shutdownNotifier = pShutdownNotifier;
    stats = pStats;
    error = pError;
    logger = pLogger;

    algorithm = algorithmFactory.newInstance();

    logger.logf(level, "%s :: creating RSE", this);
  }

  public Runnable asRunnable() {
    return asRunnable(ImmutableSet.of());
  }

  public Runnable asRunnable(Collection<AbstractState> pStatesToBeAdded) {
    // copy needed, because access to pStatesToBeAdded is done in the future
    ImmutableSet<AbstractState> copy = ImmutableSet.copyOf(pStatesToBeAdded);
    return () -> apply(copy);
  }

  /** Wrapper-method around {@link #apply0} for handling errors. */
  private void apply(Collection<AbstractState> pStatesToBeAdded) {
    int running = stats.numActiveThreads.incrementAndGet();
    stats.histActiveThreads.insertValue(running);
    stats.numMaxRSE.accumulate(reachedSetMapping.size());
    execCounter++;

    try {

      if (shutdownNotifier.shouldShutdown()) {
        pool.shutdownNow();
        return;
      }

      try {
        apply0(pStatesToBeAdded);

      } catch (CPAException | InterruptedException e) {
        logger.logException(level, e, e.getClass().getName());
      }

    } finally {
      stats.numActiveThreads.decrementAndGet();
    }
  }

  /**
   * This method should be synchronized by design of the algorithm. There exists a mapping of
   * ReachedSet to ReachedSetExecutor that guarantees single-threaded access to each ReachedSet.
   */
  private void apply0(Collection<AbstractState> pStatesToBeAdded)
      throws CPAException, InterruptedException {
    logger.logf(level, "%s :: RSE.run starting", this);

    updateStates(pStatesToBeAdded);
    analyzeReachedSet();

    logger.logf(level, "%s :: RSE.run exiting", this);
  }

  private static String id(final AbstractState state) {
    return ((ARGState) state).getStateId() + "@" + AbstractStates.extractLocation(state);
  }

  private static String id(ReachedSet pRs) {
    return id(pRs.getFirstState());
  }

  private String idd() {
    return id(rs);
  }

  /**
   * This method re-adds states to the waitlist. The states were removed due to missing blocks, and
   * we re-add them when the missing block is finished. The states are at block-start locations.
   */
  private void updateStates(Collection<AbstractState> pStatesToBeAdded) {
    for (AbstractState state : pStatesToBeAdded) {
      rs.reAddToWaitlist(state);
      dependsOn.remove(state);
    }
  }

  private void analyzeReachedSet() throws CPAException, InterruptedException {
    try {
      @SuppressWarnings("unused")
      AlgorithmStatus tmpStatus = algorithm.run(rs);
      handleTermination();

    } catch (BlockSummaryMissingException bsme) {
      handleMissingBlock(bsme);
    }
  }

  private boolean isFinished() {
    return !rs.hasWaitingState() && dependsOn.isEmpty();
  }

  private boolean endsWithTargetState() {
    return rs.getLastState() != null && AbstractStates.isTargetState(rs.getLastState());
  }

  boolean isTargetStateFound() {
    return targetStateFound;
  }

  private void handleTermination() {
    logger.logf(level, "%s :: RSE.handleTermination starting", this);

    boolean isFinished = isFinished();
    boolean endsWithTargetState = endsWithTargetState();

    if (endsWithTargetState) {
      targetStateFound = true;
    }

    if (isFinished || endsWithTargetState) {
      logger.logf(
          level,
          "%s :: finished=%s, endsWithTargetState=%s",
          this,
          isFinished,
          endsWithTargetState);

      updateCache(endsWithTargetState);
      reAddStatesToDependingReachedSets();

      if (rs == mainReachedSet) {
        logger.logf(level, "%s :: mainRS finished, shutdown threadpool", this);
        pool.shutdown();
      }

      // we never need to execute this RSE again,
      // thus we can clean up and avoid a (small) memory-leak
      synchronized (reachedSetMapping) {
        Pair<ReachedSetExecutor, CompletableFuture<Void>> p = reachedSetMapping.remove(rs);
        stats.executionCounter.insertValue(execCounter);
        // no need to wait for p.getSecond(), we assume a error-free exit after this point.
      }
    }

    logger.logf(level, "%s :: RSE.handleTermination exiting", this);
  }

  private void updateCache(boolean pEndsWithTargetState) {
    if (rs == mainReachedSet) {
      // we do not cache main reached set, because it should not be used internally
      return;
    }

    AbstractState reducedInitialState = rs.getFirstState();
    Precision reducedInitialPrecision = rs.getPrecision(reducedInitialState);
    Block block = getBlockForState(reducedInitialState);
    final Collection<AbstractState> exitStates = extractExitStates(pEndsWithTargetState, block);
    Pair<ReachedSet, Collection<AbstractState>> check =
        bamcpa.getCache().get(reducedInitialState, reducedInitialPrecision, block);
    assert check.getFirst() == rs
        : String.format(
            "reached-set for initial state should be unique: current rs = %s, cached entry = %s",
            id(rs), check.getFirst());
    if (!exitStates.equals(check.getSecond())) {
      assert check.getSecond() == null
          : String.format(
              "result-states already registered for reached-set %s: current = %s, cached = %s",
              id(rs),
              Collections2.transform(exitStates, s -> id(s)),
              Collections2.transform(check.getSecond(), s -> id(s)));
      bamcpa.getCache().put(reducedInitialState, reducedInitialPrecision, block, exitStates, null);
    }
  }

  private Collection<AbstractState> extractExitStates(boolean pEndsWithTargetState, Block pBlock) {
    if (pEndsWithTargetState) {
      assert AbstractStates.isTargetState(rs.getLastState());
      return Collections.singletonList(rs.getLastState());
    } else {
      return AbstractStates.filterLocations(rs, pBlock.getReturnNodes())
          .filter(s -> ((ARGState) s).getChildren().isEmpty())
          .toList();
    }
  }

  private void reAddStatesToDependingReachedSets() {
    synchronized (dependingFrom) {
      for (Entry<ReachedSetExecutor, Collection<AbstractState>> parent :
          dependingFrom.asMap().entrySet()) {
        registerJob(parent.getKey(), parent.getKey().asRunnable(parent.getValue()));
      }
      dependingFrom.clear();
    }
  }

  private void addDependencies(
      BlockSummaryMissingException pBsme, final ReachedSetExecutor subRse) {
    dependsOn.add(pBsme.getState());
    synchronized (subRse.dependingFrom) {
      subRse.dependingFrom.put(this, pBsme.getState());
    }
  }

  private void handleMissingBlock(BlockSummaryMissingException pBsme) {
    logger.logf(level, "%s :: RSE.handleMissingBlock starting", this);

    if (targetStateFound) {
      // ignore further sub-analyses
    }

    // register new sub-analysis as asynchronous/parallel/future work, if not existent
    synchronized (reachedSetMapping) {
      ReachedSet newRs = createAndRegisterNewReachedSet(pBsme);
      Pair<ReachedSetExecutor, CompletableFuture<Void>> p = reachedSetMapping.get(newRs);

      if (p == null) {
        // BSME interleaved with termination of sub-reached-set analysis,
        // cache-update was too slow, but cleanup of RSE in reachedSetMapping was too fast.
        // Restarting the procedure once should be sufficient,
        // such that the analysis tries a normal cache-access again.
        // --> nothing to do

      } else {
        // remove current state from waitlist to avoid exploration until all sub-blocks are done.
        // The state was removed for exploration,
        // but re-added by CPA-algorithm when throwing the exception
        assert rs.contains(pBsme.getState()) : "parent reachedset must contain entry state";
        rs.removeOnlyFromWaitlist(pBsme.getState());

        // register dependencies to wait for results and to get results, asynchronous
        ReachedSetExecutor subRse = p.getFirst();
        addDependencies(pBsme, subRse);
        logger.logf(level, "%s :: RSE.handleMissingBlock %s -> %s", this, this, id(newRs));

        // register callback to get results of terminated analysis
        registerJob(subRse, subRse.asRunnable());
      }
    }

    // register current RSE for further analysis
    registerJob(this, this.asRunnable());

    logger.logf(level, "%s :: RSE.handleMissingBlock exiting", this);
  }

  /**
   * Get the reached-set for the missing block's analysis. If we already have a valid reached-set,
   * we return it. If the reached-set was missing when throwing the exception, we check the cache
   * again. If the reached-set is missing, we create a new reached-set and the ReachedSetExecutor.
   *
   * @return a valid reached-set to be analyzed
   */
  private ReachedSet createAndRegisterNewReachedSet(BlockSummaryMissingException pBsme) {
    ReachedSet newRs = pBsme.getReachedSet();
    if (newRs == null) {
      // We are only synchronized in the current method. Thus, we need to check
      // the cache again, maybe another thread already created the needed reached-set.
      final Pair<ReachedSet, Collection<AbstractState>> pair =
          bamcpa
              .getCache()
              .get(pBsme.getReducedState(), pBsme.getReducedPrecision(), pBsme.getBlock());
      newRs = pair.getFirst(); // @Nullable
    }

    // now we can be sure, whether the sub-reached-set exists or not.
    if (newRs == null) {
      // we have not even cached a partly computed reached-set,
      // so we must compute the subgraph specification from scratch
      newRs =
          bamcpa
              .getData()
              .createAndRegisterNewReachedSet(
                  pBsme.getReducedState(), pBsme.getReducedPrecision(), pBsme.getBlock());

      // we have not even cached a partly computed reach-set,
      // so we must compute the subgraph specification from scratch
      ReachedSetExecutor subRse =
          new ReachedSetExecutor(
              bamcpa,
              newRs,
              mainReachedSet,
              reachedSetMapping,
              pool,
              algorithmFactory,
              shutdownNotifier,
              stats,
              error,
              logger);
      // register NOOP here. Callback for results is registered later, we have "lazy" computation.
      CompletableFuture<Void> future =
          CompletableFuture.runAsync(NOOP, pool).exceptionally(new ExceptionHandler(subRse));
      reachedSetMapping.put(newRs, Pair.of(subRse, future));
      logger.logf(level, "%s :: register subRSE %s", this, id(newRs));
    }
    return newRs;
  }

  /**
   * build a chain of jobs, append a new job after the last registered job for the given
   * reached-set.
   */
  private void registerJob(ReachedSetExecutor pRse, Runnable r) {
    synchronized (reachedSetMapping) {
      Pair<ReachedSetExecutor, CompletableFuture<Void>> p = reachedSetMapping.get(pRse.rs);
      assert p.getFirst() == pRse;
      CompletableFuture<Void> future =
          p.getSecond().thenRunAsync(r, pool).exceptionally(new ExceptionHandler(pRse));
      reachedSetMapping.put(pRse.rs, Pair.of(pRse, future));
    }
  }

  private Block getBlockForState(AbstractState state) {
    CFANode location = extractLocation(state);
    assert bamcpa.getBlockPartitioning().isCallNode(location)
        : "root of reached-set must be located at block entry.";
    return bamcpa.getBlockPartitioning().getBlockForCallNode(location);
  }

  @Override
  public String toString() {
    return "RSE " + idd();
  }

  class ExceptionHandler implements Function<Throwable, Void> {

    private final ReachedSetExecutor rse;

    public ExceptionHandler(ReachedSetExecutor pRse) {
      rse = pRse;
    }

    @Override
    public Void apply(Throwable e) {
      if (e instanceof RejectedExecutionException || e instanceof CompletionException) {
        // ignore, might happen when target-state is found
        // TODO cleanup waiting states and dependencies?
      } else {
        logger.logException(Level.WARNING, e, rse + " :: " + e.getClass().getSimpleName());
        error.compareAndSet(null, e);
      }
      return null;
    }
  }
}