/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.engine.view.calcnode;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import javax.time.Instant;

import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.opengamma.engine.ComputationTargetSpecification;
import com.opengamma.engine.function.EmptyFunctionParameters;
import com.opengamma.engine.function.blacklist.FunctionBlacklistMaintainer;
import com.opengamma.engine.value.ValueProperties;
import com.opengamma.engine.value.ValuePropertyNames;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.engine.view.cache.CacheSelectHint;
import com.opengamma.engine.view.calc.JobIdSource;
import com.opengamma.engine.view.calcnode.StandardJob.WholeWatchedJob;
import com.opengamma.id.UniqueId;
import com.opengamma.util.tuple.Triple;

/**
 * Tests the implementation of watched jobs.
 */
@Test
public class WatchedJobTest {

  private final ComputationTargetSpecification TARGET = new ComputationTargetSpecification("Foo");
  private final ValueSpecification VS_A = new ValueSpecification("A", TARGET, ValueProperties.with(ValuePropertyNames.FUNCTION, "A").get());
  private final ValueSpecification VS_B = new ValueSpecification("B", TARGET, ValueProperties.with(ValuePropertyNames.FUNCTION, "B").get());
  private final ValueSpecification VS_C = new ValueSpecification("C", TARGET, ValueProperties.with(ValuePropertyNames.FUNCTION, "C").get());
  private final ValueSpecification VS_D = new ValueSpecification("D", TARGET, ValueProperties.with(ValuePropertyNames.FUNCTION, "D").get());
  private final CalculationJobItem JOB_ITEM_A = new CalculationJobItem("A", new EmptyFunctionParameters(), TARGET, Collections.<ValueSpecification>emptySet(), Arrays.asList(VS_A));
  private final CalculationJobItem JOB_ITEM_AB = new CalculationJobItem("B", new EmptyFunctionParameters(), TARGET, Arrays.asList(VS_A), Arrays.asList(VS_B));
  private final CalculationJobItem JOB_ITEM_BC = new CalculationJobItem("C", new EmptyFunctionParameters(), TARGET, Arrays.asList(VS_B), Arrays.asList(VS_C));
  private final CalculationJobItem JOB_ITEM_AC = new CalculationJobItem("C", new EmptyFunctionParameters(), TARGET, Arrays.asList(VS_A), Arrays.asList(VS_C));
  private final CalculationJobItem JOB_ITEM_BCD = new CalculationJobItem("D", new EmptyFunctionParameters(), TARGET, Arrays.asList(VS_B, VS_C), Arrays.asList(VS_D));

  private static CalculationJobSpecification createJobSpecification() {
    return new CalculationJobSpecification(UniqueId.of("Cycle", "1"), "Default", Instant.now(), JobIdSource.getId());
  }

  public void testStandardJob_createWatchedJob_singleItem() {
    final JobDispatcher dispatcher = new JobDispatcher();
    final FunctionBlacklistMaintainer blacklist = Mockito.mock(FunctionBlacklistMaintainer.class);
    dispatcher.setFunctionBlacklistMaintainer(blacklist);
    final CalculationJob job = new CalculationJob(createJobSpecification(), 0, null, Arrays.asList(JOB_ITEM_A), CacheSelectHint.allShared());
    final StandardJob standard = new StandardJob(dispatcher, job, Mockito.mock(JobResultReceiver.class));
    final WatchedJob watched = standard.createWatchedJob();
    assertNull(watched);
    Mockito.verify(blacklist).failedJobItem(JOB_ITEM_A);
  }

  public void testStandardJob_createWatchedJob_noTail() {
    final JobDispatcher dispatcher = new JobDispatcher();
    final CalculationJob job = new CalculationJob(createJobSpecification(), 0, null, Arrays.asList(JOB_ITEM_AB, JOB_ITEM_BC), CacheSelectHint.allShared());
    final StandardJob standard = new StandardJob(dispatcher, job, Mockito.mock(JobResultReceiver.class));
    final WatchedJob watched = standard.createWatchedJob();
    assertTrue(watched instanceof WatchedJob.Whole);
    watched.getJob().equals(job);
  }

