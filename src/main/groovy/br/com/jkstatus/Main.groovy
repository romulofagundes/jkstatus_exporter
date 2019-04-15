package br.com.jkstatus

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.yaml.snakeyaml.Yaml


class Main {
    static final String DEFAULT_PORT = "9573"
    static final String DEFAULT_CONFIG = "/etc/jkstatus_exporter/config.yml"

    static void main(String[] args) {
        def cliBuilder = new CliBuilder(
                usage: "jkstatus_exporter --port ${Main.DEFAULT_PORT} --config ${Main.DEFAULT_CONFIG}",
                header: "Options:",
                footer: "After running: http://<ip jkstatus exporter>:<port>/probe?target=<config in configure file>"
        )
        cliBuilder.width = 80
        cliBuilder.with {
            p longOpt: 'port', type: Integer, 'Listen port.', args: 1
            config type: File, 'Listen port.', args: 1
            h longOpt: 'help', 'Print this help and exit.'
        }
        OptionAccessor options = cliBuilder.parse(args)

        if (options.help) {
            cliBuilder.usage()
            System.exit 0
        }
        def port = Main.DEFAULT_PORT
        def config = Main.DEFAULT_CONFIG
        if (options.p) {
            port = options.p
        }
        if (options.config) {
            config = options.config
        }

        config = new File(config)
        if (!config.exists()) {
            System.err << "Configuration file not found in ${config.canonicalPath}"
            System.exit(1)
        }
        def keyMapConfig
        def yaml = new Yaml()
        try {
            keyMapConfig = yaml.load(config.text)
        } catch (e) {
            System.err << "Invalid YAML file ${config.canonicalPath}"
            System.exit(1)
        }

        Server server = new Server(port.toInteger())
        ServletContextHandler context = new ServletContextHandler()
        context.setContextPath("/")
        server.setHandler(context)

        context.addServlet(new ServletHolder(new JKStatusCollector(keyMapConfig)), "/probe")

        server.start()
        server.join()
    }
}

