/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.  The ASF licenses this file to you under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package org.apache.storm.executor.bolt;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.storm.daemon.Acker;
import org.apache.storm.daemon.Task;
import org.apache.storm.executor.Executor;
import org.apache.storm.executor.ExecutorTransfer;
import org.apache.storm.hooks.info.BoltAckInfo;
import org.apache.storm.hooks.info.BoltFailInfo;
import org.apache.storm.serialization.KryoValuesDeserializer;
import org.apache.storm.serialization.KryoValuesSerializer;
import org.apache.storm.task.IOutputCollector;
import org.apache.storm.tuple.AddressedTuple;
import org.apache.storm.tuple.MessageId;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Time;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.anonymity.sgx.IntelSGX;
import edu.anonymity.sgx.IntelSGXOcall;
import edu.anonymity.sgx.Tools;
import edu.anonymity.sgx.Crypto;


public class BoltOutputCollectorImpl implements IOutputCollector {

    private static final Logger LOG = LoggerFactory.getLogger(BoltOutputCollectorImpl.class);

    private final BoltExecutor executor;
    private final Task task;
    private final int taskId;
    private final Random random;
    private final boolean isEventLoggers;
    private final ExecutorTransfer xsfer;
    private final boolean isDebug;
    private boolean ackingEnabled;
    public KryoValuesSerializer ky;

    public BoltOutputCollectorImpl(BoltExecutor executor, Task taskData, Random random,
                                   boolean isEventLoggers, boolean ackingEnabled, boolean isDebug) {
        this.executor = executor;
        this.task = taskData;
        this.taskId = taskData.getTaskId();
        this.random = random;
        this.isEventLoggers = isEventLoggers;
        this.ackingEnabled = ackingEnabled;
        this.isDebug = isDebug;
        this.xsfer = executor.getExecutorTransfer();
        this.ky = new KryoValuesSerializer(this.executor.getConf());
    }

    public byte[] serialize(List<Object> values){

        KryoValuesSerializer ky = new KryoValuesSerializer(this.executor.getConf());
        return ky.serialize(values);
    }
    public Object deserialize(byte[] data){
        KryoValuesDeserializer ky = new KryoValuesDeserializer(this.executor.getConf());
        return ky.deserialize(data);
    }



