/* Previously copied from the top-level site.properties file at compile time (which is bad idea since site.properties can change at runtime); now a constant */
if(!ORYX) var ORYX = {};
if(!ORYX.CONFIG) ORYX.CONFIG = {};
if(!ORYX.I18N) ORYX.I18N = {};
if(!ORYX.I18N.View) ORYX.I18N.View = {};
ORYX.CONFIG.EDITOR_PATH = "/editor";
/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/

/**
 * The super class for all classes in ORYX. Adds some OOP feeling to javascript.
 * See article "Object Oriented Super Class Method Calling with JavaScript" on
 * http://truecode.blogspot.com/2006/08/object-oriented-super-class-method.html
 * for a documentation on this. Fairly good article that points out errors in
 * Douglas Crockford's inheritance and super method calling approach.
 * Worth reading.
 * @class Clazz
 */
var Clazz = function() {};

/**
 * Empty constructor.
 * @methodOf Clazz.prototype
 */
Clazz.prototype.construct = function() {};

/**
 * Can be used to build up inheritances of classes.
 * @example
 * var MyClass = Clazz.extend({
 *   construct: function(myParam){
 *     // Do sth.
 *   }
 * });
 * var MySubClass = MyClass.extend({
 *   construct: function(myParam){
 *     // Use this to call constructor of super class
 *     arguments.callee.$.construct.apply(this, arguments);
 *     // Do sth.
 *   }
 * });
 * @param {Object} def The definition of the new class.
 */
Clazz.extend = function(def) {
    var classDef = function() {
        if (arguments[0] !== Clazz) { this.construct.apply(this, arguments); }
    };
    
    var proto = new this(Clazz);
    var superClass = this.prototype;
    
    for (var n in def) {
        var item = def[n];                        
        if (item instanceof Function) item.$ = superClass;
        proto[n] = item;
    }

    classDef.prototype = proto;
    
    //Give this new class the same static extend method    
    classDef.extend = this.extend;        
    return classDef;
};/**
 * Copyright (c) 2008
 * Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/

if (!ORYX) {
  var ORYX = {};
}

/**
 * @namespace Oryx name space for different utility methods
 * @name ORYX.Utils
*/
ORYX.Utils = {
    // TODO Implement namespace awareness on attribute level.
    /**
     * graft() function
     * Originally by Sean M. Burke from interglacial.com, altered for usage with
     * SVG and namespace (xmlns) support. Be sure you understand xmlns before
     * using this funtion, as it creates all grafted elements in the xmlns
     * provided by you and all element's attribures in default xmlns. If you
     * need to graft elements in a certain xmlns and wish to assign attributes
     * in both that and another xmlns, you will need to do stepwise grafting,
     * adding non-default attributes yourself or you'll have to enhance this
     * function. Latter, I would appreciate: martin�apfelfabrik.de
     * @param {Object} namespace The namespace in which
     *                    elements should be grafted.
     * @param {Object} parent The element that should contain the grafted
     *                    structure after the function returned.
     * @param {Object} t the crafting structure.
     * @param {Object} doc the document in which grafting is performed.
     */
    graft: function (namespace, parent, t, doc) {

        doc = (doc || (parent && parent.ownerDocument) || document);
        var e;
        if (t === undefined) {
            throw "Can't graft an undefined value";
        } else if (t.constructor == String) {
            e = doc.createTextNode(t);
        } else {
            for (var i = 0; i < t.length; i++) {
                if (i === 0 && t[i].constructor == String) {
                    var snared;
                    snared = t[i].match(/^([a-z][a-z0-9]*)\.([^\s\.]+)$/i);
                    if (snared) {
                        e = doc.createElementNS(namespace, snared[1]);
                        e.setAttributeNS(null, 'class', snared[2]);
                        continue;
                    }
                    snared = t[i].match(/^([a-z][a-z0-9]*)$/i);
                    if (snared) {
                        e = doc.createElementNS(namespace, snared[1]);  // but no class
                        continue;
                    }

                    // Otherwise:
                    e = doc.createElementNS(namespace, "span");
                    e.setAttribute(null, "class", "namelessFromLOL");
                }

                if (t[i] === undefined) {
                    throw "Can't graft an undefined value in a list!";
                } else if (t[i].constructor == String || t[i].constructor == Array) {
                    this.graft(namespace, e, t[i], doc);
                } else if (t[i].constructor == Number) {
                    this.graft(namespace, e, t[i].toString(), doc);
                } else if (t[i].constructor == Object) {
                    // hash's properties => element's attributes
                    for (var k in t[i]) {
                        e.setAttributeNS(null, k, t[i][k]);
                    }
                } else {

                }
            }
        }
        if (parent) {
            parent.appendChild(e);
        } else {

        }
        return e; // return the topmost created node
    },

    provideId: function () {
        var res = [], hex = '0123456789ABCDEF';

        for (var i = 0; i < 36; i++) res[i] = Math.floor(Math.random() * 0x10);

        res[14] = 4;
        res[19] = (res[19] & 0x3) | 0x8;

        for (var i = 0; i < 36; i++) res[i] = hex[res[i]];

        res[8] = res[13] = res[18] = res[23] = '-';

        return "oryx_" + res.join('');
    }
};


/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/

if(!ORYX) var ORYX = {};

/**
 * The ORYX.Log logger.
 */
ORYX.Log = {
    // oryx constants.
    ORYX_LOGLEVEL: 5,
    ORYX_LOGLEVEL_TRACE: 5,
	ORYX_LOGLEVEL_DEBUG: 4,
	ORYX_LOGLEVEL_INFO: 3,
	ORYX_LOGLEVEL_WARN: 2,
	ORYX_LOGLEVEL_ERROR: 1,
	ORYX_LOGLEVEL_FATAL: 0,
	ORYX_CONFIGURATION_DELAY: 100,
	ORYX_CONFIGURATION_WAIT_ATTEMPTS: 10,

    __appenders: [
        { append: function(message) {
                console.log(message); }}
    ],

	trace: function() {	if(ORYX.Log.ORYX_LOGLEVEL >= ORYX.Log.ORYX_LOGLEVEL_TRACE)
        ORYX.Log.__log('TRACE', arguments); },
    debug: function() { if(ORYX.Log.ORYX_LOGLEVEL >= ORYX.Log.ORYX_LOGLEVEL_DEBUG)
        ORYX.Log.__log('DEBUG', arguments); },
    info: function() { if(ORYX.Log.ORYX_LOGLEVEL >= ORYX.Log.ORYX_LOGLEVEL_INFO)
        ORYX.Log.__log('INFO', arguments); },
    warn: function() { if(ORYX.Log.ORYX_LOGLEVEL >= ORYX.Log.ORYX_LOGLEVEL_WARN)
        ORYX.Log.__log('WARN', arguments); },
    error: function() { if(ORYX.Log.ORYX_LOGLEVEL >= ORYX.Log.ORYX_LOGLEVEL_ERROR)
        ORYX.Log.__log('ERROR', arguments); },
    fatal: function() { if(ORYX.Log.ORYX_LOGLEVEL >= ORYX.Log.ORYX_LOGLEVEL_FATAL)
        ORYX.Log.__log('FATAL', arguments); },

    __log: function(prefix, messageParts) {

        messageParts[0] = (new Date()).getTime() + " "
            + prefix + " " + messageParts[0];
        var message = this.printf.apply(null, messageParts);

        ORYX.Log.__appenders.each(function(appender) {
            appender.append(message);
        });
    },

    addAppender: function(appender) {
        ORYX.Log.__appenders.push(appender);
    },

    printf: function() {
		var result = arguments[0];
		for (var i=1; i<arguments.length; i++)
			result = result.replace('%' + (i-1), arguments[i]);
		return result;
	}
};



/**
 * Copyright (c) 2010
 * Signavio GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/
if(!ORYX) var ORYX = {};

if(!ORYX.CONFIG) ORYX.CONFIG = {};

/**
 * This file contains URI constants that may be used for XMLHTTPRequests.
 */

ORYX.CONFIG.ROOT_PATH =					(ORYX.CONFIG.SERVER_HANDLER_ROOT_PREFIX) ? ORYX.CONFIG.SERVER_HANDLER_ROOT_PREFIX + "/editor/" : "../editor/"; //TODO: Remove last slash!!
ORYX.CONFIG.EXPLORER_PATH =				(ORYX.CONFIG.SERVER_HANDLER_ROOT_PREFIX) ? ORYX.CONFIG.SERVER_HANDLER_ROOT_PREFIX + "/explorer/" : "../explorer";
ORYX.CONFIG.LIBS_PATH =					(ORYX.CONFIG.SERVER_HANDLER_ROOT_PREFIX) ? ORYX.CONFIG.SERVER_HANDLER_ROOT_PREFIX + "/libs/" : "../libs";
ORYX.PATH = ORYX.CONFIG.ROOT_PATH;

/**
 * Regular Config
 */
//ORYX.CONFIG.SERVER_HANDLER_ROOT = 		(ORYX.CONFIG.SERVER_HANDLER_ROOT_PREFIX) ? ORYX.CONFIG.SERVER_HANDLER_ROOT_PREFIX + "/p" : "../p";
ORYX.CONFIG.SERVER_HANDLER_ROOT = 		ORYX.CONFIG.SERVER_HANDLER_ROOT_PREFIX;
ORYX.CONFIG.SERVER_EDITOR_HANDLER =		ORYX.CONFIG.SERVER_HANDLER_ROOT + "/editor";
ORYX.CONFIG.SERVER_MODEL_HANDLER =		ORYX.CONFIG.SERVER_HANDLER_ROOT + "/model";
ORYX.CONFIG.STENCILSET_HANDLER = 		ORYX.CONFIG.SERVER_HANDLER_ROOT + "/editor_stencilset?embedsvg=true&url=true&namespace=";
ORYX.CONFIG.STENCIL_SETS_URL = 			ORYX.CONFIG.SERVER_HANDLER_ROOT + "/editor_stencilset";
ORYX.CONFIG.PLUGINS_CONFIG =			ORYX.CONFIG.SERVER_HANDLER_ROOT + "/bpmneditor_plugins";
ORYX.CONFIG.SYNTAXCHECKER_URL =			ORYX.CONFIG.SERVER_HANDLER_ROOT + "/syntaxchecker";

ORYX.CONFIG.SS_EXTENSIONS_FOLDER =		ORYX.CONFIG.ROOT_PATH + "stencilsets/extensions/";
ORYX.CONFIG.SS_EXTENSIONS_CONFIG =		ORYX.CONFIG.SERVER_HANDLER_ROOT + "/editor_ssextensions";
ORYX.CONFIG.ORYX_NEW_URL =				"/new";
ORYX.CONFIG.BPMN_LAYOUTER =				ORYX.CONFIG.ROOT_PATH + "bpmnlayouter";