  public void testStandardJob_createWatchedJob_rewrite() {
    final JobDispatcher dispatcher = new JobDispatcher();
    final CalculationJob job1 = new CalculationJob(createJobSpecification(), 0, null, Arrays.asList(JOB_ITEM_AB), CacheSelectHint.privateValues(Arrays.asList(VS_B)));
    final CalculationJob job2 = new CalculationJob(createJobSpecification(), 0, new long[] {job1.getSpecification().getJobId() }, Arrays.asList(JOB_ITEM_BC),
        CacheSelectHint.privateValues(Arrays.asList(VS_B)));
    job1.addTail(job2);
    final StandardJob standard = new StandardJob(dispatcher, job1, Mockito.mock(JobResultReceiver.class));
    final WatchedJob watched = standard.createWatchedJob();
    assertTrue(watched instanceof WholeWatchedJob);
    assertFalse(watched.getJob().getCacheSelectHint().isPrivateValue(VS_B));
    assertNull(watched.getJob().getTail());
  }

  public void testStandardJob_adjustCacheHints() {
    final CalculationJob job1 = new CalculationJob(createJobSpecification(), 0, null, Arrays.asList(JOB_ITEM_A, JOB_ITEM_AB), CacheSelectHint.allPrivate());
    final CalculationJob job2 = new CalculationJob(createJobSpecification(), 0, new long[] {job1.getSpecification().getJobId() }, Arrays.asList(JOB_ITEM_BC),
        CacheSelectHint.privateValues(Arrays.asList(VS_B)));
    job1.addTail(job2);
    final CalculationJob adj1 = StandardJob.adjustCacheHints(job1, new HashMap<ValueSpecification, Triple<CalculationJob, ? extends Set<ValueSpecification>, ? extends Set<ValueSpecification>>>());
    assertNotNull(adj1.getTail());
    final CalculationJob adj2 = adj1.getTail().iterator().next();
    assertEquals(adj1.getJobItems(), job1.getJobItems());
    assertEquals(adj2.getJobItems(), job2.getJobItems());
    assertTrue(adj1.getCacheSelectHint().isPrivateValue(VS_A));
    assertFalse(adj1.getCacheSelectHint().isPrivateValue(VS_B));
    assertFalse(adj2.getCacheSelectHint().isPrivateValue(VS_B));
    assertFalse(adj2.getCacheSelectHint().isPrivateValue(VS_C));
  }

