package com.digitalsanctuary.spring.demo.user.profile.session;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import com.digitalsanctuary.spring.user.test.annotations.IntegrationTest;

/**
 * Guards against the session profile silently becoming a singleton.
 *
 * Spring's {@code @Scope} is not inherited, so a subclass of {@code BaseSessionProfile} annotated only with
 * {@code @Component} would be one instance shared by every HTTP session, leaking one user's profile to all
 * other users.
 */
@IntegrationTest
@DisplayName("DemoSessionProfile Scope Tests")
class DemoSessionProfileScopeTest {

    /** Bean name of the real instance behind the scoped proxy. */
    private static final String SCOPED_TARGET_BEAN_NAME = "scopedTarget.demoSessionProfile";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DemoSessionProfile demoSessionProfile;

    @Test
    @DisplayName("Bean definition is session scoped, not singleton")
    void beanDefinitionIsSessionScoped() {
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();

        assertThat(beanFactory.containsBeanDefinition(SCOPED_TARGET_BEAN_NAME))
                .as("DemoSessionProfile must be registered behind a scoped proxy (bean '%s')", SCOPED_TARGET_BEAN_NAME).isTrue();

        BeanDefinition targetDefinition = beanFactory.getBeanDefinition(SCOPED_TARGET_BEAN_NAME);
        assertThat(targetDefinition.getScope()).isEqualTo(WebApplicationContext.SCOPE_SESSION);
    }

    @Test
    @DisplayName("Injected reference is a scoped proxy, not the target instance")
    void injectedReferenceIsAScopedProxy() {
        assertThat(AopUtils.isAopProxy(demoSessionProfile)).as("injected DemoSessionProfile must be a scoped proxy").isTrue();
    }
}
