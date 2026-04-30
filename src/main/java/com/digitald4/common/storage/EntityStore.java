package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;

import com.digitald4.common.model.ModelObject;
import com.digitald4.common.util.Pair;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Set;

public interface EntityStore {
  /** Takes a Set of Pairs of EntityType and EntityId **/
  default ImmutableList<ModelObject<?>> getEntities(Set<Pair<String, Long>> entityTypeIds) {
    return getEntities(entityTypeIds.stream().collect(toImmutableSetMultimap(Pair::getLeft, Pair::getRight)));
  }

  ImmutableList<ModelObject<?>> getEntities(ImmutableSetMultimap<String, Long> entityIds);

  static String getEntityTypeId(ModelObject<?> obj) {
    return obj.getClass().getSimpleName() + "-" + obj.getId();
  }
}
