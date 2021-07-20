/**
 * #%L This file is part of "Apromore Enterprise Edition". %% Copyright (C) 2019 - 2021 Apromore Pty
 * Ltd. All Rights Reserved. %% NOTICE: All information contained herein is, and remains the
 * property of Apromore Pty Ltd and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to Apromore Pty Ltd and its suppliers and may be covered by U.S.
 * and Foreign Patents, patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material is strictly forbidden unless
 * prior written permission is obtained from Apromore Pty Ltd. #L%
 */
package org.apromore.portal.config;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@KeycloakConfiguration
@ConditionalOnProperty(prefix = "keycloak", name = "enabled", havingValue = "true")
public class PortalKeyCloakSecurity extends KeycloakWebSecurityConfigurerAdapter {
  /**
   * Registers the KeycloakAuthenticationProvider with the authentication manager.
   */
  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    auth.authenticationProvider(keycloakAuthenticationProvider());
  }

  /**
   * Defines the session authentication strategy.
   */
  @Bean
  @Override
  protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
    return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
  }

  @Bean
  public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
    return new ServletListenerRegistrationBean<HttpSessionEventPublisher>(
        new HttpSessionEventPublisher());
  }

  @Bean
  public KeycloakConfigResolver keycloakConfigResolver() {
    return new KeycloakSpringBootConfigResolver();
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    super.configure(http);

    http.headers().frameOptions().sameOrigin();
    http.csrf().ignoringAntMatchers("/zkau", "/zkau/**").and().authorizeRequests()
        // .antMatchers("/**").hasRole("USER")
        .antMatchers("*/css/**").permitAll().antMatchers("*/font/**").permitAll()
        .antMatchers("*/img/**").permitAll().antMatchers("*/themes/**").permitAll()
        .antMatchers("/**/*\\.svg").permitAll().antMatchers("*/libs/**").permitAll()
        .antMatchers("*/js/**").permitAll().antMatchers("/robots.txt").permitAll()
        .antMatchers("/sso/login").permitAll().antMatchers("/zkau").permitAll()
        .antMatchers("/zkau/web/bpmneditor/*").permitAll().anyRequest().authenticated();
  }
}