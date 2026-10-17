package com.soundmarketplace.observability;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import javax.sql.DataSource;
import java.util.List;

@Configuration
@Slf4j
public class DataSourceProxyConfiguration implements BeanPostProcessor {

    private final boolean isTraceProfile;

    public DataSourceProxyConfiguration(Environment environment) {
        this.isTraceProfile = environment.acceptsProfiles(Profiles.of("trace"));
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource ds && !(bean instanceof net.ttddyy.dsproxy.support.ProxyDataSource)) {
            return ProxyDataSourceBuilder.create(ds)
                    .name("DS-Proxy")
                    .listener(new QueryExecutionListener() {
                        @Override
                        public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
                        }

                        @Override
                        public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
                            long elapsed = execInfo.getElapsedTime();
                            QueryCountHolder.record(elapsed);

                            if (isTraceProfile) {
                                for (QueryInfo query : queryInfoList) {
                                    log.debug("SQL: {} [{}ms]", query.getQuery(), elapsed);
                                }
                            }
                        }
                    })
                    .build();
        }
        return bean;
    }
}
