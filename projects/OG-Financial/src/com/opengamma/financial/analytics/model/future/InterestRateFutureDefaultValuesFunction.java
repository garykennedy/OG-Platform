/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.future;

import java.util.Collections;
import java.util.Set;

import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetType;
import com.opengamma.engine.function.FunctionCompilationContext;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.financial.analytics.ircurve.YieldCurveFunction;
import com.opengamma.financial.property.DefaultPropertyFunction;
import com.opengamma.financial.security.FinancialSecurityUtils;
import com.opengamma.financial.security.future.InterestRateFutureSecurity;

/**
 * Dummy function for injecting default curve names into the dependency graph.
 */
public class InterestRateFutureDefaultValuesFunction extends DefaultPropertyFunction {
  
  private static final String[] s_valueNames = new String[] {
    ValueRequirementNames.PRESENT_VALUE,
    ValueRequirementNames.YIELD_CURVE_NODE_SENSITIVITIES};
  
  private final String[] _applicableCurrencyNames;
  private final String _forwardCurve;
  private final String _fundingCurve;

  public InterestRateFutureDefaultValuesFunction(final String forwardCurve, final String fundingCurve,  final String... applicableCurrencyNames) {
    super(ComputationTargetType.TRADE, true);
    _forwardCurve = forwardCurve;
    _fundingCurve = fundingCurve;
    _applicableCurrencyNames = applicableCurrencyNames;
  }

  @Override
  public boolean canApplyTo(final FunctionCompilationContext context, final ComputationTarget target) {
    if (target.getType() != ComputationTargetType.TRADE) {
      return false;
    }
    if (!(target.getTrade().getSecurity() instanceof InterestRateFutureSecurity)) {
      return false;
    }
    for (final String applicableCurrencyName : _applicableCurrencyNames) {
      if (applicableCurrencyName.equals(FinancialSecurityUtils.getCurrency(target.getTrade().getSecurity()).getCode())) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void getDefaults(final PropertyDefaults defaults) {
    for (final String valueName : s_valueNames) {
      defaults.addValuePropertyName(valueName, YieldCurveFunction.PROPERTY_FORWARD_CURVE);
      defaults.addValuePropertyName(valueName, YieldCurveFunction.PROPERTY_FUNDING_CURVE);
    }
  }

  @Override
  protected Set<String> getDefaultValue(final FunctionCompilationContext context, final ComputationTarget target, final ValueRequirement desiredValue, final String propertyName) {
    if (YieldCurveFunction.PROPERTY_FORWARD_CURVE.equals(propertyName)) {
      return Collections.singleton(_forwardCurve);
    } else if (YieldCurveFunction.PROPERTY_FUNDING_CURVE.equals(propertyName)) {
      return Collections.singleton(_fundingCurve);
    }
    return null;
  }

}