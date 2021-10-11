/*-
 * #%L
 * This file is part of "Apromore Core".
 * 
 * Copyright (C) 2015 - 2017 Queensland University of Technology.
 * %%
 * Copyright (C) 2018 - 2021 Apromore Pty Ltd.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

package org.apromore.service.loganimation2.replay;

import de.hpi.bpmn2_0.model.FlowNode;
import de.hpi.bpmn2_0.model.connector.SequenceFlow;
import de.hpi.bpmn2_0.model.gateway.ExclusiveGateway;
import org.apromore.service.loganimation2.utils.LogUtility;
import org.deckfour.xes.model.XTrace;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReplayTrace {
    private TraceNode startNode = null;
    private XTrace logTrace = null;

    //Contains replayed node in the order of timing. Only contain node of model that is actually played
    //Every element points to a trace node in the replayed trace
    //This is to support for the replayed trace, like its flatten form in timing order
    private ArrayList<TraceNode> timeOrderedReplayedNodes = new ArrayList();

    //All sequence flows in the trace.
    private ArrayList<SequenceFlow> sequenceFlows = new ArrayList();
    
    //This is a mapping from the marking in BPMN model to the marking of this trace
    //Note that the different between BPMN model and this trace is this trace has flatten structure
    //Meaning repeated path of events on BPMN model is replicated as new paths on this trace
    //key: one node in the current marking of BPMN model
    //value: corresponding node in current marking of this trace
    //During replay, markings of BPMN model and this trace must be kept synchronized.
    private Map<FlowNode,TraceNode> markingsMap = new HashMap();
    
    private AnimationParams animationParams;
    
    public ReplayTrace(XTrace trace, AnimationParams animationParams) {
        this.logTrace = trace;
        this.animationParams = animationParams;
    }
       
    public String getId() {
        if (logTrace != null) {
            return logTrace.getAttributes().get("concept:name").toString();
        }
        else {
            return "";
        }
    }
    
    public TraceNode getStart() {
        return startNode;
    }
    
    public void setStart(TraceNode startNode) {
        this.startNode = startNode;
        this.markingsMap.put(startNode.getModelNode(), startNode);
    }
    
    public DateTime getStartDate() {
        return timeOrderedReplayedNodes.get(0).getStart();
    }
    
    public DateTime getEndDate() {
        return timeOrderedReplayedNodes.get(timeOrderedReplayedNodes.size()-1).getComplete();
    }
    
    //newNode: node to add
    //curModelNode: source node to be connected with the new node
    public void add(TraceNode currentNode, TraceNode newNode) {
        FlowNode newModelNode = newNode.getModelNode();
        SequenceFlow modelFlow = null;
        
        TraceNode curNode = this.markingsMap.get(currentNode.getModelNode());
        
        SequenceFlow traceFlow = new SequenceFlow();
        traceFlow.setSourceRef(curNode);
        traceFlow.setTargetRef(newNode);
        
        //Search for original sequence id
        for (SequenceFlow flow : currentNode.getModelNode().getOutgoingSequenceFlows()) {
            if (newModelNode == flow.getTargetRef()) {
                modelFlow = flow;
                break;
            }
        }
        if (modelFlow != null) {
            traceFlow.setId(modelFlow.getId());
        }
        
        curNode.getOutgoing().add(traceFlow);
        newNode.getIncoming().add(traceFlow);
        this.sequenceFlows.add(traceFlow);
        
        this.markingsMap.put(newNode.getModelNode(), newNode);
    }
    
    public ArrayList<TraceNode> getNodes() {
        return this.timeOrderedReplayedNodes;
    }

    private void removeNode(TraceNode node) {
        this.markingsMap.remove(node.getModelNode());
        this.timeOrderedReplayedNodes.remove(node);
    }

    private void removeSequenceFlow(SequenceFlow flow) {
        flow.getSourceRef().getOutgoing().remove(flow);
        flow.getTargetRef().getIncoming().remove(flow);
        flow.setSourceRef(null);
        flow.setTargetRef(null);
        this.sequenceFlows.remove(flow);
    }
    
    public ArrayList<SequenceFlow> getSequenceFlows() {
        if (this.sequenceFlows.isEmpty()) {
            for (TraceNode node : this.timeOrderedReplayedNodes) {
                for (SequenceFlow flow : node.getOutgoingSequenceFlows()) {
                    this.sequenceFlows.add(flow);
                }
            }
        }
        return this.sequenceFlows;
    }
    
    /**
     * Remove all XOR gateways in this ReplayTrace
     * For example: StartEvent -> XOR -> A -> B -> XOR -> XOR -> C -> D -> EndEvent
     * Will become: StartEvent -> A -> B -> C -> D -> EndEvent
     */
    public void convertToNonGateways() {
        Set<TraceNode> toBeRemoved = new HashSet<>();
        TraceNode traceNode = getNext(this.getStart());
        while (traceNode != null) {
            if (traceNode.getModelNode() instanceof ExclusiveGateway) {             
                SequenceFlow incoming = (SequenceFlow)traceNode.getIncoming().get(0);
                TraceNode precedingNode = (TraceNode)incoming.getSourceRef();
                TraceNode succeedingNode = getNext(traceNode);
                // A --incoming--> XOR --outgoing--> B becomes A --outgoing--> B, removes incoming and XOR
                // A --incoming--> XOR (no succeeding node) becomes A, removes incoming and XOR                
                if (succeedingNode != null) {
                    SequenceFlow outgoing = (SequenceFlow)traceNode.getOutgoing().get(0);
                    traceNode.getOutgoing().remove(outgoing);
                    precedingNode.getOutgoing().add(outgoing);
                    outgoing.setSourceRef(precedingNode);
                }
                precedingNode.getOutgoing().remove(incoming);
                removeSequenceFlow(incoming);
                toBeRemoved.add(traceNode);
                traceNode = succeedingNode;
            }
            else {
                traceNode = getNext(traceNode);
            }
            
        }
        toBeRemoved.forEach(node -> removeNode(node));
    }

    //Add the input node to the list of nodes which have been replayed
    //This method assumes that the replayed node has been added to the list of
    //all nodes in the replayed trace.
    public void addToReplayedList(FlowNode curModelNode) {
        this.timeOrderedReplayedNodes.add(this.markingsMap.get(curModelNode));
    }
    
    private TraceNode getNext(TraceNode node) {
        return !node.getOutgoingSequenceFlows().isEmpty() 
                    ? (TraceNode)node.getOutgoingSequenceFlows().get(0).getTargetRef() 
                    : null;
    }

    public void clear() {
        startNode = null;
        logTrace = null;
        
        //Remove dependency between nodes
        for (SequenceFlow flow : this.sequenceFlows) {
            flow.setSourceRef(null);
            flow.setTargetRef(null);
        }
        sequenceFlows.clear();
        timeOrderedReplayedNodes.clear();
        markingsMap.clear();
    }
    
    public boolean isEmpty() {
        return (startNode == null || this.timeOrderedReplayedNodes.size()==0);
    }
    
    public void calcTiming() {
        calcTimingAll();
        calculateCompleteTimestamp();
    }
    
    /*
    * timeOrderedReplayedNodes keeps the trace node in replay order. Note that
    * replay order follows the order of the trace event, so they are in ascending timing order.
    * In addition, in this order, the split gateway is always after the joining gateway and their
    * branch nodes are all in between them.
    * Use the flatten form of replayed trace to calculate timing for forks and joins
    * From a fork on the flatten trace, traverse forward and backward to search for 
    * a timed node (node with real timing data). Remember there is always either a timed start event 
    * or a timed activity or end event (also timed) as two ends on the traversing direction
    * Calculate in timing order from start to end
    */
    private void calcTimingAll() {
        TraceNode node;
        DateTime timeBefore=null;
        DateTime timeAfter=null;
        int timeBeforePos = 0;
        int timeAfterPos = 0;
        long duration;   
        
        for (int i=0; i<timeOrderedReplayedNodes.size();i++) {
            node = timeOrderedReplayedNodes.get(i);
            timeBefore = null;
            timeAfter = null;
            if (!node.isTimed()) {
                
                //----------------------------------------
                // The incoming nodes to this node have been already assigned timestamps 
                // according to the traversal order starting from the Start event node (always timed)
                // Therefore, a node selects timestamp based on those of its incoming nodes.
                // This is to ensure a node's timestamp must be after all timestamp of its incoming nodes
                //----------------------------------------
                TraceNode mostRecentIncomingNode=null;
                for (TraceNode incomingNode : node.getSources()) {
                    if (timeBefore == null || timeBefore.isBefore(incomingNode.getStart())) {
                        timeBefore = incomingNode.getStart();
                        mostRecentIncomingNode = incomingNode;
                    }
                }
                timeBeforePos = timeOrderedReplayedNodes.indexOf(mostRecentIncomingNode);
                
                //----------------------------------------
                // Go forward and look for a timed node, known that it can encounter
                // either a timed node or the End event node eventually (always timed).
                // In the timeOrderedReplayedNodes array order, all nodes after 
                // this node are either subsequent and connected to it on the model or
                // in parallel with it. It is possible to set a node's timestamp 
                // after that of a parallel node because its next sequential node may have timestamp 
                // after the node in parallel. So, this ensures its timestamp is after 
                // any next node in chronological order, either sequential or parallel
                //----------------------------------------
                for (int j=i+1;j<timeOrderedReplayedNodes.size();j++) {
                    if (timeOrderedReplayedNodes.get(j).isTimed() && 
                            timeOrderedReplayedNodes.get(j).getStart().isAfter(timeBefore)) {
                        timeAfter = timeOrderedReplayedNodes.get(j).getStart();
                        timeAfterPos = j;
                        break;
                    }
                }
                
                //----------------------------------------------
                //Always take two ends of the trace plus a buffer as time limit
                //in case the replay trace has no timestamped activity at two ends 
                //NOTE: This is in case some process models cannot reach the End Event (unsound model)
                //----------------------------------------------
                if (timeBefore == null) {
                    timeBefore = (new DateTime(LogUtility.getTimestamp(logTrace.get(0)))).minusSeconds(
                                               animationParams.getStartEventToFirstEventDuration());
                }
                if (timeAfter == null) {
                    timeAfter = (new DateTime(LogUtility.getTimestamp(logTrace.get(logTrace.size()-1)))).plusSeconds(
                            animationParams.getLastEventToEndEventDuration());
                }
                
                //----------------------------------------------
                // Take average timestamp between TimeBefore and TimeAfter
                //----------------------------------------------
                duration = (new Duration(timeBefore, timeAfter)).getMillis();
                if (timeAfterPos > timeBeforePos) {
                    duration = Math.round(1.0*duration*(i-timeBeforePos)/(timeAfterPos - timeBeforePos));
                }
                node.setStart(timeBefore.plus(Double.valueOf(duration).longValue()));
            }
        }
    }

    /*
    * Set complete timestamp for every node since event log only contains one timestamp
    * By default event timestamp is set to start date of a trace node
    * The complete date is calculated by adding to the start date 10% the duration 
    * from start date to the earliest date of all target nodes
    * Assume that all nodes have start timestamp calculated and assigned (not null).
    */
    private void calculateCompleteTimestamp() {
        DateTime earliestTarget=null;
        long transferDuration;
        for (TraceNode node : this.timeOrderedReplayedNodes) {
            if (node.isActivity()) {
                if (node.getTargets().size() > 0) {
                    earliestTarget = node.getTargets().get(0).getStart();
                    for (TraceNode target : node.getTargets()) {
                        if (earliestTarget.isAfter(target.getStart())) {
                            earliestTarget = target.getStart();
                        }
                    }
                    transferDuration = (new Duration(node.getStart(),earliestTarget)).getMillis();
                    node.setComplete(node.getStart().plusMillis(Long.valueOf(Math.round(transferDuration*0.1)).intValue()));
                }
                else {
                    node.setComplete(node.getStart().plusMillis(5000));
                }
            }
            else {
                node.setComplete(node.getStart());
            }
            

        }
    }
    
    /*
     * Print this trace to log in a hierarchical view
     */
    public void print() {
        int totalIndent = 0;
        int addedIndent = 0;
        FlowNode flowNode;
        Map<String, Integer> nodeTypeIndentMap = new HashMap();
        int counterDecision = 0;
        int counterMerge = 0;
        int counterFork = 0;
        int counterJoin = 0;
        int counterORSplit = 0;
        int counterORJoin = 0;
                
                
        for (TraceNode node : this.timeOrderedReplayedNodes) {
            String nodeString = "";
            String nodeType = "";
            String branchNodes = "";
            String dateString = "";
            addedIndent = 0;

            //------------------------------------
            // Print current node
            //------------------------------------
            if (node.isActivity()) {
                nodeType = "Activity";
                branchNodes = "";
            }
            else if (node.isStartEvent()) {
                nodeType = "StartEvent";
            }
            else if (node.isEndEvent()) {
                nodeType = "EndEvent";
            }
            else if (node.isFork()) {
                nodeType = "Fork";
                counterFork++;
                branchNodes += " => ";
                for (SequenceFlow flow : node.getOutgoingSequenceFlows()) {
                    flowNode = (FlowNode)flow.getTargetRef();
                    branchNodes += flowNode.getName();
                    branchNodes += "+";
                }
                addedIndent += 2;
                nodeTypeIndentMap.put(nodeType+counterFork, totalIndent);
            }
            else if (node.isJoin()) {
                nodeType = "Join";
                counterJoin++;
                branchNodes += " <= ";
                for (SequenceFlow flow : node.getIncomingSequenceFlows()) {
                    flowNode = (FlowNode)flow.getSourceRef();
                    branchNodes += flowNode.getName();
                    branchNodes += "+";
                }            
                if (nodeTypeIndentMap.containsKey("Fork" + counterJoin)) {
                    totalIndent = nodeTypeIndentMap.get("Fork" + counterJoin).intValue();
                } else {
                    addedIndent -= 2;
                }
            }
            else if (node.isDecision()) {
                nodeType = "Decision";
                counterDecision++;
                branchNodes += " -> ";
                for (SequenceFlow flow : node.getOutgoingSequenceFlows()) {
                    flowNode = (FlowNode)flow.getTargetRef();
                    branchNodes += flowNode.getName();
                    branchNodes += "+";
                }     
                addedIndent += 2;
                nodeTypeIndentMap.put(nodeType+counterDecision, totalIndent);
            }
            else if (node.isMerge()) {
                nodeType = "Merge";
                counterMerge++;
                branchNodes += " <- ";
                for (SequenceFlow flow : node.getIncomingSequenceFlows()) {
                    flowNode = (FlowNode)flow.getSourceRef();
                    branchNodes += flowNode.getName();
                    branchNodes += "+";
                }   
                if (nodeTypeIndentMap.containsKey("Decision" + counterMerge)) {
                    totalIndent = nodeTypeIndentMap.get("Decision" + counterMerge).intValue();
                } else {
                    addedIndent -= 2;
                }
            }
            else if (node.isORSplit()) {
                nodeType = "OR-Split";
                counterORSplit++;
                branchNodes += " => ";
                for (SequenceFlow flow : node.getOutgoingSequenceFlows()) {
                    flowNode = (FlowNode)flow.getTargetRef();
                    branchNodes += flowNode.getName();
                    branchNodes += "+";
                }
                addedIndent += 2;
                nodeTypeIndentMap.put(nodeType+counterORSplit, totalIndent);
            }
            else if (node.isORJoin()) {
                nodeType = "OR-Join";
                counterORJoin++;
                branchNodes += " <= ";
                for (SequenceFlow flow : node.getIncomingSequenceFlows()) {
                    flowNode = (FlowNode)flow.getSourceRef();
                    branchNodes += flowNode.getName();
                    branchNodes += "+";
                }   
                if (nodeTypeIndentMap.containsKey("OR-Split" + counterORJoin)) {
                    totalIndent = nodeTypeIndentMap.get("OR-Split" + counterORJoin).intValue();
                } else {
                    addedIndent -= 2;
                }                
            }

            if (node.getStart() != null) {
                dateString = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(node.getStart().toDate());
            }

            nodeString += (nodeType + ":" + node.getName() + ":" + dateString + ":" + branchNodes);
            nodeString = this.padLeft(nodeString, totalIndent);
            
//            LOGGER.info(nodeString);
            totalIndent += addedIndent;
        }

        
    }    
    
    private String padLeft(String s, int n) {
        if (n <= 0)
            return s;
        int noOfSpaces = n * 2;
        StringBuilder output = new StringBuilder(s.length() + noOfSpaces);
        while (noOfSpaces > 0) {
            output.append(" ");
            noOfSpaces--;
        }
        output.append(s);
        return output.toString();
    }
    
}
