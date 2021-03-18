/*-
 * #%L
 * This file is part of "Apromore Core".
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
package org.apromore.plugin.portal.useradmin;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.*;

import org.apromore.portal.types.EventQueueEvents;
import org.apromore.portal.types.EventQueueTypes;
import org.apromore.commons.datetime.DateTimeUtils;
import org.apromore.dao.model.Folder;
import org.apromore.dao.model.Log;
import org.apromore.dao.model.Process;
import org.apromore.dao.model.User;
import org.apromore.dao.model.Group;
import org.apromore.dao.model.Group.Type;
import org.apromore.portal.common.Item;
import org.apromore.portal.common.ItemType;
import org.apromore.portal.accesscontrol.model.Assignment;
import org.apromore.portal.common.notification.Notification;
import org.apromore.portal.model.*;
import org.apromore.service.*;
import org.apromore.util.AccessType;

/**
 * Controller for handling transfer ownership
 * Corresponds to resources/user-admin/transfer-ownership.zul
 */
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class DeleteUserController extends SelectorComposer<Window> {

    private static Logger LOGGER = LoggerFactory.getLogger(DeleteUserController.class);

    @WireVariable("workspaceService")
    private WorkspaceService workspaceService;

    @WireVariable("securityService")
    private SecurityService securityService;

    @WireVariable("authorizationService")
    private AuthorizationService authorizationService;

    @WireVariable("userService")
    private UserService userService;

    private Map<String, Object> argMap = (Map<String, Object>) Executions.getCurrent().getArg();
    private User selectedUser = (User) argMap.get("selectedUser");
    private String selectedUserName;
    private Window container;

    private Item selectedItem;
    private Integer selectedItemId;
    private String selectedItemName;

    private Map<Group, AccessType> groupAccessTypeMap;
    private ListModelList<User> transferToModel;
    private ListModelList<Assignment> assignmentModel;
    private ListModelList<Item> ownedModel;

    @Wire("#transferFromLabel")
    Label transferFromLabel;

    @Wire("#deletedUserLabel")
    Label deletedUserLabel;

    @Wire("#transferToCombobox")
    Combobox transferToCombobox;

    @Wire("#candidateAssigneeAdd")
    Button candidateAssigneeAdd;

    @Wire("#btnApply")
    Button btnApply;

    @Wire("#ownedListbox")
    Listbox ownedListbox;

    @Wire("#assignmentListbox")
    Listbox assignmentListbox;

    @Wire("#deleteOptionTransfer")
    Radio deleteOptionTransfer;

    @Wire("#deleteOptionPurge")
    Radio deleteOptionPurge;

    public DeleteUserController() throws Exception {
        pullArgs();
    }

    private void pullArgs() throws Exception {
        Map<String, Object> argMap = (Map<String, Object>) Executions.getCurrent().getArg();
        selectedUser = (User) argMap.get("selectedUser");
        selectedUserName = selectedUser.getUsername();
    }
    
    @Override
    public void doAfterCompose(Window win) throws Exception {
        super.doAfterCompose(win);
        container = win;
        transferFromLabel.setValue(selectedUserName);
        deletedUserLabel.setValue(selectedUserName);
        loadTransferTo();
        loadOwnedList();
    }

    private void destroy() {
        getSelf().detach();
    }

    @SuppressWarnings("unchecked")
    private void loadTransferTo() {
        List<User> users = userService.findAllUsers();
        transferToModel = new ListModelList<>(users, false);
        transferToModel.setMultiple(false);
        transferToCombobox.setModel(ListModels.toListSubModel(transferToModel, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                String input = (String) o1;
                User user = (User) o2;
                return user.getUsername().toLowerCase().contains(input.toLowerCase()) ? 0 : 1;
            }
        }, 20));
    }

    private void loadAssignments(Map<Group, AccessType> groupAccessTypeMap) {
        List<Assignment> assignments = new ArrayList<Assignment>();

        for (Map.Entry<Group, AccessType> entry : groupAccessTypeMap.entrySet()) {
            Group group = entry.getKey();
            AccessType accessType = entry.getValue();
            String rowGuid = group.getRowGuid();
            Assignment assignment = new Assignment(group.getName(), rowGuid, Type.USER, accessType.getLabel());
            assignments.add(assignment);
        }
        assignmentModel = new ListModelList<>(assignments, false);
        assignmentListbox.setMultiple(false);
        assignmentListbox.setModel(assignmentModel);
    }

    private void loadOwnedList() {
        List<Item> items = getOwnedList(selectedUser);
        ownedModel = new ListModelList<>(items, false);
        ownedListbox.setMultiple(false);
        ownedListbox.setModel(ownedModel);
    }

    private void clearAssignments() {
        assignmentModel = new ListModelList<>();
        assignmentListbox.setMultiple(false);
        assignmentListbox.setModel(assignmentModel);
    }

    private boolean isLogSelected() {
        if (selectedItem == null) {
            return false;
        }
        return selectedItem.getClass().equals(LogSummaryType.class);
    }

    private void setSelectedItem(Item selectedItem) {
        this.selectedItem = selectedItem;

        if (selectedItem == null) {
            clearAssignments();
            return;
        }
        ItemType selectedItemType = selectedItem.getType();
        selectedItemId = selectedItem.getId();
        if (selectedItemType == ItemType.FOLDER) {
            groupAccessTypeMap = authorizationService.getFolderAccessType(selectedItemId);
        } else if (selectedItemType == ItemType.MODEL) {
            groupAccessTypeMap = authorizationService.getProcessAccessType(selectedItemId);
        } else if (selectedItemType == ItemType.LOG) {
            groupAccessTypeMap = authorizationService.getLogAccessType(selectedItemId);
        } else {
            return;
        }
        loadAssignments(groupAccessTypeMap);
    }

    List<Item> getOwnedList(User user) {
        List<Folder> folders = workspaceService.getSingleOwnerFolderByUser(user);
        List<Log> logs = workspaceService.getSingleOwnerLogByUser(user);
        List<Process> processes = workspaceService.getSingleOwnerProcessByUser(user);

        List<Item> items = new ArrayList<Item>();
        for (Folder folder: folders) {
            Integer id = folder.getId();
            String name = folder.getName();
            // TO DO: Standardise date over all item types
            // Date date = folder.getDateModified();
            ItemType type = ItemType.FOLDER;
            Item item = new Item(id, name, type, "");
            items.add(item);
        }
        for (Log log: logs) {
            Integer id = log.getId();
            String name = log.getName();
            // TO DO: Standardise date over all item types
            // String date = log.getCreateDate();
            ItemType type = ItemType.LOG;
            Item item = new Item(id, name, type, "");
            items.add(item);
        }
        for (Process process: processes) {
            Integer id = process.getId();
            String name = process.getName();
            String date = process.getCreateDate();
            ItemType type = ItemType.MODEL;
            Item item = new Item(id, name, type, DateTimeUtils.normalize(date));
            items.add(item);
        }
        return items;
    }

    private void applyTransfer() {
        Set<User> users = transferToModel.getSelection();
        if (users != null && users.size() == 1) {
            User targetUser = users.iterator().next();
            try {
                workspaceService.transferOwnership(selectedUser, targetUser);
                Notification.info("Transfer ownership is successfully applied");
                EventQueues.lookup(EventQueueTypes.TRANSFER_OWNERSHIP, EventQueues.DESKTOP, true)
                    .publish(new Event(EventQueueEvents.ON_TRANSFERRED, null, selectedUser));
            } catch(Exception e) {
                Notification.error("An error occurred while trying to transfer ownership");
            }
        } else {
            Notification.error("No target user is specified");
        }
    }

    private void purgeOwnedAssets() {
        try {
            workspaceService.deleteOwnerlessArtifact(selectedUser);
            Notification.info("All items owned by the deleted user are successfully deleted");
            EventQueues.lookup(EventQueueTypes.PURGE_ASSETS, EventQueues.DESKTOP, true)
                .publish(new Event(EventQueueEvents.ON_PURGED, null, selectedUser));
        } catch(Exception e) {
            Notification.error("An error occurred while trying to delete all the deleted user's items");
        }

    }

    @Listen("onSelect = #ownedListbox")
    public void onSelectOwnedListbox(SelectEvent event) throws Exception {
        Set<Item> items = event.getSelectedObjects();
        if (items.size() == 1) {
            setSelectedItem(items.iterator().next());
        } else {
            clearAssignments();
        }
    }

    @Listen("onClick = #btnApply")
    public void onClickBtnApply() {
        destroy();
        if (deleteOptionTransfer.isChecked()) {
            applyTransfer();
        }
        if (deleteOptionPurge.isChecked()) {
            purgeOwnedAssets();
        }
    }

    @Listen("onClick = #btnCancel")
    public void onClickBtnCancel() {
        destroy();
    }

}