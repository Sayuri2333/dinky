/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.dinky.context;

import org.dinky.aop.ProcessAspect;
import org.dinky.data.enums.ProcessStatus;
import org.dinky.data.enums.ProcessStepType;
import org.dinky.data.enums.ProcessType;
import org.dinky.data.enums.SseTopic;
import org.dinky.data.enums.Status;
import org.dinky.data.exception.BusException;
import org.dinky.data.model.ProcessEntity;
import org.dinky.data.model.ProcessStepEntity;
import org.dinky.utils.LogUtil;

import org.apache.http.util.TextUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.MDC;

import com.alibaba.fastjson2.JSONObject;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.StrFormatter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleContextHolder {
    protected static final ConsoleContextHolder instance = new ConsoleContextHolder();

    /**
     * Get an instance of ConsoleContextHolder.
     *
     * @return ConsoleContextHolder instance
     */
    // 单例模式
    public static ConsoleContextHolder getInstances() {
        return instance;
    }

    // 用于记录目前所有线程中存放的ProcessEntity的Map
    private final Map<String, ProcessEntity> logPross = new ConcurrentHashMap<>();

    /**
     * Get a list of all processes
     */
    // 获取所有在跟踪的进程
    public List<ProcessEntity> list() {
        return new ArrayList<>(logPross.values());
    }

    // 获取指定的进程
    public ProcessEntity getProcess(String processName) {
        // 如果在map中存放了，就返回存放的
        if (logPross.containsKey(processName)) {
            return logPross.get(processName);
        }
        try {
            // 不然就去本地文件目录中找。存放的地点应该是$user.dir$/tmp/log/$processName$.json
            String filePath = String.format("%s/tmp/log/%s.json", System.getProperty("user.dir"), processName);
            // 读取文件中的内容为String类型
            String string = FileUtil.readString(filePath, StandardCharsets.UTF_8);
            // 把文本字符串转换为ProcessEntity对象
            return JSONObject.parseObject(string, ProcessEntity.class);
        } catch (Exception e) {
            log.warn("Get process {} failed, maybe not exits", processName);
            return null;
        }
    }

    /**
     * Add log messages to specific processes and process steps.
     * 向进程或者进程步中添加log信息
     *
     * @param processName process name
     * @param stepPid     process step type
     * @param logLine     messages
     * @throws BusException Throws an exception if the process does not exist
     */
    public void appendLog(String processName, String stepPid, String logLine, boolean recordGlobal) {
        // 从map中获取进程实例
        if (!logPross.containsKey(processName)) {
            log.debug("Process {} does not exist, This log was abandoned", processName);
            return;
        }
        ProcessEntity process = logPross.get(processName);
        // 如果是global，就向进程本身添加log
        if (recordGlobal) {
            process.appendLog(logLine);
        }
        // 如果提供了step id，那就向进程步中也添加log
        if (stepPid != null) {
            ProcessStepEntity stepNode = getStepNode(stepPid, getStepsMap(processName));
            if (stepNode != null) {
                // 找到的话就添加log，并更新最近更新时间
                stepNode.appendLog(logLine);
                process.setLastUpdateStep(stepNode);
            } else {
                log.error("process step not found {},{}", processName, stepPid);
            }
            process.setLastUpdateStep(stepNode);
        }
        //   /TOPIC/PROCESS_CONSOLE/FlinkSubmit/12
        String topic = StrFormatter.format("{}/{}", SseTopic.PROCESS_CONSOLE.getValue(), processName);
        // 异步发送
        CompletableFuture.runAsync(() -> {
            // 使用SseSessionContextHolder向前端发送消息
            SseSessionContextHolder.sendTopic(topic, process);
        });
    }

    /**
     * Register a new process.
     *
     * @param type        process type
     * @param processName process name
     * @throws RuntimeException Throws an exception if the process already exists
     */
    public void registerProcess(ProcessType type, String processName) throws RuntimeException {
        if (logPross.containsKey(processName)) {
            throw new BusException(Status.PROCESS_REGISTER_EXITS);
        }
        ProcessEntity entity = ProcessEntity.builder()
                .key(UUID.fastUUID().toString())
                .log(new StringBuilder())
                .status(ProcessStatus.INITIALIZING)
                .type(type)
                .title(type.getValue())
                .startTime(LocalDateTime.now())
                .children(new CopyOnWriteArrayList<>())
                .build();
        logPross.put(processName, entity);
        appendLog(processName, null, "Start Process:" + processName, true);
    }

    /**
     * Register a new process step.
     *
     * @param type          process step type
     * @param processName   process name
     * @param parentStepPid parent step
     * @throws RuntimeException Throws an exception if the process does not exist
     */
    public ProcessStepEntity registerProcessStep(ProcessStepType type, String processName, String parentStepPid)
            throws RuntimeException {
        if (!logPross.containsKey(processName)) {
            throw new BusException(StrFormatter.format("Process {} does not exist", type));
        }
        ProcessEntity process = logPross.get(processName);
        process.setStatus(ProcessStatus.RUNNING);
        ProcessStepEntity processStepEntity = ProcessStepEntity.builder()
                .key(UUID.fastUUID().toString())
                .status(ProcessStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .type(type)
                .title(type.getDesc().getMessage())
                .log(new StringBuilder())
                .children(new CopyOnWriteArrayList<>())
                .build();

        if (TextUtils.isEmpty(parentStepPid)) {
            // parentStep为空表示为顶级节点
            process.getChildren().add(processStepEntity);
        } else {
            ProcessStepEntity stepNode = getStepNode(parentStepPid, process.getChildren());
            if (stepNode == null) {
                log.error("registerProcessStep {} failed in {}", type.getDesc(), processName);
            } else {
                stepNode.getChildren().add(processStepEntity);
            }
        }
        return processStepEntity;
    }

    /**
     * Mark the process as completed.
     *
     * @param processName process name
     * @param status      Process status
     * @param e           exception object, optional
     */
    public void finishedProcess(String processName, ProcessStatus status, Throwable e) {
        if (!logPross.containsKey(processName)) {
            return;
        }
        ProcessEntity process = logPross.get(processName);
        process.setStatus(status);
        process.setEndTime(LocalDateTime.now());
        process.setTime(
                Duration.between(process.getStartTime(), process.getEndTime()).toMillis());
        if (e != null) {
            appendLog(processName, null, LogUtil.getError(e.getCause()), true);
        }
        String filePath = String.format("%s/tmp/log/%s.json", System.getProperty("user.dir"), processName);
        if (FileUtil.exist(filePath)) {
            Assert.isTrue(FileUtil.del(filePath));
        }
        FileUtil.writeUtf8String(JSONObject.toJSONString(process), filePath);
        appendLog(processName, null, StrFormatter.format("Process {} exit with status:{}", processName, status), true);
        logPross.remove(processName);
    }

    /**
     * Mark process step as completed.
     *
     * @param processName process name
     * @param step        process step type
     * @param status      Process step status
     * @param e           exception object, optional
     */
    public void finishedStep(String processName, ProcessStepEntity step, ProcessStatus status, Exception e) {
        if (!logPross.containsKey(processName)) {
            return;
        }
        step.setStatus(status);
        step.setEndTime(LocalDateTime.now());
        step.setTime(Duration.between(step.getStartTime(), step.getEndTime()).toMillis());
        if (e != null) {
            appendLog(processName, step.getKey(), LogUtil.getError(e.getCause()), false);
        }
        appendLog(
                processName,
                step.getKey(),
                StrFormatter.format("Process Step {} exit with status:{}", step.getType(), status),
                true);
    }

    private ProcessStepEntity getStepNode(String stepPid, CopyOnWriteArrayList<ProcessStepEntity> stepsMap) {
        ProcessStepEntity stepNode = findStepNode(stepPid, stepsMap);
        if (stepNode != null) {
            return stepNode;
        }
        String errorStr = StrFormatter.format(
                "Get Parent Node Failed, This is most likely a Dinky bug, "
                        + "please report the following information back to the community：\nProcess:{},\nstep:{},\nprocessNam:{}",
                JSONObject.toJSONString(logPross),
                stepPid,
                MDC.get(ProcessAspect.PROCESS_NAME));
        log.debug(errorStr);
        return null;
    }

    /**
     * 递归查找节点
     */
    private ProcessStepEntity findStepNode(String stepPid, CopyOnWriteArrayList<ProcessStepEntity> stepsMap) {
        // 深度优先搜索，匹配step Id符合的进程步对象返回
        for (ProcessStepEntity processStepEntity : stepsMap) {
            if (processStepEntity.getKey().equals(stepPid)) {
                return processStepEntity;
            } else {
                ProcessStepEntity stepNode = findStepNode(stepPid, processStepEntity.getChildren());
                if (stepNode != null) {
                    return stepNode;
                }
            }
        }
        return null;
    }

    private CopyOnWriteArrayList<ProcessStepEntity> getStepsMap(String processName) {
        return logPross.get(processName).getChildren();
    }
}