ORYX.CONFIG.GLOSSARY_PATH = "/glossary";
ORYX.CONFIG.GLOSSARY_PROPERTY_SUFFIX = "_glossary";
ORYX.CONFIG.GLOSSARY_PROPERTY_DIRTY_SUFFIX = "_glossary_dirty";

/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/
if(!ORYX) var ORYX = {};

if(!ORYX.CONFIG) ORYX.CONFIG = {};

/**
 * Signavio specific variables
 */
ORYX.CONFIG.BACKEND_SWITCH 		= 		true;
ORYX.CONFIG.PANEL_LEFT_COLLAPSED 	= 	true;
ORYX.CONFIG.PANEL_LEFT_WIDTH 	= 		200;
ORYX.CONFIG.PANEL_RIGHT_COLLAPSED 	= 	true;
ORYX.CONFIG.PANEL_RIGHT_WIDTH	= 		200;
ORYX.CONFIG.APPNAME = 					'Signavio';
ORYX.CONFIG.WEB_URL = 					"explorer";

ORYX.CONFIG.PDF_EXPORT_URL = '/bpmneditor' + '/editor/pdf';
ORYX.CONFIG.BIMP_URL = "http://bimp.cs.ut.ee/uploadsignavio";
ORYX.CONFIG.DIAGRAM_PRINTER_URL = "/printsvg";
ORYX.CONFIG.LICENSE_URL = "/LICENSE";

ORYX.CONFIG.BLANK_IMAGE = ORYX.CONFIG.LIBS_PATH + '/ext-2.0.2/resources/images/default/s.gif';


/* Show grid line while dragging */
ORYX.CONFIG.SHOW_GRIDLINE = 			false;

/* Editor-Mode */
ORYX.CONFIG.MODE_READONLY =				"readonly";
ORYX.CONFIG.MODE_FULLSCREEN =			"fullscreen";
ORYX.CONFIG.WINDOW_HEIGHT = 			550;
ORYX.CONFIG.PREVENT_LOADINGMASK_AT_READY = true;

/* Plugins */
ORYX.CONFIG.PLUGINS_ENABLED =			true;
ORYX.CONFIG.PLUGINS_FOLDER =			"Plugins/";
ORYX.CONFIG.BPMN20_SCHEMA_VALIDATION_ON = true;

/* Namespaces */
ORYX.CONFIG.NAMESPACE_ORYX =			"http://www.b3mn.org/oryx";
ORYX.CONFIG.NAMESPACE_SVG =				"http://www.w3.org/2000/svg";

/* UI */
ORYX.CONFIG.CANVAS_WIDTH =				1485;
ORYX.CONFIG.CANVAS_HEIGHT =				1050;
ORYX.CONFIG.CANVAS_RESIZE_INTERVAL =	300;
ORYX.CONFIG.SELECTED_AREA_PADDING =		4;
ORYX.CONFIG.CANVAS_BACKGROUND_COLOR =	"none";
ORYX.CONFIG.GRID_DISTANCE =				30;
ORYX.CONFIG.GRID_ENABLED =				true;
ORYX.CONFIG.ZOOM_OFFSET =				0.1;
ORYX.CONFIG.DEFAULT_SHAPE_MARGIN =		60;
ORYX.CONFIG.SCALERS_SIZE =				7;
ORYX.CONFIG.MINIMUM_SIZE =				20;
ORYX.CONFIG.MAXIMUM_SIZE =				10000;
ORYX.CONFIG.OFFSET_MAGNET =				15;
ORYX.CONFIG.OFFSET_EDGE_LABEL_TOP =		8;
ORYX.CONFIG.OFFSET_EDGE_LABEL_BOTTOM =	8;
ORYX.CONFIG.OFFSET_EDGE_BOUNDS =		5;
ORYX.CONFIG.COPY_MOVE_OFFSET =			30;
ORYX.CONFIG.BORDER_OFFSET =				14;
ORYX.CONFIG.MAX_NUM_SHAPES_NO_GROUP	=	12;
ORYX.CONFIG.SHAPEMENU_CREATE_OFFSET_CORNER = 30;
ORYX.CONFIG.SHAPEMENU_CREATE_OFFSET = 45;

/* Shape-Menu Align */
ORYX.CONFIG.SHAPEMENU_RIGHT =			"Oryx_Right";
ORYX.CONFIG.SHAPEMENU_BOTTOM =			"Oryx_Bottom";
ORYX.CONFIG.SHAPEMENU_LEFT =			"Oryx_Left";
ORYX.CONFIG.SHAPEMENU_TOP =				"Oryx_Top";

/* Morph-Menu Item */
ORYX.CONFIG.MORPHITEM_DISABLED =		"Oryx_MorphItem_disabled";

/* Property type names */
ORYX.CONFIG.TYPE_STRING =				"string";
ORYX.CONFIG.TYPE_BOOLEAN =				"boolean";
ORYX.CONFIG.TYPE_INTEGER =				"integer";
ORYX.CONFIG.TYPE_FLOAT =				"float";
ORYX.CONFIG.TYPE_COLOR =				"color";
ORYX.CONFIG.TYPE_DATE =					"date";
ORYX.CONFIG.TYPE_CHOICE =				"choice";
ORYX.CONFIG.TYPE_URL =					"url";
ORYX.CONFIG.TYPE_DIAGRAM_LINK =			"diagramlink";
ORYX.CONFIG.TYPE_COMPLEX =				"complex";
ORYX.CONFIG.TYPE_TEXT =					"text";
ORYX.CONFIG.TYPE_EPC_FREQ = 			"epcfrequency";
ORYX.CONFIG.TYPE_GLOSSARY_LINK =		"glossarylink";

/* Vertical line distance of multiline labels */
ORYX.CONFIG.LABEL_LINE_DISTANCE =		2;
ORYX.CONFIG.LABEL_DEFAULT_LINE_HEIGHT =	12;

/* Open Morph Menu with Hover */
ORYX.CONFIG.ENABLE_MORPHMENU_BY_HOVER = false;

/* Editor constants come here */
ORYX.CONFIG.EDITOR_ALIGN_BOTTOM =		0x01;
ORYX.CONFIG.EDITOR_ALIGN_MIDDLE =		0x02;
ORYX.CONFIG.EDITOR_ALIGN_TOP =			0x04;
ORYX.CONFIG.EDITOR_ALIGN_LEFT =			0x08;
ORYX.CONFIG.EDITOR_ALIGN_CENTER =		0x10;
ORYX.CONFIG.EDITOR_ALIGN_RIGHT =		0x20;
ORYX.CONFIG.EDITOR_ALIGN_SIZE =			0x30;

/* Event types */
ORYX.CONFIG.EVENT_MOUSEDOWN =			"mousedown";
ORYX.CONFIG.EVENT_MOUSEUP =				"mouseup";
ORYX.CONFIG.EVENT_MOUSEOVER =			"mouseover";
ORYX.CONFIG.EVENT_MOUSEOUT =			"mouseout";
ORYX.CONFIG.EVENT_MOUSEMOVE =			"mousemove";
ORYX.CONFIG.EVENT_DBLCLICK =			"dblclick";
ORYX.CONFIG.EVENT_KEYDOWN =				"keydown";
ORYX.CONFIG.EVENT_KEYUP =				"keyup";
ORYX.CONFIG.EVENT_LOADED =				"editorloaded";
ORYX.CONFIG.EVENT_EXECUTE_COMMANDS =		"executeCommands";
ORYX.CONFIG.EVENT_STENCIL_SET_LOADED =		"stencilSetLoaded";
ORYX.CONFIG.EVENT_SELECTION_CHANGED =		"selectionchanged";
ORYX.CONFIG.EVENT_SHAPEADDED =				"shapeadded";
ORYX.CONFIG.EVENT_SHAPEREMOVED =			"shaperemoved";
ORYX.CONFIG.EVENT_PROPERTY_CHANGED =		"propertyChanged";
ORYX.CONFIG.EVENT_DRAGDROP_START =			"dragdrop.start";
ORYX.CONFIG.EVENT_SHAPE_MENU_CLOSE =		"shape.menu.close";
ORYX.CONFIG.EVENT_DRAGDROP_END =			"dragdrop.end";
ORYX.CONFIG.EVENT_RESIZE_START =			"resize.start";
ORYX.CONFIG.EVENT_RESIZE_END =				"resize.end";
ORYX.CONFIG.EVENT_DRAGDOCKER_DOCKED =		"dragDocker.docked";
ORYX.CONFIG.EVENT_HIGHLIGHT_SHOW =			"highlight.showHighlight";
ORYX.CONFIG.EVENT_HIGHLIGHT_HIDE =			"highlight.hideHighlight";
ORYX.CONFIG.EVENT_LOADING_ENABLE =			"loading.enable";
ORYX.CONFIG.EVENT_LOADING_DISABLE =			"loading.disable";
ORYX.CONFIG.EVENT_LOADING_STATUS =			"loading.status";
ORYX.CONFIG.EVENT_OVERLAY_SHOW =			"overlay.show";
ORYX.CONFIG.EVENT_OVERLAY_HIDE =			"overlay.hide";
ORYX.CONFIG.EVENT_ARRANGEMENT_TOP =			"arrangement.setToTop";
ORYX.CONFIG.EVENT_ARRANGEMENT_BACK =		"arrangement.setToBack";
ORYX.CONFIG.EVENT_ARRANGEMENT_FORWARD =		"arrangement.setForward";
ORYX.CONFIG.EVENT_ARRANGEMENT_BACKWARD =	"arrangement.setBackward";
ORYX.CONFIG.EVENT_PROPWINDOW_PROP_CHANGED =	"propertyWindow.propertyChanged";
ORYX.CONFIG.EVENT_LAYOUT_ROWS =				"layout.rows";
ORYX.CONFIG.EVENT_LAYOUT_BPEL =				"layout.BPEL";
ORYX.CONFIG.EVENT_LAYOUT_BPEL_VERTICAL =    "layout.BPEL.vertical";
ORYX.CONFIG.EVENT_LAYOUT_BPEL_HORIZONTAL =  "layout.BPEL.horizontal";
ORYX.CONFIG.EVENT_LAYOUT_BPEL_SINGLECHILD = "layout.BPEL.singlechild";
ORYX.CONFIG.EVENT_LAYOUT_BPEL_AUTORESIZE =	"layout.BPEL.autoresize";
ORYX.CONFIG.EVENT_AUTOLAYOUT_LAYOUT =		"autolayout.layout";
ORYX.CONFIG.EVENT_UNDO_EXECUTE =			"undo.execute";
ORYX.CONFIG.EVENT_UNDO_ROLLBACK =			"undo.rollback";
ORYX.CONFIG.EVENT_BUTTON_UPDATE =           "toolbar.button.update";
ORYX.CONFIG.EVENT_LAYOUT = 					"layout.dolayout";
ORYX.CONFIG.EVENT_GLOSSARY_LINK_EDIT = 		"glossary.link.edit";
ORYX.CONFIG.EVENT_GLOSSARY_SHOW =			"glossary.show.info";
ORYX.CONFIG.EVENT_GLOSSARY_NEW =			"glossary.show.new";
ORYX.CONFIG.EVENT_DOCKERDRAG = 				"dragTheDocker";
ORYX.CONFIG.EVENT_SHOW_PROPERTYWINDOW =		"propertywindow.show";
ORYX.CONFIG.EVENT_ABOUT_TO_SAVE = "file.aboutToSave";

