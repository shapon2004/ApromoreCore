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
/**
 *  Color picker
 *  based on spectrum
 *
 *  Dependencies:
 *  - spectrum
 *  - Ap.common.palette.gradation
 */
(function() {

    // Apromore default palette
    let palette = Object.entries(Ap.common.palette.gradation).map(entry => entry[1]); // flatten the palette

    Ap.colorPicker = Ap.colorPicker || {};

    Object.assign(Ap.colorPicker, {
        install: function (id) {
            return jq(id).spectrum({
              type: "color",
              showInput: true,
              showInitial: true,
              showAlpha: false,
              allowEmpty: false,
              palette
            });
        },
        set: function (id, hexColor) {
            jq(id).spectrum('set', hexColor);
        },
        get: function (id) {
            return jq(id).spectrum('get');
        },
        onChange: function (id, f) {
            jq(id).on('change.spectrum', function(e, tinycolor) {
                let color = tinycolor.toHexString();
                if (f) {
                    f(color);
                }
            });
        },
        onChangeZK: function (id, zkWidgetId, event) {
            jq(id).on('change.spectrum', function(e, tinycolor) {
                let color = tinycolor.toHexString();
                zAu.send(new zk.Event(zk.Widget.$(zkWidgetId), event, { color }));
            });
        }
    });

})();

