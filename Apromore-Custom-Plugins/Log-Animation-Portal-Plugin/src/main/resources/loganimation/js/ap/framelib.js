'use strict'

/**
 * Each ElementFrame represents a token on an element at a point in time
 */
class ElementFrame {
    constructor(elementId, timePoint) {
        this._elementId = elementId;
        this._timePoint = timePoint;
    }

    getElementId() {
        return this._elementId;
    }

    getTimePoint() {
        return this._timePoint;
    }
}

/**
 * A sequence of ElementFrame objects belonging to an elementId over a period of time
 */
class ElementFrames {
    constructor() {
        this._elementId = undefined;
        this._elementFrames = new Array();
        this._timePoints = new Array();
    }

    getElementFrames() {
        return this._elementFrames;
    }

    add(elementFrame) {
        if (elementFrame instanceof ElementFrame) {
            if (this._elementId == undefined) {
                this._elementId = elementFrame.getElementId();

            }
            if (this._elementId === elementFrame.getElementId()) {
                this._elementFrames.push(elementFrame);
                this._timePoints.push(elementFrame.getTimePoint());
            }
        }
    }

    getElementId() {
        return this._elementId;
    }

    getTimepoints() {
        return this._timePoints;
    }
}


/**
 * A frame for a case which contains multiple ElementFrame object
 * For example, a CaseFrame has multiple tokens on a number of
 * sequence flows after an AND split gateway
 */
class CaseFrame {
    constructor(caseId, timePoint) {
        this._caseId = caseId;
        this._timePoint = timePoint;
        this._elementFrames = new Array();
    }

    getCaseId() {
        return this._caseId;
    }

    getTimePoint() {
        return this._timePoint;
    }

    getElementFrames() {
        return this._elementFrames;
    }

    addElementFrame(elementFrame) {
        if (elementFrame instanceof ElementFrame) {
            this._elementFrames.push(elementFrame);
        }
    }
}

/**
 * A sequence of CaseFrame objects belonging to a CaseId over a period of time
 * It also summarizes the ElementFrame for each elementId over this period
 */
class CaseFrames {
    constructor() {
        this._caseId = undefined;
        this._caseFrames = new Array();
        this._elementFrames = new Map(); // elementId => ElementFrames
    }

    /**
     *
     * @param {CaseFrame} caseFrame
     */
    add(caseFrame) {
        if (caseFrame instanceof CaseFrame) {
            if (this._caseId === undefined) {
                this._caseId = caseFrame.getCaseId();
            }

            if (caseFrame.getCaseId() === this._caseId) {
                this._caseFrames.push(caseFrame);
                for (const elementFrame in caseFrame.getElementFrames()) {
                    if (!this._elementFrames.has(elementFrame.getElementId())) {
                        this._elementFrames.set(elementFrame.getElementId(), new Array());
                    }
                    this._elementFrames.get(elementFrame.getElementId).push(elementFrame);
                }
            }
        }
    }

    getCaseFrames() {
        return this._caseFrames;
    }

    getCaseId() {
        return this._caseId;
    }

    getElementFrames() {
        return this._elementFrames.values;
    }

    getElementIds() {
        return this._elementFrames.keys();
    }

    getElementFramesByElementId(elementId) {
        return this._elementFrames.get(elementId);
    }
}

/**
 * An animation frame contains many CaseFrame objects at a point in time
 * These CaseFrame objects can be of different cases
 */
class Frame {
    /**
     * _caseFrames: map caseId => {CaseFrames}
     */
    constructor (timePoint) {
        this._timePoint = timePoint;
        this._caseFrames = new Map();
    }

    getTimePoint() {
        return this._timePoint;
    }

    /**
     *
     * @param {String} caseId
     * @param {CaseFrame} caseFrame
     */
    addCaseFrame(caseFrame) {
        if (caseFrame instanceof CaseFrame) {
            this._caseFrames.set(caseFrame.getCaseId(), caseFrame);
        }
    }

    getCaseFrames() {
        return this._caseFrames.values();
    }

    getCaseFramesByCaseId(caseId) {
        return this._caseFrames.get(caseId);
    }