/* Selection Shapes Highlights */
ORYX.CONFIG.SELECTION_HIGHLIGHT_SIZE =				5;
ORYX.CONFIG.SELECTION_HIGHLIGHT_COLOR =				"#4444FF";
ORYX.CONFIG.SELECTION_HIGHLIGHT_COLOR2 =			"#9999FF";
ORYX.CONFIG.SELECTION_HIGHLIGHT_STYLE_CORNER = 		"corner";
ORYX.CONFIG.SELECTION_HIGHLIGHT_STYLE_RECTANGLE = 	"rectangle";
ORYX.CONFIG.SELECTION_VALID_COLOR =					"#00FF00";
ORYX.CONFIG.SELECTION_INVALID_COLOR =				"#FF0000";
ORYX.CONFIG.DOCKER_DOCKED_COLOR =		"#00FF00";
ORYX.CONFIG.DOCKER_UNDOCKED_COLOR =		"#FF0000";
ORYX.CONFIG.DOCKER_SNAP_OFFSET =		10;

/* Copy & Paste */
ORYX.CONFIG.EDIT_OFFSET_PASTE =			10;

/* Key-Codes */
ORYX.CONFIG.KEY_CODE_X = 				88;
ORYX.CONFIG.KEY_CODE_C = 				67;
ORYX.CONFIG.KEY_CODE_V = 				86;
ORYX.CONFIG.KEY_CODE_DELETE = 			46;
ORYX.CONFIG.KEY_CODE_META =				224;
ORYX.CONFIG.KEY_CODE_BACKSPACE =		8;
ORYX.CONFIG.KEY_CODE_LEFT =				37;
ORYX.CONFIG.KEY_CODE_RIGHT =			39;
ORYX.CONFIG.KEY_CODE_UP =				38;
ORYX.CONFIG.KEY_CODE_DOWN =				40;

// TODO Determine where the lowercase constants are still used and remove them from here.
ORYX.CONFIG.KEY_Code_enter =			12;
ORYX.CONFIG.KEY_Code_left =				37;
ORYX.CONFIG.KEY_Code_right =			39;
ORYX.CONFIG.KEY_Code_top =				38;
ORYX.CONFIG.KEY_Code_bottom =			40;

/* Supported Meta Keys */
ORYX.CONFIG.META_KEY_META_CTRL = 		"metactrl";
ORYX.CONFIG.META_KEY_ALT = 				"alt";
ORYX.CONFIG.META_KEY_SHIFT = 			"shift";

/* Key Actions */
ORYX.CONFIG.KEY_ACTION_DOWN = 			"down";
ORYX.CONFIG.KEY_ACTION_UP = 			"up";

ORYX.CONFIG.REMOTE_WINDOW_HEIGHT_DEFAULT = 300;
ORYX.CONFIG.REMOTE_WINDOW_WIDTH_DEFAULT = 300;
/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/

/**
 @namespace Global Oryx name space
 @name ORYX
 */
if (!ORYX) {
    var ORYX = {};
}

/**
 * The Editor class.
 */