  public void testStandardJob_WholeWatchedJob() {
    final CalculationJob job1 = new CalculationJob(createJobSpecification(), 0, null, Arrays.asList(JOB_ITEM_A), CacheSelectHint.allShared());
    final CalculationJob job2 = new CalculationJob(createJobSpecification(), 0, new long[] {job1.getSpecification().getJobId() },
        Arrays.asList(JOB_ITEM_AB), CacheSelectHint.allShared());
    job1.addTail(job2);
    final CalculationJob job3 = new CalculationJob(createJobSpecification(), 0, new long[] {job1.getSpecification().getJobId() },
        Arrays.asList(JOB_ITEM_AC), CacheSelectHint.allShared());
    job1.addTail(job3);
    final CalculationJob job4 = new CalculationJob(createJobSpecification(), 0, new long[] {job2.getSpecification().getJobId(), job3.getSpecification().getJobId() },
        Arrays.asList(JOB_ITEM_BCD), CacheSelectHint.allShared());
    job3.addTail(job4);
    final JobResultReceiver receiver = Mockito.mock(JobResultReceiver.class);
    final Queue<DispatchableJob> dispatched = new LinkedList<DispatchableJob>();
    final JobDispatcher dispatcher = new JobDispatcher() {
      @Override
      protected void dispatchJobImpl(final DispatchableJob job) {
        dispatched.add(job);
      }
    };
    final StandardJob parent = new StandardJob(dispatcher, job1, receiver);
    final WholeWatchedJob wjob1 = parent.createWholeWatchedJob(job1);
    final CalculationJobResult result1 = new CalculationJobResult(job1.getSpecification(), 0, Arrays.asList(CalculationJobResultItem.success()), "Test");
    wjob1.getResultReceiver(result1).resultReceived(result1);
    Mockito.verify(receiver).resultReceived(result1);
    Mockito.reset();
    assertEquals(dispatched.size(), 2);
    final WholeWatchedJob wjob2 = (WholeWatchedJob) dispatched.poll();
    final WholeWatchedJob wjob3 = (WholeWatchedJob) dispatched.poll();
    final CalculationJobResult result2 = new CalculationJobResult(job2.getSpecification(), 0, Arrays.asList(CalculationJobResultItem.success()), "Test");
    final CalculationJobResult result3 = new CalculationJobResult(job3.getSpecification(), 0, Arrays.asList(CalculationJobResultItem.success()), "Test");
    wjob3.getResultReceiver(result3).resultReceived(result3);
    Mockito.verify(receiver).resultReceived(result3);
    Mockito.reset();
    assertEquals(dispatched.size(), 0);
    wjob2.getResultReceiver(result2).resultReceived(result2);
    Mockito.verify(receiver).resultReceived(result2);
    Mockito.reset();
    assertEquals(dispatched.size(), 1);
    final WholeWatchedJob wjob4 = (WholeWatchedJob) dispatched.poll();
    final CalculationJobResult result4 = new CalculationJobResult(job4.getSpecification(), 0, Arrays.asList(CalculationJobResultItem.success()), "Test");
    wjob4.getResultReceiver(result4).resultReceived(result4);
    Mockito.verify(receiver).resultReceived(result4);
    Mockito.reset();
  }

  public void testWatchedJob_prepareRetryJob_singleItem() {
    final JobDispatcher dispatcher = new JobDispatcher();
    final FunctionBlacklistMaintainer blacklist = Mockito.mock(FunctionBlacklistMaintainer.class);
    dispatcher.setFunctionBlacklistMaintainer(blacklist);
    final CalculationJob job = new CalculationJob(createJobSpecification(), 0, null, Arrays.asList(JOB_ITEM_A), CacheSelectHint.allShared());
    final JobResultReceiver receiver = Mockito.mock(JobResultReceiver.class);
    final StandardJob standard = new StandardJob(dispatcher, job, receiver);
    final WatchedJob watched = new WatchedJob.Whole(standard, job, receiver);
    watched.prepareRetryJob(null);
    Mockito.verify(blacklist).failedJobItem(JOB_ITEM_A);
  }

  public void testWatchedJob_prepareRetryJob_split_one() {
    final Queue<DispatchableJob> dispatched = new LinkedList<DispatchableJob>();
    final JobDispatcher dispatcher = new JobDispatcher() {
      @Override
      protected void dispatchJobImpl(final DispatchableJob job) {
        dispatched.add(job);
      }
    };
    final CalculationJob job = new CalculationJob(createJobSpecification(), 0, null, Arrays.asList(JOB_ITEM_A, JOB_ITEM_AB), CacheSelectHint.privateValues(Arrays.asList(VS_A)));
    final JobResultReceiver receiver = Mockito.mock(JobResultReceiver.class);
    final StandardJob standard = new StandardJob(dispatcher, job, receiver);
    final WatchedJob watched = new WatchedJob.Whole(standard, job, receiver);
    final DispatchableJob split = watched.prepareRetryJob(null);
    final CalculationJob job1 = split.getJob();
    assertEquals(job1.getJobItems(), Arrays.asList(JOB_ITEM_A));
    assertFalse(job1.getCacheSelectHint().isPrivateValue(VS_A));
    final CalculationJobResult result1 = new CalculationJobResult(job1.getSpecification(), 0, Arrays.asList(CalculationJobResultItem.success()), "Test");
    split.getResultReceiver(result1).resultReceived(result1);
    Mockito.verifyZeroInteractions(receiver);
    assertEquals(dispatched.size(), 1);
    final DispatchableJob next = dispatched.poll();
    final CalculationJob job2 = next.getJob();
    assertEquals(job2.getJobItems(), Arrays.asList(JOB_ITEM_AB));
    assertFalse(job2.getCacheSelectHint().isPrivateValue(VS_A));
    final CalculationJobResult result2 = new CalculationJobResult(job2.getSpecification(), 0, Arrays.asList(CalculationJobResultItem.failure("Foo", "Bar")), "Test");
    next.getResultReceiver(result2).resultReceived(result2);
    Mockito.verify(receiver).resultReceived(
        new CalculationJobResult(job.getSpecification(), 0, Arrays.asList(CalculationJobResultItem.success(), CalculationJobResultItem.failure("Foo", "Bar")), "Test"));
    assertTrue(job1.getSpecification().getJobId() != job2.getSpecification().getJobId());
    assertTrue(job2.getSpecification().getJobId() == job.getSpecification().getJobId());
  }

