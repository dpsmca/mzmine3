/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes.submodules;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

/**
 *
 */
public class OptionalModuleComponent extends BorderPane {

  protected final ParameterSetupPane paramPane;
  protected final CheckBox checkBox;
  private final Button setButton;
  private boolean hidden = true;


  public OptionalModuleComponent(ParameterSet embeddedParameters) {
    this(embeddedParameters, "", false);
  }

  public OptionalModuleComponent(ParameterSet embeddedParameters, String title, boolean active) {
    super();
    paramPane = new ParameterSetupPane(true, embeddedParameters, false, false, null, true, false);

    checkBox = new CheckBox(title);
    setSelected(active);

    checkBox.selectedProperty().addListener((ob, ov, nv) -> applyCheckBoxState());
    applyCheckBoxState();

    setButton = new Button("Show");
    setButton.setOnAction(e -> {
      hidden = !hidden;
      // change text
      setButton.setText(hidden ? "Show" : "Hide");
      setTop(hidden ? null : paramPane);
    });
    setButton.setDisable(!active);

    var pane = new FlowPane();
    pane.setHgap(7d);
    pane.getChildren().addAll(checkBox, setButton);

    setTop(pane);
  }

  public ParameterSetupPane getEmbeddedParameterPane() {
    return paramPane;
  }

  public boolean isSelected() {
    return checkBox.isSelected();
  }

  public void setSelected(boolean state) {
    checkBox.setSelected(state);
  }

  public void setToolTipText(String toolTip) {
    checkBox.setTooltip(new Tooltip(toolTip));
  }

  public CheckBox getCheckbox() {
    return checkBox;
  }

  protected void applyCheckBoxState() {
    setButton.setDisable(!checkBox.isSelected());
    paramPane.getParametersAndComponents().values()
        .forEach(node -> node.setDisable(!checkBox.isSelected()));
  }

  public void setParameterValuesToComponents() {
    paramPane.setParameterValuesToComponents();
  }

  public void updateParameterSetFromComponents() {
    paramPane.updateParameterSetFromComponents();
  }
}