ORYX.Editor = {
    construct: function (config) {
        "use strict";

        this._canvas = undefined;
        this.zoomLevel = 1.0;
        this.availablePlugins = [];
        this.activatedPlugins = [];
        this.pluginsData = [];
        this.modelMetaData = config;
        this.layout_regions = undefined;
        this.layout = undefined;

        var model = config;
        if (config.model) {
            model = config.model;
        }

        this.id = model.resourceId;
        if (!this.id) {
            this.id = model.id;
            if (!this.id) {
                this.id = ORYX.Utils.provideId();
            }
        }

        this.showPlugins = !('showPlugins' in model) || model.showPlugins;

        var langs = (config.languages || []).sort(function (k, h) {
            return config.position - config.position;
        });

        // Defines if the editor should be fullscreen or not
        this.fullscreen = config.fullscreen !== false;

        this.useSimulationPanel = config.useSimulationPanel || false;

        // CREATES the canvas
        this._createCanvas(model.stencil ? model.stencil.id : null, model.properties, langs);

        // GENERATES the whole EXT.VIEWPORT
        this._generateGUI();

        // LOAD the plugins
        if (this.showPlugins) {
            window.setTimeout(function () {
                this.loadPlugins();
                this.activatePlugins();
            }.bind(this), 100);
        }

        // LOAD the content of the current editor instance
        //window.setTimeout(function () {
            // Attach the editor must be the LAST THING AFTER ALL HAS BEEN LOADED
            this.getCanvas().attachEditor(new BpmnJS({
                container: '#' + this.getCanvas().rootNode.id,
                keyboard: {
                    bindTo: window
                },
                propertiesPanel: this.useSimulationPanel ? {
                    parent: '#js-properties-panel'
                } : undefined
            }));

            if (config && config.xml) {
                this.importXML(config.xml);
            }

            // Fixed the problem that the viewport can not
            // start with collapsed panels correctly
            if (ORYX.CONFIG.PANEL_RIGHT_COLLAPSED === true) {
                this.layout_regions.east.collapse();
            }
            if (ORYX.CONFIG.PANEL_LEFT_COLLAPSED === true) {
                this.layout_regions.west.collapse();
            }
        //}.bind(this), 200);


    },

    zoomFitToModel: function () {
        this.getCanvas().zoomFitToModel();
    },

    /**
     * Generate the whole viewport of the
     * Editor and initialized the Ext-Framework
     */
    _generateGUI: function () {
        "use strict";

        // Defines the layout height if it's NOT fullscreen
        var layoutHeight = ORYX.CONFIG.WINDOW_HEIGHT;

        /**
         * Extend the Region implementation so that,
         * the clicking area can be extend to the whole collapse area and
         * an title can now be shown.
         */
        var oldGetCollapsedEl = Ext.layout.BorderLayout.Region.prototype.getCollapsedEl;
        Ext.layout.BorderLayout.Region.prototype.getCollapsedEl = function () {
            oldGetCollapsedEl.apply(this, arguments);

            if (this.collapseMode !== 'mini' && this.floatable === false && this.expandTriggerAll === true) {
                this.collapsedEl.addClassOnOver("x-layout-collapsed-over");
                this.collapsedEl.on("mouseover", this.collapsedEl.addClass.bind(this.collapsedEl, "x-layout-collapsed-over"));
                this.collapsedEl.on("click", this.onExpandClick, this);
            }

            if (this.collapseTitle) {
                // Use SVG to rotate text
                var svg = ORYX.Utils.graft("http://www.w3.org/2000/svg", this.collapsedEl.dom,
                    ['svg', {style: "position:relative;left:" + (this.position === "west" ? 4 : 6) + "px;top:" + (this.position === "west" ? 2 : 5) + "px;"},
                        ['text', {transform: "rotate(90)", x: 0, y: 0, "stroke-width": "0px", fill: "#EEEEEE", style: "font-weight:bold;", "font-size": "11"}, this.collapseTitle]
                    ]),
                    text = svg.childNodes[0];
                svg.setAttribute("xmlns:svg", "http://www.w3.org/2000/svg");

                // Rotate the west into the other side
                if (this.position === "west" && text.getComputedTextLength instanceof Function) {
                    // Wait till rendered
                    window.setTimeout(function () {
                        var length = text.getComputedTextLength();
                        text.setAttributeNS(null, "transform", "rotate(-90, " + ((length / 2) + 7) + ", " + ((length / 2) - 3) + ")");
                    }, 1)
                }
                delete this.collapseTitle;
            }
            return this.collapsedEl;
        };

        // DEFINITION OF THE VIEWPORT AREAS
        this.layout_regions = {

            // DEFINES TOP-AREA
            north: new Ext.Panel({ //TOOO make a composite of the oryx header and addable elements (for toolbar), second one should contain margins
                region: 'north',
                cls: 'x-panel-editor-north',
                autoEl: 'div',
                border: false
            }),

            // DEFINES RIGHT-AREA
            east: new Ext.Panel({
                region: 'east',
                layout: 'fit',
                cls: 'x-panel-editor-east',
                collapseTitle: ORYX.I18N.View.East,
                titleCollapse: true,
                border: false,
                cmargins: {left: 0, right: 0},
                floatable: false,
                expandTriggerAll: true,
                collapsible: true,
                width: 450,
                split: true,
                title: "Simulation Parameters",
                items: {
                    layout: "fit",
                    autoHeight: true,
                    el: document.getElementById("js-properties-panel")
                }
            }),

            // DEFINES BOTTOM-AREA
            south: new Ext.Panel({
                region: 'south',
                cls: 'x-panel-editor-south',
                autoEl: 'div',
                border: false
            }),

            //DEFINES LEFT-AREA
            west: new Ext.Panel({
                region: 'west',
                layout: 'anchor',
                autoEl: 'div',
                cls: 'x-panel-editor-west',
                collapsible: true,
                titleCollapse: true,
                collapseTitle: ORYX.I18N.View.West,
                width: ORYX.CONFIG.PANEL_LEFT_WIDTH || 10,
                autoScroll: Ext.isIPad ? false : true,
                cmargins: {left: 0, right: 0},
                floatable: false,
                expandTriggerAll: true,
                split: true,
                title: "West"
            }),

            // DEFINES CENTER-AREA (FOR THE EDITOR)
            center: new Ext.Panel({
                region: 'center',
                cls: 'x-panel-editor-center',
                autoScroll: false,
                items: {
                    layout: "fit",
                    autoHeight: true,
                    el: this.getCanvas().rootNode
                }
            }),

            info: new Ext.Panel({
                region: "south",
                cls: "x-panel-editor-info",
                autoEl: "div",
                border: false,
                layout: "fit",
                cmargins: {
                    top: 0,
                    bottom: 0,
                    left: 0,
                    right: 0
                },
                collapseTitle: "Information",
                floatable: false,
                titleCollapse: false,
                expandTriggerAll: true,
                collapsible: true,
                split: true,
                title: "Information",
                height: 100,
                tools: [
                    {
                        id: "close",
                        handler: function (g, f, e) {
                            e.hide();
                            e.ownerCt.layout.layout()
                        }
                    }
                ]
            })
        };

        // Config for the Ext.Viewport
        var layout_config = {
            layout: "border",
            items: [this.layout_regions.north, this.layout_regions.east, this.layout_regions.south, this.layout_regions.west, new Ext.Panel({
                layout: "border",
                region: "center",
                border: false,
                items: [this.layout_regions.center, this.layout_regions.info]
            })]
        };

        // IF Fullscreen, use a viewport
        if (this.fullscreen) {
            this.layout = new Ext.Viewport(layout_config);

            // IF NOT, use a panel and render it to the given id
        } else {
            layout_config.renderTo = this.id;
            //layout_config.height = layoutHeight;
            layout_config.height = this.getEditorNode().clientHeight; // the panel and the containing div should be of the same height
            this.layout = new Ext.Panel(layout_config)
        }

        if (!this.useSimulationPanel) {
            this.layout_regions.east.hide();
        }

        this.layout_regions.west.hide();
        this.layout_regions.info.hide();
        if (Ext.isIPad && "undefined" != typeof iScroll) {
            this.getCanvas().iscroll = new iScroll(this.layout_regions.center.body.dom.firstChild, {
                touchCount: 2
            })
        }

        // Set the editor to the center, and refresh the size
        this.getEditorNode().setAttributeNS(null, 'align', 'left');
        this.getCanvas().rootNode.setAttributeNS(null, 'align', 'left');
        // this.getCanvas().setSize({
        //     width: ORYX.CONFIG.CANVAS_WIDTH,
        //     height: ORYX.CONFIG.CANVAS_HEIGHT
        // });

    },

    /**
     * adds a component to the specified region
     *
     * @param {String} region
     * @param {Ext.Component} component
     * @param {String} title, optional
     * @return {Ext.Component} dom reference to the current region or null if specified region is unknown
     */
    addToRegion: function (region, component, title) {
        if (region.toLowerCase && this.layout_regions[region.toLowerCase()]) {
            var current_region = this.layout_regions[region.toLowerCase()];

            current_region.add(component);

            ORYX.Log.debug("original dimensions of region %0: %1 x %2", current_region.region, current_region.width, current_region.height);

            // update dimensions of region if required.
            if (!current_region.width && component.initialConfig && component.initialConfig.width) {
                ORYX.Log.debug("resizing width of region %0: %1", current_region.region, component.initialConfig.width);
                current_region.setWidth(component.initialConfig.width)
            }
            if (component.initialConfig && component.initialConfig.height) {
                ORYX.Log.debug("resizing height of region %0: %1", current_region.region, component.initialConfig.height);
                var current_height = current_region.height || 0;
                current_region.height = component.initialConfig.height + current_height;
                current_region.setHeight(component.initialConfig.height + current_height)
            }

            // set title if provided as parameter.
            if (typeof title == "string") {
                current_region.setTitle(title);
            }

            // trigger doLayout() and show the pane
            current_region.ownerCt.doLayout();
            current_region.show();

            if (Ext.isMac) {
                this.resizeFix();
            }

            return current_region;
        }

        return null;
    },

    getAvailablePlugins: function () {
        var curAvailablePlugins = this.availablePlugins.clone();
        curAvailablePlugins.each(function (plugin) {
            if (this.activatedPlugins.find(function (loadedPlugin) {
                return loadedPlugin.type == this.name;
            }.bind(plugin))) {
                plugin.engaged = true;
            } else {
                plugin.engaged = false;
            }
        }.bind(this));
        return curAvailablePlugins;
    },

    loadScript: function (url, callback) {
        var script = document.createElement("script");
        script.type = "text/javascript";
        if (script.readyState) {  //IE
            script.onreadystatechange = function () {
                if (script.readyState == "loaded" || script.readyState == "complete") {
                    script.onreadystatechange = null;
                    callback();
                }
            };
        } else {  //Others
            script.onload = function () {
                callback();
            };
        }
        script.src = url;
        document.getElementsByTagName("head")[0].appendChild(script);
    },
    /**
     * activate Plugin
     *
     * @param {String} name
     * @param {Function} callback
     *        callback(sucess, [errorCode])
     *            errorCodes: NOTUSEINSTENCILSET, REQUIRESTENCILSET, NOTFOUND, YETACTIVATED
     */
    activatePluginByName: function (name, callback, loadTry) {

        var match = this.getAvailablePlugins().find(function (value) {
            return value.name == name
        });
        if (match && (!match.engaged || (match.engaged === 'false'))) {
            var facade = this._getPluginFacade();
            var me = this;
            ORYX.Log.debug("Initializing plugin '%0'", match.name);

            try {

                var className = eval(match.name);
                var newPlugin = new className(facade, match);
                newPlugin.type = match.name;

                // If there is an GUI-Plugin, they get all Plugins-Offer-Meta-Data
                if (newPlugin.registryChanged)
                    newPlugin.registryChanged(me.pluginsData);

                // If there have an onSelection-Method it will pushed to the Editor Event-Handler
                // if (newPlugin.onSelectionChanged)
                //     me.registerOnEvent(ORYX.CONFIG.EVENT_SELECTION_CHANGED, newPlugin.onSelectionChanged.bind(newPlugin));
                this.activatedPlugins.push(newPlugin);
                this.activatedPlugins.each(function (loaded) {
                    if (loaded.registryChanged)
                        loaded.registryChanged(this.pluginsData);
                }.bind(me));
                callback(true);

            } catch (e) {
                ORYX.Log.warn("Plugin %0 is not available", match.name);
                if (!!loadTry) {
                    callback(false, "INITFAILED");
                    return;
                }
                this.loadScript("plugins/scripts/" + match.source, this.activatePluginByName.bind(this, match.name, callback, true));
            }
        } else {
            callback(false, match ? "NOTFOUND" : "YETACTIVATED");
            //TODO error handling
        }
    },

    /**
     *  Laden der Plugins
     */
    activatePlugins: function () {

        // if there should be plugins but still are none, try again.
        // TODO this should wait for every plugin respectively.
        /*if (!ORYX.Plugins && ORYX.availablePlugins.length > 0) {
         window.setTimeout(this.loadPlugins.bind(this), 100);
         return;
         }*/

        var me = this;
        var newPlugins = [];
        var facade = this._getPluginFacade();

        this.availablePlugins.each(function (value) {
            ORYX.Log.debug("Initializing plugin '%0'", value.name);
            try {
                var className = eval(value.name);
                if (className) {
                    var plugin = new className(facade, value);
                    plugin.type = value.name;
                    newPlugins.push(plugin);
                    plugin.engaged = true;
                }
            } catch (e) {
                ORYX.Log.warn("Plugin %0 is not available", value.name);
                ORYX.Log.error("Error: " + e.message);
            }
        });

        newPlugins.each(function (value) {
            // If there is an GUI-Plugin, they get all Plugins-Offer-Meta-Data
            if (value.registryChanged)
                value.registryChanged(me.pluginsData);

            // If there have an onSelection-Method it will pushed to the Editor Event-Handler
            // if (value.onSelectionChanged)
            //     me.registerOnEvent(ORYX.CONFIG.EVENT_SELECTION_CHANGED, value.onSelectionChanged.bind(value));
        });

        this.activatedPlugins = newPlugins;

        // Hack for the Scrollbars
        if (Ext.isMac) {
            ORYX.Editor.resizeFix();
        }
    },

    getEditorNode: function () {
        return document.getElementById(this.id);
    },

    /**
     * Creates the Canvas
     * @param {String} [stencilType] The stencil type used for creating the canvas. If not given, a stencil with myBeRoot = true from current stencil set is taken.
     * @param {Object} [canvasConfig] Any canvas properties (like language).
     */
    _createCanvas: function (stencilType, canvasConfig, lang) {
        this._canvas = new ORYX.Canvas({
            width: ORYX.CONFIG.CANVAS_WIDTH,
            height: ORYX.CONFIG.CANVAS_HEIGHT,
            id: ORYX.Utils.provideId(),
            parentNode: this.getEditorNode(),
            language: lang
        });
    },

    getCanvas: function() {
        return this._canvas;
    },

    getSimulationDrawer: function() {
        return this.layout_regions.east;
    },

    /**
     * Returns a per-editor singleton plugin facade.
     * To be used in plugin initialization.
     */
    _getPluginFacade: function () {
        if (!(this._pluginFacade)) {
            this._pluginFacade = (function () {
                return {
                    activatePluginByName: this.activatePluginByName.bind(this),
                    getAvailablePlugins: this.getAvailablePlugins.bind(this),
                    offer: this.offer.bind(this),
                    getStencilSets: function() {return {}},
                    getStencilSetExtensionDefinition: function () {return {}},
                    getRules: function() {return {}},
                    loadStencilSet: function() {},
                    createShape: function() {},
                    deleteShape: function() {},
                    getSelection: function() {},
                    setSelection: function() {},
                    updateSelection: function() {},
                    getCanvas: this.getCanvas.bind(this),
                    getSimulationDrawer: this.getSimulationDrawer.bind(this),
                    useSimulationPanel: this.useSimulationPanel,
                    importJSON: function() {},
                    importERDF: function() {},
                    getERDF: function() {},
                    getJSON: function() {},
                    getXML: this.getXML.bind(this),
                    getSVG: this.getSVG.bind(this),
                    getSerializedJSON: function() {},
                    executeCommands: function() {},
                    isExecutingCommands: function() {},
                    registerOnEvent: function() {},
                    unregisterOnEvent: function() {},
                    raiseEvent: function() {},
                    enableEvent: function() {},
                    disableEvent: function() {},
                    eventCoordinates: function() {},
                    addToRegion: this.addToRegion.bind(this),
                    getAllLanguages: function() {return {}}
                }
            }.bind(this)())
        }
        return this._pluginFacade;
    },

    getXML: function() {
        return this.getCanvas().getXML();
    },

    getSVG: function() {
        return this.getCanvas().getSVG();
    },

    importXML: function(xml) {
        this.getCanvas().importXML(xml);
    },

    offer: function (pluginData) {
        if (!this.pluginsData.member(pluginData)) {
            this.pluginsData.push(pluginData);
        }
    },

    /**
     * When working with Ext, conditionally the window needs to be resized. To do
     * so, use this class method. Resize is deferred until 100ms, and all subsequent
     * resizeBugFix calls are ignored until the initially requested resize is
     * performed.
     */
    resizeFix: function () {
        if (!this._resizeFixTimeout) {
            this._resizeFixTimeout = window.setTimeout(function () {
                window.resizeBy(1, 1);
                window.resizeBy(-1, -1);
                this._resizefixTimeout = null;
            }, 100);
        }
    },

    /**
     * First bootstrapping layer. The Oryx loading procedure begins. In this
     * step, all preliminaries that are not in the responsibility of Oryx to be
     * met have to be checked here, such as the existance of the prototpe
     * library in the current execution environment. After that, the second
     * bootstrapping layer is being invoked. Failing to ensure that any
     * preliminary condition is not met has to fail with an error.
     */
    load: function() {

        if (ORYX.CONFIG.PREVENT_LOADINGMASK_AT_READY !== true) {
            var waitingpanel = new Ext.Window({renderTo:Ext.getBody(),id:'oryx-loading-panel',bodyStyle:'padding: 8px;background:white',title:ORYX.I18N.Oryx.title,width:'auto',height:'auto',modal:true,resizable:false,closable:false,html:'<span style="font-size:11px;">' + ORYX.I18N.Oryx.pleaseWait + '</span>'})
            waitingpanel.show()
        }

        ORYX.Log.debug("Oryx begins loading procedure.");

        // check for prototype
        // if( (typeof Prototype=='undefined') ||
        //     (typeof Element == 'undefined') ||
        //     (typeof Element.Methods=='undefined') ||
        //     parseFloat(Prototype.Version.split(".")[0] + "." +
        //         Prototype.Version.split(".")[1]) < 1.5)
        //
        //     throw("Application requires the Prototype JavaScript framework >= 1.5.3");
        //
        // ORYX.Log.debug("Prototype > 1.5 found.");

        // continue loading.
        this.loadPlugins();
    },

    /**
     * Load a list of predefined plugins from the server
     */
    loadPlugins: function() {

        // load plugins if enabled.
        if(ORYX.CONFIG.PLUGINS_ENABLED)
            this._loadPlugins()
        else
            ORYX.Log.warn("Ignoring plugins, loading Core only.");
    },

    _loadPlugins: function() {
        var me = this;
        var source = ORYX.CONFIG.PLUGINS_CONFIG;

        ORYX.Log.debug("Loading plugin configuration from '%0'.", source);
        new Ajax.Request(source, {
            asynchronous: false,
            method: 'get',
            onSuccess: function(result) {

                /*
                 * This is the method that is being called when the plugin
                 * configuration was successfully loaded from the server. The
                 * file has to be processed and the contents need to be
                 * considered for further plugin requireation.
                 */

                ORYX.Log.info("Plugin configuration file loaded.");

                // get plugins.xml content
                var resultXml = result.responseXML;
                console.log('Plugin list:', resultXml);

                // TODO: Describe how properties are handled.
                // Get the globale Properties
                var globalProperties = [];
                var preferences = $A(resultXml.getElementsByTagName("properties"));
                preferences.each( function(p) {

                    var props = $A(p.childNodes);
                    props.each( function(prop) {
                        var property = new Hash();

                        // get all attributes from the node and set to global properties
                        var attributes = $A(prop.attributes)
                        attributes.each(function(attr){property[attr.nodeName] = attr.nodeValue});
                        if(attributes.length > 0) { globalProperties.push(property) };
                    });
                });


                // TODO Why are we using XML if we don't respect structure anyway?
                // for each plugin element in the configuration..
                var plugin = resultXml.getElementsByTagName("plugin");
                $A(plugin).each( function(node) {

                    // get all element's attributes.
                    // TODO: What about: var pluginData = $H(node.attributes) !?
                    var pluginData = new Hash();

                    //pluginData: for one plugin
                    //.properties: contain all properties in the plugins.xml
                    //.requires: contains the requires property for the plugin
                    //.source: source javascript
                    //.name: name
                    //.notUseIn:

                    $A(node.attributes).each( function(attr){
                        pluginData[attr.nodeName] = attr.nodeValue});

                    // ensure there's a name attribute.
                    if(!pluginData['name']) {
                        ORYX.Log.error("A plugin is not providing a name. Ingnoring this plugin.");
                        return;
                    }

                    // ensure there's a source attribute.
                    if(!pluginData['source']) {
                        ORYX.Log.error("Plugin with name '%0' doesn't provide a source attribute.", pluginData['name']);
                        return;
                    }

                    // Get all private Properties
                    var propertyNodes = node.getElementsByTagName("property");
                    var properties = [];
                    $A(propertyNodes).each(function(prop) {
                        var property = new Hash();

                        // Get all Attributes from the Node
                        var attributes = $A(prop.attributes)
                        attributes.each(function(attr){property[attr.nodeName] = attr.nodeValue});
                        if(attributes.length > 0) { properties.push(property) };

                    });

                    // Set all Global-Properties to the Properties
                    properties = properties.concat(globalProperties);

                    // Set Properties to Plugin-Data
                    pluginData['properties'] = properties;

                    // Get the RequieredNodes
                    var requireNodes = node.getElementsByTagName("requires");
                    var requires;
                    $A(requireNodes).each(function(req) {
                        var namespace = $A(req.attributes).find(function(attr){ return attr.name == "namespace"})
                        if( namespace && namespace.nodeValue ){
                            if( !requires ){
                                requires = {namespaces:[]}
                            }

                            requires.namespaces.push(namespace.nodeValue)
                        }
                    });

                    // Set Requires to the Plugin-Data, if there is one
                    if( requires ){
                        pluginData['requires'] = requires;
                    }


                    // Get the RequieredNodes
                    var notUsesInNodes = node.getElementsByTagName("notUsesIn");
                    var notUsesIn;
                    $A(notUsesInNodes).each(function(not) {
                        var namespace = $A(not.attributes).find(function(attr){ return attr.name == "namespace"})
                        if( namespace && namespace.nodeValue ){
                            if( !notUsesIn ){
                                notUsesIn = {namespaces:[]}
                            }

                            notUsesIn.namespaces.push(namespace.nodeValue)
                        }
                    });

                    // Set Requires to the Plugin-Data, if there is one
                    if( notUsesIn ){
                        pluginData['notUsesIn'] = notUsesIn;
                    }


                    var url = ORYX.PATH + ORYX.CONFIG.PLUGINS_FOLDER + pluginData['source'];

                    ORYX.Log.debug("Requireing '%0'", url);

                    // Add the Script-Tag to the Site
                    //Kickstart.require(url);

                    ORYX.Log.info("Plugin '%0' successfully loaded.", pluginData['name']);

                    // Add the Plugin-Data to all available Plugins
                    me.availablePlugins.push(pluginData);

                });

            },
            onFailure: me._loadPluginsOnFails
        });

    },

    _loadPluginsOnFails: function(result) {
        ORYX.Log.error("Plugin configuration file not available.");
    },

    toggleFullScreen: function () {
        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen();
        } else {
            if (document.exitFullscreen) {
                document.exitFullscreen();
            }
        }
    }
};

