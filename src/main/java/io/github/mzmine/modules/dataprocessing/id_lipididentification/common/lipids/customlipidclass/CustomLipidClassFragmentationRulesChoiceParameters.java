/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.customlipidclass;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CustomLipidClassFragmentationRulesChoiceParameters implements
    UserParameter<LipidFragmentationRule[], CustomLipidClassFragmentationRulesChoiceComponent> {

  private final String name;
  private final String description;
  private LipidFragmentationRule[] choices;
  private LipidFragmentationRule[] values;

  /**
   * Create the parameter.
   *
   * @param name name of the parameter.
   * @param description description of the parameter.
   */
  public CustomLipidClassFragmentationRulesChoiceParameters(String name, String description,
      LipidFragmentationRule[] choices) {
    this.name = name;
    this.description = description;
    this.choices = choices;
    this.values = choices;
  }

  @Override
  public CustomLipidClassFragmentationRulesChoiceComponent createEditingComponent() {
    return new CustomLipidClassFragmentationRulesChoiceComponent(choices);
  }

  @Override
  public void setValueFromComponent(
      final CustomLipidClassFragmentationRulesChoiceComponent component) {
    values = component.getValue().toArray(new LipidFragmentationRule[0]);
    choices =
        component.getChoices().toArray(new LipidFragmentationRule[0]);
  }

  @Override
  public void setValueToComponent(CustomLipidClassFragmentationRulesChoiceComponent component,
      @Nullable LipidFragmentationRule[] newValue) {
    if (newValue == null) {
      component.setValue(List.of());
      return;
    }
    component.setValue(Arrays.asList(newValue));
  }

  @Override
  public CustomLipidClassFragmentationRulesChoiceParameters cloneParameter() {

    final CustomLipidClassFragmentationRulesChoiceParameters copy =
        new CustomLipidClassFragmentationRulesChoiceParameters(name, description, choices);
    copy.setValue(values);
    return copy;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public LipidFragmentationRule[] getValue() {
    return values;
  }

  public LipidFragmentationRule[] getChoices() {
    return choices;
  }

  @Override
  public void setValue(LipidFragmentationRule[] newValue) {
    this.values = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList items = xmlElement.getElementsByTagName("item");
    ArrayList<LipidFragmentationRule> newValues = new ArrayList<>();
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      for (int j = 0; j < choices.length; j++) {
        if (choices[j].toString().equals(itemString)) {
          newValues.add(choices[j]);
        }
      }
    }
    this.values = newValues.toArray(new LipidFragmentationRule[0]);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (values == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();
    for (LipidFragmentationRule item : values) {
      Element newElement = parentDocument.createElement("item");
      newElement.setTextContent(item.toString());
      xmlElement.appendChild(newElement);
    }
  }
}
