package com.walmartlabs.concord.server.agent;

import com.walmartlabs.concord.server.api.agent.CancelJobCommand;
import com.walmartlabs.concord.server.api.process.ProcessEntry;
import com.walmartlabs.concord.server.process.queue.ProcessQueueDao;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AgentManager {

    private final ProcessQueueDao queueDao;
    private final CommandQueueImpl commandQueue;

    @Inject
    public AgentManager(ProcessQueueDao queueDao, CommandQueueImpl commandQueue) {
        this.queueDao = queueDao;
        this.commandQueue = commandQueue;
    }

    public void killProcess(String instanceId) {
        ProcessEntry e = queueDao.get(instanceId);
        if (e == null) {
            // TODO throw an exception?
            return;
        }

        String agentId = e.getLastAgentId();
        if (agentId == null) {
            // TODO throw an exception?
            return;
        }

        commandQueue.add(agentId, new CancelJobCommand(instanceId));
    }
}