ORYX.Editor = Clazz.extend(ORYX.Editor);




/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/

/**
 * Init namespaces
 */
if (!ORYX) {
    var ORYX = {};
}

ORYX.Canvas = {
    /** @lends ORYX.Core.Canvas.prototype */

    /**
     * Constructor
     */
    construct: function(options) {
        this.zoomLevel = 1;
        this._editor = undefined;

        if (!(options && options.width && options.height)) {
            ORYX.Log.fatal("Canvas is missing mandatory parameters options.width and options.height.");
            return;
        }

        this.className = "ORYX_Editor";
        this.resourceId = options.id;
        this.nodes = [];
        this.edges = [];

        this.rootNode = ORYX.Utils.graft("http://www.w3.org/1999/xhtml", options.parentNode,
            ['div', {id: options.id, width: options.width, height: options.height}
            ]);
        //this.rootNode.addClassName(this.className);
        this.rootNode.classList.add(this.className);

    },

    getScrollNode: function () {
        "use strict";
        return Ext.get(this.rootNode).parent("div{overflow=auto}", true);
    },


    attachEditor: function (editor) {
        this._editor = editor;
    },

    getSVGContainer: function() {
        return $("div.ORYX_Editor div.bjs-container div.djs-container svg")[0];
    },

    getSVGViewport: function() {
        return $("div.ORYX_Editor div.bjs-container div.djs-container svg g.viewport")[0];
    },

    getSourceNodeId: function (sequenceFlowId) {
        var foundId;
        var elements = this._editor.getDefinitions().rootElements[0].flowElements;
        elements.forEach(function(element) {
            if (!foundId && element.$type == "bpmn:SequenceFlow" && element.id == sequenceFlowId) {
                foundId = element.sourceRef.id;
            }
        });
        return foundId;
    },

    getTargetNodeId: function (sequenceFlowId) {
        var foundId;
        var flowElements = this._editor.getDefinitions().rootElements[0].flowElements;
        flowElements.forEach(function(element) {
            if (!foundId && element.$type == "bpmn:SequenceFlow" && element.id == sequenceFlowId) {
                foundId = element.targetRef.id;
            }
        });
        return foundId;
    },

    getIncomingFlowId: function (nodeId) {
        var foundId;
        var flowElements = this._editor.getDefinitions().rootElements[0].flowElements;
        flowElements.forEach(function(element) {
            if (!foundId && element.$type == "bpmn:SequenceFlow" && element.targetRef.id == nodeId) {
                foundId = element.id;
            }
        });
        return foundId;
    },

    getOutgoingFlowId: function (nodeId) {
        var foundId;
        var elements = this._editor.getDefinitions().rootElements[0].flowElements;
        elements.forEach(function(element) {
            if (!foundId && element.$type == "bpmn:SequenceFlow" && element.sourceRef.id == nodeId) {
                foundId = element.id;
            }
        });
        return foundId;
    },

    getLanguage: function () {
        //return (this.language || (this.languages || []).first()) || "de_de";
        return ORYX.I18N.Language;
    },

    toString: function () {
        return "Canvas " + this.id;
    },

    importXML: function(xml) {
      // this._editor.importXML(xml, function(err) {
      //   if (err) {
      //     return console.error('could not import BPMN 2.0 diagram', err);
      //   }
      //   this.zoomFitToModel();
      // }.bind(this));

      //EXPERIMENTING WITH THE BELOW TO FIX ARROWS NOT SNAP TO EDGES WHEN OPENING MODELS
      //Some BPMN files are not compatible with bpmn.io
      var editor = this._editor;
      this._editor.importXML(xml, function(err) {
        if (err) {
          return console.error('could not import BPMN 2.0 diagram', err);
        }

        var eventBus = editor.get('eventBus');
        var connectionDocking = editor.get('connectionDocking');
        var elementRegistry = editor.get('elementRegistry');
        var connections = elementRegistry.filter(function(e) {
          return e.waypoints;
        });
        connections.forEach(function(connection) {
          connection.waypoints = connectionDocking.getCroppedWaypoints(connection);
        });
        eventBus.fire('elements.changed', { elements: connections });

        this.zoomFitToModel();
      }.bind(this));
    },

    getXML: function() {
        var bpmnXML;
        this._editor.saveXML({ format: true }, function(err, xml) {
            bpmnXML = xml;
        });
        return bpmnXML;
    },

    getSVG: function() {
        var bpmnSVG;
        this._editor.saveSVG(function(err, svg) {
            bpmnSVG = svg;
        });
        return bpmnSVG;
    },

    zoomFitToModel: function() {
        if (this._editor) {
            var canvas = this._editor.get('canvas');
            // zoom to fit full viewport
            canvas.zoom('fit-viewport');
            var viewbox = canvas.viewbox();
            canvas.viewbox({
                x: viewbox.x - 200,
                y: viewbox.y,
                width: viewbox.outer.width * 1.5,
                height: viewbox.outer.height * 1.5
            });
        }
    },

    zoomIn: function() {
        this._editor.get('editorActions').trigger('stepZoom', { value: 1 });
    },


    zoomOut: function() {
        this._editor.get('editorActions').trigger('stepZoom', { value: -1 });
    },

    zoomDefault: function() {
        editorActions.trigger('zoom', { value: 1 });
    },

    createShape: function(type, x, y, w, h) {
        var modelling = this._editor.get('modeling');
        var parent = this._editor.get('canvas').getRootElement();
        //console.log('parent', parent);
        var shape = modelling.createShape({type:type, width:w, height:h}, {x:x, y:y}, parent);
        return shape.id;
    },

    updateProperties: function(elementId, properties) {
        var modelling = this._editor.get('modeling');
        var registry = this._editor.get('elementRegistry');
        modelling.updateProperties(registry.get(elementId), properties);
    },


    createSequenceFlow: function (source, target, attrs) {
        var attrs2 = {};
        Object.assign(attrs2,{type:'bpmn:SequenceFlow'});
        if (attrs.waypoints) {
            Object.assign(attrs2,{waypoints: attrs.waypoints});
        }
        var modelling = this._editor.get('modeling');
        var registry = this._editor.get('elementRegistry');
        var flow = modelling.connect(registry.get(source), registry.get(target), attrs2);
        //console.log(flow);
        return flow.id;
    },

    createAssociation: function (source, target, attrs) {
        var attrs2 = {};
        Object.assign(attrs2,{type:'bpmn:Association'});
        if (attrs.waypoints) {
            Object.assign(attrs2,{waypoints: attrs.waypoints});
        }
        var modelling = this._editor.get('modeling');
        var registry = this._editor.get('elementRegistry');
        var assoc = Object.assign(assoc, modelling.connect(registry.get(source), registry.get(target), attrs2));
        return assoc.id;
    },

    highlight: function (elementId) {
        //console.log("Highlighting elementId: " + elementId);
        var self = this;
        var element = self._editor.get('elementRegistry').get(elementId);
        var modelling = self._editor.get('modeling');
        //console.log(element);
        modelling.setColor([element],{stroke:'red'});
    },

    colorElements: function (elementIds, color) {
        var elements = [];
        var registry = this._editor.get('elementRegistry');
        elementIds.forEach(function(elementId) {
            elements.push(registry.get(elementId));
        });
        var modelling = this._editor.get('modeling');
        modelling.setColor(elements, {stroke:color});
    },

    colorElement: function (elementId, color) {
        var modelling = this._editor.get('modeling');
        var element = this._editor.get('elementRegistry').get(elementId);
        modelling.setColor([element],{stroke:color});
    },

    fillColor: function (elementId, color) {
        var modelling = this._editor.get('modeling');
        var element = this._editor.get('elementRegistry').get(elementId);
        modelling.setColor([element],{fill:color});
    },

    greyOut: function(elementIds) {
        var elementRegistry = this._editor.get('elementRegistry');
        var self = this;
        elementIds.forEach(function(id) {
            console.log('_elements', elementRegistry._elements);
            var gfx = elementRegistry.getGraphics(id);
            var visual = gfx.children[0];
            visual.setAttributeNS(null, "style", "opacity: 0.25");
        });

    },

    normalizeAll: function() {
        var registry = this._editor.get('elementRegistry');
        var modelling = this._editor.get('modeling');
        modelling.setColor(registry.getAll(), {stroke:'black'});
    },

    removeShapes: function(shapeIds) {
        var registry = this._editor.get('elementRegistry');
        var modelling = this._editor.get('modeling');
        console.log(shapeIds);
        var shapes = [];
        shapeIds.forEach(function(shapeId) {
            shapes.push(registry.get(shapeId));
        });
        modelling.removeElements(shapes);
    },

    getAllElementIds: function() {
        var ids = [];
        var elementRegistry = this._editor.get('elementRegistry');
        elementRegistry.getAll().forEach(function(element) {
            ids.push(element.id);
        });
        return ids;
    },

    shapeCenter: function (shapeId) {
        var position = {};
        var registry = this._editor.get('elementRegistry');
        var shape = registry.get(shapeId);
        //console.log('Shape of ' + shapeId);
        //console.log(shape);
        //console.log(shape.x);
        position.x = (shape.x + shape.width/2);
        position.y = (shape.y + shape.height/2);
        return position;
    },

    clear: function() {
        this._editor.clear();
    },

    registerActionHandler: function(handlerName, handler) {
        var commandStack = this._editor.get('commandStack');
        commandStack.registerHandler(handlerName, handler);
    },

    executeActionHandler: function(handlerName, context) {
        var commandStack = this._editor.get('commandStack');
        commandStack.execute(handlerName, context);
    },

    getCenter: function (shapeId) {
        var shape = this._editor.get('elementRegistry').get(shapeId);
        return {
            x: shape.x + (shape.width || 0) / 2,
            y: shape.y + (shape.height || 0) / 2
        }
    },

    // Center viewbox to an element
    // From https://forum.bpmn.io/t/centering-zooming-view-to-a-specific-element/1536/6
    centerElement: function(elementId) {
        // assuming we center on a shape.
        // for connections we must compute the bounding box
        // based on the connection's waypoints
        var bbox = elementRegistry.get(elementId);

        var currentViewbox = canvas.viewbox();

        var elementMid = {
          x: bbox.x + bbox.width / 2,
          y: bbox.y + bbox.height / 2
        };

        canvas.viewbox({
          x: elementMid.x - currentViewbox.width / 2,
          y: elementMid.y - currentViewbox.height / 2,
          width: currentViewbox.width,
          height: currentViewbox.height
        });
    },

  _getActionStack: function() {
    return this._editor.get('commandStack')._stack;
  },

  _getCurrentStackIndex: function() {
    return this._editor.get('commandStack')._stackIdx;
  },

  // Get all base action indexes backward from the current command stack index
  // The first element in the result is the earliest base action and so on
  _getBaseActions: function() {
    var actions = this._getActionStack();
    var stackIndex = this._getCurrentStackIndex();
    var baseActionIndexes = [];
    for (var i=0; i<=stackIndex; i++) {
      if (i==0 || (actions[i].id != actions[i-1].id)) {
        baseActionIndexes.push(i);
      }
    }
    return baseActionIndexes;
  },

  undo: function() {
    this._editor.get('commandStack').undo();
  },

  // Undo to the point before an action (actionName is the input)
  // Nothing happens if the action is not found
  // The number of undo times is the number of base actions from the current stack index
  undoSeriesUntil: function(actionName) {
    var actions = this._getActionStack();
    var baseActions = this._getBaseActions();
    var baseActionNum = 0;
    for (var i=baseActions.length-1; i>=0; i--) {
      if (actions[baseActions[i]].command == actionName) {
        baseActionNum = baseActions.length - i;
        break;
      }
    }

    console.log('baseActionNum', baseActionNum);

    while (baseActionNum > 0) {
      this.undo();
      baseActionNum--;
    }
  },

  canUndo: function() {
    if (!this._editor) {
      return false;
    }
    else {
      return this._editor.get('commandStack').canUndo();
    }
  },

  redo: function() {
    this._editor.get('commandStack').redo();
  },

  canRedo: function() {
    if (!this._editor) {
      return false;
    }
    else {
      return this._editor.get('commandStack').canRedo();
    }
  },

  getLastBaseAction: function() {
    var actions = this._getActionStack();
    var baseActions = this._getBaseActions();
    if (baseActions.length > 0) {
      return actions[baseActions[baseActions.length-1]].command;
    }
    else {
      return '';
    }
  },

  // Get the next latest base action in the command stack
  // that is not in the excluding list
  getNextBaseActionExcluding: function(excludingActions) {
    var actions = this._getActionStack();
    var baseActionIndexes = this._getBaseActions();
    if (baseActionIndexes.length >= 2) {
      for (var i = baseActionIndexes.length-2; i>=0; i--) {
        if (excludingActions.indexOf(actions[baseActionIndexes[i]].command) < 0) {
          return actions[baseActionIndexes[i]].command;
        }
      }
    }
    return '';
  },

  addCommandStackChangeListener: function(callback) {
    this._editor.on('commandStack.changed', callback);
  },

  addEventBusListener: function(eventCode, callback) {
    this._editor.get('eventBus').on(eventCode, callback);
  }

};