    getCaseIds() {
        return this._caseFrames.keys();
    }
}

/**
 * A sequence of Frame objects
 * It also summarizes CaseFrame objects by caseId from the Frame objects
 */
class Frames {
    /**
     * _frames: array of {Frame}
     * _caseFrames: map caseId => {CaseFrames}
     */
    constructor(startTimePoint, endTimePoint) {
        this._startTimePoint = startTimePoint;
        this._endTimePoint = endTimePoint;
        this._frames = new Array();
        this._caseFrames = new Map();
    }

    getStartTimePoint() {
        return this._startTimePoint;
    }

    getEndTimePoint() {
        return this._endTimePoint;
    }

    /**
     *
     * @param {Frame} frame
     */
    addFrame(frame) {
        if (frame instanceof Frame) {
            this._frames.push(frame);
            for (const caseId in frame.getCaseIds()) {
                if (!this._caseFrames.has(caseId)) {
                    this._caseFrames.set(caseId, new CaseFrames());
                }
                this._caseFrames.get(caseId).add(caseId, frame.getCaseFramesByCaseId(caseId));
            }
        }
    }

    getFrames () {
        return this._frames;
    }

    getCaseFrames() {
        return this._caseFrames.values();
    }

    getCaseFramesByCaseId(caseId) {
        return this._caseFrames.get(caseId);
    }

    getCaseIds() {
        return this._caseFrames.keys();
    }
}

/**
 * This class is to convert from a sequence of frames to SVG animation elements
 */
class SVGAnimationCreator {
    /**
     *
     * @param {Frames} frames
     */
    constructor(frames) {
        this._frames = frames;
        this._elements = new Array(); // array of SVG elements
    }

    createAnimation() {
        for (const caseFrames in this._frames.getCaseFrames()) {
            for (elementId in caseFrames.getElementIds()) {
                let oneElementFrames = caseFrames.getElementFramesByElementId(elementId);
                let elementId = oneElementFrames.getElementId();
                let timePoints = oneElementFrames.getTimepoints();
                this._elements.push(this._createMarker());
            }
        }
    }

    _createMarker (t, dt, path, begin, dur, offset) {
        let marker = document.createElementNS(SVG_NS, 'g')
        marker.setAttributeNS(null, 'stroke', 'none')

        let animateMotion = document.createElementNS(SVG_NS, 'animateMotion')
        animateMotion.setAttributeNS(null, 'begin', begin / dt)
        animateMotion.setAttributeNS(null, 'dur', dur / dt)
        animateMotion.setAttributeNS(null, 'fill', 'freeze')
        animateMotion.setAttributeNS(null, 'path', path)
        animateMotion.setAttributeNS(null, 'rotate', 'auto')
        marker.appendChild(animateMotion)

        let circle = document.createElementNS(SVG_NS, 'circle')
        // Bruce 15/6/2015: add offset as a parameter, add 'rotate' attribute, put markers of different logs on separate lines.
        // let offset = 2;
        // circle.setAttributeNS(null, "cx", offset * Math.sin(this.offsetAngle));
        // circle.setAttributeNS(null, "cy", offset * Math.cos(this.offsetAngle));
        circle.setAttributeNS(null, 'cx', 0)
        circle.setAttributeNS(null, 'cy', offset)
        circle.setAttributeNS(null, 'r', 5)
        circle.setAttributeNS(null, 'fill', this.color)
        marker.appendChild(circle)

        let text = document.createElementNS(SVG_NS, 'text')
        // text.setAttributeNS(null,"x",offset * Math.sin(this.offsetAngle));
        // text.setAttributeNS(null,"y",offset * Math.cos(this.offsetAngle) - 10);
        text.setAttributeNS(null, 'x', 0)
        text.setAttributeNS(null, 'y', offset - 10)
        text.setAttributeNS(
            null,
            'style',
            'fill: black; text-anchor: middle' +
            (this.controller.caseLabelsVisible ? '' : '; visibility: hidden'),
        )
        text.appendChild(document.createTextNode(this.label))
        marker.appendChild(text)

        return marker
    }
}