    @Override
    public List<Integer> emit(String streamId, Collection<Tuple> anchors, List<Object> tuple) {
        try {

            try {
                //Need to add crypto.sgx_encrypt
                byte[] encryptedData;
                if (!(streamId.contains("ack") || streamId.contains("metrics")) && tuple!=null)
                {
                    byte[] rawData =  ky.serialize(tuple);
                    //byte[] rawData =  new byte[1];
                    //encryptedData = rawData;
                    encryptedData = Crypto.sgx_encrypt(rawData, false);

                    return annotated_emit(
                            (String)Tools.deep_copy(streamId),
                            (Collection<Tuple>)Tools.deep_copy(anchors),
                            (List<Object>)Tools.deep_copy(tuple),
                            (byte[])Tools.deep_copy(encryptedData),
                            task,
                            ackingEnabled,
                            random,
                            executor,
                            taskId,
                            xsfer,
                            isEventLoggers
                    );


                }
                else {
                    return boltEmit(streamId, anchors, tuple, null);
                }

            }
            catch (UnsatisfiedLinkError ex){
                return boltEmit(streamId, anchors, tuple, null);
            }


        } catch (InterruptedException e) {
            LOG.warn("Thread interrupted when emiting tuple.");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void emitDirect(int taskId, String streamId, Collection<Tuple> anchors, List<Object> tuple) {
        try {
            boltEmit(streamId, anchors, tuple, taskId);
        } catch (InterruptedException e) {
            LOG.warn("Thread interrupted when emiting tuple.");
            throw new RuntimeException(e);
        }
    }



    @IntelSGXOcall
    public static List<Integer> annotated_emit(String streamId, Collection<Tuple> anchors, List<Object> values, byte[] encryptedData,Task task, boolean ackingEnabled, Random random, BoltExecutor executor, int taskId, ExecutorTransfer xsfer, boolean isEventLoggers) {
        //LOG.info("EEmitting TUPLE {} Encrypted value: {}", values, encryptedData);
        List<Integer> outTasks = task.getOutgoingTasks(streamId, values);

        for (int i = 0; i < outTasks.size(); ++i) {
            Integer t = outTasks.get(i);
            MessageId msgId;
            if (ackingEnabled && anchors != null) {
                final Map<Long, Long> anchorsToIds = new HashMap<>();
                for (Tuple a : anchors) {  // perf critical path. would be nice to avoid iterator allocation here and below
                    Set<Long> rootIds = a.getMessageId().getAnchorsToIds().keySet();
                    if (rootIds.size() > 0) {
                        long edgeId = MessageId.generateId(random);
                        ((TupleImpl) a).updateAckVal(edgeId);
                        for (Long rootId : rootIds) {
                            Long curr = anchorsToIds.get(rootId);
                            if (curr == null) {
                                curr = 0L;
                            }
                            anchorsToIds.put(rootId, Utils.bitXor(curr, edgeId));
                        }
                    }
                }
                msgId = MessageId.makeId(anchorsToIds);
            } else {
                msgId = MessageId.makeUnanchored();
            }

            List<Object> encryptedValues = new ArrayList<>();


            if (!(streamId.contains("ack") || streamId.contains("metrics")))
            {
                encryptedValues.add(encryptedData);
            }
            else {
                encryptedValues = values;
            }



            TupleImpl tupleExt = new TupleImpl(
                    executor.getWorkerTopologyContext(), encryptedValues, executor.getComponentId(), taskId, streamId, msgId);

            xsfer.tryTransfer(new AddressedTuple(t, tupleExt), executor.getPendingEmits());


        }
        if (isEventLoggers) {
            task.sendToEventLogger(executor, values, executor.getComponentId(), null, random, executor.getPendingEmits());
        }
        return outTasks;

    }


    private List<Integer> boltEmit(String streamId, Collection<Tuple> anchors, List<Object> values,
                                   Integer targetTaskId) throws InterruptedException {
        List<Integer> outTasks;
        if (targetTaskId != null) {
            outTasks = task.getOutgoingTasks(targetTaskId, streamId, values);
        } else {
            outTasks = task.getOutgoingTasks(streamId, values);
        }

        for (int i = 0; i < outTasks.size(); ++i) {
            Integer t = outTasks.get(i);
            MessageId msgId;
            if (ackingEnabled && anchors != null) {
                final Map<Long, Long> anchorsToIds = new HashMap<>();
                for (Tuple a : anchors) {  // perf critical path. would be nice to avoid iterator allocation here and below
                    Set<Long> rootIds = a.getMessageId().getAnchorsToIds().keySet();
                    if (rootIds.size() > 0) {
                        long edgeId = MessageId.generateId(random);
                        ((TupleImpl) a).updateAckVal(edgeId);
                        for (Long rootId : rootIds) {
                            putXor(anchorsToIds, rootId, edgeId);
                        }
                    }
                }
                msgId = MessageId.makeId(anchorsToIds);
            } else {
                msgId = MessageId.makeUnanchored();
            }
            TupleImpl tupleExt = new TupleImpl(
                    executor.getWorkerTopologyContext(), values, executor.getComponentId(), taskId, streamId, msgId);
            xsfer.tryTransfer(new AddressedTuple(t, tupleExt), executor.getPendingEmits());
        }
        if (isEventLoggers) {
            task.sendToEventLogger(executor, values, executor.getComponentId(), null, random, executor.getPendingEmits());
        }
        return outTasks;
    }




    @IntelSGXOcall
    public static void annotated_ack(boolean ackingEnabled, Tuple input, Task task, BoltExecutor executor, boolean isDebug, int taskId){
        if (!ackingEnabled) {
            return;
        }
        long ackValue = ((TupleImpl) input).getAckVal();
        Map<Long, Long> anchorsToIds = input.getMessageId().getAnchorsToIds();
        for (Map.Entry<Long, Long> entry : anchorsToIds.entrySet()) {
            task.sendUnanchored(Acker.ACKER_ACK_STREAM_ID,
                    new Values(entry.getKey(), Utils.bitXor(entry.getValue(), ackValue)),
                    executor.getExecutorTransfer(), executor.getPendingEmits());
        }
        long delta = tupleTimeDelta((TupleImpl) input);
        if (isDebug) {
            LOG.info("BOLT ack TASK: {} TIME: {} TUPLE: {}", taskId, delta, input);
        }

        if (!task.getUserContext().getHooks().isEmpty()) {
            BoltAckInfo boltAckInfo = new BoltAckInfo(input, taskId, delta);
            boltAckInfo.applyOn(task.getUserContext());
        }
        if (delta >= 0) {
            executor.getStats().boltAckedTuple(input.getSourceComponent(), input.getSourceStreamId(), delta,
                    task.getTaskMetrics().getAcked(input.getSourceStreamId()));
        }
    }

    @IntelSGXOcall
    public static void annotated_fail(boolean ackingEnabled, Tuple input, Task task, BoltExecutor executor, boolean isDebug, int taskId){

        if (!ackingEnabled) {
            return;
        }
        Set<Long> roots = input.getMessageId().getAnchors();
        for (Long root : roots) {
            task.sendUnanchored(Acker.ACKER_FAIL_STREAM_ID,
                    new Values(root), executor.getExecutorTransfer(), executor.getPendingEmits());
        }
        long delta = tupleTimeDelta((TupleImpl) input);
        if (isDebug) {
            LOG.info("BOLT fail TASK: {} TIME: {} TUPLE: {}", taskId, delta, input);
        }
        BoltFailInfo boltFailInfo = new BoltFailInfo(input, taskId, delta);
        boltFailInfo.applyOn(task.getUserContext());
        if (delta >= 0) {
            executor.getStats().boltFailedTuple(input.getSourceComponent(), input.getSourceStreamId(), delta,
                    task.getTaskMetrics().getFailed(input.getSourceStreamId()));
        }
    }




    @Override
    public void ack(Tuple input) {
        try {
            annotated_ack(
                    ackingEnabled,
                    (Tuple)Tools.deep_copy(input),
                    task,
                    executor,
                    isDebug,
                    taskId
            );
        }
        catch (UnsatisfiedLinkError ex){
            if (!ackingEnabled) {
                return;
            }
            long ackValue = ((TupleImpl) input).getAckVal();
            Map<Long, Long> anchorsToIds = input.getMessageId().getAnchorsToIds();
            for (Map.Entry<Long, Long> entry : anchorsToIds.entrySet()) {
                task.sendUnanchored(Acker.ACKER_ACK_STREAM_ID,
                        new Values(entry.getKey(), Utils.bitXor(entry.getValue(), ackValue)),
                        executor.getExecutorTransfer(), executor.getPendingEmits());
            }
            long delta = tupleTimeDelta((TupleImpl) input);
            if (isDebug) {
                LOG.info("BOLT ack TASK: {} TIME: {} TUPLE: {}", taskId, delta, input);
            }
            if (!task.getUserContext().getHooks().isEmpty()) {
                BoltAckInfo boltAckInfo = new BoltAckInfo(input, taskId, delta);
                boltAckInfo.applyOn(task.getUserContext());
            }
            if (delta >= 0) {
                executor.getStats().boltAckedTuple(input.getSourceComponent(), input.getSourceStreamId(), delta,
                        task.getTaskMetrics().getAcked(input.getSourceStreamId()));
            }
        }

    }


    @Override
    public void fail(Tuple input) {

        try {
            annotated_fail(
                    ackingEnabled,
                    (Tuple)Tools.deep_copy(input),
                    task,
                    executor,
                    isDebug,
                    taskId
            );
        }
        catch (UnsatisfiedLinkError ex){
            if (!ackingEnabled) {
                return;
            }
            Set<Long> roots = input.getMessageId().getAnchors();
            for (Long root : roots) {
                task.sendUnanchored(Acker.ACKER_FAIL_STREAM_ID,
                        new Values(root), executor.getExecutorTransfer(), executor.getPendingEmits());
            }
            long delta = tupleTimeDelta((TupleImpl) input);
            if (isDebug) {
                LOG.info("BOLT fail TASK: {} TIME: {} TUPLE: {}", taskId, delta, input);
            }
            BoltFailInfo boltFailInfo = new BoltFailInfo(input, taskId, delta);
            boltFailInfo.applyOn(task.getUserContext());
            if (delta >= 0) {
                executor.getStats().boltFailedTuple(input.getSourceComponent(), input.getSourceStreamId(), delta,
                        task.getTaskMetrics().getFailed(input.getSourceStreamId()));
            }
        }

    }

    @Override
    public void resetTimeout(Tuple input) {
        Set<Long> roots = input.getMessageId().getAnchors();
        for (Long root : roots) {
            task.sendUnanchored(Acker.ACKER_RESET_TIMEOUT_STREAM_ID, new Values(root),
                    executor.getExecutorTransfer(), executor.getPendingEmits());
        }
    }

    @Override
    public void flush() {
        try {
            xsfer.flush();
        } catch (InterruptedException e) {
            LOG.warn("Bolt thread interrupted during flush()");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportError(Throwable error) {
        executor.getErrorReportingMetrics().incrReportedErrorCount();
        executor.getReportError().report(error);
    }

    private static long tupleTimeDelta(TupleImpl tuple) {
        Long ms = tuple.getProcessSampleStartTime();
        if (ms != null) {
            return Time.deltaMs(ms);
        }
        return -1;
    }

    private void putXor(Map<Long, Long> pending, Long key, Long id) {
        Long curr = pending.get(key);
        if (curr == null) {
            curr = 0L;
        }
        pending.put(key, Utils.bitXor(curr, id));
    }
}