ORYX.Canvas = Clazz.extend(ORYX.Canvas);

/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/
if(!ORYX){ var ORYX = {} }
if(!ORYX.Plugins){ ORYX.Plugins = {} }

ORYX.Plugins.ApromoreSave = Clazz.extend({

    facade:undefined,

    changeSymbol:"*",

    construct:function (facade) {
        this.facade = facade;

        this.facade.offer({
            'name':ORYX.I18N.Save.save,
            'functionality':this.save.bind(this, false),
            'group':ORYX.I18N.Save.group,
            'icon':ORYX.PATH + "images/disk.png",
            'description':ORYX.I18N.Save.saveDesc,
            'index':1,
            'minShape':0,
            'maxShape':0,
            keyCodes:[
                {
                    metaKeys:[ORYX.CONFIG.META_KEY_META_CTRL],
                    keyCode:83, // s-Keycode
                    keyAction:ORYX.CONFIG.KEY_ACTION_UP
                }
            ]
        });

        // document.addEventListener("keydown", function (e) {
        //     if (e.ctrlKey && e.keyCode === 83) {
        //         Event.stop(e);
        //     }
        // }, false);


        this.facade.offer({
            'name':ORYX.I18N.Save.saveAs,
            'functionality':this.save.bind(this, true),
            'group':ORYX.I18N.Save.group,
            'icon':ORYX.PATH + "images/disk_multi.png",
            'description':ORYX.I18N.Save.saveAsDesc,
            'index':2,
            'minShape':0,
            'maxShape':0
        });

        // window.onbeforeunload = this.onUnLoad.bind(this);
        // this.changeDifference = 0;
        //
        // // Register on event for executing commands --> store all commands in a stack
        // this.facade.registerOnEvent(ORYX.CONFIG.EVENT_UNDO_EXECUTE, function () {
        //     this.changeDifference++;
        //     this.updateTitle();
        // }.bind(this));
        // this.facade.registerOnEvent(ORYX.CONFIG.EVENT_EXECUTE_COMMANDS, function () {
        //     this.changeDifference++;
        //     this.updateTitle();
        // }.bind(this));
        // this.facade.registerOnEvent(ORYX.CONFIG.EVENT_UNDO_ROLLBACK, function () {
        //     this.changeDifference--;
        //     this.updateTitle();
        // }.bind(this));

    },

    updateTitle:function () {

    },

    onUnLoad:function () {

    },

    /**
     * Saves the current process to the server.
     */
    save:function (forceNew, event) {
        if (this.saving) {
            return false;
        }

        this.saving = true;

        var xml = this.facade.getXML();
        var svg = this.facade.getSVG();

        if (forceNew) {
            if (ORYX.Plugins.ApromoreSave.apromoreSaveAs) {
                ORYX.Plugins.ApromoreSave.apromoreSaveAs(xml, svg);
            } else {
                alert("Apromore Save As method is missing!");
            }
        } else {
            if (ORYX.Plugins.ApromoreSave.apromoreSave) {
                ORYX.Plugins.ApromoreSave.apromoreSave(xml, svg);
            } else {
                alert("Apromore Save method is missing!");
            }
        }

        this.saving = false;
        return true;
    }

});


