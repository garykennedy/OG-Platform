/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.masterdb.security.hibernate.option;

import static com.opengamma.masterdb.security.hibernate.Converters.currencyBeanToCurrency;
import static com.opengamma.masterdb.security.hibernate.Converters.expiryBeanToExpiry;
import static com.opengamma.masterdb.security.hibernate.Converters.expiryToExpiryBean;
import static com.opengamma.masterdb.security.hibernate.Converters.identifierBeanToIdentifier;
import static com.opengamma.masterdb.security.hibernate.Converters.identifierToIdentifierBean;

import com.opengamma.financial.security.option.AmericanExerciseType;
import com.opengamma.financial.security.option.AsianExerciseType;
import com.opengamma.financial.security.option.BermudanExerciseType;
import com.opengamma.financial.security.option.EquityIndexOptionSecurity;
import com.opengamma.financial.security.option.EuropeanExerciseType;
import com.opengamma.financial.security.option.ExerciseType;
import com.opengamma.financial.security.option.ExerciseTypeVisitor;
import com.opengamma.masterdb.security.hibernate.AbstractSecurityBeanOperation;
import com.opengamma.masterdb.security.hibernate.HibernateSecurityMasterDao;
import com.opengamma.masterdb.security.hibernate.OperationContext;

/**
 * EquityIndexOptionSecurityBeanOperation
 */
public final class EquityIndexOptionSecurityBeanOperation  extends AbstractSecurityBeanOperation<EquityIndexOptionSecurity, EquityIndexOptionSecurityBean> {

  /**
   * Singleton
   */
  public static final EquityIndexOptionSecurityBeanOperation INSTANCE = new EquityIndexOptionSecurityBeanOperation();
  
  private EquityIndexOptionSecurityBeanOperation() {
    super(EquityIndexOptionSecurity.SECURITY_TYPE, EquityIndexOptionSecurity.class, EquityIndexOptionSecurityBean.class);
  }

  @Override
  public EquityIndexOptionSecurityBean createBean(OperationContext context, HibernateSecurityMasterDao secMasterSession, EquityIndexOptionSecurity security) {
    final EquityIndexOptionSecurityBean bean = new EquityIndexOptionSecurityBean();
    bean.setOptionExerciseType(OptionExerciseType.identify(security.getExerciseType()));
    bean.setOptionType(security.getOptionType());
    bean.setStrike(security.getStrike());
    bean.setExpiry(expiryToExpiryBean(security.getExpiry()));
    bean.setUnderlying(identifierToIdentifierBean(security.getUnderlyingIdentifier()));
    bean.setCurrency(secMasterSession.getOrCreateCurrencyBean(security.getCurrency().getCode()));
    bean.setExchange(secMasterSession.getOrCreateExchangeBean(security.getExchange(), ""));
    bean.setPointValue(security.getPointValue());
    return bean;
  }

  @Override
  public EquityIndexOptionSecurity createSecurity(OperationContext context, EquityIndexOptionSecurityBean bean) {
    final ExerciseType exerciseType = bean.getOptionExerciseType().accept(new ExerciseTypeVisitor<ExerciseType>() {

      @Override
      public ExerciseType visitAmericanExerciseType(AmericanExerciseType exerciseType) {
        return new AmericanExerciseType();
      }

      @Override
      public ExerciseType visitAsianExerciseType(AsianExerciseType exerciseType) {
        return new AsianExerciseType();
      }

      @Override
      public ExerciseType visitBermudanExerciseType(BermudanExerciseType exerciseType) {
        return new BermudanExerciseType();
      }

      @Override
      public ExerciseType visitEuropeanExerciseType(EuropeanExerciseType exerciseType) {
        return new EuropeanExerciseType();
      }
    });
    

    EquityIndexOptionSecurity sec = new EquityIndexOptionSecurity(bean.getOptionType(), 
        bean.getStrike(), 
        currencyBeanToCurrency(bean.getCurrency()), 
        identifierBeanToIdentifier(bean.getUnderlying()), 
        exerciseType, 
        expiryBeanToExpiry(bean.getExpiry()), 
        bean.getPointValue(), 
        bean.getExchange().getName());
    return sec;
  }

}