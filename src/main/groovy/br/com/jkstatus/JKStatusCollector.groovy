package br.com.jkstatus

import br.com.jkstatus.prometheus.Counter
import br.com.jkstatus.prometheus.Gauge
import groovy.util.slurpersupport.GPathResult
import groovyx.net.http.RESTClient

import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import static groovyx.net.http.ContentType.*

class JKStatusCollector extends HttpServlet {
    def mapConfFile

    JKStatusCollector(mapConfFile) {
        this.mapConfFile = mapConfFile
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        def param = req.getParameter("target")
        Writer writer = resp.getWriter()
        if (!param || !this.mapConfFile[param]) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND)
            writer.println("Configuration not found, review your conf file.")
        } else {
            resp.setStatus(HttpServletResponse.SC_OK)
            resp.setContentType("text/plain")
            def confProps = this.mapConfFile[param]
            long tInit = new Date().time
            def jkstatusSite = new RESTClient(confProps.url)
            if (confProps.login && confProps.pass) {
                jkstatusSite.auth.basic confProps.login, confProps.pass
            }
            def response = jkstatusSite.get(path: confProps.context, query: [mime: 'xml'], contentType: TEXT,
                    headers: [Accept: 'application/xml'])
            def xml = new XmlSlurper().parseText(response.data.text)
            new Gauge("JKStatus Server Info", "jk_status_info").parse(writer, [
                    name      : xml.server.@name,
                    server    : xml.server.@port,
                    web_server: xml.software.@web_server,
                    version   : xml.software.@jk_version
            ])
            if(!xml?.balancers?.@count?.isEmpty()){
                renderBalancers(writer,xml)
            }

            long tEnd = new Date().time - tInit
            new Gauge("JKStatus Response Render/Status", "jk_status_response_render").parse(writer, [name: xml.server.@name], tEnd)
        }
        writer.flush()
        writer.close()
    }

    def renderBalancers(PrintWriter writer, GPathResult xml){
        new Counter("JKStatus Total Balancer", "jk_status_balancers_count").parse(writer, [:], xml.balancers.@count?.toFloat())
        xml?.balancers?.balancer?.each { bal ->
            new Gauge("JKStatus Balancer Info", "jk_status_balancers").parse(writer, [
                    name                : bal.@name,
                    type                : bal.@type,
                    sticky_session      : bal.@sticky_session.toBoolean() ? 1 : 0,
                    sticky_session_force: bal.@sticky_session_force.toBoolean() ? 1 : 0,
                    method              : bal.@method,
                    lock                : bal.@lock
            ], [
                    new Gauge.Value("retries", bal.@retries.toFloat()),
                    new Gauge.Value("recover_time", bal.@recover_time.toFloat()),
                    new Gauge.Value("member_count", bal.@member_count.toFloat()),
                    new Gauge.Value("good", bal.@good.toFloat()),
                    new Gauge.Value("degraded", bal.@degraded.toFloat()),
                    new Gauge.Value("bad", bal.@bad.toFloat()),
                    new Gauge.Value("busy", bal.@busy.toFloat()),
                    new Gauge.Value("max_busy", bal.@max_busy.toFloat()),
                    new Gauge.Value("map_count", bal.@map_count.toFloat()),
                    new Gauge.Value("time_to_maintenance_min", bal.@time_to_maintenance_min.toFloat()),
                    new Gauge.Value("time_to_maintenance_max", bal.@time_to_maintenance_max.toFloat()),
                    new Gauge.Value("last_reset_at", bal.@last_reset_at.toFloat()),
                    new Gauge.Value("last_reset_ago", bal.@last_reset_ago.toFloat()),
            ])
            bal?.member?.each { member ->
                new Gauge("JKStatus Member Info", "jk_status_balancer_member").parse(writer, [
                        balancer  : bal.@name,
                        name      : member.@name,
                        protocol  : member.@type,
                        port      : member.@port,
                        address   : member.@address,
                        source    : member.@source,
                        activation: member.@activation,
                        route     : member.@route,
                        redirect  : member.@redirect,
                        domain    : member.@domain,

                ], [
                        new Gauge.Value("connection_pool_timeout", member.@connection_pool_timeout.toFloat()),
                        new Gauge.Value("connect_timeout", member.@connect_timeout.toFloat()),
                        new Gauge.Value("ping_timeout", member.@ping_timeout.toFloat()),
                        new Gauge.Value("prepost_timeout", member.@prepost_timeout.toFloat()),
                        new Gauge.Value("reply_timeout", member.@reply_timeout.toFloat()),
                        new Gauge.Value("connection_ping_interval", member.@connection_ping_interval.toFloat()),
                        new Gauge.Value("retries", member.@retries.toFloat()),
                        new Gauge.Value("recovery_options", member.@recovery_options.toFloat()),
                        new Gauge.Value("busy_limit", member.@busy_limit.toFloat()),
                        new Gauge.Value("max_packet_size", member.@max_packet_size.toFloat()),
                        new Gauge.Value("lbfactor", member.@lbfactor.toFloat()),
                        new Gauge.Value("distance", member.@distance.toFloat()),
                        new Gauge.Value("lbmult", member.@lbmult.toFloat()),
                        new Gauge.Value("lbvalue", member.@lbvalue.toFloat()),
                        new Gauge.Value("elected", member.@elected.toFloat()),
                        new Gauge.Value("sessions", member.@sessions.toFloat()),
                        new Gauge.Value("errors", member.@errors.toFloat()),
                        new Gauge.Value("client_errors", member.@client_errors.toFloat()),
                        new Gauge.Value("reply_timeouts", member.@reply_timeouts.toFloat()),
                        new Gauge.Value("transferred", member.@transferred.toFloat()),
                        new Gauge.Value("read", member.@read.toFloat()),
                        new Gauge.Value("busy", member.@busy.toFloat()),
                        new Gauge.Value("max_busy", member.@max_busy.toFloat()),
                        new Gauge.Value("connected", member.@connected.toFloat()),
                        new Gauge.Value("max_connected", member.@max_connected.toFloat()),
                        new Gauge.Value("time_to_recover_min", member.@max_connected.toFloat()),
                        new Gauge.Value("time_to_recover_max", member.@max_connected.toFloat()),
                        new Gauge.Value("last_reset_at", member.@max_connected.toFloat()),
                        new Gauge.Value("last_reset_ago", member.@max_connected.toFloat()),
                ])
                def state = member.@state?.toString()?.toUpperCase()?.contains("OK") ? 1 : 0
                new Counter("JKStatus Member State", "jkstatus_state_member").parse(writer, [
                        balancer: bal.@name,
                        name    : member.@name,
                ], state.toFloat())
            }
        }
    }

}
