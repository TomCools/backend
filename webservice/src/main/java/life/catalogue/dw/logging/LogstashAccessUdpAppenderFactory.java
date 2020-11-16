package life.catalogue.dw.logging;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import net.logstash.logback.appender.LogstashAccessUdpSocketAppender;
import net.logstash.logback.layout.LogstashAccessLayout;

/**
 * A console appender that logs the same MDC fields as the UDP logger,
 * but alos logs the basic http access fields using a LogstashAccessEncoder extended with the following fields:
 *
 *  - User-Agent header
 */
@JsonTypeName("logstash-access")
public class LogstashAccessUdpAppenderFactory extends LogstashAppenderFactory<IAccessEvent> {

  private int port;
  private String host;


  @JsonProperty
  public int getPort() {
    return port;
  }

  @JsonProperty
  public void setPort(int port) {
    this.port = port;
  }

  @JsonProperty
  public String getHost() {
    return host;
  }

  @JsonProperty
  public void setHost(String host) {
    this.host = host;
  }

  @Override
  Appender<IAccessEvent> buildAppender(LoggerContext context, String customJson) {
    LogstashAccessUdpSocketAppender appender = new LogstashAccessUdpSocketAppender();
    appender.setName("logstash-appender");
    appender.setHost(host);
    appender.setPort(port);

    LogstashAccessLayout layout = new LogstashAccessLayout();
    layout.setTimeZone("UTC");
    layout.setCustomFields(customJson);
    layout.setIncludeContext(false);
    // expose User-Agent header
    layout.addProvider(new UserAgentJsonProvider());
    layout.start();

    appender.setLayout(layout);
    return appender;
  }
}