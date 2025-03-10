/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.rest.service.api.runtime.task;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.variable.QueryVariable;
import org.flowable.rest.service.api.engine.variable.QueryVariable.QueryVariableOperation;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.TaskQueryProperty;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Shared logic for resources related to Tasks.
 * 
 * @author Frederik Heremans
 */
public class TaskBaseResource {

    private static HashMap<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("id", TaskQueryProperty.TASK_ID);
        properties.put("name", TaskQueryProperty.NAME);
        properties.put("description", TaskQueryProperty.DESCRIPTION);
        properties.put("dueDate", TaskQueryProperty.DUE_DATE);
        properties.put("createTime", TaskQueryProperty.CREATE_TIME);
        properties.put("priority", TaskQueryProperty.PRIORITY);
        properties.put("executionId", TaskQueryProperty.EXECUTION_ID);
        properties.put("processInstanceId", TaskQueryProperty.PROCESS_INSTANCE_ID);
        properties.put("tenantId", TaskQueryProperty.TENANT_ID);
    }

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected HistoryService historyService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    protected DelegationState getDelegationState(String delegationState) {
        DelegationState state = null;
        if (delegationState != null) {
            if (DelegationState.RESOLVED.name().toLowerCase().equals(delegationState)) {
                return DelegationState.RESOLVED;
            } else if (DelegationState.PENDING.name().toLowerCase().equals(delegationState)) {
                return DelegationState.PENDING;
            } else {
                throw new FlowableIllegalArgumentException("Illegal value for delegationState: " + delegationState);
            }
        }
        return state;
    }

    /**
     * Populate the task based on the values that are present in the given {@link TaskRequest}.
     */
    protected void populateTaskFromRequest(Task task, TaskRequest taskRequest) {
        if (taskRequest.isNameSet()) {
            task.setName(taskRequest.getName());
        }
        if (taskRequest.isAssigneeSet()) {
            task.setAssignee(taskRequest.getAssignee());
        }
        if (taskRequest.isDescriptionSet()) {
            task.setDescription(taskRequest.getDescription());
        }
        if (taskRequest.isDuedateSet()) {
            task.setDueDate(taskRequest.getDueDate());
        }
        if (taskRequest.isOwnerSet()) {
            task.setOwner(taskRequest.getOwner());
        }
        if (taskRequest.isParentTaskIdSet()) {
            task.setParentTaskId(taskRequest.getParentTaskId());
        }
        if (taskRequest.isPrioritySet()) {
            task.setPriority(taskRequest.getPriority());
        }
        if (taskRequest.isCategorySet()) {
            task.setCategory(taskRequest.getCategory());
        }
        if (taskRequest.isTenantIdSet()) {
            task.setTenantId(taskRequest.getTenantId());
        }
        if (taskRequest.isFormKeySet()) {
            task.setFormKey(taskRequest.getFormKey());
        }

        if (taskRequest.isDelegationStateSet()) {
            DelegationState delegationState = getDelegationState(taskRequest.getDelegationState());
            task.setDelegationState(delegationState);
        }
    }

    protected DataResponse<TaskResponse> getTasksFromQueryRequest(TaskQueryRequest request, Map<String, String> requestParams) {

        TaskQuery taskQuery = taskService.createTaskQuery();

        // Populate filter-parameters
        if (request.getName() != null) {
            taskQuery.taskName(request.getName());
        }
        if (request.getNameLike() != null) {
            taskQuery.taskNameLike(request.getNameLike());
        }
        if (request.getDescription() != null) {
            taskQuery.taskDescription(request.getDescription());
        }
        if (request.getDescriptionLike() != null) {
            taskQuery.taskDescriptionLike(request.getDescriptionLike());
        }
        if (request.getPriority() != null) {
            taskQuery.taskPriority(request.getPriority());
        }
        if (request.getMinimumPriority() != null) {
            taskQuery.taskMinPriority(request.getMinimumPriority());
        }
        if (request.getMaximumPriority() != null) {
            taskQuery.taskMaxPriority(request.getMaximumPriority());
        }
        if (request.getAssignee() != null) {
            taskQuery.taskAssignee(request.getAssignee());
        }
        if (request.getAssigneeLike() != null) {
            taskQuery.taskAssigneeLike(request.getAssigneeLike());
        }
        if (request.getOwner() != null) {
            taskQuery.taskOwner(request.getOwner());
        }
        if (request.getOwnerLike() != null) {
            taskQuery.taskOwnerLike(request.getOwnerLike());
        }
        if (request.getUnassigned() != null) {
            taskQuery.taskUnassigned();
        }
        if (request.getDelegationState() != null) {
            DelegationState state = getDelegationState(request.getDelegationState());
            if (state != null) {
                taskQuery.taskDelegationState(state);
            }
        }
        if (request.getCandidateUser() != null) {
            taskQuery.taskCandidateUser(request.getCandidateUser());
        }
        if (request.getInvolvedUser() != null) {
            taskQuery.taskInvolvedUser(request.getInvolvedUser());
        }
        if (request.getCandidateGroup() != null) {
            taskQuery.taskCandidateGroup(request.getCandidateGroup());
        }
        if (request.getCandidateGroupIn() != null) {
            taskQuery.taskCandidateGroupIn(request.getCandidateGroupIn());
        }
        if (request.getProcessInstanceId() != null) {
            taskQuery.processInstanceId(request.getProcessInstanceId());
        }
        if (request.getProcessInstanceIdWithChildren() != null) {
            taskQuery.processInstanceIdWithChildren(request.getProcessInstanceIdWithChildren());
        }
        if (request.getProcessInstanceBusinessKey() != null) {
            taskQuery.processInstanceBusinessKey(request.getProcessInstanceBusinessKey());
        }
        if (request.getExecutionId() != null) {
            taskQuery.executionId(request.getExecutionId());
        }
        if (request.getCreatedOn() != null) {
            taskQuery.taskCreatedOn(request.getCreatedOn());
        }
        if (request.getCreatedBefore() != null) {
            taskQuery.taskCreatedBefore(request.getCreatedBefore());
        }
        if (request.getCreatedAfter() != null) {
            taskQuery.taskCreatedAfter(request.getCreatedAfter());
        }
        if (request.getExcludeSubTasks() != null) {
            if (request.getExcludeSubTasks().booleanValue()) {
                taskQuery.excludeSubtasks();
            }
        }

        if (request.getTaskDefinitionKey() != null) {
            taskQuery.taskDefinitionKey(request.getTaskDefinitionKey());
        }

        if (request.getTaskDefinitionKeyLike() != null) {
            taskQuery.taskDefinitionKeyLike(request.getTaskDefinitionKeyLike());
        }
        if (request.getDueDate() != null) {
            taskQuery.taskDueDate(request.getDueDate());
        }
        if (request.getDueBefore() != null) {
            taskQuery.taskDueBefore(request.getDueBefore());
        }
        if (request.getDueAfter() != null) {
            taskQuery.taskDueAfter(request.getDueAfter());
        }
        if (request.getWithoutDueDate() != null && request.getWithoutDueDate()) {
            taskQuery.withoutTaskDueDate();
        }

        if (request.getActive() != null) {
            if (request.getActive().booleanValue()) {
                taskQuery.active();
            } else {
                taskQuery.suspended();
            }
        }

        if (request.getIncludeTaskLocalVariables() != null) {
            if (request.getIncludeTaskLocalVariables()) {
                taskQuery.includeTaskLocalVariables();
            }
        }
        if (request.getIncludeProcessVariables() != null) {
            if (request.getIncludeProcessVariables()) {
                taskQuery.includeProcessVariables();
            }
        }

        if (request.getProcessInstanceBusinessKeyLike() != null) {
            taskQuery.processInstanceBusinessKeyLike(request.getProcessInstanceBusinessKeyLike());
        }

        if (request.getProcessDefinitionId() != null) {
            taskQuery.processDefinitionId(request.getProcessDefinitionId());
        }

        if (request.getProcessDefinitionKey() != null) {
            taskQuery.processDefinitionKey(request.getProcessDefinitionKey());
        }

        if (request.getProcessDefinitionKeyLike() != null) {
            taskQuery.processDefinitionKeyLike(request.getProcessDefinitionKeyLike());
        }

        if (request.getProcessDefinitionName() != null) {
            taskQuery.processDefinitionName(request.getProcessDefinitionName());
        }

        if (request.getProcessDefinitionNameLike() != null) {
            taskQuery.processDefinitionNameLike(request.getProcessDefinitionNameLike());
        }

        if (request.getTaskVariables() != null) {
            addTaskvariables(taskQuery, request.getTaskVariables());
        }

        if (request.getProcessInstanceVariables() != null) {
            addProcessvariables(taskQuery, request.getProcessInstanceVariables());
        }
        
        if (request.getScopeDefinitionId() != null) {
            taskQuery.scopeDefinitionId(request.getScopeDefinitionId());
        }
        
        if (request.getScopeId() != null) {
            taskQuery.scopeId(request.getScopeId());
        }
        
        if (request.getScopeType() != null) {
            taskQuery.scopeType(request.getScopeType());
        }

        if (request.getTenantId() != null) {
            taskQuery.taskTenantId(request.getTenantId());
        }

        if (request.getTenantIdLike() != null) {
            taskQuery.taskTenantIdLike(request.getTenantIdLike());
        }

        if (Boolean.TRUE.equals(request.getWithoutTenantId())) {
            taskQuery.taskWithoutTenantId();
        }

        if (request.getCandidateOrAssigned() != null) {
            taskQuery.taskCandidateOrAssigned(request.getCandidateOrAssigned());
        }

        if (request.getCategory() != null) {
            taskQuery.taskCategory(request.getCategory());
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessTaskInfoWithQuery(taskQuery, request);
        }

        return paginateList(requestParams, request, taskQuery, "id", properties, restResponseFactory::createTaskResponseList);
    }

    protected void addTaskvariables(TaskQuery taskQuery, List<QueryVariable> variables) {

        for (QueryVariable variable : variables) {
            if (variable.getVariableOperation() == null) {
                throw new FlowableIllegalArgumentException("Variable operation is missing for variable: " + variable.getName());
            }
            if (variable.getValue() == null) {
                throw new FlowableIllegalArgumentException("Variable value is missing for variable: " + variable.getName());
            }

            boolean nameLess = variable.getName() == null;

            Object actualValue = restResponseFactory.getVariableValue(variable);

            // A value-only query is only possible using equals-operator
            if (nameLess && variable.getVariableOperation() != QueryVariableOperation.EQUALS) {
                throw new FlowableIllegalArgumentException("Value-only query (without a variable-name) is only supported when using 'equals' operation.");
            }

            switch (variable.getVariableOperation()) {

            case EQUALS:
                if (nameLess) {
                    taskQuery.taskVariableValueEquals(actualValue);
                } else {
                    taskQuery.taskVariableValueEquals(variable.getName(), actualValue);
                }
                break;

            case EQUALS_IGNORE_CASE:
                if (actualValue instanceof String) {
                    taskQuery.taskVariableValueEqualsIgnoreCase(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported when ignoring casing, but was: " + actualValue.getClass().getName());
                }
                break;

            case NOT_EQUALS:
                taskQuery.taskVariableValueNotEquals(variable.getName(), actualValue);
                break;

            case NOT_EQUALS_IGNORE_CASE:
                if (actualValue instanceof String) {
                    taskQuery.taskVariableValueNotEqualsIgnoreCase(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported when ignoring casing, but was: " + actualValue.getClass().getName());
                }
                break;

            case GREATER_THAN:
                taskQuery.taskVariableValueGreaterThan(variable.getName(), actualValue);
                break;

            case GREATER_THAN_OR_EQUALS:
                taskQuery.taskVariableValueGreaterThanOrEqual(variable.getName(), actualValue);
                break;

            case LESS_THAN:
                taskQuery.taskVariableValueLessThan(variable.getName(), actualValue);
                break;

            case LESS_THAN_OR_EQUALS:
                taskQuery.taskVariableValueLessThanOrEqual(variable.getName(), actualValue);
                break;

            case LIKE:
                if (actualValue instanceof String) {
                    taskQuery.taskVariableValueLike(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported using like, but was: " + actualValue.getClass().getName());
                }
                break;

            case LIKE_IGNORE_CASE:
                if (actualValue instanceof String) {
                    taskQuery.taskVariableValueLikeIgnoreCase(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported using like, but was: " + actualValue.getClass().getName());
                }
                break;
            default:
                throw new FlowableIllegalArgumentException("Unsupported variable query operation: " + variable.getVariableOperation());
            }
        }
    }

    protected void addProcessvariables(TaskQuery taskQuery, List<QueryVariable> variables) {
        for (QueryVariable variable : variables) {
            if (variable.getVariableOperation() == null) {
                throw new FlowableIllegalArgumentException("Variable operation is missing for variable: " + variable.getName());
            }
            if (variable.getValue() == null) {
                throw new FlowableIllegalArgumentException("Variable value is missing for variable: " + variable.getName());
            }

            boolean nameLess = variable.getName() == null;

            Object actualValue = restResponseFactory.getVariableValue(variable);

            // A value-only query is only possible using equals-operator
            if (nameLess && variable.getVariableOperation() != QueryVariableOperation.EQUALS) {
                throw new FlowableIllegalArgumentException("Value-only query (without a variable-name) is only supported when using 'equals' operation.");
            }

            switch (variable.getVariableOperation()) {

            case EQUALS:
                if (nameLess) {
                    taskQuery.processVariableValueEquals(actualValue);
                } else {
                    taskQuery.processVariableValueEquals(variable.getName(), actualValue);
                }
                break;

            case EQUALS_IGNORE_CASE:
                if (actualValue instanceof String) {
                    taskQuery.processVariableValueEqualsIgnoreCase(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported when ignoring casing, but was: " + actualValue.getClass().getName());
                }
                break;

            case NOT_EQUALS:
                taskQuery.processVariableValueNotEquals(variable.getName(), actualValue);
                break;

            case NOT_EQUALS_IGNORE_CASE:
                if (actualValue instanceof String) {
                    taskQuery.processVariableValueNotEqualsIgnoreCase(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported when ignoring casing, but was: " + actualValue.getClass().getName());
                }
                break;

            case GREATER_THAN:
                taskQuery.processVariableValueGreaterThan(variable.getName(), actualValue);
                break;

            case GREATER_THAN_OR_EQUALS:
                taskQuery.processVariableValueGreaterThanOrEqual(variable.getName(), actualValue);
                break;

            case LESS_THAN:
                taskQuery.processVariableValueLessThan(variable.getName(), actualValue);
                break;

            case LESS_THAN_OR_EQUALS:
                taskQuery.processVariableValueLessThanOrEqual(variable.getName(), actualValue);
                break;

            case LIKE:
                if (actualValue instanceof String) {
                    taskQuery.processVariableValueLike(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported using like, but was: " + actualValue.getClass().getName());
                }
                break;

            case LIKE_IGNORE_CASE:
                if (actualValue instanceof String) {
                    taskQuery.processVariableValueLikeIgnoreCase(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported using like, but was: " + actualValue.getClass().getName());
                }
                break;

            default:
                throw new FlowableIllegalArgumentException("Unsupported variable query operation: " + variable.getVariableOperation());
            }
        }
    }

    /**
     * Get valid task from request. Throws exception if task does not exist or if task id is not provided.
     */
    protected Task getTaskFromRequest(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new FlowableObjectNotFoundException("Could not find a task with id '" + taskId + "'.", Task.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessTaskInfoById(task);
        }
        
        return task;
    }

    /**
     * Get valid history task from request. Throws exception if task does not exist or if task id is not provided.
     */
    protected HistoricTaskInstance getHistoricTaskFromRequest(String taskId) {
        HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new FlowableObjectNotFoundException("Could not find a task with id '" + taskId + "'.", Task.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessHistoryTaskInfoById(task);
        }
        
        return task;
    }
}
