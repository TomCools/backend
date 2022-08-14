package life.catalogue.resources;

import life.catalogue.WsServerConfig;
import life.catalogue.api.exception.NotFoundException;
import life.catalogue.dw.jersey.MoreHttpHeaders;
import life.catalogue.dw.jersey.MoreMediaTypes;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;

@Path("/dataset/{key}/archive")
// there are many unofficial mime types around for zip
@Produces({
  MediaType.APPLICATION_OCTET_STREAM,
  MoreMediaTypes.APP_ZIP, MoreMediaTypes.APP_ZIP_ALT1, MoreMediaTypes.APP_ZIP_ALT2, MoreMediaTypes.APP_ZIP_ALT3
})
public class DatasetArchiveResource {
  private final WsServerConfig cfg;

  public DatasetArchiveResource(WsServerConfig cfg) {
    this.cfg = cfg;
  }

  @GET
  public Response archive(@PathParam("key") int key, @QueryParam("attempt") Integer attempt) {
    File source;
    if (attempt == null) {
      source = cfg.normalizer.lastestArchiveSymlink(key);
    } else {
      source = cfg.normalizer.archive(key, attempt);
    }
    if (!source.exists()) {
      throw new NotFoundException(key, "Archive for dataset " + key + " not found");
    }
    StreamingOutput stream = os -> {
      InputStream in = new FileInputStream(source);
      IOUtils.copy(in, os);
      os.flush();
    };

    return Response.ok(stream)
      .type(MoreMediaTypes.APP_ZIP)
      .header(MoreHttpHeaders.CONTENT_DISPOSITION, ResourceUtils.fileAttachment("dataset-" + key + ".zip"))
      .build();
  }

}
