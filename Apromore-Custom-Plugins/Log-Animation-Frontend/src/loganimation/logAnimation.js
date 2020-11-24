/*-
 * #%L
 * This file is part of "Apromore Core".
 *
 * Copyright (C) 2017 Queensland University of Technology.
 * %%
 * Copyright (C) 2018 - 2020 The University of Melbourne.
 * %%
 *
 * "Apromore" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * "Apromore" is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.
 * If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

'use strict';

import AnimationController from "./animationController";
import {ORYX} from "../bpmneditor/apromoreEditor";
/*
// $.noConflict();
window.$j = jQuery.noConflict();
$j.browser = {};
$j.browser.mozilla =
    /mozilla/.test(navigator.userAgent.toLowerCase()) &&
    !/webkit/.test(navigator.userAgent.toLowerCase());
$j.browser.webkit = /webkit/.test(navigator.userAgent.toLowerCase());
$j.browser.opera = /opera/.test(navigator.userAgent.toLowerCase());
$j.browser.msie = /msie/.test(navigator.userAgent.toLowerCase());
*/

/*
ORYX.Plugins.ApromoreSave.apromoreSaveAs = function(xml, svg) {
  zAu.send(new zk.Event(zk.Widget.$(jq("$win")), "onSaveAs", xml));
};

ORYX.Plugins.ApromoreSave.apromoreSave = function(xml, svg) {
  zAu.send(new zk.Event(zk.Widget.$(jq("$win")), "onSave", xml));
};
*/

export default class LogAnimation {
  constructor(editorParentId, xml, url, namespace, animationData, pluginExecutionId) {
    let editor = this._initEditor(editorParentId, xml, url, namespace);
    window.setTimeout((function() {
      this.controller = this._initAnimationController(editor.getCanvas(), animationData, pluginExecutionId);
    }).bind(this), 1000);
  }

  getAnimationController() {
    return this.controller;
  }

  _initAnimationController(editorCanvas, animationData, pluginExecutionId) {
    let controller = new AnimationController(editorCanvas, pluginExecutionId);
    this.setPlayControls(true); // Disable play controls as the controller init. may take time
    controller.initialize(animationData);
    this.setPlayControls(false);
    document.title = "Apromore - Log Animator";
    return controller;
  }

  /**
   *
   * @param {String} editorParentId: id of the div element hosting the editor
   * @param {String} xml: XML content of the BPMN map/model
   * @param {String} url:
   * @param namespace
   * @returns {*}
   */
  _initEditor(editorParentId, xml, url, namespace) {
    return new ORYX.Editor({
      xml,
      model: {
        id: editorParentId,
        showPlugins: false,
        stencilset: {
          url,
          namespace
        }
      },
      fullscreen: true // false
    });
  }

  setPlayControls(disabled) {
    $j("#start").get(0).disabled = disabled;
    $j("#pause").get(0).disabled = disabled;
    $j("#forward").get(0).disabled = disabled;
    $j("#backward").get(0).disabled = disabled;
    $j("#end").get(0).disabled = disabled;
    $j("#speed-control").get(0).disabled = disabled;
  }

  playPause(e) {
    this.controller.playPause(e);
  }

  fastForward() {
    this.controller.fastForward();
  }

  fastBackward() {
    this.controller.fastBackward();
  }

  gotoStart (e) {
    this.controller.gotoStart();
  }

  gotoEnd(e) {
    this.controller.gotoEnd();
  }

};