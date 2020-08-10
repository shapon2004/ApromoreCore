
/**
 * Manage the conceptual time and real animation time.
 * Conceptual time consists of sequential time points in integers, e.g. 0, 1, 2...
 * Real time consists of real time points in seconds, e.g. 0, 0.1, 0.2, 0.5, 1, 2,...
 * Conceptual and real time should be separated for adjustment, e.g. the animation speed
 */
class TimeController {
    constructor() {
        this._startConceptTimePoint = 0;
        this._startRealTimePoint = 0;
        this._convertionFactor = 1; //conversion ratio between conceptual and real time points
    }

    getStartConceptTimePoint() {
        return this._startConceptTimePoint;
    }

    getStartReadTimePoint() {
        return this._startRealTimePoint;
    }

    getRealTimePoint(conceptTimePoint) {
        return conceptTimePoint*this._convertionFactor;
    }

    getRealTimePointText(conceptTimePoint) {
        return conceptTimePoint*this._convertionFactor + "s";
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
     * @param {Frames} frames
     * @param {AnimationModel} animationModel
     * @param {TimeController} timeController
     * @param {FormatController} formatController
     */
    constructor(frames, animationModel, timeController, formatController) {
        this._frames = undefined;
        this._elements = new Array(); // array of SVG elements
        this._animationModel = animationModel;
        this._timeController = timeController;
        this._formatController = formatController;
    }

    /**
     * Create animation elements in SVG
     * @param {Frames} frames: set of frames to be animated
     */
    createAnimation(frames) {
        if (!frames) return;
        this._frames = frames;
        for (const caseFrames in this._frames.getCaseFrames()) {
            for (const elementId in caseFrames.getElementIds()) {
                let oneElementFrames = caseFrames.getElementFramesByElementId(elementId);
                let elementId = oneElementFrames.getElementId();
                let timePoints = oneElementFrames.getTimepoints();

                let element = this._animationModel.getAnimationElement(elementId);
                let realStartTimePoint = this._timeController.getRealTimePoint(timePoints[0]);
                let realEndTimePoint = this._timeController.getRealTimePoint(timePoints[timePoints.length() - 1]);

                this._elements.push(this._createMarker(element.getPath(), realStartTimePoint, realEndTimePoint,
                                                        this._formatController.getTokenRaisedLevel(),
                                                        this._formatController.getTokenColor()));
            }
        }
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