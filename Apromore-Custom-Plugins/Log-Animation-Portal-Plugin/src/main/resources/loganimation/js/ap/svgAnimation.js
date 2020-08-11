
/**
 * Manage the frame index and frame time.
 * Frame indexes are sequential integers, e.g. 0, 1, 2...
 * Frame time is a point in time (from an origin) in seconds, e.g. 0, 0.1, 0.2, 0.5, 1, 2,...
 */
class TimeController {
    constructor() {
        this._startFrameIndex = 0;
        this._startFrameTime = 0;
        this._convertionFactor = 1; //conversion ratio between conceptual and real time points
    }

    getStartFrameIndex() {
        return this._startFrameIndex;
    }

    getStartFrameTime() {
        return this._startFrameTime;
    }

    getFrameTime(frameIndex) {
        return frameIndex*this._convertionFactor;
    }

    getFrameTimeString(frameIndex) {
        return frameIndex*this._convertionFactor + "s";
    }
}

/**
 * This object controls the format/styling for animation
 */
class FormatController {
    constructor() {
        this._tokenRaisedLevel = 0; // the gap between the token and the underlying line
        this._tokenColor = undefined;
    }

    getTokenRaisedLevel() {
        return this._tokenRaisedLevel;
    }

    getTokenColor() {
        return this._tokenColor;
    }
}

/**
 * This class is to convert from a sequence of frames to SVG animation elements
 */
class SVGAnimator {
    /**
     *
     * @param {Buffer} playBuffer
     * @param {AnimationModel} animationModel
     * @param {TimeController} timeController
     * @param {FormatController} formatController
     * @param {SVGDocument} svgDoc
     */
    constructor(playBuffer, animationModel, timeController, formatController,
                svgDoc) {
        this._playBuffer = playBuffer;
        this._elements = new Array(); // array of SVG elements
        this._animationModel = animationModel;
        this._timeController = timeController;
        this._formatController = formatController;
        this._svgDoc = svgDoc;
        setTimeout(this._animate, 1000);
    }

    /**
     * Create animation elements in SVG
     * @param {Frames} frames: set of frames to be animated
     */
    _animate() {
        if (!this._playBuffer.isEmpty()) return;
        let frames = this._playBuffer.readNextChunk();
        this._elements.forEach(element => this._svgDoc.remove(element));
        for (const caseFrames in frames.getCaseFrames()) {
            for (const elementId in caseFrames.getElementIds()) {
                let oneElementFrames = caseFrames.getElementFramesByElementId(elementId);
                let elementId = oneElementFrames.getElementId();
                let frameIndexes = oneElementFrames.getFrameIndexes();

                let element = this._animationModel.getAnimationElement(elementId);
                let elementStartTime = this._timeController.getFrameTime(frameIndexes[0]);
                let elementEndTime = this._timeController.getFrameTime(frameIndexes[frameIndexes.length() - 1]);

                this._elements.push(this._createMarker(element.getPath(), elementStartTime, elementEndTime,
                                                        this._formatController.getTokenRaisedLevel(),
                                                        this._formatController.getTokenColor()));
            }
        }

        this._elements.forEach(element => this._svgDoc.appendChild(element));
    }

    _createMarker (path, begin, dur, raisedLevel, color) {
        let marker = document.createElementNS(SVG_NS, 'g')
        marker.setAttributeNS(null, 'stroke', 'none')

        let animateMotion = document.createElementNS(SVG_NS, 'animateMotion')
        animateMotion.setAttributeNS(null, 'begin', begin)
        animateMotion.setAttributeNS(null, 'dur', dur)
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
        circle.setAttributeNS(null, 'cy', raisedLevel)
        circle.setAttributeNS(null, 'r', 5)
        circle.setAttributeNS(null, 'fill', color)
        marker.appendChild(circle)

        return marker
    }


}