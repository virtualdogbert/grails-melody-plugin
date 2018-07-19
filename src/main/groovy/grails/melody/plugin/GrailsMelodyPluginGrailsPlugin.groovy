package grails.melody.plugin

import grails.plugins.Plugin
import net.bull.javamelody.JdbcWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.sql.DataSource

class GrailsMelodyPluginGrailsPlugin extends Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(GrailsMelodyPluginGrailsPlugin.class)

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = '3.2 > *'
    def loadAfter = ['spring-security-core', 'acegi', 'shiro', 'quartz']
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            'grails-app/views/*',
            'grails-app/init/*'
    ]

    def title = 'JavaMelody Grails Plugin'
    def description = 'Integrate JavaMelody monitoring into Grails application.'
    def documentation = 'http://grails.org/plugin/grails-melody'

    def license = 'ASL'
    def organization = [name: 'JavaMelody', url: 'https://github.com/javamelody/javamelody/wiki']

    def developers = [
            [name: 'Liu Chao', email: 'liuchao@goal98.com'],
            [name: 'Emeric Vernat', email: 'evernat@free.fr'],
            [name: 'Sérgio Michels', email: 'sergiomichelss@gmail.com']
    ]

    def issueManagement = [system: 'GitHub', url: 'https://github.com/javamelody/grails-melody-plugin/issues']
    def scm = [url: 'https://github.com/javamelody/grails-melody-plugin.git']

    void doWithDynamicMethods() {
        new MelodyInterceptorEnhancer().enhance(getGrailsApplication())
    }

    void doWithApplicationContext() {
        //Need to wrap the datasources here, because BeanPostProcessor didn't worked.
        def beans = getApplicationContext().getBeansOfType(DataSource)

        def processProxy = { beanName, bean ->
            LOG.debug "Wrapping DataSource - $beanName"
            bean.targetDataSource = JdbcWrapper.SINGLETON.createDataSourceProxy(bean.targetDataSource)
        }

        // Attempt lazy DataSources for Grails 3.2 and before
        boolean lazyProxied = false
        beans.each { beanName, bean ->
            if (beanName.contains('Lazy')) {
                processProxy(beanName, bean)
                lazyProxied = true
            }
        }

        // Attempt DataSources for Grails 3.3 and newer if no lazy proxy created
        if (!lazyProxied) {
            beans.each { beanName, bean ->
                if (beanName.equals('dataSource')) {
                    processProxy(beanName, bean)
                }
            }
        }
    }

    Closure doWithSpring() {
        { ->
            melodyConfig(MelodyConfig)
        }
    }
}
