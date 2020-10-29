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

// $.noConflict();
window.$j = jQuery.noConflict();
$j.browser = {};
$j.browser.mozilla =
    /mozilla/.test(navigator.userAgent.toLowerCase()) &&
    !/webkit/.test(navigator.userAgent.toLowerCase());
$j.browser.webkit = /webkit/.test(navigator.userAgent.toLowerCase());
$j.browser.opera = /opera/.test(navigator.userAgent.toLowerCase());
$j.browser.msie = /msie/.test(navigator.userAgent.toLowerCase());

ORYX.Plugins.ApromoreSave.apromoreSaveAs = function(xml, svg) {
  zAu.send(new zk.Event(zk.Widget.$(jq("$win")), "onSaveAs", xml));
};

ORYX.Plugins.ApromoreSave.apromoreSave = function(xml, svg) {
  zAu.send(new zk.Event(zk.Widget.$(jq("$win")), "onSave", xml));
};

Ap.la.session = (function() {
  const SPEED_CONTROL = "#speed-control";
  let STEP_VALUES = [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 2.25, 2.5];
  let editor, controller, animationData;

  function init(xml, url, namespace, data, pluginExecutionId) {
    animationData = data;
    editor = initEditor(xml, url, namespace);
    controller = new AnimationController(editor.getCanvas(), pluginExecutionId);
    initSpeedControl();
  }

  function initEditor(xml, url, namespace) {
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

  function setPlayControls(disabled) {
    $j("#start").get(0).disabled = disabled;
    $j("#pause").get(0).disabled = disabled;
    $j("#forward").get(0).disabled = disabled;
    $j("#backward").get(0).disabled = disabled;
    $j("#end").get(0).disabled = disabled;
    $j(SPEED_CONTROL).get(0).disabled = disabled;
  }

  function initController() {
    if (!animationData) {
      // No data
      return;
    }
    setPlayControls(true); // Disable play controls as the controller init. may take time
    controller.initialize(animationData);
    setPlayControls(false);
    document.title = "Apromore - Log Animator";
  }

  function initSpeedControl() {
    let speedControl = $j(SPEED_CONTROL)

    speedControl.slider({
      orientation: "horizontal",
      step: 1,
      min: 1,
      max: 8,
      value: 4
    });
    speedControl.slider("float", {
      handle: true,
      pips: true,
      labels: ['0.25', '0.5', '0.75', '1', '1.25', '1.5', '1.75', '2'],
      prefix: "",
      suffix: ""
    });

    let lastSliderValue = speedControl.slider("value");
    speedControl.on("slidechange", function(event, ui) {
      let speedRatio = STEP_VALUES[ui.value - 1] / STEP_VALUES[lastSliderValue - 1];
      controller.changeSpeed(STEP_VALUES[ui.value - 1]);
      lastSliderValue = ui.value;
    });
  }

  return {
    init: init,
    initController: initController,
    getController: function() {
      return controller;
    },
    playPause: function(e) {
      controller.playPause(e);
    },
    fastForward: function() {
      controller.fastForward();
    },
    fastBackward: function() {
      controller.fastBackward();
    },
    gotoStart: function(e) {
      controller.gotoStart();
    },
    gotoEnd: function(e) {
      controller.gotoEnd();
    },
    toggleCaseLabelVisibility: function() {
      let input = $j("input#toggleCaseLabelVisibility")[0];
      controller.setCaseLabelsVisible(input.checked);
    }
  };
})();
