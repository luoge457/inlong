/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.plugin.task;

import org.apache.inlong.agent.common.AgentThreadFactory;
import org.apache.inlong.agent.conf.TaskProfile;
import org.apache.inlong.agent.constant.TaskConstants;
import org.apache.inlong.agent.core.task.TaskManager;
import org.apache.inlong.agent.plugin.AgentBaseTestsHelper;
import org.apache.inlong.agent.plugin.task.file.LogFileTask;
import org.apache.inlong.common.enums.TaskStateEnum;

import com.google.gson.Gson;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LogFileTask.class)
@PowerMockIgnore({"javax.management.*"})
public class TestLogFileTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestLogFileTask.class);
    private static final ClassLoader LOADER = TestLogFileTask.class.getClassLoader();
    private static LogFileTask task;
    private static AgentBaseTestsHelper helper;
    private static final Gson GSON = new Gson();
    private static TaskManager manager;
    private static MockInstanceManager instanceManager = new MockInstanceManager();
    private static String tempResourceName;
    private static String resourceName;
    private static String fileName;
    private static String dataTime;
    private static final ThreadPoolExecutor EXECUTOR_SERVICE = new ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            1L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new AgentThreadFactory("TestLogfileCollectTask"));

    @BeforeClass
    public static void setup() {
        helper = new AgentBaseTestsHelper(TestLogFileTask.class.getName()).setupAgentHome();
        resourceName = LOADER.getResource("testScan/20230928_1/test_1.txt").getPath();
        tempResourceName = LOADER.getResource("testScan/temp.txt").getPath();
        File f = new File(tempResourceName);
        String pattern = f.getParent() + "/YYYYMMDD_[0-9]+/test_[0-9]+.txt";
        TaskProfile taskProfile = helper.getTaskProfile(1, pattern, true, 0L, 0L, TaskStateEnum.RUNNING, "D",
                "GMT+8:00");
        try {
            String startStr = "2023-09-20 00:00:00";
            String endStr = "2023-09-30 00:00:00";
            Date parse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(startStr);
            long start = parse.getTime();
            parse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(endStr);
            long end = parse.getTime();
            taskProfile.setLong(TaskConstants.TASK_START_TIME, start);
            taskProfile.setLong(TaskConstants.TASK_END_TIME, end);
            manager = new TaskManager();
            task = PowerMockito.spy(new LogFileTask());
            PowerMockito.doAnswer(invocation -> {
                fileName = invocation.getArgument(0);
                dataTime = invocation.getArgument(1);
                return null;
            }).when(task, "addToEvenMap", Mockito.anyString(), Mockito.anyString());
            Assert.assertTrue(task.isProfileValid(taskProfile));
            manager.getTaskStore().storeTask(taskProfile);
            task.init(manager, taskProfile, manager.getInstanceBasicStore());
            EXECUTOR_SERVICE.submit(task);
        } catch (Exception e) {
            LOGGER.error("source init error {}", e);
            Assert.assertTrue("source init error", false);
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        task.destroy();
        helper.teardownAgentHome();
    }

    @Test
    public void testTaskManager() throws Exception {
        await().atMost(2, TimeUnit.SECONDS).until(() -> fileName != null && dataTime != null);
        Assert.assertTrue(fileName.compareTo(resourceName) == 0);
        Assert.assertTrue(dataTime.compareTo("20230928") == 0);
        PowerMockito.verifyPrivate(task, Mockito.times(1))
                .invoke("addToEvenMap", Mockito.anyString(), Mockito.anyString());
    }
}