  public void testWatchedJob_prepareRetryJob_split_two() {
    final Queue<DispatchableJob> dispatched = new LinkedList<DispatchableJob>();
    final JobDispatcher dispatcher = new JobDispatcher() {
      @Override
      protected void dispatchJobImpl(final DispatchableJob job) {
        dispatched.add(job);
      }
    };
    final CalculationJob job = new CalculationJob(createJobSpecification(), 0, null, Arrays.asList(JOB_ITEM_A, JOB_ITEM_AB, JOB_ITEM_BC, JOB_ITEM_BCD), CacheSelectHint.sharedValues(Arrays
        .asList(VS_D)));
    final JobResultReceiver receiver = Mockito.mock(JobResultReceiver.class);
    final StandardJob standard = new StandardJob(dispatcher, job, receiver);
    final WatchedJob watched = new WatchedJob.Whole(standard, job, receiver);
    final DispatchableJob split = watched.prepareRetryJob(null);
    final CalculationJob job1 = split.getJob();
    assertEquals(job1.getJobItems(), Arrays.asList(JOB_ITEM_A, JOB_ITEM_AB));
    assertTrue(job1.getCacheSelectHint().isPrivateValue(VS_A));
    assertFalse(job1.getCacheSelectHint().isPrivateValue(VS_B));
    final CalculationJobResult result1 = new CalculationJobResult(job1.getSpecification(), 0, Arrays.asList(CalculationJobResultItem.success(), CalculationJobResultItem.success()), "1");
    split.getResultReceiver(result1).resultReceived(result1);
    Mockito.verifyZeroInteractions(receiver);
    assertEquals(dispatched.size(), 1);
    final DispatchableJob next = dispatched.poll();
    final CalculationJob job2 = next.getJob();
    assertEquals(job2.getJobItems(), Arrays.asList(JOB_ITEM_BC, JOB_ITEM_BCD));
    assertFalse(job2.getCacheSelectHint().isPrivateValue(VS_B));
    assertTrue(job2.getCacheSelectHint().isPrivateValue(VS_C));
    assertFalse(job2.getCacheSelectHint().isPrivateValue(VS_D));
    final CalculationJobResult result2 = new CalculationJobResult(job2.getSpecification(), 0, Arrays.asList(CalculationJobResultItem.failure("Foo", "Bar"), CalculationJobResultItem.success()), "2");
    next.getResultReceiver(result2).resultReceived(result2);
    Mockito.verify(receiver).resultReceived(
        new CalculationJobResult(job.getSpecification(), 0, Arrays.asList(CalculationJobResultItem.success(), CalculationJobResultItem.success(), CalculationJobResultItem.failure("Foo", "Bar"),
            CalculationJobResultItem.success()), "1, 2"));
    assertTrue(job1.getSpecification().getJobId() != job2.getSpecification().getJobId());
    assertTrue(job2.getSpecification().getJobId() == job.getSpecification().getJobId());
  }

}
