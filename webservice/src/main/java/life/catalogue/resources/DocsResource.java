package life.catalogue.resources;

import life.catalogue.WsServerConfig;
import life.catalogue.common.io.Resources;
import life.catalogue.common.ws.MoreMediaTypes;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import io.swagger.v3.oas.models.OpenAPI;

/**
 *
 */
@Path("/")
public class DocsResource {
  private final String version;
  private final OpenAPI openApi;

  public DocsResource(WsServerConfig cfg, OpenAPI openApi) {
    version = cfg.versionString();
    this.openApi = openApi;
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  public InputStream docs() throws IOException {
    return Resources.stream("swagger.html");
  }

  @GET
  @Path("/openapi")
  @Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.APP_YAML, MoreMediaTypes.TEXT_YAML})
  public OpenAPI openApi(@Context HttpHeaders headers, @Context UriInfo uriInfo) {
    return openApi;
  }

  @GET
  @Path("/version")
  @Produces(MediaType.TEXT_PLAIN)
  public String version() {
    return version;
  }

}
