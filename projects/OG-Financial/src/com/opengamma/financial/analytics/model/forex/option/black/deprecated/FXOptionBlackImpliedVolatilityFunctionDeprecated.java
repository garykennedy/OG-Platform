/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.forex.option.black.deprecated;

import java.util.Collections;
import java.util.Set;

import com.opengamma.analytics.financial.forex.calculator.ImpliedVolatilityBlackForexCalculator;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.model.option.definition.SmileDeltaTermStructureDataBundle;
import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.engine.value.ValueSpecification;

/**
 * Function to compute the implied volatility for Forex options in the Black model.
 * @deprecated Use the version that does not refer to funding or forward curves
 * @see FXOptionBlackImpliedVolatilityFunction
 */
@Deprecated
public class FXOptionBlackImpliedVolatilityFunctionDeprecated extends FXOptionBlackMultiValuedFunctionDeprecated {

  /**
   * The related calculator.
   */
  private static final ImpliedVolatilityBlackForexCalculator CALCULATOR = ImpliedVolatilityBlackForexCalculator.getInstance();

  public FXOptionBlackImpliedVolatilityFunctionDeprecated() {
    super(ValueRequirementNames.SECURITY_IMPLIED_VOLATILITY);
  }

  @Override
  protected Set<ComputedValue> getResult(final InstrumentDerivative fxOption, final SmileDeltaTermStructureDataBundle data, final ValueSpecification spec) {
    final Double result = CALCULATOR.visit(fxOption, data);
    return Collections.singleton(new ComputedValue(spec, result));
  }

}