/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/
if (!ORYX.Plugins)
    ORYX.Plugins = new Object();

ORYX.Plugins.Export = Clazz.extend({

    facade: undefined,

    construct: function(facade){
        this.facade = facade;

        this.facade.offer({
            'name': ORYX.I18N.File.svg,
            'functionality': this.exportSVG.bind(this),
            'group': ORYX.I18N.File.group,
            'icon': ORYX.PATH + "images/exportsvg.png",
            'description': ORYX.I18N.File.svgDesc,
            'index': 3,
            'minShape': 0,
            'maxShape': 0
        });

        this.facade.offer({
            'name': ORYX.I18N.File.bpmn,
            'functionality': this.exportBPMN.bind(this),
            'group': ORYX.I18N.File.group,
            'icon': ORYX.PATH + "images/exportbpmn.png",
            'description': ORYX.I18N.File.bpmnDesc,
            'index': 4,
            'minShape': 0,
            'maxShape': 0
        });

    },

    exportSVG: function() {
        var svg = this.facade.getSVG();
        var hiddenElement = document.createElement('a');
        hiddenElement.href = 'data:application/bpmn20-xml;charset=UTF-8,' + encodeURIComponent(svg);
        hiddenElement.target = '_blank';
        hiddenElement.download = 'diagram.svg';
        hiddenElement.click();
    },

    exportBPMN: function() {
        var xml = this.facade.getXML();
        var hiddenElement = document.createElement('a');
        hiddenElement.href = 'data:application/bpmn20-xml;charset=UTF-8,' + encodeURIComponent(xml);
        hiddenElement.target = '_blank';
        hiddenElement.download = 'diagram.bpmn';
        hiddenElement.click();
    }

});

/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/
if (!ORYX.Plugins)
    ORYX.Plugins = new Object();

ORYX.Plugins.File = Clazz.extend({

    facade: undefined,

    construct: function(facade){
        this.facade = facade;

        this.facade.offer({
            'name': ORYX.I18N.File.pdf,
            'functionality': this.exportPDF.bind(this),
            'group': ORYX.I18N.File.group,
            'icon': ORYX.PATH + "images/page_white_acrobat.png",
            'description': ORYX.I18N.File.pdfDesc,
            'index': 5,
            'minShape': 0,
            'maxShape': 0
        });
    },

    exportPDF: function() {
        var myMask = new Ext.LoadMask(Ext.getBody(), {msg:"Please wait..."});
        myMask.show();

        var resource = location.href;

        // Get the serialized svg image source
        var svgClone = this.facade.getCanvas().getSVG();
        //var svgDOM = DataManager.serialize(svgClone);

        // Send the svg to the server.
        new Ajax.Request(ORYX.CONFIG.PDF_EXPORT_URL, {
            method: 'POST',
            parameters: {
                resource: resource,
                data: svgClone,
                format: "pdf"
            },
            onSuccess: (function(request){
                myMask.hide();
                // Because the pdf may be opened in the same window as the
                // process, yet the process may not have been saved, we're
                // opening every other representation in a new window.
                // location.href = request.responseText
                window.open(request.responseText);
            }).bind(this),
            onFailure: (function(){
                myMask.hide();
                Ext.Msg.alert(ORYX.I18N.Oryx.title, ORYX.I18N.File.genPDFFailed);
            }).bind(this)
        });
    }

});

/**
 * @namespace Oryx name space for plugins
 * @name ORYX.Plugins
 */
if (!ORYX.Plugins)
    ORYX.Plugins = new Object();

/**
 * The simulation panel plugin offers functionality to change model simulation parameters over the
 * simulation parameters panel.
 *
 * @class ORYX.Plugins.SimulationPanel
 * @extends Clazz
 * @param {Object} facade The editor facade for plugins.
 */
ORYX.Plugins.SimulationPanel = Clazz.extend({
    /** @lends ORYX.Plugins.SimulationPanel.prototype */
    facade: undefined,

    construct: function (facade) {
        this.facade = facade;

        /* Register toggle simulation panel */
        this.facade.offer({
            'name': ORYX.I18N.SimulationPanel.toggleSimulationDrawer,
            'functionality': this.toggleSimulationDrawer.bind(this),
            'group': ORYX.I18N.SimulationPanel.group,
            'description': ORYX.I18N.SimulationPanel.toggleSimulationDrawerDesc,
            'index': 1,
            'minShape': 0,
            'maxShape': 0,
            isEnabled : function(){ return facade.useSimulationPanel}.bind(this),
        });
    },

    /**
     * Shortcut for performing an expand or collapse based on the current state of the panel.
     */
    toggleSimulationDrawer: function () {
        this.facade.getSimulationDrawer().toggleCollapse(true);
    }
});/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/


if(!ORYX.Plugins) {
	ORYX.Plugins = new Object();
}

ORYX.Plugins.Toolbar = Clazz.extend({

	facade: undefined,
	plugs:	[],

	construct: function(facade, ownPluginData) {
		this.facade = facade;

		this.groupIndex = new Hash();
		ownPluginData.properties.each((function(value){
			if(value.group && value.index != undefined) {
				this.groupIndex[value.group] = value.index
			}
		}).bind(this));

		Ext.QuickTips.init();

		this.buttons = [];
        this.facade.registerOnEvent(ORYX.CONFIG.EVENT_BUTTON_UPDATE, this.onButtonUpdate.bind(this));
	},

    /**
     * Can be used to manipulate the state of a button.
     * @example
     * this.facade.raiseEvent({
     *   type: ORYX.CONFIG.EVENT_BUTTON_UPDATE,
     *   id: this.buttonId, // have to be generated before and set in the offer method
     *   pressed: true
     * });
     * @param {Object} event
     */
    onButtonUpdate: function(event){
        var button = this.buttons.find(function(button){
            return button.id === event.id;
        });

        if(event.pressed !== undefined){
            button.buttonInstance.toggle(event.pressed);
        }
    },

	registryChanged: function(pluginsData) {
        // Sort plugins by group and index
		var newPlugs =  pluginsData.sortBy((function(value) {
			return ((this.groupIndex[value.group] != undefined ? this.groupIndex[value.group] : "" ) + value.group + "" + value.index).toLowerCase();
		}).bind(this));
		var plugs = $A(newPlugs).findAll(function(value){
										return !this.plugs.include(value) && (!value.target || value.target === ORYX.Plugins.Toolbar)
									}.bind(this));
		if(plugs.length<1)
			return;

		this.buttons = [];

		ORYX.Log.trace("Creating a toolbar.")

        if(!this.toolbar){
			this.toolbar = new Ext.ux.SlicedToolbar({
			height: 24
		});
				var region = this.facade.addToRegion("north", this.toolbar, "Toolbar");
		}


		var currentGroupsName = this.plugs.last()?this.plugs.last().group:plugs[0].group;

        // Map used to store all drop down buttons of current group
        var currentGroupsDropDownButton = {};


		plugs.each((function(value) {
			if(!value.name) {return}
			this.plugs.push(value);
            // Add seperator if new group begins
			if(currentGroupsName != value.group) {
			    this.toolbar.add('-');
				currentGroupsName = value.group;
                currentGroupsDropDownButton = {};
			}

            // If an drop down group icon is provided, a split button should be used
            if(value.dropDownGroupIcon){
                var splitButton = currentGroupsDropDownButton[value.dropDownGroupIcon];

                // Create a new split button if this is the first plugin using it
                if(splitButton === undefined){
                    splitButton = currentGroupsDropDownButton[value.dropDownGroupIcon] = new Ext.Toolbar.SplitButton({
                        cls: "x-btn-icon", //show icon only
                        icon: value.dropDownGroupIcon,
                        menu: new Ext.menu.Menu({
                            items: [] // items are added later on
                        }),
                        listeners: {
                          click: function(button, event){
                            // The "normal" button should behave like the arrow button
                            if(!button.menu.isVisible() && !button.ignoreNextClick){
                                button.showMenu();
                            } else {
                                button.hideMenu();
                            }
                          }
                        }
                    });

                    this.toolbar.add(splitButton);
                }

                // General config button which will be used either to create a normal button
                // or a check button (if toggling is enabled)
                var buttonCfg = {
                    icon: value.icon,
                    text: value.name,
                    itemId: value.id,
                    handler: value.toggle ? undefined : value.functionality,
                    checkHandler: value.toggle ? value.functionality : undefined,
                    listeners: {
                        render: function(item){
                            // After rendering, a tool tip should be added to component
                            if (value.description) {
                                new Ext.ToolTip({
                                    target: item.getEl(),
                                    title: value.description
                                })
                            }
                        }
                    }
                }

                // Create buttons depending on toggle
                if(value.toggle) {
                    var button = new Ext.menu.CheckItem(buttonCfg);
                } else {
                    var button = new Ext.menu.Item(buttonCfg);
                }

                splitButton.menu.add(button);

            } else if(value.addFill) {
				this.toolbar.addFill();
			} else { // create normal, simple button
                var button = new Ext.Toolbar.Button({
                    icon:           value.icon,         // icons can also be specified inline
                    cls:            'x-btn-icon',       // Class who shows only the icon
                    itemId:         value.id,
					tooltip:        value.description,  // Set the tooltip
                    tooltipType:    'title',            // Tooltip will be shown as in the html-title attribute
                    handler:        value.toggle ? null : value.functionality,  // Handler for mouse click
                    enableToggle:   value.toggle, // Option for enabling toggling
                    toggleHandler:  value.toggle ? value.functionality : null // Handler for toggle (Parameters: button, active)
                });

                this.toolbar.add(button);

                button.getEl().onclick = function() {this.blur()}
            }

			value['buttonInstance'] = button;
			this.buttons.push(value);

		}).bind(this));

		this.enableButtons([]);

        //TODO this should be done when resizing and adding elements!!!!
        this.toolbar.calcSlices();
		window.addEventListener("resize", function(event){this.toolbar.calcSlices()}.bind(this), false);
		window.addEventListener("onresize", function(event){this.toolbar.calcSlices()}.bind(this), false);

	},

	onSelectionChanged: function(event) {
		this.enableButtons(event.elements);
	},

	enableButtons: function(elements) {
		// Show the Buttons
		this.buttons.each((function(value){
			value.buttonInstance.enable();

			// If there is less elements than minShapes
			if(value.minShape && value.minShape > elements.length)
				value.buttonInstance.disable();
			// If there is more elements than minShapes
			if(value.maxShape && value.maxShape < elements.length)
				value.buttonInstance.disable();
			// If the plugin is not enabled
			if(value.isEnabled && !value.isEnabled(value.buttonInstance))
				value.buttonInstance.disable();

		}).bind(this));
	}
});

