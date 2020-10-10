package com.haiyang.flowablespringboot;

import org.apache.commons.io.FileUtils;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * @author chenlw 2020-10-10
 * @since
 */
public class DynamicActivitiProcessTest {


    @Test
    public void testDynamicDeploy() throws Exception {
        ProcessEngineConfiguration processEngineConfiguration = ProcessEngineConfiguration
                .createStandaloneProcessEngineConfiguration();
        //Jdbc设置
        String jdbcDriver = "com.mysql.cj.jdbc.Driver";
        processEngineConfiguration.setJdbcDriver(jdbcDriver);
        String jdbcUrl = "jdbc:mysql://127.0.0.1:3307/flowable_test?serverTimezone=UTC&nullCatalogMeansCurrent=true";
        processEngineConfiguration.setJdbcUrl(jdbcUrl);
        String jdbcUsername = "root";
        processEngineConfiguration.setJdbcUsername(jdbcUsername);
        String jdbcPassword = "123456";
        processEngineConfiguration.setJdbcPassword(jdbcPassword);
        //        public static final String DB_SCHEMA_UPDATE_FALSE = "false";//不自动创建新表
        /**
         * Creates the schema when the process engine is being created and drops
         * the schema when the process engine is being closed.
         */
        //        public static final String DB_SCHEMA_UPDATE_CREATE_DROP = "create-drop";//每次运行创建新表
        /**
         * Upon building of the process engine, a check is performed and an
         * update of the schema is performed if it is necessary.
         */
        //        public static final String DB_SCHEMA_UPDATE_TRUE = "true";设置自动对表结构进行改进和升级
        //设置是否自动更新
        processEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        //获取引擎对象
        ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();
        processEngine.close();

        // 1. Build up the model from scratch
        BpmnModel model = new BpmnModel();
        Process process = new Process();
        model.addProcess(process);
        process.setId("my-process");

        process.addFlowElement(createStartEvent());
        process.addFlowElement(createUserTask("task1", "First task", "fred"));
        process.addFlowElement(createUserTask("task2", "Second task", "john"));
        process.addFlowElement(createEndEvent());

        process.addFlowElement(createSequenceFlow("start", "task1"));
        process.addFlowElement(createSequenceFlow("task1", "task2"));
        process.addFlowElement(createSequenceFlow("task2", "end"));

        // 2. Generate graphical information
        //  new BpmnAutoLayout(model).execute();

        // 3. Deploy the process to the engine
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                .addBpmnModel("dynamic-model.bpmn", model).name("Dynamic process deployment").deploy();

        // 4. Start a process instance
        ProcessInstance processInstance = processEngine.getRuntimeService()
                .startProcessInstanceByKey("my-process");

        // 5. Check if task is available
        List<Task> tasks = processEngine.getTaskService().createTaskQuery()
                .processInstanceId(processInstance.getId()).list();

        Assert.assertEquals(1, tasks.size());
        Assert.assertEquals("First task", tasks.get(0).getName());
        Assert.assertEquals("fred", tasks.get(0).getAssignee());

        // 6. Save process diagram to a file
        InputStream processDiagram = processEngine.getRepositoryService().getProcessDiagram(processInstance.getProcessDefinitionId());
        FileUtils.copyInputStreamToFile(processDiagram, new File("target/diagram.png"));

        // 7. Save resulting BPMN xml to a file
        InputStream processBpmn = processEngine.getRepositoryService().getResourceAsStream(deployment.getId(), "dynamic-model.bpmn");
        FileUtils.copyInputStreamToFile(processBpmn, new File("target/process.bpmn20.xml"));
    }

    protected UserTask createUserTask(String id, String name, String assignee) {
        UserTask userTask = new UserTask();
        userTask.setName(name);
        userTask.setId(id);
        userTask.setAssignee(assignee);
        return userTask;
    }

    protected SequenceFlow createSequenceFlow(String from, String to) {
        SequenceFlow flow = new SequenceFlow();
        flow.setSourceRef(from);
        flow.setTargetRef(to);
        return flow;
    }

    protected StartEvent createStartEvent() {
        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");
        return startEvent;
    }

    protected EndEvent createEndEvent() {
        EndEvent endEvent = new EndEvent();
        endEvent.setId("end");
        return endEvent;
    }

}
