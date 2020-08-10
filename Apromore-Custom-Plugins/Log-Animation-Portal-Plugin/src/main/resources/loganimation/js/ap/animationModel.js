/**
 * This class represents a process model or graph for the anmation to take place on.
 * It is separated from the Model or Graph object used by third-party libraries such as bpmn.io or cytoscape.js
 */
class AnimationModel {
    constructor() {
        this._nodes = new Map(); // nodeId -> Node
        this._arcs = new Map(); // arcId -> Flow
    }

    getAnimationElement(elementId) {
        let element = this.getNode(elementId);
        if (!element) {
            element = this.getArc(elementId);
        }
        return element;
    }

    addNode(node) {
        if (node instanceof Node) {
            this._nodes.set(node.getId(), node)
        }
    }

    getNode(nodeId) {
        return this._nodes.get(nodeId);
    }

    getNodes() {
        return this._nodes.values;
    }

    addArc(arc) {
        if (arc instanceof Arc) {
            this._arcs.set(arc.getId(), arc)
        }
    }

    getArc(arcId) {
        return this._arcs.get(arcId);
    }

    getArs() {
        return this._arcs.values;
    }
}

class AnimationElement {
    constructor(id) {
        this._id = id;
        this._attributes = new Attributes();
    }

    getId() {
        return this._id;
    }

    getPath() {

    }
}

class Node extends AnimationElement {
    constructor(nodeId) {
        super(nodeId);
    }

    getPath() {

    }
}

class Arc extends AnimationElement {
    constructor(arcId) {
        super(arcId);
    }

    getPath() {

    }
}

class Attributes {
    constructor() {
        this._attributes = new Map;
    }

    setAttributes(key, value) {
        this._attributes.set(key, value);
    }

    getAttribute(key) {
        return this._attributes.get(key);
    }

    removeAttribute(key) {
        this._attributes.delete(key);
    }
}