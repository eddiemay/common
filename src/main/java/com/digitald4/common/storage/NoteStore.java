package com.digitald4.common.storage;

import static com.digitald4.common.util.FormatText.toUpperCamel;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Streams.stream;
import static java.lang.Long.parseLong;

import com.digitald4.common.model.ModelObject;
import com.digitald4.common.model.Note;
import com.digitald4.common.storage.Transaction.Op;
import com.digitald4.common.util.Pair;
import com.google.common.collect.ImmutableMap;
import javax.inject.Inject;
import javax.inject.Provider;

public class NoteStore extends GenericLongStore<Note> {
  private final EntityStore entityStore;

  @Inject
  public NoteStore(Provider<DAO> daoProvider, EntityStore entityStore) {
    super(Note.class, daoProvider);
    this.entityStore = entityStore;
  }

  @Override
  protected Iterable<Op<Note>> preprocess(Iterable<Op<Note>> ops) {
    var notes = stream(ops).map(Op::getEntity)
        .peek(note -> note.setEntityType(toUpperCamel(note.getEntityType())))
        .collect(toImmutableList());
    ImmutableMap<String, String> entityNames = entityStore
        .getEntities(notes.stream()
            .map(n -> Pair.of(n.getEntityType(), parseLong(n.getEntityId())))
            .collect(toImmutableSet()))
        .stream().collect(toImmutableMap(EntityStore::getEntityTypeId, ModelObject::toString));

    notes.forEach(n -> n.setEntityName(entityNames.get(n.getEntityType() + "-" + n.getEntityId())));

    return ops;
  }
}
