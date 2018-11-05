package org.col.resources;

import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.col.api.model.Dataset;
import org.col.api.model.Page;
import org.col.api.model.ResultPage;
import org.col.api.search.DatasetSearchRequest;
import org.col.db.dao.DatasetDao;
import org.col.db.mapper.DatasetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/dataset")
@SuppressWarnings("static-method")
public class DatasetResource extends CRUDResource<Dataset> {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(DatasetResource.class);
  private final SqlSessionFactory factory;

  public DatasetResource(SqlSessionFactory factory) {
    super(Dataset.class, DatasetMapper.class);
    this.factory = factory;
  }

  @GET
  public ResultPage<Dataset> list(@Valid @BeanParam Page page, @BeanParam DatasetSearchRequest req,
                                  @Context SqlSession session) {
    return new DatasetDao(session).search(req, page);
  }

  @GET
  @Path("{key}/logo")
  public void logo(@PathParam("key") int key) {
    //TODO:::
  }
}
