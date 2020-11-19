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
import {$ as $j} from 'jquery';

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

export class LogAnimation {
  constructor(xml, url, namespace, data, pluginExecutionId) {
    this.animationData = data;
    this.editor = this.initEditor(xml, url, namespace);
    this.controller = new AnimationController(this.editor.getCanvas(), pluginExecutionId);
    this.initSpeedControl();
  }

  /*
  init(xml, url, namespace, data, pluginExecutionId) {
    this.animationData = data;
    this.editor = initEditor(xml, url, namespace);
    this.controller = new AnimationController(editor.getCanvas(), pluginExecutionId);
    initSpeedControl();
  }
  */

  initEditor(xml, url, namespace) {
    return new ORYX.Editor({
      xml,
      model: {
        id: "editorcanvas",
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
    $j(this.SPEED_CONTROL).get(0).disabled = disabled;
  }

  initController() {
    if (!this.animationData) {
      // No data
      return;
    }
    this.setPlayControls(true); // Disable play controls as the controller init. may take time
    this.controller.initialize(this.animationData);
    this.setPlayControls(false);
    document.title = "Apromore - Log Animator";
  }

  getController() {
    return this.controller;
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

  toggleCaseLabelVisibility() {
    let input = $j("input#toggleCaseLabelVisibility")[0];
    this.controller.setCaseLabelsVisible(input.checked);
  }

};