Ext.ns("Ext.ux");
Ext.ux.SlicedToolbar = Ext.extend(Ext.Toolbar, {
    currentSlice: 0,
    iconStandardWidth: 22, //22 px
    seperatorStandardWidth: 2, //2px, minwidth for Ext.Toolbar.Fill
    toolbarStandardPadding: 2,

    initComponent: function(){
        Ext.apply(this, {
        });
        Ext.ux.SlicedToolbar.superclass.initComponent.apply(this, arguments);
    },

    onRender: function(){
        Ext.ux.SlicedToolbar.superclass.onRender.apply(this, arguments);
    },

    onResize: function(){
        Ext.ux.SlicedToolbar.superclass.onResize.apply(this, arguments);
    },

    calcSlices: function(){
        var slice = 0;
        this.sliceMap = {};
        var sliceWidth = 0;
        var toolbarWidth = this.getEl().getWidth();

        this.items.getRange().each(function(item, index){
            //Remove all next and prev buttons
            if (item.helperItem) {
                item.destroy();
                return;
            }

            var itemWidth = item.getEl().getWidth();

            if(sliceWidth + itemWidth + 5 * this.iconStandardWidth > toolbarWidth){
                var itemIndex = this.items.indexOf(item);

                this.insertSlicingButton("next", slice, itemIndex);

                if (slice !== 0) {
                    this.insertSlicingButton("prev", slice, itemIndex);
                }

                this.insertSlicingSeperator(slice, itemIndex);

                slice += 1;
                sliceWidth = 0;
            }

            this.sliceMap[item.id] = slice;
            sliceWidth += itemWidth;
        }.bind(this));

        // Add prev button at the end
        if(slice > 0){
            this.insertSlicingSeperator(slice, this.items.getCount()+1);
            this.insertSlicingButton("prev", slice, this.items.getCount()+1);
            var spacer = new Ext.Toolbar.Spacer();
            this.insertSlicedHelperButton(spacer, slice, this.items.getCount()+1);
            Ext.get(spacer.id).setWidth(this.iconStandardWidth);
        }

        this.maxSlice = slice;

        // Update view
        this.setCurrentSlice(this.currentSlice);
    },

    insertSlicedButton: function(button, slice, index){
        this.insertButton(index, button);
        this.sliceMap[button.id] = slice;
    },

    insertSlicedHelperButton: function(button, slice, index){
        button.helperItem = true;
        this.insertSlicedButton(button, slice, index);
    },

    insertSlicingSeperator: function(slice, index){
        // Align right
        this.insertSlicedHelperButton(new Ext.Toolbar.Fill(), slice, index);
    },

    // type => next or prev
    insertSlicingButton: function(type, slice, index){
        var nextHandler = function(){this.setCurrentSlice(this.currentSlice+1)}.bind(this);
        var prevHandler = function(){this.setCurrentSlice(this.currentSlice-1)}.bind(this);

        var button = new Ext.Toolbar.Button({
            cls: "x-btn-icon",
            icon: ORYX.CONFIG.ROOT_PATH + "images/toolbar_"+type+".png",
            handler: (type === "next") ? nextHandler : prevHandler
        });

        this.insertSlicedHelperButton(button, slice, index);
    },

    setCurrentSlice: function(slice){
        if(slice > this.maxSlice || slice < 0) return;

        this.currentSlice = slice;

        this.items.getRange().each(function(item){
            item.setVisible(slice === this.sliceMap[item.id]);
        }.bind(this));
    }
});/**
 * Copyright (c) 2008
 * Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/


/**
 * This plugin offer the functionality of undo/redo
 * Therewith the command pattern is used.
 *
 * A Plugin which want that the changes could get undo/redo has
 * to implement a command-class (which implements the method .execute(), .rollback()).
 * Those instance of class must be execute thru the facade.executeCommands(). If so,
 * those command get stored here in the undo/redo stack and can get reset/restore.
 *
 **/

if (!ORYX.Plugins)
    ORYX.Plugins = new Object();

ORYX.Plugins.Undo = Clazz.extend({

	// Defines the facade
    facade		: undefined,

	// Defines the undo/redo Stack
	undoStack	: [],
	redoStack	: [],

	// Constructor
    construct: function(facade){

        this.facade = facade;

		// Offers the functionality of undo
        this.facade.offer({
			name			: ORYX.I18N.Undo.undo,
			description		: ORYX.I18N.Undo.undoDesc,
			icon			: ORYX.PATH + "images/arrow_undo.png",
			keyCodes: [{
					metaKeys: [ORYX.CONFIG.META_KEY_META_CTRL],
					keyCode: 90,
					keyAction: ORYX.CONFIG.KEY_ACTION_DOWN
				}
		 	],
			functionality	: this.doUndo.bind(this),
			group			: ORYX.I18N.Undo.group,
			isEnabled		: function(){ return true }.bind(this),
			index			: 0
		});

		// Offers the functionality of redo
        this.facade.offer({
			name			: ORYX.I18N.Undo.redo,
			description		: ORYX.I18N.Undo.redoDesc,
			icon			: ORYX.PATH + "images/arrow_redo.png",
			keyCodes: [{
					metaKeys: [ORYX.CONFIG.META_KEY_META_CTRL],
					keyCode: 89,
					keyAction: ORYX.CONFIG.KEY_ACTION_DOWN
				}
		 	],
			functionality	: this.doRedo.bind(this),
			group			: ORYX.I18N.Undo.group,
			isEnabled		: function(){ return true}.bind(this),
			index			: 1
		});

		// Register on event for executing commands --> store all commands in a stack
		//this.facade.registerOnEvent(ORYX.CONFIG.EVENT_EXECUTE_COMMANDS, this.handleExecuteCommands.bind(this) );

	},

	/**
	 * Stores all executed commands in a stack
	 *
	 * @param {Object} evt
	 */
	handleExecuteCommands: function( evt ){

	},

	/**
	 * Does the undo
	 *
	 */
	doUndo: function(){
        this.facade.getCanvas().undo();
	},

	/**
	 * Does the redo
	 *
	 */
	doRedo: function(){
        this.facade.getCanvas().redo();
	}

});
/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/

/**
 * @namespace Oryx name space for plugins
 * @name ORYX.Plugins
 */
if (!ORYX.Plugins)
    ORYX.Plugins = new Object();

/**
 * The view plugin offers all of zooming functionality accessible over the
 * tool bar. This are zoom in, zoom out, zoom to standard, zoom fit to model.
 *
 * @class ORYX.Plugins.View
 * @extends Clazz
 * @param {Object} facade The editor facade for plugins.
 */
ORYX.Plugins.View = Clazz.extend({
    /** @lends ORYX.Plugins.View.prototype */
    facade: undefined,

    construct: function (facade) {
        this.facade = facade;

        /* Register zoom in */
        this.facade.offer({
            'name': ORYX.I18N.View.zoomIn,
            'functionality': this.zoomIn.bind(this),
            'group': ORYX.I18N.View.group,
            'icon': ORYX.PATH + "images/magnifier_zoom_in.png",
            'description': ORYX.I18N.View.zoomInDesc,
            'index': 1,
            'minShape': 0,
            'maxShape': 0});

        /* Register zoom out */
        this.facade.offer({
            'name': ORYX.I18N.View.zoomOut,
            'functionality': this.zoomOut.bind(this),
            'group': ORYX.I18N.View.group,
            'icon': ORYX.PATH + "images/magnifier_zoom_out.png",
            'description': ORYX.I18N.View.zoomOutDesc,
            'index': 2,
            'minShape': 0,
            'maxShape': 0});

        /* Register zoom fit to model */
        this.facade.offer({
            'name': ORYX.I18N.View.zoomFitToModel,
            'functionality': this.zoomFitToModel.bind(this),
            'group': ORYX.I18N.View.group,
            'icon': ORYX.PATH + "images/image.png",
            'description': ORYX.I18N.View.zoomFitToModelDesc,
            'index': 4,
            'minShape': 0,
            'maxShape': 0 });
    },

    zoomIn: function (factor) {
        this.facade.getCanvas().zoomIn();
    },

    zoomOut: function (factor) {
        this.facade.getCanvas().zoomOut();
    },



    /**
     * It calculates the zoom level to fit whole model into the visible area
     * of the canvas. Than the model gets zoomed and the position of the
     * scroll bars are adjusted.
     *
     */
    zoomFitToModel: function () {
        this.facade.getCanvas().zoomFitToModel();
    }
});

export {ORYX};