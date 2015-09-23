package vee.services.support;

import vee.comm.GlobalConstants;
import vee.comm.ServiceLoaderUtil;
import vee.services.IServiceBusService;
import vee.services.ServiceBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-10  <br/>
 */
public class RemoteServiceAutoDeployAndInjectListener implements ServletContextListener, GlobalConstants {

    private static final Logger log = LoggerFactory.getLogger( RemoteServiceAutoDeployAndInjectListener.class );

    private ServiceBus serviceBus;

    @Override
    public void contextInitialized( ServletContextEvent servletContextEvent ) {
        final ServletContext servletContext = servletContextEvent.getServletContext();
        final WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext( servletContext );
        final IServiceBusService serviceBusService = ServiceLoaderUtil.loadService( IServiceBusService.class );
        serviceBus = serviceBusService.loadServiceBus( GLOBAL_SHARED_SERVICE_GROUP );

        deployByShared();
        if ( null != springContext ) {
            deployByAnnotation( springContext );
            injectServices( springContext );
        }
    }

    private void deployByShared() {
        RemoteServiceSharedRegistry.bindServiceBus( serviceBus );
        RemoteServiceSharedRegistry.deployServices();
    }

    private void deployByAnnotation( ApplicationContext context ) {
        Map<String, Object> services = context.getBeansWithAnnotation( AutoDeploy.class );
        doDeployServices( services );
    }

    private void injectServices( ApplicationContext context ) {
        String[] beanNames = context.getBeanDefinitionNames();
        doInject( beanNames, context );
    }

    @SuppressWarnings( "unchecked" )
    private void doDeployServices( Map<String, Object> services ) {
        if ( null == services || services.isEmpty() ) {
            return;
        }
        for ( Map.Entry<String, Object> en : services.entrySet() ) {
            Class<?> serviceCls = en.getValue().getClass();
            AutoDeploy ad = serviceCls.getAnnotation( AutoDeploy.class );
            if ( null == ad ) {
                throw new IllegalStateException( " annotation: " + AutoDeploy.class.getName() + " lost for service: " + en.getKey() );
            } else {
                Class serviceInterface = ad.type();
                if ( null == serviceInterface ) {
                    throw new IllegalStateException( " service interface not specified for service: " + en.getKey() );
                }
                Object serviceInstance = en.getValue();
                if ( serviceInterface.isInstance( serviceInstance ) ) {
                    String serviceName = ad.name();
                    if ( null == serviceName || serviceName.isEmpty() ) {
                        serviceName = serviceInterface.getName();
                    }
                    serviceBus.deployService( serviceName, serviceInterface, serviceInstance );
                    log.info( "service '{}' deployed and serve for remote.", serviceName );
                } else {
                    throw new IllegalStateException( "service: " + en.getKey() + " is not a instance of '" + serviceInterface.getName() + '\'' );
                }
            }
        }
    }

    private void doInject( String[] beanNames, ApplicationContext context ) {
        for ( String name : beanNames ) {
            Object bean = context.getBean( name );
            Class<?> beanType = bean.getClass();
            Field[] fields = beanType.getDeclaredFields();
            try {
                injectFields( name, bean, beanType, fields );
            } catch ( IllegalAccessException e ) {
                log.info( "do injection failed for bean: {} ", name );
                throw new RuntimeException( e );
            }
        }
    }

    private void injectFields( String beanName, Object bean, Class<?> beanType, Field[] fields ) throws IllegalAccessException {
        for ( Field f : fields ) {
            Class<?> type = f.getType();
            FromRemote fromRemote = f.getAnnotation( FromRemote.class );
            if ( null != fromRemote ) {
                if ( type.isInterface() ) {
                    String serviceName = fromRemote.name();
                    if ( null == serviceName || serviceName.isEmpty() ) {
                        serviceName = type.getName();
                    }
                    f.setAccessible( true );
                    if ( null != f.get( bean ) ) {
                        log.warn( "inject error: inject twice or field( {} ) of bean( {} ) originally not null.", f.getName(), beanName );
                        continue;
                    }
                    Object service = serviceBus.loadService( serviceName, type );
                    f.set( bean, service );
                    log.info( "Injected remote service(name:{}, type:{}) for bean(name:{}, type:{}) on field(name:{}) ",
                            new Object[] {serviceName, type.getName(), beanName, beanType.getName(), f.getName()} );
                } else {
                    throw new IllegalStateException( '\'' + type.getName() + "' is not a interface, can't inject remote service." );
                }
            }
        }
    }

    @Override
    public void contextDestroyed( ServletContextEvent servletContextEvent ) {
        try {
            serviceBus.close();
        } catch ( Exception e ) {
            log.error( "close service bus error:", e );
        }
    }

}
