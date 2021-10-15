package life.catalogue.exporter;

import life.catalogue.WsServerConfig;
import life.catalogue.api.model.ExportRequest;
import life.catalogue.api.vocab.DataFormat;
import life.catalogue.common.io.CompressionUtil;
import life.catalogue.common.io.UTF8IoUtils;
import life.catalogue.db.tree.DotPrinter;
import life.catalogue.db.tree.NewickPrinter;
import life.catalogue.db.tree.PrinterFactory;
import life.catalogue.img.ImageService;

import org.apache.commons.io.FileUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class DotExporter extends DatasetExporter {
  private static final Logger LOG = LoggerFactory.getLogger(DotExporter.class);
  private File f;

  public DotExporter(ExportRequest req, int userKey, SqlSessionFactory factory, WsServerConfig cfg, ImageService imageService) {
    super(req, userKey, DataFormat.DOT, false, factory, cfg, imageService);
  }

  @Override
  public void export() throws Exception {
    // do we have a full dataset export request?
    f = new File(tmpDir, "dataset-"+req.getDatasetKey()+".txt");
    try (Writer writer = UTF8IoUtils.writerFromFile(f)) {
      DotPrinter printer = PrinterFactory.dataset(DotPrinter.class, req.getDatasetKey(), req.getTaxonID(), req.isSynonyms(), req.getMinRank(), factory, writer);
      int cnt = printer.print();
      LOG.info("Written {} usages to DOT tree for dataset {}", cnt, req.getDatasetKey());
      counter.set(printer.getCounter());
    }
  }

}
