package org.col.db.mapper;

import org.apache.ibatis.annotations.Param;
import org.col.api.model.*;

import java.util.List;

/**
 *
 */
public interface NameMapper {

	int count(@Param("datasetKey") Integer datasetKey);

	List<Name> list(@Param("datasetKey") Integer datasetKey, @Param("page") Page page);

	Integer lookupKey(@Param("id") String id, @Param("datasetKey") int datasetKey);

	Name get(@Param("key") int key);

	Name getByTaxon(@Param("taxonKey") int taxonKey);

  /**
   * Creates a new name.
   * If the homotypic group key is not yet set the newly created name key will be
   * used to point to the name itself
   * @param name
   */
  void create(Name name);

	/**
	 * Lists all homotypic names based on the same homotypic name key
   *
	 * @param nameKey name key of the homotypic group
	 */
  List<Name> homotypicGroup(@Param("key") int nameKey);

  /**
   * Returns the list of names published in the same reference.
   */
  List<Name> listByReference(@Param("refKey") int publishedInKey);

}
