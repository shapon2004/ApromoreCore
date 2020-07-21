Ap.userAdmin = Ap.userAdmin || {}
Ap.userAdmin.editUser = (userName, index) => {
  zAu.send(new zk.Event(zk.Widget.$('$userEditBtn'), 'onExecute', userName));
}

Ap.userAdmin.editGroup = (groupName, index) => {
  zAu.send(new zk.Event(zk.Widget.$('$groupEditBtn'), 'onExecute', groupName));
}